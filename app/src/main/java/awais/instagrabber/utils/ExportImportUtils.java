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

    public static void importData(@NonNull Context context,
                                  @ExportImportFlags int flags,
                                  @NonNull Uri uri,
                                  String password,
                                  FetchListener<Boolean> fetchListener) throws PasswordUtils.IncorrectPasswordException {
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            if (stream == null) return;
            int configType = stream.read();
            StringBuilder builder = new StringBuilder();
            int c;
            while ((c = stream.read()) != -1) {
                builder.append((char) c);
            }
            if (configType == 'A') {
                // password
                if (TextUtils.isEmpty(password)) return;
                try {
                    byte[] passwordBytes = password.getBytes();
                    byte[] bytes = new byte[32];
                    System.arraycopy(passwordBytes, 0, bytes, 0, Math.min(passwordBytes.length, 32));
                    ExportImportUtils.importJson(context,
                               new String(PasswordUtils.dec(builder.toString(), bytes)),
                               flags,
                               fetchListener);
                } catch (PasswordUtils.IncorrectPasswordException e) {
                    throw e;
                } catch (Exception e) {
                    if (fetchListener != null) fetchListener.onResult(false);
                    if (BuildConfig.DEBUG) Log.e(ExportImportUtils.TAG, "Error importing backup", e);
                }
            } else if (configType == 'Z') {
                ExportImportUtils.importJson(context,
                           new String(Base64.decode(builder.toString(), Base64.DEFAULT | Base64.NO_PADDING | Base64.NO_WRAP)),
                           flags,
                           fetchListener);

            } else {
                Toast.makeText(context, "File is corrupted!", Toast.LENGTH_LONG).show();
                if (fetchListener != null) fetchListener.onResult(false);
            }
        } catch (final PasswordUtils.IncorrectPasswordException e) {
            // separately handle incorrect password
            throw e;
        } catch (Exception e) {
            if (fetchListener != null) fetchListener.onResult(false);
            if (BuildConfig.DEBUG) Log.e(ExportImportUtils.TAG, "", e);
        }
    }

    private static void importJson(Context context,
                                   @NonNull String json,
                                   @ExportImportFlags int flags,
                                   FetchListener<Boolean> fetchListener) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            if ((flags & ExportImportUtils.FLAG_SETTINGS) == ExportImportUtils.FLAG_SETTINGS && jsonObject.has("settings")) {
                ExportImportUtils.importSettings(jsonObject);
            }
            if ((flags & ExportImportUtils.FLAG_COOKIES) == ExportImportUtils.FLAG_COOKIES && jsonObject.has("cookies")) {
                ExportImportUtils.importAccounts(context, jsonObject);
            }
            if ((flags & ExportImportUtils.FLAG_FAVORITES) == ExportImportUtils.FLAG_FAVORITES && jsonObject.has("favs")) {
                ExportImportUtils.importFavorites(context, jsonObject);
            }
            if (fetchListener != null) fetchListener.onResult(true);
        } catch (Exception e) {
            if (fetchListener != null) fetchListener.onResult(false);
            if (BuildConfig.DEBUG) Log.e(ExportImportUtils.TAG, "", e);
        }
    }

    private static void importFavorites(Context context, JSONObject jsonObject) throws JSONException {
        JSONArray favs = jsonObject.getJSONArray("favs");
        for (int i = 0; i < favs.length(); i++) {
            JSONObject favsObject = favs.getJSONObject(i);
            String queryText = favsObject.optString("q");
            if (TextUtils.isEmpty(queryText)) continue;
            Pair<FavoriteType, String> favoriteTypeQueryPair;
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
            long epochMillis = favsObject.getLong("d");
            Favorite favorite = new Favorite(
                    0,
                    query,
                    favoriteType,
                    favsObject.optString("s"),
                    favoriteType == FavoriteType.USER ? favsObject.optString("pic_url") : null,
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
            );
            // Log.d(TAG, "importJson: favoriteModel: " + favoriteModel);
            FavoriteRepository favRepo = FavoriteRepository.Companion.getInstance(context);
            favRepo.getFavorite(
                    query,
                    favoriteType,
                    CoroutineUtilsKt.getContinuation((favorite1, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                        if (throwable != null) {
                            Log.e(ExportImportUtils.TAG, "importFavorites: ", throwable);
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

    private static void importAccounts(Context context,
                                       JSONObject jsonObject) {
        List<Account> accounts = new ArrayList<>();
        try {
            JSONArray cookies = jsonObject.getJSONArray("cookies");
            for (int i = 0; i < cookies.length(); i++) {
                JSONObject cookieObject = cookies.getJSONObject(i);
                Account account = new Account(
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
        } catch (final Exception e) {
            Log.e(ExportImportUtils.TAG, "importAccounts: Error parsing json", e);
            return;
        }
        AccountRepository.Companion
                .getInstance(context)
                .insertOrUpdateAccounts(accounts, CoroutineUtilsKt.getContinuation((unit, throwable) -> {}, Dispatchers.getIO()));
    }

    private static void importSettings(JSONObject jsonObject) {
        try {
            JSONObject objSettings = jsonObject.getJSONObject("settings");
            Iterator<String> keys = objSettings.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object val = objSettings.opt(key);
                // Log.d(TAG, "importJson: key: " + key + ", val: " + val);
                if (val instanceof String) {
                    settingsHelper.putString(key, (String) val);
                } else if (val instanceof Integer) {
                    settingsHelper.putInteger(key, (int) val);
                } else if (val instanceof Boolean) {
                    settingsHelper.putBoolean(key, (boolean) val);
                }
            }
        } catch (final Exception e) {
            Log.e(ExportImportUtils.TAG, "importSettings error", e);
        }
    }

    public static boolean isEncrypted(@NonNull Context context,
                                      @NonNull Uri uri) {
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            if (stream == null) return false;
            int configType = stream.read();
            if (configType == 'A') {
                return true;
            }
        } catch (Exception e) {
            Log.e(ExportImportUtils.TAG, "isEncrypted", e);
        }
        return false;
    }

    public static void exportData(@NonNull Context context,
                                  @ExportImportFlags int flags,
                                  @NonNull Uri uri,
                                  String password,
                                  FetchListener<Boolean> fetchListener) {
        ExportImportUtils.getExportString(flags, context, exportString -> {
            if (TextUtils.isEmpty(exportString)) return;
            boolean isPass = !TextUtils.isEmpty(password);
            byte[] exportBytes = null;
            if (isPass) {
                byte[] passwordBytes = password.getBytes();
                byte[] bytes = new byte[32];
                System.arraycopy(passwordBytes, 0, bytes, 0, Math.min(passwordBytes.length, 32));
                try {
                    exportBytes = PasswordUtils.enc(exportString, bytes);
                } catch (Exception e) {
                    if (fetchListener != null) fetchListener.onResult(false);
                    if (BuildConfig.DEBUG) Log.e(ExportImportUtils.TAG, "", e);
                }
            } else {
                exportBytes = Base64.encode(exportString.getBytes(), Base64.DEFAULT | Base64.NO_WRAP | Base64.NO_PADDING);
            }
            if (exportBytes != null && exportBytes.length > 1) {
                try (OutputStream stream = context.getContentResolver().openOutputStream(uri)) {
                    if (stream == null) return;
                    stream.write(isPass ? 'A' : 'Z');
                    stream.write(exportBytes);
                    if (fetchListener != null) fetchListener.onResult(true);
                } catch (final Exception e) {
                    if (fetchListener != null) fetchListener.onResult(false);
                    if (BuildConfig.DEBUG) Log.e(ExportImportUtils.TAG, "", e);
                }
                return;
            }
            if (fetchListener != null) {
                fetchListener.onResult(false);
            }
        });

    }

    private static void getExportString(@ExportImportFlags int flags,
                                        @NonNull Context context,
                                        OnExportStringCreatedCallback callback) {
        if (callback == null) return;
        try {
            ImmutableList.Builder<ListenableFuture<?>> futures = ImmutableList.builder();
            futures.add((flags & ExportImportUtils.FLAG_SETTINGS) == ExportImportUtils.FLAG_SETTINGS
                        ? ExportImportUtils.getSettings(context)
                        : Futures.immediateFuture(null));
            futures.add((flags & ExportImportUtils.FLAG_COOKIES) == ExportImportUtils.FLAG_COOKIES
                        ? ExportImportUtils.getCookies(context)
                        : Futures.immediateFuture(null));
            futures.add((flags & ExportImportUtils.FLAG_FAVORITES) == ExportImportUtils.FLAG_FAVORITES
                        ? ExportImportUtils.getFavorites(context)
                        : Futures.immediateFuture(null));
            //noinspection UnstableApiUsage
            ListenableFuture<List<Object>> allFutures = Futures.allAsList(futures.build());
            Futures.addCallback(allFutures, new FutureCallback<List<Object>>() {
                @Override
                public void onSuccess(List<Object> result) {
                    JSONObject jsonObject = new JSONObject();
                    if (result == null) {
                        callback.onCreated(jsonObject.toString());
                        return;
                    }
                    try {
                        JSONObject settings = (JSONObject) result.get(0);
                        if (settings != null) {
                            jsonObject.put("settings", settings);
                        }
                    } catch (final Exception e) {
                        Log.e(ExportImportUtils.TAG, "error getting settings: ", e);
                    }
                    try {
                        JSONArray accounts = (JSONArray) result.get(1);
                        if (accounts != null) {
                            jsonObject.put("cookies", accounts);
                        }
                    } catch (final Exception e) {
                        Log.e(ExportImportUtils.TAG, "error getting accounts", e);
                    }
                    try {
                        JSONArray favorites = (JSONArray) result.get(2);
                        if (favorites != null) {
                            jsonObject.put("favs", favorites);
                        }
                    } catch (final Exception e) {
                        Log.e(ExportImportUtils.TAG, "error getting favorites: ", e);
                    }
                    callback.onCreated(jsonObject.toString());
                }

                @Override
                public void onFailure(@NonNull Throwable t) {
                    Log.e(ExportImportUtils.TAG, "onFailure: ", t);
                    callback.onCreated(null);
                }
            }, AppExecutors.INSTANCE.getTasksThread());
            return;
        } catch (Exception e) {
            //            if (logCollector != null) logCollector.appendException(e, LogFile.UTILS_EXPORT, "getExportString");
            if (BuildConfig.DEBUG) Log.e(ExportImportUtils.TAG, "", e);
        }
        callback.onCreated(null);
    }

    @NonNull
    private static ListenableFuture<JSONObject> getSettings(@NonNull Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        return AppExecutors.INSTANCE.getTasksThread().submit(() -> {
            Map<String, ?> allPrefs = sharedPreferences.getAll();
            if (allPrefs == null) {
                return new JSONObject();
            }
            try {
                JSONObject jsonObject = new JSONObject(allPrefs);
                jsonObject.remove(Constants.COOKIE);
                jsonObject.remove(Constants.DEVICE_UUID);
                jsonObject.remove(Constants.PREV_INSTALL_VERSION);
                jsonObject.remove(Constants.BROWSER_UA_CODE);
                jsonObject.remove(Constants.BROWSER_UA);
                jsonObject.remove(Constants.APP_UA_CODE);
                jsonObject.remove(Constants.APP_UA);
                return jsonObject;
            } catch (final Exception e) {
                Log.e(ExportImportUtils.TAG, "Error exporting settings", e);
            }
            return new JSONObject();
        });
    }

    private static ListenableFuture<JSONArray> getFavorites(Context context) {
        SettableFuture<JSONArray> future = SettableFuture.create();
        FavoriteRepository favoriteRepository = FavoriteRepository.Companion.getInstance(context);
        favoriteRepository.getAllFavorites(
                CoroutineUtilsKt.getContinuation((favorites, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                    if (throwable != null) {
                        future.set(new JSONArray());
                        Log.e(ExportImportUtils.TAG, "getFavorites: ", throwable);
                        return;
                    }
                    JSONArray jsonArray = new JSONArray();
                    try {
                        for (Favorite favorite : favorites) {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("q", favorite.getQuery());
                            jsonObject.put("type", favorite.getType().toString());
                            jsonObject.put("s", favorite.getDisplayName());
                            jsonObject.put("pic_url", favorite.getPicUrl());
                            jsonObject.put("d", favorite.getDateAdded().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
                            jsonArray.put(jsonObject);
                        }
                    } catch (final Exception e) {
                        if (BuildConfig.DEBUG) {
                            Log.e(ExportImportUtils.TAG, "Error exporting favorites", e);
                        }
                    }
                    future.set(jsonArray);
                }), Dispatchers.getIO())
        );
        return future;
    }

    private static ListenableFuture<JSONArray> getCookies(Context context) {
        SettableFuture<JSONArray> future = SettableFuture.create();
        AccountRepository accountRepository = AccountRepository.Companion.getInstance(context);
        accountRepository.getAllAccounts(
                CoroutineUtilsKt.getContinuation((accounts, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                    if (throwable != null) {
                        Log.e(ExportImportUtils.TAG, "getCookies: ", throwable);
                        future.set(new JSONArray());
                        return;
                    }
                    JSONArray jsonArray = new JSONArray();
                    try {
                        for (Account cookie : accounts) {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("i", cookie.getUid());
                            jsonObject.put("u", cookie.getUsername());
                            jsonObject.put("c", cookie.getCookie());
                            jsonObject.put("full_name", cookie.getFullName());
                            jsonObject.put("profile_pic", cookie.getProfilePic());
                            jsonArray.put(jsonObject);
                        }
                    } catch (final Exception e) {
                        if (BuildConfig.DEBUG) {
                            Log.e(ExportImportUtils.TAG, "Error exporting accounts", e);
                        }
                    }
                    future.set(jsonArray);
                }), Dispatchers.getIO())
        );
        return future;
    }

    @IntDef(value = {ExportImportUtils.FLAG_COOKIES, ExportImportUtils.FLAG_FAVORITES, ExportImportUtils.FLAG_SETTINGS}, flag = true)
    @interface ExportImportFlags {}

    public interface OnExportStringCreatedCallback {
        void onCreated(String exportString);
    }
}