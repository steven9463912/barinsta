package awais.instagrabber.broadcasts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DMRefreshBroadcastReceiver extends BroadcastReceiver {
    public static final String ACTION_REFRESH_DM = "action_refresh_dm";
    private final OnDMRefreshCallback callback;

    public DMRefreshBroadcastReceiver(OnDMRefreshCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (this.callback == null) return;
        String action = intent.getAction();
        if (action == null) return;
        if (!action.equals(DMRefreshBroadcastReceiver.ACTION_REFRESH_DM)) return;
        this.callback.onReceive();
    }

    public interface OnDMRefreshCallback {
        void onReceive();
    }
}
