package awais.instagrabber.fragments.settings;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import awais.instagrabber.R;

public class NotificationsPreferencesFragment extends BasePreferencesFragment {
    @Override
    void setupPreferenceScreen(PreferenceScreen screen) {
        Context context = this.getContext();
        if (context == null) return;
        screen.addPreference(this.getActivityNotificationsPreference(context));
        // screen.addPreference(getDMNotificationsPreference(context));
    }

    private Preference getActivityNotificationsPreference(@NonNull Context context) {
        return PreferenceHelper.getSwitchPreference(
                context,
                PreferenceKeys.CHECK_ACTIVITY,
                R.string.activity_setting,
                -1,
                false,
                (preference, newValue) -> {
                    this.shouldRecreate();
                    return true;
                });
    }

    private Preference getDMNotificationsPreference(@NonNull Context context) {
        return PreferenceHelper.getSwitchPreference(
                context,
                PreferenceKeys.PREF_ENABLE_DM_NOTIFICATIONS,
                R.string.enable_dm_notifications,
                -1,
                false,
                null);
    }
}
