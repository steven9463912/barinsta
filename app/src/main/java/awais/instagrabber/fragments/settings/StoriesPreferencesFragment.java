package awais.instagrabber.fragments.settings;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import awais.instagrabber.R;

public class StoriesPreferencesFragment extends BasePreferencesFragment {
    @Override
    void setupPreferenceScreen(PreferenceScreen screen) {
        Context context = this.getContext();
        if (context == null) return;
        screen.addPreference(this.getStorySortPreference(context));
        screen.addPreference(this.getHideMutedReelsPreference(context));
        screen.addPreference(this.getMarkStoriesSeenPreference(context));
        screen.addPreference(this.getAutoPlayPreference(context));
        screen.addPreference(this.getStoryListPreference(context));
    }

    private Preference getStorySortPreference(@NonNull Context context) {
        ListPreference preference = new ListPreference(context);
        preference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        int length = this.getResources().getStringArray(R.array.story_sorts).length;
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            values[i] = String.valueOf(i);
        }
        preference.setKey(PreferenceKeys.STORY_SORT);
        preference.setTitle(R.string.story_sort_setting);
        preference.setDialogTitle(R.string.story_sort_setting);
        preference.setEntries(R.array.story_sorts);
        preference.setIconSpaceReserved(false);
        preference.setEntryValues(values);
        return preference;
    }

    private Preference getHideMutedReelsPreference(@NonNull Context context) {
        SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(PreferenceKeys.HIDE_MUTED_REELS);
        preference.setTitle(R.string.hide_muted_reels_setting);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getMarkStoriesSeenPreference(@NonNull Context context) {
        SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(PreferenceKeys.MARK_AS_SEEN);
        preference.setTitle(R.string.mark_as_seen_setting);
        preference.setSummary(R.string.mark_as_seen_setting_summary);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getAutoPlayPreference(@NonNull Context context) {
        SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(PreferenceKeys.AUTOPLAY_VIDEOS_STORIES);
        preference.setTitle(R.string.autoplay_stories_setting);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getStoryListPreference(@NonNull Context context) {
        SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(PreferenceKeys.PREF_STORY_SHOW_LIST);
        preference.setTitle(R.string.story_list_setting);
        preference.setSummary(R.string.story_list_setting_summary);
        preference.setIconSpaceReserved(false);
        return preference;
    }
}
