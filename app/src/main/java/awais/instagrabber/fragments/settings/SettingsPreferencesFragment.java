package awais.instagrabber.fragments.settings;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.google.common.collect.ImmutableList;

import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.TextUtils;

import static awais.instagrabber.fragments.settings.SettingsPreferencesFragmentDirections.actionSettingsToDm;
import static awais.instagrabber.fragments.settings.SettingsPreferencesFragmentDirections.actionSettingsToDownloads;
import static awais.instagrabber.fragments.settings.SettingsPreferencesFragmentDirections.actionSettingsToGeneral;
import static awais.instagrabber.fragments.settings.SettingsPreferencesFragmentDirections.actionSettingsToLocale;
import static awais.instagrabber.fragments.settings.SettingsPreferencesFragmentDirections.actionSettingsToNotifications;
import static awais.instagrabber.fragments.settings.SettingsPreferencesFragmentDirections.actionSettingsToPost;
import static awais.instagrabber.fragments.settings.SettingsPreferencesFragmentDirections.actionSettingsToStories;
import static awais.instagrabber.fragments.settings.SettingsPreferencesFragmentDirections.actionSettingsToTheme;
import static awais.instagrabber.utils.Utils.settingsHelper;

public class SettingsPreferencesFragment extends BasePreferencesFragment {
    private static final String TAG = SettingsPreferencesFragment.class.getSimpleName();
    private static final List<SettingScreen> screens = ImmutableList.of(
            new SettingScreen(R.string.pref_category_general, actionSettingsToGeneral()),
            new SettingScreen(R.string.pref_category_theme, actionSettingsToTheme()),
            new SettingScreen(R.string.pref_category_locale, actionSettingsToLocale()),
            new SettingScreen(R.string.pref_category_post, actionSettingsToPost()),
            new SettingScreen(R.string.pref_category_stories, actionSettingsToStories(), true),
            new SettingScreen(R.string.pref_category_dm, actionSettingsToDm(), true),
            new SettingScreen(R.string.pref_category_notifications, actionSettingsToNotifications(), true),
            new SettingScreen(R.string.pref_category_downloads, actionSettingsToDownloads())
    );

    @Override
    void setupPreferenceScreen(PreferenceScreen screen) {
        Context context = this.getContext();
        if (context == null) return;
        String cookie = settingsHelper.getString(Constants.COOKIE);
        boolean isLoggedIn = !TextUtils.isEmpty(cookie) && CookieUtils.getUserIdFromCookie(cookie) > 0;
        for (SettingScreen settingScreen : SettingsPreferencesFragment.screens) {
            if (settingScreen.isLoginRequired() && !isLoggedIn) continue;
            screen.addPreference(this.getNavPreference(context, settingScreen));
        }
        //        else {
        //            final PreferenceCategory anonUsersPreferenceCategory = new PreferenceCategory(context);
        //            screen.addPreference(anonUsersPreferenceCategory);
        //            anonUsersPreferenceCategory.setIconSpaceReserved(false);
        //            anonUsersPreferenceCategory.setTitle(R.string.anonymous_settings);
        //        }
    }

    private Preference getNavPreference(@NonNull Context context,
                                        @NonNull SettingScreen settingScreen) {
        Preference preference = new Preference(context);
        preference.setTitle(settingScreen.getTitleResId());
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(preference1 -> {
            NavHostFragment.findNavController(this).navigate(settingScreen.getDirections());
            return true;
        });
        return preference;
    }

    private static class SettingScreen {
        private final int titleResId;
        private final NavDirections directions;
        private final boolean loginRequired;

        public SettingScreen(@StringRes int titleResId, NavDirections directions) {
            this(titleResId, directions, false);
        }

        public SettingScreen(@StringRes int titleResId, NavDirections directions, boolean loginRequired) {
            this.titleResId = titleResId;
            this.directions = directions;
            this.loginRequired = loginRequired;
        }

        public int getTitleResId() {
            return this.titleResId;
        }

        public NavDirections getDirections() {
            return this.directions;
        }

        public boolean isLoginRequired() {
            return this.loginRequired;
        }
    }
}
