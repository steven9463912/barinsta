package awais.instagrabber.fragments.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import awais.instagrabber.R;
import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.LocaleUtils;

public abstract class BasePreferencesFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    private boolean shouldRecreate;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        PreferenceManager preferenceManager = this.getPreferenceManager();
        preferenceManager.setSharedPreferencesName(Constants.SHARED_PREFERENCES_NAME);
        preferenceManager.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        Context context = this.getContext();
        if (context == null) return;
        PreferenceScreen screen = preferenceManager.createPreferenceScreen(context);
        this.setupPreferenceScreen(screen);
        this.setPreferenceScreen(screen);
    }

    abstract void setupPreferenceScreen(PreferenceScreen screen);

    protected void shouldRecreate() {
        shouldRecreate = true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (!this.shouldRecreate) return;
        MainActivity activity = (MainActivity) this.getActivity();
        if (activity == null) return;
        if (key.equals(PreferenceKeys.APP_LANGUAGE)) {
            LocaleUtils.setLocale(activity.getBaseContext());
        }
        this.shouldRecreate = false;
        activity.recreate();
    }

    @NonNull
    protected Preference getDivider(Context context) {
        Preference divider = new Preference(context);
        divider.setLayoutResource(R.layout.item_pref_divider);
        return divider;
    }
}
