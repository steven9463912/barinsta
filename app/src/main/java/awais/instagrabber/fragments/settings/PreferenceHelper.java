package awais.instagrabber.fragments.settings;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

public final class PreferenceHelper {

    public static SwitchPreferenceCompat getSwitchPreference(@NonNull Context context,
                                                             @NonNull String key,
                                                             @StringRes int titleResId,
                                                             @StringRes int summaryResId,
                                                             boolean iconSpaceReserved,
                                                             Preference.OnPreferenceChangeListener onPreferenceChangeListener) {
        SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(key);
        preference.setTitle(titleResId);
        preference.setIconSpaceReserved(iconSpaceReserved);
        if (summaryResId != -1) {
            preference.setSummary(summaryResId);
        }
        if (onPreferenceChangeListener != null) {
            preference.setOnPreferenceChangeListener(onPreferenceChangeListener);
        }
        return preference;
    }
}
