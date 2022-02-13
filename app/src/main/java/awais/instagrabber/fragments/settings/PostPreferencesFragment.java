package awais.instagrabber.fragments.settings;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import awais.instagrabber.R;
import awais.instagrabber.dialogs.KeywordsFilterDialog;

public class PostPreferencesFragment extends BasePreferencesFragment {
    @Override
    void setupPreferenceScreen(PreferenceScreen screen) {
        Context context = this.getContext();
        if (context == null) return;
        // generalCategory.addPreference(getAutoPlayVideosPreference(context));
        screen.addPreference(this.getBackgroundPlayPreference(context));
        screen.addPreference(this.getAlwaysMuteVideosPreference(context));
        screen.addPreference(this.getToggleKeywordFilterPreference(context));
        screen.addPreference(this.getEditKeywordFilterPreference(context));
    }

//    private Preference getAutoPlayVideosPreference(@NonNull final Context context) {
//        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
//        preference.setKey(Constants.AUTOPLAY_VIDEOS);
//        preference.setTitle(R.string.post_viewer_autoplay_video);
//        preference.setIconSpaceReserved(false);
//        return preference;
//    }

    private Preference getBackgroundPlayPreference(@NonNull Context context) {
        SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(PreferenceKeys.PLAY_IN_BACKGROUND);
        preference.setTitle(R.string.post_viewer_background_play);
        preference.setSummary(R.string.post_viewer_background_play_summary);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getAlwaysMuteVideosPreference(@NonNull Context context) {
        SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(PreferenceKeys.MUTED_VIDEOS);
        preference.setTitle(R.string.post_viewer_muted_autoplay);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getToggleKeywordFilterPreference(@NonNull Context context) {
        SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setKey(PreferenceKeys.TOGGLE_KEYWORD_FILTER);
        preference.setDefaultValue(false);
        preference.setTitle(R.string.toggle_keyword_filter);
        preference.setIconSpaceReserved(false);
        return preference;
    }

    private Preference getEditKeywordFilterPreference(@NonNull Context context){
        Preference preference = new Preference(context);
        preference.setTitle(R.string.edit_keyword_filter);
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(view ->{
            new KeywordsFilterDialog().show(this.getParentFragmentManager(), null);
            return true;
        });
        return preference;
    }
}
