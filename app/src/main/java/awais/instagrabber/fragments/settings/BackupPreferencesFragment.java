package awais.instagrabber.fragments.settings;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import awais.instagrabber.R;
import awais.instagrabber.dialogs.CreateBackupDialogFragment;
import awais.instagrabber.dialogs.RestoreBackupDialogFragment;

public class BackupPreferencesFragment extends BasePreferencesFragment {

    @Override
    void setupPreferenceScreen(PreferenceScreen screen) {
        Context context = this.getContext();
        if (context == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 23) {
            PreferenceCategory autoCategory = new PreferenceCategory(context);
            screen.addPreference(autoCategory);
            autoCategory.setTitle(R.string.auto_backup);
            autoCategory.addPreference(this.getAboutPreference(context, true));
            autoCategory.addPreference(this.getWarningPreference(context, true));
            autoCategory.addPreference(this.getAutoBackupPreference(context));
        }
        PreferenceCategory manualCategory = new PreferenceCategory(context);
        screen.addPreference(manualCategory);
        manualCategory.setTitle(R.string.manual_backup);
        manualCategory.addPreference(this.getAboutPreference(context, false));
        manualCategory.addPreference(this.getWarningPreference(context, false));
        manualCategory.addPreference(this.getCreatePreference(context));
        manualCategory.addPreference(this.getRestorePreference(context));
    }

    private Preference getAboutPreference(@NonNull Context context,
                                          @NonNull boolean auto) {
        Preference preference = new Preference(context);
        preference.setSummary(auto ? R.string.auto_backup_summary : R.string.backup_summary);
        preference.setEnabled(false);
        preference.setIcon(R.drawable.ic_outline_info_24);
        preference.setIconSpaceReserved(true);
        return preference;
    }

    private Preference getWarningPreference(@NonNull Context context,
                                            @NonNull boolean auto) {
        Preference preference = new Preference(context);
        preference.setSummary(auto ? R.string.auto_backup_warning : R.string.backup_warning);
        preference.setEnabled(false);
        preference.setIcon(R.drawable.ic_warning);
        preference.setIconSpaceReserved(true);
        return preference;
    }

    private Preference getAutoBackupPreference(@NonNull Context context) {
        SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(PreferenceKeys.PREF_AUTO_BACKUP_ENABLED);
        preference.setTitle(R.string.auto_backup_setting);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getCreatePreference(@NonNull Context context) {
        Preference preference = new Preference(context);
        preference.setTitle(R.string.create_backup);
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(preference1 -> {
            FragmentManager fragmentManager = this.getParentFragmentManager();
            CreateBackupDialogFragment fragment = new CreateBackupDialogFragment(result -> {
                View view = this.getView();
                if (view != null) {
                    Snackbar.make(view,
                                  result ? R.string.dialog_export_success
                                         : R.string.dialog_export_failed,
                                  BaseTransientBottomBar.LENGTH_LONG)
                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                            .setAction(R.string.ok, v -> {})
                            .show();
                    return;
                }
                Toast.makeText(context,
                               result ? R.string.dialog_export_success
                                      : R.string.dialog_export_failed,
                               Toast.LENGTH_LONG)
                     .show();
            });
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
              .add(fragment, "createBackup")
              .commit();
            return true;
        });
        return preference;
    }

    private Preference getRestorePreference(@NonNull Context context) {
        Preference preference = new Preference(context);
        preference.setTitle(R.string.restore_backup);
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(preference1 -> {
            FragmentManager fragmentManager = this.getParentFragmentManager();
            RestoreBackupDialogFragment fragment = new RestoreBackupDialogFragment(result -> {
                View view = this.getView();
                if (view != null) {
                    Snackbar.make(view,
                                  result ? R.string.dialog_import_success
                                         : R.string.dialog_import_failed,
                                  BaseTransientBottomBar.LENGTH_LONG)
                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                            .setAction(R.string.ok, v -> {})
                            .addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                                @Override
                                public void onDismissed(Snackbar transientBottomBar, int event) {
                                    BackupPreferencesFragment.this.recreateActivity(result);
                                }
                            })
                            .show();
                    return;
                }
                this.recreateActivity(result);
            });
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
              .add(fragment, "restoreBackup")
              .commit();
            return true;
        });
        return preference;
    }

    private void recreateActivity(boolean result) {
        if (!result) return;
        FragmentActivity activity = this.getActivity();
        if (activity == null) return;
        activity.recreate();
    }
}
