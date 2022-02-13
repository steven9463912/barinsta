package awais.instagrabber.fragments.settings;

import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;

import java.util.Objects;

import awais.instagrabber.R;
import awais.instagrabber.customviews.helpers.TextWatcherAdapter;
import awais.instagrabber.databinding.PrefAutoRefreshDmFreqBinding;
import awais.instagrabber.services.DMSyncAlarmReceiver;
import awais.instagrabber.services.DMSyncService;
import awais.instagrabber.utils.Debouncer;
import awais.instagrabber.utils.TextUtils;

import static awais.instagrabber.fragments.settings.PreferenceKeys.PREF_ENABLE_DM_AUTO_REFRESH_FREQ_NUMBER;
import static awais.instagrabber.fragments.settings.PreferenceKeys.PREF_ENABLE_DM_AUTO_REFRESH_FREQ_UNIT;
import static awais.instagrabber.utils.Utils.settingsHelper;

public class DMPreferencesFragment extends BasePreferencesFragment {
    private static final String TAG = DMPreferencesFragment.class.getSimpleName();

    @Override
    void setupPreferenceScreen(PreferenceScreen screen) {
        Context context = this.getContext();
        if (context == null) return;
        screen.addPreference(this.getMarkDMSeenPreference(context));
        // screen.addPreference(getAutoRefreshDMPreference(context));
        // screen.addPreference(getAutoRefreshDMFreqPreference(context));
    }

    private Preference getMarkDMSeenPreference(@NonNull Context context) {
        return PreferenceHelper.getSwitchPreference(
                context,
                PreferenceKeys.DM_MARK_AS_SEEN,
                R.string.dm_mark_as_seen_setting,
                R.string.dm_mark_as_seen_setting_summary,
                false,
                null
        );
    }

    private Preference getAutoRefreshDMPreference(@NonNull Context context) {
        return PreferenceHelper.getSwitchPreference(
                context,
                PreferenceKeys.PREF_ENABLE_DM_AUTO_REFRESH,
                R.string.enable_dm_auto_refesh,
                -1,
                false,
                (preference, newValue) -> {
                    if (!(newValue instanceof Boolean)) return false;
                    boolean enabled = (Boolean) newValue;
                    if (enabled) {
                        DMSyncAlarmReceiver.setAlarm(context);
                        return true;
                    }
                    DMSyncAlarmReceiver.cancelAlarm(context);
                    try {
                        Context applicationContext = context.getApplicationContext();
                        applicationContext.stopService(new Intent(applicationContext, DMSyncService.class));
                    } catch (final Exception e) {
                        Log.e(DMPreferencesFragment.TAG, "getAutoRefreshDMPreference: ", e);
                    }
                    return true;
                }
        );
    }

    private Preference getAutoRefreshDMFreqPreference(@NonNull Context context) {
        return new AutoRefreshDMFrePreference(context);
    }

    public static class AutoRefreshDMFrePreference extends Preference {
        private static final String TAG = AutoRefreshDMFrePreference.class.getSimpleName();
        private static final String DEBOUNCE_KEY = "dm_sync_service_update";
        public static final int INTERVAL = 2000;

        private final Debouncer.Callback<String> changeCallback;

        private Debouncer<String> serviceUpdateDebouncer;
        private PrefAutoRefreshDmFreqBinding binding;

        public AutoRefreshDMFrePreference(Context context) {
            super(context);
            this.setLayoutResource(R.layout.pref_auto_refresh_dm_freq);
            // setKey(key);
            this.setIconSpaceReserved(false);
            this.changeCallback = new Debouncer.Callback<String>() {
                @Override
                public void call(String key) {
                    DMSyncAlarmReceiver.setAlarm(context);
                }

                @Override
                public void onError(Throwable t) {
                    Log.e(AutoRefreshDMFrePreference.TAG, "onError: ", t);
                }
            };
            this.serviceUpdateDebouncer = new Debouncer<>(this.changeCallback, AutoRefreshDMFrePreference.INTERVAL);
        }

        @Override
        public void onDependencyChanged(Preference dependency, boolean disableDependent) {
            // super.onDependencyChanged(dependency, disableDependent);
            if (this.binding == null) return;
            this.binding.startText.setEnabled(!disableDependent);
            this.binding.freqNum.setEnabled(!disableDependent);
            this.binding.freqUnit.setEnabled(!disableDependent);
            if (disableDependent) {
                this.serviceUpdateDebouncer.terminate();
                return;
            }
            this.serviceUpdateDebouncer = new Debouncer<>(this.changeCallback, AutoRefreshDMFrePreference.INTERVAL);
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder holder) {
            super.onBindViewHolder(holder);
            this.setDependency(PreferenceKeys.PREF_ENABLE_DM_AUTO_REFRESH);
            this.binding = PrefAutoRefreshDmFreqBinding.bind(holder.itemView);
            Context context = this.getContext();
            if (context == null) return;
            this.setupUnitSpinner(context);
            this.setupNumberEditText(context);
        }

        private void setupUnitSpinner(Context context) {
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context,
                                                                                       R.array.dm_auto_refresh_freq_unit_labels,
                                                                                       android.R.layout.simple_spinner_item);
            String[] values = context.getResources().getStringArray(R.array.dm_auto_refresh_freq_units);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            this.binding.freqUnit.setAdapter(adapter);

            String unit = settingsHelper.getString(PREF_ENABLE_DM_AUTO_REFRESH_FREQ_UNIT);
            if (TextUtils.isEmpty(unit)) {
                unit = "secs";
            }
            int position = 0;
            for (int i = 0; i < values.length; i++) {
                if (Objects.equals(unit, values[i])) {
                    position = i;
                    break;
                }
            }
            this.binding.freqUnit.setSelection(position);
            this.binding.freqUnit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    settingsHelper.putString(PREF_ENABLE_DM_AUTO_REFRESH_FREQ_UNIT, values[position]);
                    if (!AutoRefreshDMFrePreference.this.isEnabled()) {
                        AutoRefreshDMFrePreference.this.serviceUpdateDebouncer.terminate();
                        return;
                    }
                    AutoRefreshDMFrePreference.this.serviceUpdateDebouncer.call(AutoRefreshDMFrePreference.DEBOUNCE_KEY);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        private void setupNumberEditText(Context context) {
            int currentValue = settingsHelper.getInteger(PREF_ENABLE_DM_AUTO_REFRESH_FREQ_NUMBER);
            if (currentValue <= 0) {
                currentValue = 30;
            }
            this.binding.freqNum.setText(String.valueOf(currentValue));
            this.binding.freqNum.addTextChangedListener(new TextWatcherAdapter() {

                @Override
                public void afterTextChanged(Editable s) {
                    if (TextUtils.isEmpty(s)) return;
                    try {
                        int value = Integer.parseInt(s.toString());
                        if (value <= 0) return;
                        settingsHelper.putInteger(PREF_ENABLE_DM_AUTO_REFRESH_FREQ_NUMBER, value);
                        if (!AutoRefreshDMFrePreference.this.isEnabled()) {
                            AutoRefreshDMFrePreference.this.serviceUpdateDebouncer.terminate();
                            return;
                        }
                        AutoRefreshDMFrePreference.this.serviceUpdateDebouncer.call(AutoRefreshDMFrePreference.DEBOUNCE_KEY);
                    } catch (final Exception e) {
                        Log.e(AutoRefreshDMFrePreference.TAG, "afterTextChanged: ", e);
                    }
                }
            });
        }
    }
}
