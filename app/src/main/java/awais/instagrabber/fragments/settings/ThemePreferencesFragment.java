package awais.instagrabber.fragments.settings;

import android.content.Context;
import android.content.res.TypedArray;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import awais.instagrabber.R;
import awais.instagrabber.utils.Constants;

public class ThemePreferencesFragment extends BasePreferencesFragment {
    @Override
    void setupPreferenceScreen(PreferenceScreen screen) {
        Context context = this.getContext();
        if (context == null) return;
        screen.addPreference(this.getThemePreference(context));
        screen.addPreference(this.getLightThemePreference(context));
        screen.addPreference(this.getDarkThemePreference(context));
    }

    private Preference getThemePreference(@NonNull Context context) {
        ListPreference preference = new ListPreference(context);
        preference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        int length = this.getResources().getStringArray(R.array.theme_presets).length;
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            values[i] = String.valueOf(i);
        }
        preference.setKey(PreferenceKeys.APP_THEME);
        preference.setTitle(R.string.theme_settings);
        preference.setDialogTitle(R.string.theme_settings);
        preference.setEntries(R.array.theme_presets);
        preference.setIconSpaceReserved(false);
        preference.setEntryValues(values);
        preference.setOnPreferenceChangeListener((preference1, newValue) -> {
            this.shouldRecreate();
            return true;
        });
        return preference;
    }

    private Preference getLightThemePreference(Context context) {
        ListPreference preference = new ListPreference(context);
        preference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        TypedArray lightThemeValues = this.getResources().obtainTypedArray(R.array.light_theme_values);
        int length = lightThemeValues.length();
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            int resourceId = lightThemeValues.getResourceId(i, -1);
            if (resourceId < 0) continue;
            values[i] = this.getResources().getResourceEntryName(resourceId);
        }
        lightThemeValues.recycle();
        preference.setKey(Constants.PREF_LIGHT_THEME);
        preference.setTitle(R.string.light_theme_settings);
        preference.setDialogTitle(R.string.light_theme_settings);
        preference.setEntries(R.array.light_themes);
        preference.setIconSpaceReserved(false);
        preference.setEntryValues(values);
        preference.setOnPreferenceChangeListener((preference1, newValue) -> {
            this.shouldRecreate();
            return true;
        });
        return preference;
    }

    private Preference getDarkThemePreference(Context context) {
        ListPreference preference = new ListPreference(context);
        preference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        TypedArray darkThemeValues = this.getResources().obtainTypedArray(R.array.dark_theme_values);
        int length = darkThemeValues.length();
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            int resourceId = darkThemeValues.getResourceId(i, -1);
            if (resourceId < 0) continue;
            values[i] = this.getResources().getResourceEntryName(resourceId);
        }
        darkThemeValues.recycle();
        preference.setKey(Constants.PREF_DARK_THEME);
        preference.setTitle(R.string.dark_theme_settings);
        preference.setDialogTitle(R.string.dark_theme_settings);
        preference.setEntries(R.array.dark_themes);
        preference.setIconSpaceReserved(false);
        preference.setEntryValues(values);
        preference.setOnPreferenceChangeListener((preference1, newValue) -> {
            this.shouldRecreate();
            return true;
        });
        return preference;
    }
}
