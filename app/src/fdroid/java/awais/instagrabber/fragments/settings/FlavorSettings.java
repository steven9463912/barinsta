package awais.instagrabber.fragments.settings;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;

import java.util.Collections;
import java.util.List;

public final class FlavorSettings implements IFlavorSettings {

    private static FlavorSettings instance;

    private FlavorSettings() {
    }

    public static FlavorSettings getInstance() {
        if (FlavorSettings.instance == null) {
            FlavorSettings.instance = new FlavorSettings();
        }
        return FlavorSettings.instance;
    }

    @NonNull
    @Override
    public List<Preference> getPreferences(@NonNull Context context,
                                           @NonNull FragmentManager fragmentManager,
                                           @NonNull SettingCategory settingCategory) {
        // switch (settingCategory) {
        //     default:
        //         break;
        // }
        return Collections.emptyList();
    }
}
