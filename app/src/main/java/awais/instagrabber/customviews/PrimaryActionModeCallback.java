package awais.instagrabber.customviews;

import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

public class PrimaryActionModeCallback implements ActionMode.Callback {
    private ActionMode mode;
    private final int menuRes;
    private final Callbacks callbacks;

    public PrimaryActionModeCallback(int menuRes, Callbacks callbacks) {
        this.menuRes = menuRes;
        this.callbacks = callbacks;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        this.mode = mode;
        mode.getMenuInflater().inflate(this.menuRes, menu);
        if (this.callbacks != null) {
            this.callbacks.onCreate(mode, menu);
        }
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (this.callbacks != null) {
            return this.callbacks.onActionItemClicked(mode, item);
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        if (this.callbacks != null) {
            this.callbacks.onDestroy(mode);
        }
        this.mode = null;
    }

    public abstract static class CallbacksHelper implements Callbacks {
        public void onCreate(ActionMode mode, Menu menu) {

        }

        @Override
        public void onDestroy(ActionMode mode) {

        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }
    }

    public interface Callbacks {
        void onCreate(ActionMode mode, Menu menu);

        void onDestroy(ActionMode mode);

        boolean onActionItemClicked(ActionMode mode, MenuItem item);
    }
}
