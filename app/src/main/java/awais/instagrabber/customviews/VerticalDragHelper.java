package awais.instagrabber.customviews;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;

import androidx.annotation.NonNull;

public class VerticalDragHelper {
    // private static final String TAG = "VerticalDragHelper";
    private static final double SWIPE_THRESHOLD_VELOCITY = 80;

    private final View view;

    private GestureDetector gestureDetector;
    private Context context;
    private double flingVelocity;
    private OnVerticalDragListener onVerticalDragListener;

    private final GestureDetector.OnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            VerticalDragHelper.this.view.performClick();
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            final double yDir = e1.getY() - e2.getY();
            // Log.d(TAG, "onFling: yDir: " + yDir);
            if (yDir < -VerticalDragHelper.SWIPE_THRESHOLD_VELOCITY || yDir > VerticalDragHelper.SWIPE_THRESHOLD_VELOCITY) {
                VerticalDragHelper.this.flingVelocity = yDir;
            }
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    };

    private float prevRawY;
    private boolean isDragging;
    private float prevRawX;
    private float dX;
    private float prevDY;

    public VerticalDragHelper(@NonNull View view) {
        this.view = view;
        Context context = view.getContext();
        if (context == null) return;
        this.context = context;
        this.init();
    }

    public void setOnVerticalDragListener(@NonNull OnVerticalDragListener onVerticalDragListener) {
        this.onVerticalDragListener = onVerticalDragListener;
    }

    protected void init() {
        this.gestureDetector = new GestureDetector(this.context, this.gestureListener);
    }

    public boolean onDragTouch(MotionEvent event) {
        if (this.onVerticalDragListener == null) {
            return false;
        }
        if (this.gestureDetector.onTouchEvent(event)) {
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_MOVE:
                boolean handled = false;
                float rawY = event.getRawY();
                float dY = rawY - this.prevRawY;
                if (!this.isDragging) {
                    float rawX = event.getRawX();
                    if (this.prevRawX != 0) {
                        this.dX = rawX - this.prevRawX;
                    }
                    this.prevRawX = rawX;
                    if (this.prevRawY != 0) {
                        float dYAbs = Math.abs(dY - this.prevDY);
                        if (!this.isDragging && dYAbs < 50) {
                            float abs = Math.abs(dY) - Math.abs(this.dX);
                            if (abs > 0) {
                                this.isDragging = true;
                            }
                        }
                    }
                }
                if (this.isDragging) {
                    ViewParent parent = this.view.getParent();
                    parent.requestDisallowInterceptTouchEvent(true);
                    this.onVerticalDragListener.onDrag(dY);
                    handled = true;
                }
                this.prevDY = dY;
                this.prevRawY = rawY;
                return handled;
            case MotionEvent.ACTION_UP:
                // Log.d(TAG, "onDragTouch: reset prevRawY");
                this.prevRawY = 0;
                if (this.flingVelocity != 0) {
                    this.onVerticalDragListener.onFling(this.flingVelocity);
                    this.flingVelocity = 0;
                    this.isDragging = false;
                    return true;
                }
                if (this.isDragging) {
                    this.onVerticalDragListener.onDragEnd();
                    this.isDragging = false;
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    public boolean isDragging() {
        return this.isDragging;
    }

    public boolean onGestureTouchEvent(MotionEvent event) {
        return this.gestureDetector.onTouchEvent(event);
    }

    public interface OnVerticalDragListener {
        void onDrag(float dY);

        void onDragEnd();

        void onFling(double flingVelocity);
    }
}
