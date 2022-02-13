package awais.instagrabber.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

import awais.instagrabber.fragments.settings.PreferenceKeys;
import awais.instagrabber.utils.Constants;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class DMSyncAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = DMSyncAlarmReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean enabled = settingsHelper.getBoolean(PreferenceKeys.PREF_ENABLE_DM_AUTO_REFRESH);
        if (!enabled) {
            // If somehow the alarm was triggered even when auto refresh is disabled
            DMSyncAlarmReceiver.cancelAlarm(context);
            return;
        }
        try {
            Context applicationContext = context.getApplicationContext();
            ContextCompat.startForegroundService(applicationContext, new Intent(applicationContext, DMSyncService.class));
        } catch (final Exception e) {
            Log.e(DMSyncAlarmReceiver.TAG, "onReceive: ", e);
        }
    }

    public static void setAlarm(@NonNull Context context) {
        Log.d(DMSyncAlarmReceiver.TAG, "setting DMSyncService Alarm");
        AlarmManager alarmManager = DMSyncAlarmReceiver.getAlarmManager(context);
        if (alarmManager == null) return;
        PendingIntent pendingIntent = DMSyncAlarmReceiver.getPendingIntent(context);
        alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(), DMSyncAlarmReceiver.getIntervalMillis(), pendingIntent);
    }

    public static void cancelAlarm(@NonNull Context context) {
        Log.d(DMSyncAlarmReceiver.TAG, "cancelling DMSyncService Alarm");
        AlarmManager alarmManager = DMSyncAlarmReceiver.getAlarmManager(context);
        if (alarmManager == null) return;
        PendingIntent pendingIntent = DMSyncAlarmReceiver.getPendingIntent(context);
        alarmManager.cancel(pendingIntent);
    }

    private static AlarmManager getAlarmManager(@NonNull Context context) {
        return (AlarmManager) context.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
    }

    private static PendingIntent getPendingIntent(@NonNull Context context) {
        Context applicationContext = context.getApplicationContext();
        Intent intent = new Intent(applicationContext, DMSyncAlarmReceiver.class);
        return PendingIntent.getBroadcast(applicationContext,
                                          Constants.DM_SYNC_SERVICE_REQUEST_CODE,
                                          intent,
                                          PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static long getIntervalMillis() {
        int amount = settingsHelper.getInteger(PreferenceKeys.PREF_ENABLE_DM_AUTO_REFRESH_FREQ_NUMBER);
        if (amount <= 0) {
            amount = 30;
        }
        String unit = settingsHelper.getString(PreferenceKeys.PREF_ENABLE_DM_AUTO_REFRESH_FREQ_UNIT);
        TemporalUnit temporalUnit;
        switch (unit) {
            case "mins":
                temporalUnit = ChronoUnit.MINUTES;
                break;
            default:
            case "secs":
                temporalUnit = ChronoUnit.SECONDS;
        }
        return Duration.of(amount, temporalUnit).toMillis();
    }
}