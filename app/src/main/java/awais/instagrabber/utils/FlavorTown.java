package awais.instagrabber.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import awais.instagrabber.BuildConfig;
import awais.instagrabber.R;
import awaisomereport.CrashReporterHelper;

import static awais.instagrabber.utils.Utils.settingsHelper;

public final class FlavorTown {
    private static final String TAG = "FlavorTown";
    private static final UpdateChecker UPDATE_CHECKER = UpdateChecker.getInstance();
    private static final Pattern VERSION_NAME_PATTERN = Pattern.compile("v?(\\d+\\.\\d+\\.\\d+)(?:_?)(\\w*)(?:-?)(\\w*)");

    private static boolean checking;

    public static void updateCheck(@NonNull AppCompatActivity context) {
        FlavorTown.updateCheck(context, false);
    }

    public static void updateCheck(@NonNull AppCompatActivity context,
                                   boolean force) {
        if (FlavorTown.checking) return;
        FlavorTown.checking = true;
        AppExecutors.INSTANCE.getNetworkIO().execute(() -> {
            String onlineVersionName = FlavorTown.UPDATE_CHECKER.getLatestVersion();
            if (onlineVersionName == null) return;
            String onlineVersion = FlavorTown.getVersion(onlineVersionName);
            String localVersion = FlavorTown.getVersion(BuildConfig.VERSION_NAME);
            if (Objects.equals(onlineVersion, localVersion)) {
                if (force) {
                    AppExecutors.INSTANCE.getMainThread().execute(() -> {
                        Context applicationContext = context.getApplicationContext();
                        // Check if app was closed or crashed before reaching here
                        if (applicationContext == null) return;
                        // Show toast if version number preference was tapped
                        Toast.makeText(applicationContext, R.string.on_latest_version, Toast.LENGTH_SHORT).show();
                    });
                }
                return;
            }
            boolean shouldShowDialog = UpdateCheckCommon.shouldShowUpdateDialog(force, onlineVersionName);
            if (!shouldShowDialog) return;
            UpdateCheckCommon.showUpdateDialog(context, onlineVersionName, (dialog, which) -> {
                FlavorTown.UPDATE_CHECKER.onDownload(context);
                dialog.dismiss();
            });
        });
    }

    private static String getVersion(@NonNull String versionName) {
        Matcher matcher = FlavorTown.VERSION_NAME_PATTERN.matcher(versionName);
        if (!matcher.matches()) return versionName;
        try {
            return matcher.group(1);
        } catch (final Exception e) {
            Log.e(FlavorTown.TAG, "getVersion: ", e);
        }
        return versionName;
    }

    public static void changelogCheck(@NonNull Context context) {
        if (settingsHelper.getInteger(Constants.PREV_INSTALL_VERSION) >= BuildConfig.VERSION_CODE) return;
        int appUaCode = settingsHelper.getInteger(Constants.APP_UA_CODE);
        int browserUaCode = settingsHelper.getInteger(Constants.BROWSER_UA_CODE);
        if (browserUaCode == -1 || browserUaCode >= UserAgentUtils.browsers.length) {
            browserUaCode = ThreadLocalRandom.current().nextInt(0, UserAgentUtils.browsers.length);
            settingsHelper.putInteger(Constants.BROWSER_UA_CODE, browserUaCode);
        }
        if (appUaCode == -1 || appUaCode >= UserAgentUtils.devices.length) {
            appUaCode = ThreadLocalRandom.current().nextInt(0, UserAgentUtils.devices.length);
            settingsHelper.putInteger(Constants.APP_UA_CODE, appUaCode);
        }
        String appUa = UserAgentUtils.generateAppUA(appUaCode, LocaleUtils.getCurrentLocale().getLanguage());
        settingsHelper.putString(Constants.APP_UA, appUa);
        String browserUa = UserAgentUtils.generateBrowserUA(browserUaCode);
        settingsHelper.putString(Constants.BROWSER_UA, browserUa);
        AppExecutors.INSTANCE.getDiskIO().execute(() -> CrashReporterHelper.deleteAllStacktraceFiles(context));
        Toast.makeText(context, R.string.updated, Toast.LENGTH_SHORT).show();
        settingsHelper.putInteger(Constants.PREV_INSTALL_VERSION, BuildConfig.VERSION_CODE);
    }
}