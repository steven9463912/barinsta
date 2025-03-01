package awais.instagrabber.fragments.settings;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import awais.instagrabber.R;

public class AboutFragment extends BasePreferencesFragment {
    private static AppCompatTextView customPathTextView;

    @Override
    void setupPreferenceScreen(PreferenceScreen screen) {
        Context context = this.getContext();
        if (context == null) return;
        PreferenceCategory generalCategory = new PreferenceCategory(context);
        screen.addPreference(generalCategory);
        generalCategory.setTitle(R.string.pref_category_general);
        generalCategory.setIconSpaceReserved(false);
        generalCategory.addPreference(this.getDocsPreference());
        generalCategory.addPreference(this.getRepoPreference());
        generalCategory.addPreference(this.getFeedbackPreference());

        PreferenceCategory licenseCategory = new PreferenceCategory(context);
        screen.addPreference(licenseCategory);
        licenseCategory.setTitle(R.string.about_category_license);
        licenseCategory.setIconSpaceReserved(false);
        licenseCategory.addPreference(this.getLicensePreference());
        licenseCategory.addPreference(this.getLiabilityPreference());

        PreferenceCategory thirdPartyCategory = new PreferenceCategory(context);
        screen.addPreference(thirdPartyCategory);
        thirdPartyCategory.setTitle(R.string.about_category_3pt);
        thirdPartyCategory.setIconSpaceReserved(false);
        // alphabetical order!!!
        thirdPartyCategory.addPreference(this.get3ptPreference(
                context,
                "Apache Commons Imaging",
                "Copyright 2007-2020 The Apache Software Foundation. Apache 2.0. This product includes software developed at The Apache Software Foundation (http://www.apache.org/).",
                "https://commons.apache.org/proper/commons-imaging/"
        ));
        thirdPartyCategory.addPreference(this.get3ptPreference(
                context,
                "AutoLinkTextViewV2",
                "Copyright (C) 2019 Arman Chatikyan. Apache 2.0.",
                "https://github.com/armcha/AutoLinkTextViewV2"
        ));
        thirdPartyCategory.addPreference(this.get3ptPreference(
                context,
                "ExoPlayer",
                "Copyright (C) 2016 The Android Open Source Project. Apache 2.0.",
                "https://exoplayer.dev"
        ));
        thirdPartyCategory.addPreference(this.get3ptPreference(
                context,
                "Fresco",
                "Copyright (c) Facebook, Inc. and its affiliates. MIT License.",
                "https://frescolib.org"
        ));
        thirdPartyCategory.addPreference(this.get3ptPreference(
                context,
                "GPUImage",
                "Copyright 2018 CyberAgent, Inc. Apache 2.0.",
                "https://github.com/cats-oss/android-gpuimage"
        ));
        thirdPartyCategory.addPreference(this.get3ptPreference(
                context,
                "Material Design Icons",
                "Copyright (C) 2014 Austin Andrews & Google LLC. Apache 2.0.",
                "https://materialdesignicons.com"
        ));
        thirdPartyCategory.addPreference(this.get3ptPreference(
                context,
                "Process Phoenix",
                "Copyright (C) 2015 Jake Wharton. Apache 2.0.",
                "https://github.com/JakeWharton/ProcessPhoenix"
        ));
        thirdPartyCategory.addPreference(this.get3ptPreference(
                context,
                "Retrofit",
                "Copyright 2013 Square, Inc. Apache 2.0.",
                "https://square.github.io/retrofit/"
        ));
        thirdPartyCategory.addPreference(this.get3ptPreference(
                context,
                "uCrop",
                "Copyright 2017 Yalantis. Apache 2.0.",
                "https://github.com/Yalantis/uCrop"
        ));
    }

    private Preference getDocsPreference() {
        Context context = this.getContext();
        if (context == null) return null;
        Preference preference = new Preference(context);
        preference.setTitle(R.string.about_documentation);
        preference.setSummary(R.string.about_documentation_summary);
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(p -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://barinsta.austinhuang.me"));
            this.startActivity(intent);
            return true;
        });
        return preference;
    }

    private Preference getRepoPreference() {
        Context context = this.getContext();
        if (context == null) return null;
        Preference preference = new Preference(context);
        preference.setTitle(R.string.about_repository);
        preference.setSummary(R.string.about_repository_summary);
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(p -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://github.com/austinhuang0131/barinsta"));
            this.startActivity(intent);
            return true;
        });
        return preference;
    }

    private Preference getFeedbackPreference() {
        Context context = this.getContext();
        if (context == null) return null;
        Preference preference = new Preference(context);
        preference.setTitle(R.string.about_feedback);
        preference.setSummary(R.string.about_feedback_summary);
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(p -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("message/rfc822")
                    .putExtra(Intent.EXTRA_EMAIL, this.getString(R.string.about_feedback_summary))
                    .putExtra(Intent.EXTRA_TEXT, "Please note that your email address and the entire content will be published onto GitHub issues. If you do not wish to do that, use other contact methods instead.");
            if (intent.resolveActivity(context.getPackageManager()) != null) this.startActivity(intent);
            return true;
        });
        return preference;
    }

    private Preference getLicensePreference() {
        Context context = this.getContext();
        if (context == null) return null;
        Preference preference = new Preference(context);
        preference.setSummary(R.string.license);
        preference.setEnabled(false);
        preference.setIcon(R.drawable.ic_outline_info_24);
        preference.setIconSpaceReserved(true);
        return preference;
    }

    private Preference getLiabilityPreference() {
        Context context = this.getContext();
        if (context == null) return null;
        Preference preference = new Preference(context);
        preference.setSummary(R.string.liability);
        preference.setEnabled(false);
        preference.setIcon(R.drawable.ic_warning);
        preference.setIconSpaceReserved(true);
        return preference;
    }

    private Preference get3ptPreference(@NonNull Context context,
                                        String title,
                                        String summary,
                                        String url) {
        Preference preference = new Preference(context);
        preference.setTitle(title);
        preference.setSummary(summary);
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(p -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            this.startActivity(intent);
            return true;
        });
        return preference;
    }
}
