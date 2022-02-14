package awais.instagrabber.customviews.helpers;

import android.app.Activity;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.PopupWindow;

public class HeightProvider extends PopupWindow implements ViewTreeObserver.OnGlobalLayoutListener {
    private final Activity mActivity;
    private final View rootView;
    private HeightListener listener;
    private int heightMax;

    public HeightProvider(final Activity activity) {
        super(activity);
        mActivity = activity;

        this.rootView = new View(activity);
        this.setContentView(this.rootView);

        this.rootView.getViewTreeObserver().addOnGlobalLayoutListener(this);
        this.setBackgroundDrawable(new ColorDrawable(0));

        this.setWidth(0);
        this.setHeight(ViewGroup.LayoutParams.MATCH_PARENT);

        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        this.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
    }

    public HeightProvider init() {
        if (!this.isShowing()) {
            View view = this.mActivity.getWindow().getDecorView();
            view.post(() -> this.showAtLocation(view, Gravity.NO_GRAVITY, 0, 0));
        }
        return this;
    }

    public HeightProvider setHeightListener(final HeightListener listener) {
        this.listener = listener;
        return this;
    }

    @Override
    public void onGlobalLayout() {
        final Rect rect = new Rect();
        this.rootView.getWindowVisibleDisplayFrame(rect);
        if (rect.bottom > this.heightMax) {
            this.heightMax = rect.bottom;
        }
        
        final int keyboardHeight = this.heightMax - rect.bottom;
        if (this.listener != null) {
            this.listener.onHeightChanged(keyboardHeight);
        }
    }

    public interface HeightListener {
        void onHeightChanged(int height);
    }
}

