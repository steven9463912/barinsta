package awais.instagrabber.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.db.entities.Account;
import awais.instagrabber.db.entities.Favorite;
import awais.instagrabber.db.repositories.AccountRepository;
import awais.instagrabber.db.repositories.FavoriteRepository;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.enums.FavoriteType;
import kotlinx.coroutines.Dispatchers;

import static awais.instagrabber.utils.Utils.settingsHelper;

//import awaisomereport.LogCollector.LogFile;
//import static awais.instagrabber.utils.Utils.logCollector;

public final class ExportImportUtils {
    private static final String TAG = "ExportImportUtils";

    public static final int FLAG_COOKIES = 1;
    public static final int FLAG_FAVORITES = 1 << 1;
    public static final int FLAG_SETTINGS = 1 << 2;

    public static void importData(@NonNull final Context context,
                                  @ExportImportFlags final int flags,
                                  @NonNull final Uri uri,
                                  final String password,
                                  final FetchListener<Boolean> fetchListener) throws PasswordUtils.IncorrectPasswordException {
        try (final InputStream stream = context.getContentResolver().openInputStream(uri)) {
            if (stream == null) return;
            final int configType = stream.read();
            final StringBuilder builder = new StringBuilder();
            int c;
            while ((c = stream.read()) != -1) {
                builder.append((char) c);
            }
            if (configType == 'A') {
                // password
                if (TextUtils.isEmpty(password)) return;
                try {
                    final byte[] passwordBytes = password.getBytes();
                    final byte[] bytes = new byte[32];
                    System.arraycopy(passwordBytes, 0, bytes, 0, Math.min(passwordBytes.length, 32));
                    importJson(context,
                               new String(PasswordUtils.dec(builder.toString(), bytes)),
                               flags,
                               fetchListener);
                } catch (final PasswordUtils.IncorrectPasswordException e) {
                    throw e;
                } catch (final Exception e) {
                    if (fetchListener != null) fetchListener.onResult(false);
                    if (BuildConfig.DEBUG) Log.e(TAG, "Error importing backup", e);
                }
            } else if (configType == 'Z') {
                importJson(context,
                           new String(Base64.decode(builder.toString(), Base64.DEFAULT | Base64.NO_PADDING | Base64.NO_WRAP)),
                           flags,
                           fetchListener);

            } else {
                Toast.makeText(context, "File is corrupted!", Toast.LENGTH_LONG).show();
                if (fetchListener != null) fetchListener.onResult(false);
            }
        } catch (PasswordUtils.IncorrectPasswordException e) {
            // separately handle incorrect password
            throw e;
        } catch (final Exception e) {
            if (fetchListener != null) fetchListener.onResult(false);
            if (BuildConfig.DEBUG) Log.e(TAG, "", e);
        }
    }

    private static void importJson(final Context context,
                                   @NonNull final String json,
                                   @ExportImportFlags final int flags,
                                   final FetchListener<Boolean> fetchListener) {
        try {
            final JSONObject jsonObject = new JSONObject(json);
            if ((flags & FLAG_SETTINGS) == FLAG_SETTINGS && jsonObject.has("settings")) {
                importSettings(jsonObject);
            }
            if ((flags & FLAG_COOKIES) == FLAG_COOKIES && jsonObject.has("cookies")) {
                importAccounts(context, jsonObject);
            }
            if ((flags & FLAG_FAVORITES) == FLAG_FAVORITES && jsonObject.has("favs")) {
                importFavorites(context, jsonObject);
            }
            if (fetchListener != null) fetchListener.onResult(true);
        } catch (final Exception e) {
            if (fetchListener != null) fetchListener.onResult(false);
            if (BuildConfig.DEBUG) Log.e(TAG, "", e);
        }
    }

    private static void importFavorites(final Context context, final JSONObject jsonObject) throws JSONException {
        final JSONArray favs = jsonObject.getJSONArray("favs");
        for (int i = 0; i < favs.length(); i++) {
            final JSONObject favsObject = favs.getJSONObject(i);
            final String queryText = favsObject.optString("q");
            if (TextUtils.isEmpty(queryText)) continue;
            final Pair<FavoriteType, String> favoriteTypeQueryPair;
            String query = null;
            FavoriteType favoriteType = null;
            if (queryText.contains("@")
                    || queryText.contains("#")
                    || queryText.contains("/")) {
                favoriteTypeQueryPair = Utils.migrateOldFavQuery(queryText);
                if (favoriteTypeQueryPair != null) {
                    query = favoriteTypeQueryPair.second;
                    favoriteType = favoriteTypeQueryPair.first;
                }
            } else {
                query = queryText;
                favoriteType = FavoriteType.valueOf(favsObject.optString("type"));
            }
            if (query == null || favoriteType == null) {
                continue;
            }
            final long epochMillis = favsObject.getLong("d");
            final Favorite favorite = new Favorite(
                    0,
                    query,
                    favoriteType,
                    favsObject.optString("s"),
                    favoriteType == FavoriteType.USER ? favsObject.optString("pic_url") : null,
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
            );
            // Log.d(TAG, "importJson: favoriteModel: " + favoriteModel);
            final FavoriteRepository favRepo = FavoriteRepository.Companion.getInstance(context);
            favRepo.getFavorite(
                    query,
                    favoriteType,
                    CoroutineUtilsKt.getContinuation((favorite1, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                        if (throwable != null) {
                            Log.e(TAG, "importFavorites: ", throwable);
                            return;
                        }
                        if (favorite1 == null) {
                            favRepo.insertOrUpdateFavorite(favorite, CoroutineUtilsKt.getContinuation((unit, throwable1) -> {}, Dispatchers.getIO()));
                        }
                        // local has priority since it's more frequently updated
                    }), Dispatchers.getIO())
            );
        }
    }

    private static void importAccounts(final Context context,
                                       final JSONObject jsonObject) {
        final List<Account> accounts = new ArrayList<>();
        try {
            final JSONArray cookies = jsonObject.getJSONArray("cookies");
            for (int i = 0; i < cookies.length(); i++) {
                final JSONObject cookieObject = cookies.getJSONObject(i);
                final Account account = new Account(
                        -1,
                        cookieObject.optString("i"),
                        cookieObject.optString("u"),
                        cookieObject.optString("c"),
                        cookieObject.optString("full_name"),
                        cookieObject.optString("profile_pic")
                );
                if (!account.isValid()) continue;
                accounts.add(account);
            }
        } catch (Exception e) {
            Log.e(TAG, "importAccounts: Error parsing json", e);
            return;
        }
        AccountRepository.Companion
                .getInstance(context)
                .insertOrUpdateAccounts(accounts, CoroutineUtilsKt.getContinuation((unit, throwable) -> {}, Dispatchers.getIO()));
    }

    private static void importSettings(final JSONObject jsonObject) {
        try {
            final JSONObject objSettings = jsonObject.getJSONObject("settings");
            final Iterator<String> keys = objSettings.keys();
            while (keys.hasNext()) {
                final String key = keys.next();
                final Object val = objSettings.opt(key);
                // Log.d(TAG, "importJson: key: " + key + ", val: " + val);
                if (val instanceof String) {
                    settingsHelper.putString(key, (String) val);
                } else if (val instanceof Integer) {
                    settingsHelper.putInteger(key, (int) val);
                } else if (val instanceof Boolean) {
                    settingsHelper.putBoolean(key, (boolean) val);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "importSettings error", e);
        }
    }

    public static boolean isEncrypted(@NonNull final Context context,
                                      @NonNull final Uri uri) {
        try (final InputStream stream = context.getContentResolver().openInputStream(uri)) {
            if (stream == null) return false;
            final int configType = stream.read();
            if (configType == 'A') {
                return true;
            }
        } catch (final Exception e) {
            Log.e(TAG, "isEncrypted", e);
        }
        return false;
    }

    public static void exportData(@NonNull final Context context,
                                  @ExportImportFlags final int flags,
                                  @NonNull final Uri uri,
                                  final String password,
                                  final FetchListener<Boolean> fetchListener) {
        getExportString(flags, context, exportString -> {
            if (TextUtils.isEmpty(exportString)) return;
            final boolean isPass = !TextUtils.isEmpty(password);
            byte[] exportBytes = null;
            if (isPass) {
                final byte[] passwordBytes = password.getBytes();
                final byte[] bytes = new byte[32];
                System.arraycopy(passwordBytes, 0, bytes, 0, Math.min(passwordBytes.length, 32));
                try {
                    exportBytes = PasswordUtils.enc(exportString, bytes);
                } catch (final Exception e) {
                    if (fetchListener != null) fetchListener.onResult(false);
                    if (BuildConfig.DEBUG) Log.e(TAG, "", e);
                }
            } else {
                exportBytes = Base64.encode(exportString.getBytes(), Base64.DEFAULT | Base64.NO_WRAP | Base64.NO_PADDING);
            }
            if (exportBytes != null && exportBytes.length > 1) {
                try (final OutputStream stream = context.getContentResolver().openOutputStream(uri)) {
                    if (stream == null) return;
                    stream.write(isPass ? 'A' : 'Z');
                    stream.write(exportBytes);
                    if (fetchListener != null) fetchListener.onResult(true);
                } catch (Exception e) {
                    if (fetchListener != null) fetchListener.onResult(false);
                    if (BuildConfig.DEBUG) Log.e(TAG, "", e);
                }
                return;
            }
            if (fetchListener != null) {
                fetchListener.onResult(false);
            }
        });

    }

    private static void getExportString(@ExportImportFlags final int flags,
                                        @NonNull final Context context,
                                        final OnExportStringCreatedCallback callback) {
        if (callback == null) return;
        try {
            final ImmutableList.Builder<ListenableFuture<?>> futures = ImmutableList.builder();
            futures.add((flags & FLAG_SETTINGS) == FLAG_SETTINGS
                        ? getSettings(context)
                        : Futures.immediateFuture(null));
            futures.add((flags & FLAG_COOKIES) == FLAG_COOKIES
                        ? getCookies(context)
                        : Futures.immediateFuture(null));
            futures.add((flags & FLAG_FAVORITES) == FLAG_FAVORITES
                        ? getFavorites(context)
                        : Futures.immediateFuture(null));
            //noinspection UnstableApiUsage
            final ListenableFuture<List<Object>> allFutures = Futures.allAsList(futures.build());
            Futures.addCallback(allFutures, new FutureCallback<List<Object>>() {
                @Override
                public void onSuccess(final List<Object> result) {
                    final JSONObject jsonObject = new JSONObject();
                    if (result == null) {
                        callback.onCreated(jsonObject.toString());
                        return;
                    }
                    try {
                        final JSONObject settings = (JSONObject) result.get(0);
                        if (settings != null) {
                            jsonObject.put("settings", settings);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "error getting settings: ", e);
                    }
                    try {
                        final JSONArray accounts = (JSONArray) result.get(1);
                        if (accounts != null) {
                            jsonObject.put("cookies", accounts);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "error getting accounts", e);
                    }
                    try {
                        final JSONArray favorites = (JSONArray) result.get(2);
                        if (favorites != null) {
                            jsonObject.put("favs", favorites);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "error getting favorites: ", e);
                    }
                    callback.onCreated(jsonObject.toString());
                }

                @Override
                public void onFailure(@NonNull final Throwable t) {
                    Log.e(TAG, "onFailure: ", t);
                    callback.onCreated(null);
                }
            }, AppExecutors.INSTANCE.getTasksThread());
            return;
        } catch (final Exception e) {
            //            if (logCollector != null) logCollector.appendException(e, LogFile.UTILS_EXPORT, "getExportString");
            if (BuildConfig.DEBUG) Log.e(TAG, "", e);
        }
        callback.onCreated(null);
    }

    @NonNull
    private static ListenableFuture<JSONObject> getSettings(@NonNull final Context context) {
        final SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        return AppExecutors.INSTANCE.getTasksThread().submit(() -> {
            final Map<String, ?> allPrefs = sharedPreferences.getAll();
            if (allPrefs == null) {
                return new JSONObject();
            }
            try {
                final JSONObject jsonObject = new JSONObject(allPrefs);
                jsonObject.remove(Constants.COOKIE);
                jsonObject.remove(Constants.DEVICE_UUID);
                jsonObject.remove(Constants.PREV_INSTALL_VERSION);
                jsonObject.remove(Constants.BROWSER_UA_CODE);
                jsonObject.remove(Constants.BROWSER_UA);
                jsonObject.remove(Constants.APP_UA_CODE);
                jsonObject.remove(Constants.APP_UA);
                return jsonObject;
            } catch (Exception e) {
                Log.e(TAG, "Error exporting settings", e);
            }
            return new JSONObject();
        });
    }

    private static ListenableFuture<JSONArray> getFavorites(final Context context) {
        final SettableFuture<JSONArray> future = SettableFuture.create();
        final FavoriteRepository favoriteRepository = FavoriteRepository.Companion.getInstance(context);
        favoriteRepository.getAllFavorites(
                CoroutineUtilsKt.getContinuation((favorites, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                    if (throwable != null) {
                        future.set(new JSONArray());
                        Log.e(TAG, "getFavorites: ", throwable);
                        return;
                    }
                    final JSONArray jsonArray = new JSONArray();
                    try {
                        for (final Favorite favorite : favorites) {
                            final JSONObject jsonObject = new JSONObject();
                            jsonObject.put("q", favorite.getQuery());
                            jsonObject.put("type", favorite.getType().toString());
                            jsonObject.put("s", favorite.getDisplayName());
                            jsonObject.put("pic_url", favorite.getPicUrl());
                            jsonObject.put("d", favorite.getDateAdded().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
                            jsonArray.put(jsonObject);
                        }
                    } catch (Exception e) {
                        if (BuildConfig.DEBUG) {
                            Log.e(TAG, "Error exporting favorites", e);
                        }
                    }
                    future.set(jsonArray);
                }), Dispatchers.getIO())
        );
        return future;
    }

    private static ListenableFuture<JSONArray> getCookies(final Context context) {
        final SettableFuture<JSONArray> future = SettableFuture.create();
        final AccountRepository accountRepository = AccountRepository.Companion.getInstance(context);
        accountRepository.getAllAccounts(
                CoroutineUtilsKt.getContinuation((accounts, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                    if (throwable != null) {
                        Log.e(TAG, "getCookies: ", throwable);
                        future.set(new JSONArray());
                        return;
                    }
                    final JSONArray jsonArray = new JSONArray();
                    try {
                        for (final Account cookie : accounts) {
                            final JSONObject jsonObject = new JSONObject();
                            jsonObject.put("i", cookie.getUid());
                            jsonObject.put("u", cookie.getUsername());
                            jsonObject.put("c", cookie.getCookie());
                            jsonObject.put("full_name", cookie.getFullName());
                            jsonObject.put("profile_pic", cookie.getProfilePic());
                            jsonArray.put(jsonObject);
                        }
                    } catch (Exception e) {
                        if (BuildConfig.DEBUG) {
                            Log.e(TAG, "Error exporting accounts", e);
                        }
                    }
                    future.set(jsonArray);
                }), Dispatchers.getIO())
        );
        return future;
    }

    @IntDef(value = {FLAG_COOKIES, FLAG_FAVORITES, FLAG_SETTINGS}, flag = true)
    @interface ExportImportFlags {}

    public interface OnExportStringCreatedCallback {
        void onCreated(String exportString);
    }
}