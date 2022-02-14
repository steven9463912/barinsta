package awais.instagrabber.customviews;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DirectItemFrameLayout extends FrameLayout {
    private static final String TAG = DirectItemFrameLayout.class.getSimpleName();

    private boolean longPressed;
    private float touchX;
    private float touchY;
    private OnItemLongClickListener onItemLongClickListener;
    private int touchSlop;

    private final Handler handler = new Handler();
    private final Runnable longPressRunnable = () -> {
        this.longPressed = true;
        if (this.onItemLongClickListener != null) {
            this.onItemLongClickListener.onLongClick(this, this.touchX, this.touchY);
        }
    };
    private final Runnable longPressStartRunnable = () -> {
        if (this.onItemLongClickListener != null) {
            this.onItemLongClickListener.onLongClickStart(this);
        }
    };

    public DirectItemFrameLayout(@NonNull Context context) {
        super(context);
        this.init(context);
    }

    public DirectItemFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.init(context);
    }

    public DirectItemFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init(context);
    }

    public DirectItemFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.init(context);
    }

    private void init(Context context) {
        final ViewConfiguration vc = ViewConfiguration.get(context);
        this.touchSlop = vc.getScaledTouchSlop();
    }

    public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
        this.onItemLongClickListener = onItemLongClickListener;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                this.longPressed = false;
                this.handler.postDelayed(this.longPressRunnable, ViewConfiguration.getLongPressTimeout());
                this.handler.postDelayed(this.longPressStartRunnable, ViewConfiguration.getTapTimeout());
                this.touchX = ev.getRawX();
                this.touchY = ev.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                float diffX = this.touchX - ev.getRawX();
                float diffXAbs = Math.abs(diffX);
                boolean isMoved = diffXAbs > this.touchSlop || Math.abs(this.touchY - ev.getRawY()) > this.touchSlop;
                if (this.longPressed || isMoved) {
                    this.handler.removeCallbacks(this.longPressStartRunnable);
                    this.handler.removeCallbacks(this.longPressRunnable);
                    if (!this.longPressed) {
                        if (this.onItemLongClickListener != null) {
                            this.onItemLongClickListener.onLongClickCancel(this);
                        }
                    }
                    // if (diffXAbs > touchSlop) {
                    //     setTranslationX(-diffX);
                    // }
                }
                break;
            case MotionEvent.ACTION_UP:
                this.handler.removeCallbacks(this.longPressRunnable);
                this.handler.removeCallbacks(this.longPressStartRunnable);
                if (this.longPressed) {
                    return true;
                }
                if (this.onItemLongClickListener != null) {
                    this.onItemLongClickListener.onLongClickCancel(this);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                this.handler.removeCallbacks(this.longPressRunnable);
                this.handler.removeCallbacks(this.longPressStartRunnable);
                if (this.onItemLongClickListener != null) {
                    this.onItemLongClickListener.onLongClickCancel(this);
                }
                break;
        }
        boolean dispatchTouchEvent = super.dispatchTouchEvent(ev);
        if (ev.getAction() == MotionEvent.ACTION_DOWN && !dispatchTouchEvent) {
            return true;
        }
        return dispatchTouchEvent;
    }

    public interface OnItemLongClickListener {
        void onLongClickStart(View view);

        void onLongClickCancel(View view);

        void onLongClick(View view, float x, float y);
    }
}
