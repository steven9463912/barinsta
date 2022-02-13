package awais.instagrabber.utils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.provider.Browser;
import android.provider.DocumentsContract;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import awais.instagrabber.R;
import awais.instagrabber.models.PostsLayoutPreferences;
import awais.instagrabber.models.enums.FavoriteType;

public final class Utils {
    private static final String TAG = "Utils";
    private static final int VIDEO_CACHE_MAX_BYTES = 10 * 1024 * 1024;

    // public static LogCollector logCollector;
    public static SettingsHelper settingsHelper;
    public static boolean sessionVolumeFull;
    public static final MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
    public static final DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
    public static ClipboardManager clipboardManager;
    public static SimpleCache simpleCache;
    private static int statusBarHeight;
    private static int actionBarHeight;
    public static String cacheDir;
    private static int defaultStatusBarColor;
    private static Object[] volumes;

    public static int convertDpToPx(float dp) {
        return Math.round((dp * Utils.displayMetrics.densityDpi) / DisplayMetrics.DENSITY_DEFAULT);
    }

    public static void copyText(@NonNull Context context, CharSequence string) {
        if (Utils.clipboardManager == null) {
            Utils.clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        }
        int toastMessage = R.string.clipboard_error;
        if (Utils.clipboardManager != null) {
            try {
                Utils.clipboardManager.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.app_name), string));
                toastMessage = R.string.clipboard_copied;
            } catch (final Exception e) {
                Log.e(Utils.TAG, "copyText: ", e);
            }
        }
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show();
    }

    public static Map<String, String> sign(Map<String, Object> form) {
        // final String signed = sign(Constants.SIGNATURE_KEY, new JSONObject(form).toString());
        // if (signed == null) {
        //     return null;
        // }
        Map<String, String> map = new HashMap<>();
        // map.put("ig_sig_key_version", Constants.SIGNATURE_VERSION);
        // map.put("signed_body", signed);
        map.put("signed_body", "SIGNATURE." + new JSONObject(form));
        return map;
    }

    // public static String sign(final String key, final String message) {
    //     try {
    //         final Mac hasher = Mac.getInstance("HmacSHA256");
    //         hasher.init(new SecretKeySpec(key.getBytes(), "HmacSHA256"));
    //         byte[] hash = hasher.doFinal(message.getBytes());
    //         final StringBuilder hexString = new StringBuilder();
    //         for (byte b : hash) {
    //             final String hex = Integer.toHexString(0xff & b);
    //             if (hex.length() == 1) hexString.append('0');
    //             hexString.append(hex);
    //         }
    //         return hexString.toString() + "." + message;
    //     } catch (Exception e) {
    //         Log.e(TAG, "Error signing", e);
    //         return null;
    //     }
    // }

    public static String getMimeType(@NonNull Uri uri, ContentResolver contentResolver) {
        final String mimeType;
        String scheme = uri.getScheme();
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
        if (TextUtils.isEmpty(scheme)) {
            mimeType = Utils.mimeTypeMap.getMimeTypeFromExtension(fileExtension.toLowerCase());
        } else {
            if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
                mimeType = contentResolver.getType(uri);
            } else {
                mimeType = Utils.mimeTypeMap.getMimeTypeFromExtension(fileExtension.toLowerCase());
            }
        }
        if (mimeType == null) return null;
        return mimeType.toLowerCase();
    }

    public static SimpleCache getSimpleCacheInstance(Context context) {
        if (context == null) {
            return null;
        }
        ExoDatabaseProvider exoDatabaseProvider = new ExoDatabaseProvider(context);
        File cacheDir = context.getCacheDir();
        if (Utils.simpleCache == null && cacheDir != null) {
            Utils.simpleCache = new SimpleCache(cacheDir, new LeastRecentlyUsedCacheEvictor(Utils.VIDEO_CACHE_MAX_BYTES), exoDatabaseProvider);
        }
        return Utils.simpleCache;
    }

    @Nullable
    public static Pair<FavoriteType, String> migrateOldFavQuery(String queryText) {
        if (queryText.startsWith("@")) {
            return new Pair<>(FavoriteType.USER, queryText.substring(1));
        } else if (queryText.contains("/")) {
            return new Pair<>(FavoriteType.LOCATION, queryText.substring(0, queryText.indexOf("/")));
        } else if (queryText.startsWith("#")) {
            return new Pair<>(FavoriteType.HASHTAG, queryText.substring(1));
        }
        return null;
    }

    public static int getStatusBarHeight(Context context) {
        if (Utils.statusBarHeight > 0) {
            return Utils.statusBarHeight;
        }
        final int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            Utils.statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
        }
        return Utils.statusBarHeight;
    }

    public static int getActionBarHeight(@NonNull Context context) {
        if (Utils.actionBarHeight > 0) {
            return Utils.actionBarHeight;
        }
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            Utils.actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, Utils.displayMetrics);
        }
        return Utils.actionBarHeight;
    }

    public static void openURL(Context context, String url) {
        if (context == null || TextUtils.isEmpty(url)) {
            return;
        }
        try {
            String url1 = url;
            // add http:// if string doesn't have http:// or https://
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url1 = "http://" + url;
            }
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url1));
            i.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
            i.putExtra(Browser.EXTRA_CREATE_NEW_TAB, true);
            context.startActivity(i);
        } catch (final ActivityNotFoundException e) {
            Log.e(Utils.TAG, "openURL: No activity found to handle URLs", e);
            Toast.makeText(context, context.getString(R.string.no_external_app_url), Toast.LENGTH_LONG).show();
        } catch (final Exception e) {
            Log.e(Utils.TAG, "openURL", e);
        }
    }

    public static void openEmailAddress(Context context, String emailAddress) {
        if (context == null || TextUtils.isEmpty(emailAddress)) {
            return;
        }
        final Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + emailAddress));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "");
        context.startActivity(emailIntent);
    }

    public static void displayToastAboveView(@NonNull Context context,
                                             @NonNull View view,
                                             @NonNull String text) {
        Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.START,
                         view.getLeft(),
                         view.getTop());
        toast.show();
    }

    public static PostsLayoutPreferences getPostsLayoutPreferences(String layoutPreferenceKey) {
        PostsLayoutPreferences layoutPreferences = PostsLayoutPreferences.fromJson(Utils.settingsHelper.getString(layoutPreferenceKey));
        if (layoutPreferences == null) {
            layoutPreferences = PostsLayoutPreferences.builder().build();
            Utils.settingsHelper.putString(layoutPreferenceKey, layoutPreferences.getJson());
        }
        return layoutPreferences;
    }

    private static Field mAttachInfoField;
    private static Field mStableInsetsField;

    public static int getViewInset(final View view) {
        if (view == null
                || view.getHeight() == Utils.displayMetrics.heightPixels
                || view.getHeight() == Utils.displayMetrics.widthPixels - Utils.getStatusBarHeight(view.getContext())) {
            return 0;
        }
        try {
            if (Utils.mAttachInfoField == null) {
                //noinspection JavaReflectionMemberAccess
                Utils.mAttachInfoField = View.class.getDeclaredField("mAttachInfo");
                Utils.mAttachInfoField.setAccessible(true);
            }
            final Object mAttachInfo = Utils.mAttachInfoField.get(view);
            if (mAttachInfo != null) {
                if (Utils.mStableInsetsField == null) {
                    Utils.mStableInsetsField = mAttachInfo.getClass().getDeclaredField("mStableInsets");
                    Utils.mStableInsetsField.setAccessible(true);
                }
                final Rect insets = (Rect) Utils.mStableInsetsField.get(mAttachInfo);
                if (insets == null) {
                    return 0;
                }
                return insets.bottom;
            }
        } catch (final Exception e) {
            Log.e(Utils.TAG, "getViewInset", e);
        }
        return 0;
    }

    public static int getThemeAccentColor(final Context context) {
        final int colorAttr;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            colorAttr = android.R.attr.colorAccent;
        } else {
            //Get colorAccent defined for AppCompat
            colorAttr = context.getResources().getIdentifier("colorAccent", "attr", context.getPackageName());
        }
        final TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(colorAttr, outValue, true);
        return outValue.data;
    }

    public static int getAttrValue(@NonNull Context context, int attr) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, outValue, true);
        return outValue.data;
    }

    public static int getAttrResId(@NonNull Context context, int attr) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, outValue, true);
        return outValue.resourceId;
    }

    public static void transparentStatusBar(Activity activity,
                                            boolean enable,
                                            boolean fullscreen) {
        if (activity == null) return;
        ActionBar actionBar = ((AppCompatActivity) activity).getSupportActionBar();
        Window window = activity.getWindow();
        View decorView = window.getDecorView();
        if (enable) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            if (actionBar != null) {
                actionBar.hide();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Utils.defaultStatusBarColor = window.getStatusBarColor();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                // FOR TRANSPARENT NAVIGATION BAR
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                window.setStatusBarColor(Color.TRANSPARENT);
                Log.d(Utils.TAG, "Setting Color Transparent " + Color.TRANSPARENT + " Default Color " + Utils.defaultStatusBarColor);
                return;
            }
            Log.d(Utils.TAG, "Setting Color Trans " + Color.TRANSPARENT);
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            return;
        }
        if (fullscreen) {
            final int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
            return;
        }
        if (actionBar != null) {
            actionBar.show();
        }
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            window.setStatusBarColor(Utils.defaultStatusBarColor);
            return;
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    }

    // public static void mediaScanFile(@NonNull final Context context,
    //                                  @NonNull File file,
    //                                  @NonNull final OnScanCompletedListener callback) {
    //     //noinspection UnstableApiUsage
    //     final String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(Files.getFileExtension(file.getName()));
    //     MediaScannerConnection.scanFile(
    //             context,
    //             new String[]{file.getAbsolutePath()},
    //             new String[]{mimeType},
    //             callback
    //     );
    // }

    public static void showKeyboard(@NonNull View view) {
        try {
            Context context = view.getContext();
            if (context == null) return;
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm == null) return;
            view.requestFocus();
            boolean shown = imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            if (!shown) {
                Log.e(Utils.TAG, "showKeyboard: System did not display the keyboard");
            }
        } catch (final Exception e) {
            Log.e(Utils.TAG, "showKeyboard: ", e);
        }
    }

    public static void hideKeyboard(View view) {
        if (view == null) return;
        Context context = view.getContext();
        if (context == null) return;
        try {
            InputMethodManager manager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (manager == null) return;
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        } catch (final Exception e) {
            Log.e(Utils.TAG, "hideKeyboard: ", e);
        }
    }

    public static Drawable getAnimatableDrawable(@NonNull Context context,
                                                 @DrawableRes int drawableResId) {
        Drawable drawable;
        if (Build.VERSION.SDK_INT >= 24) {
            drawable = ContextCompat.getDrawable(context, drawableResId);
        } else {
            drawable = AnimatedVectorDrawableCompat.create(context, drawableResId);
        }
        return drawable;
    }

    public static void enabledKeepScreenOn(@NonNull Activity activity) {
        Window window = activity.getWindow();
        if (window == null) return;
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public static void disableKeepScreenOn(@NonNull Activity activity) {
        Window window = activity.getWindow();
        if (window == null) return;
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public static <T> void moveItem(final int sourceIndex, final int targetIndex, final List<T> list) {
        if (sourceIndex <= targetIndex) {
            Collections.rotate(list.subList(sourceIndex, targetIndex + 1), -1);
        } else {
            Collections.rotate(list.subList(targetIndex, sourceIndex + 1), 1);
        }
    }

    // public static void scanDocumentFile(@NonNull final Context context,
    //                                     @NonNull final DocumentFile documentFile,
    //                                     @NonNull final OnScanCompletedListener callback) {
    //     if (!documentFile.isFile() || !documentFile.exists()) {
    //         Log.d(TAG, "scanDocumentFile: " + documentFile);
    //         callback.onScanCompleted(null, null);
    //         return;
    //     }
    //     File file = null;
    //     try {
    //         file = getDocumentFileRealPath(context, documentFile);
    //     } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
    //         Log.e(TAG, "scanDocumentFile: ", e);
    //     }
    //     if (file == null) return;
    //     MediaScannerConnection.scanFile(context,
    //                                     new String[]{file.getAbsolutePath()},
    //                                     new String[]{documentFile.getType()},
    //                                     callback);
    // }

    public static File getDocumentFileRealPath(@NonNull Context context,
                                               @NonNull DocumentFile documentFile)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String docId = DocumentsContract.getDocumentId(documentFile.getUri());
        String[] split = docId.split(":");
        String type = split[0];

        if (type.equalsIgnoreCase("primary")) {
            return new File(Environment.getExternalStorageDirectory(), split[1]);
        } else if (type.equalsIgnoreCase("raw")) {
            return new File(split[1]);
        } else {
            if (Utils.volumes == null) {
                StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
                if (sm == null) return null;
                Method getVolumeListMethod = sm.getClass().getMethod("getVolumeList");
                Utils.volumes = (Object[]) getVolumeListMethod.invoke(sm);
            }
            if (Utils.volumes == null) return null;
            for (final Object volume : Utils.volumes) {
                Method getUuidMethod = volume.getClass().getMethod("getUuid");
                String uuid = (String) getUuidMethod.invoke(volume);

                if (type.equalsIgnoreCase(uuid)) {
                    Method getPathMethod = volume.getClass().getMethod("getPath");
                    String path = (String) getPathMethod.invoke(volume);
                    return new File(path, split[1]);
                }
            }
        }

        return null;
    }

    public static void setupSelectedDir(@NonNull Context context,
                                        @NonNull Intent intent) throws DownloadUtils.ReselectDocumentTreeException {
        Uri dirUri = intent.getData();
        Log.d(Utils.TAG, "onActivityResult: " + dirUri);
        if (dirUri == null) return;
        int takeFlags = intent.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        context.getContentResolver().takePersistableUriPermission(dirUri, takeFlags);
        // re-init DownloadUtils
        DownloadUtils.init(context, dirUri.toString());
    }

    @NonNull
    public static Point getNavigationBarSize(@NonNull final Context context) {
        final Point appUsableSize = Utils.getAppUsableScreenSize(context);
        final Point realScreenSize = Utils.getRealScreenSize(context);

        // navigation bar on the right
        if (appUsableSize.x < realScreenSize.x) {
            return new Point(realScreenSize.x - appUsableSize.x, appUsableSize.y);
        }

        // navigation bar at the bottom
        if (appUsableSize.y < realScreenSize.y) {
            return new Point(appUsableSize.x, realScreenSize.y - appUsableSize.y);
        }

        // navigation bar is not present
        return new Point();
    }

    @NonNull
    public static Point getAppUsableScreenSize(@NonNull final Context context) {
        final WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        final Display display = windowManager.getDefaultDisplay();
        final Point size = new Point();
        display.getSize(size);
        return size;
    }

    @NonNull
    public static Point getRealScreenSize(@NonNull final Context context) {
        final WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        final Display display = windowManager.getDefaultDisplay();
        final Point size = new Point();
        display.getRealSize(size);
        return size;
    }

    public static <F, S> LiveData<Pair<F, S>> zipLiveData(@NonNull LiveData<F> firstLiveData,
                                                          @NonNull LiveData<S> secondLiveData) {
        ZippedLiveData<F, S> zippedLiveData = new ZippedLiveData<>();
        zippedLiveData.addFirstSource(firstLiveData);
        zippedLiveData.addSecondSource(secondLiveData);
        return zippedLiveData;
    }

    public static class ZippedLiveData<F, S> extends MediatorLiveData<Pair<F, S>> {
        private F lastF;
        private S lastS;

        private void update() {
            final F localLastF = this.lastF;
            final S localLastS = this.lastS;
            if (localLastF != null && localLastS != null) {
                this.setValue(new Pair<>(localLastF, localLastS));
            }
        }

        public void addFirstSource(@NonNull LiveData<F> firstLiveData) {
            this.addSource(firstLiveData, f -> {
                this.lastF = f;
                this.update();
            });
        }

        public void addSecondSource(@NonNull LiveData<S> secondLiveData) {
            this.addSource(secondLiveData, s -> {
                this.lastS = s;
                this.update();
            });
        }
    }
}
