package awais.instagrabber.fragments.settings;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import java.time.format.DateTimeFormatter;

import awais.instagrabber.R;
import awais.instagrabber.dialogs.TimeSettingsDialog;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.LocaleUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.UserAgentUtils;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class LocalePreferencesFragment extends BasePreferencesFragment {
    @Override
    void setupPreferenceScreen(PreferenceScreen screen) {
        Context context = this.getContext();
        if (context == null) return;
        screen.addPreference(this.getLanguagePreference(context));
        screen.addPreference(this.getPostTimeFormatPreference(context));
    }

    private Preference getLanguagePreference(@NonNull Context context) {
        ListPreference preference = new ListPreference(context);
        preference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        int length = this.getResources().getStringArray(R.array.languages).length;
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            values[i] = String.valueOf(i);
        }
        preference.setKey(PreferenceKeys.APP_LANGUAGE);
        preference.setTitle(R.string.select_language);
        preference.setDialogTitle(R.string.select_language);
        preference.setEntries(R.array.languages);
        preference.setIconSpaceReserved(false);
        preference.setEntryValues(values);
        preference.setOnPreferenceChangeListener((preference1, newValue) -> {
            this.shouldRecreate();
            int appUaCode = settingsHelper.getInteger(Constants.APP_UA_CODE);
            String appUa = UserAgentUtils.generateAppUA(appUaCode, LocaleUtils.getCurrentLocale().getLanguage());
            settingsHelper.putString(Constants.APP_UA, appUa);
            return true;
        });
        return preference;
    }

    private Preference getPostTimeFormatPreference(@NonNull Context context) {
        Preference preference = new Preference(context);
        preference.setTitle(R.string.time_settings);
        preference.setSummary(TextUtils.nowToString());
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(preference1 -> {
            new TimeSettingsDialog(
                    settingsHelper.getBoolean(PreferenceKeys.CUSTOM_DATE_TIME_FORMAT_ENABLED),
                    settingsHelper.getString(PreferenceKeys.CUSTOM_DATE_TIME_FORMAT),
                    settingsHelper.getString(PreferenceKeys.DATE_TIME_SELECTION),
                    settingsHelper.getBoolean(PreferenceKeys.SWAP_DATE_TIME_FORMAT_ENABLED),
                    (isCustomFormat,
                     spTimeFormatSelectedItemPosition,
                     spSeparatorSelectedItemPosition,
                     spDateFormatSelectedItemPosition,
                     selectedFormat,
                     swapDateTime) -> {
                        settingsHelper.putBoolean(PreferenceKeys.CUSTOM_DATE_TIME_FORMAT_ENABLED, isCustomFormat);
                        settingsHelper.putBoolean(PreferenceKeys.SWAP_DATE_TIME_FORMAT_ENABLED, swapDateTime);
                        if (isCustomFormat) {
                            settingsHelper.putString(PreferenceKeys.CUSTOM_DATE_TIME_FORMAT, selectedFormat);
                        } else {
                            String formatSelectionUpdated = spTimeFormatSelectedItemPosition + ";"
                                    + spSeparatorSelectedItemPosition + ';'
                                    + spDateFormatSelectedItemPosition; // time;separator;date
                            settingsHelper.putString(PreferenceKeys.DATE_TIME_FORMAT, selectedFormat);
                            settingsHelper.putString(PreferenceKeys.DATE_TIME_SELECTION, formatSelectionUpdated);
                        }
                        TextUtils.setFormatter(DateTimeFormatter.ofPattern(selectedFormat, LocaleUtils.getCurrentLocale()));
                        preference.setSummary(TextUtils.nowToString());
                    }
            ).show(this.getParentFragmentManager(), null);
            return true;
        });
        return preference;
    }
}
