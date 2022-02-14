package awais.instagrabber.fragments.settings;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import awais.instagrabber.R;
import awais.instagrabber.dialogs.ConfirmDialogFragment;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

import static android.app.Activity.RESULT_OK;
import static awais.instagrabber.activities.DirectorySelectActivity.SELECT_DIR_REQUEST_CODE;
import static awais.instagrabber.utils.Utils.settingsHelper;

public class DownloadsPreferencesFragment extends BasePreferencesFragment {
    private static final String TAG = DownloadsPreferencesFragment.class.getSimpleName();
    private Preference dirPreference;

    @Override
    void setupPreferenceScreen(PreferenceScreen screen) {
        Context context = this.getContext();
        if (context == null) return;
        screen.addPreference(this.getDownloadUserFolderPreference(context));
        screen.addPreference(this.getSaveToCustomFolderPreference(context));
        screen.addPreference(this.getPrependUsernameToFilenamePreference(context));
    }

    private Preference getDownloadUserFolderPreference(@NonNull Context context) {
        SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(PreferenceKeys.DOWNLOAD_USER_FOLDER);
        preference.setTitle(R.string.download_user_folder);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getSaveToCustomFolderPreference(@NonNull Context context) {
        this.dirPreference = new Preference(context);
        this.dirPreference.setIconSpaceReserved(false);
        this.dirPreference.setTitle(R.string.barinsta_folder);
        String currentValue = settingsHelper.getString(PreferenceKeys.PREF_BARINSTA_DIR_URI);
        if (TextUtils.isEmpty(currentValue)) this.dirPreference.setSummary("");
        else {
            String path;
            try {
                path = URLDecoder.decode(currentValue, StandardCharsets.UTF_8.toString());
            } catch (final UnsupportedEncodingException e) {
                path = currentValue;
            }
            this.dirPreference.setSummary(path);
        }
        this.dirPreference.setOnPreferenceClickListener(p -> {
            this.openDirectoryChooser(DownloadUtils.getRootDirUri());
            return true;
        });
        return this.dirPreference;
    }

    private void openDirectoryChooser(Uri initialUri) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && initialUri != null) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri);
        }
        try {
            this.startActivityForResult(intent, SELECT_DIR_REQUEST_CODE);
        } catch (final ActivityNotFoundException e) {
            Log.e(DownloadsPreferencesFragment.TAG, "openDirectoryChooser: ", e);
            this.showErrorDialog(this.getString(R.string.no_directory_picker_activity));
        } catch (final Exception e) {
            Log.e(DownloadsPreferencesFragment.TAG, "openDirectoryChooser: ", e);
        }
    }

    @SuppressLint("StringFormatInvalid")
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode != SELECT_DIR_REQUEST_CODE) return;
        if (resultCode != RESULT_OK) return;
        if (data == null || data.getData() == null) return;
        Context context = this.getContext();
        if (context == null) return;
        AppExecutors.INSTANCE.getMainThread().execute(() -> {
            try {
                Utils.setupSelectedDir(context, data);
                String path;
                try {
                    path = URLDecoder.decode(data.getData().toString(), StandardCharsets.UTF_8.name());
                } catch (final Exception e) {
                    path = data.getData().toString();
                }
                this.dirPreference.setSummary(path);
            } catch (final Exception e) {
                // Should not come to this point.
                // If it does, we have to show this error to the user so that they can report it.
                try (StringWriter sw = new StringWriter();
                     PrintWriter pw = new PrintWriter(sw)) {
                    e.printStackTrace(pw);
                    this.showErrorDialog("com.android.externalstorage.documents".equals(data.getData().getAuthority())
                                    ? "Please report this error to the developers:\n\n" + sw
                                    : this.getString(R.string.dir_select_no_download_folder, data.getData().getAuthority()));
                } catch (final IOException ioException) {
                    Log.e(DownloadsPreferencesFragment.TAG, "onActivityResult: ", ioException);
                }
            }
        }, 500);
    }

    private void showErrorDialog(String message) {
        ConfirmDialogFragment dialogFragment = ConfirmDialogFragment.newInstance(
                123,
                R.string.error,
                message,
                R.string.ok,
                0,
                0
        );
        dialogFragment.show(this.getChildFragmentManager(), ConfirmDialogFragment.class.getSimpleName());
    }

    private Preference getPrependUsernameToFilenamePreference(@NonNull Context context) {
        SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(PreferenceKeys.DOWNLOAD_PREPEND_USER_NAME);
        preference.setTitle(R.string.download_prepend_username);
        preference.setIconSpaceReserved(false);
        return preference;
    }
}
