package awais.instagrabber.fragments.settings;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.dialogs.ConfirmDialogFragment;
import awais.instagrabber.dialogs.TabOrderPreferenceDialogFragment;
import awais.instagrabber.models.Tab;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.NavigationHelperKt;
import awais.instagrabber.utils.TextUtils;
import kotlin.Pair;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class GeneralPreferencesFragment extends BasePreferencesFragment implements TabOrderPreferenceDialogFragment.Callback {

    @Override
    void setupPreferenceScreen(PreferenceScreen screen) {
        Context context = this.getContext();
        if (context == null) return;
        String cookie = settingsHelper.getString(Constants.COOKIE);
        boolean isLoggedIn = !TextUtils.isEmpty(cookie) && CookieUtils.getUserIdFromCookie(cookie) > 0;
        if (isLoggedIn) {
            screen.addPreference(this.getDefaultTabPreference(context));
            screen.addPreference(this.getTabOrderPreference(context));
        }
        screen.addPreference(this.getDisableScreenTransitionsPreference(context));
        screen.addPreference(this.getUpdateCheckPreference(context));
        screen.addPreference(this.getFlagSecurePreference(context));
        screen.addPreference(this.getSearchFocusPreference(context));
        List<Preference> preferences = FlavorSettings
                .getInstance()
                .getPreferences(
                        context,
                        this.getChildFragmentManager(),
                        SettingCategory.GENERAL
                );
        for (Preference preference : preferences) {
            screen.addPreference(preference);
        }
    }

    private Preference getDefaultTabPreference(@NonNull Context context) {
        ListPreference preference = new ListPreference(context);
        preference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        Pair<List<Tab>, List<Tab>> listPair = NavigationHelperKt.getLoggedInNavTabs(context);
        List<Tab> tabs = listPair.getFirst();
        String[] titles = tabs.stream()
                                    .map(Tab::getTitle)
                                    .toArray(String[]::new);
        String[] navGraphFileNames = tabs.stream()
                                               .map(tab -> NavigationHelperKt.getNavGraphNameForNavRootId(tab.getNavigationRootId()))
                                               .toArray(String[]::new);
        preference.setKey(Constants.DEFAULT_TAB);
        preference.setTitle(R.string.pref_start_screen);
        preference.setDialogTitle(R.string.pref_start_screen);
        preference.setEntries(titles);
        preference.setEntryValues(navGraphFileNames);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    @NonNull
    private Preference getTabOrderPreference(@NonNull Context context) {
        Preference preference = new Preference(context);
        preference.setTitle(R.string.tab_order);
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(preference1 -> {
            TabOrderPreferenceDialogFragment dialogFragment = TabOrderPreferenceDialogFragment.newInstance();
            dialogFragment.show(this.getChildFragmentManager(), "tab_order_dialog");
            return true;
        });
        return preference;
    }

    private Preference getDisableScreenTransitionsPreference(@NonNull Context context) {
        SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(PreferenceKeys.PREF_DISABLE_SCREEN_TRANSITIONS);
        preference.setTitle(R.string.disable_screen_transitions);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getUpdateCheckPreference(@NonNull Context context) {
        SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(PreferenceKeys.CHECK_UPDATES);
        preference.setTitle(R.string.update_check);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getFlagSecurePreference(@NonNull Context context) {
        return PreferenceHelper.getSwitchPreference(
                context,
                PreferenceKeys.FLAG_SECURE,
                R.string.flag_secure,
                -1,
                false,
                (preference, newValue) -> {
                    this.shouldRecreate();
                    return true;
                });
    }

    private Preference getSearchFocusPreference(@NonNull Context context) {
        SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(PreferenceKeys.PREF_SEARCH_FOCUS_KEYBOARD);
        preference.setTitle(R.string.pref_search_focus_keyboard);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    @Override
    public void onSave(boolean orderHasChanged) {
        if (!orderHasChanged) return;
        ConfirmDialogFragment dialogFragment = ConfirmDialogFragment.newInstance(
                111,
                0,
                R.string.tab_order_start_next_launch,
                R.string.ok,
                0,
                0);
        dialogFragment.show(this.getChildFragmentManager(), "tab_order_set_dialog");
    }

    @Override
    public void onCancel() {

    }
}
