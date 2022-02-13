package awais.instagrabber.customviews.helpers;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import awais.instagrabber.interfaces.SwipeEvent;

public final class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
    public static final int SWIPE_THRESHOLD = 200;
    public static final int SWIPE_VELOCITY_THRESHOLD = 200;
    private final SwipeEvent swipeEvent;

    public SwipeGestureListener(SwipeEvent swipeEvent) {
        this.swipeEvent = swipeEvent;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        try {
            float diffY = e2.getY() - e1.getY();
            float diffX = e2.getX() - e1.getX();
            float diffXAbs = Math.abs(diffX);
            if (diffXAbs > Math.abs(diffY) && diffXAbs > SwipeGestureListener.SWIPE_THRESHOLD && Math.abs(velocityX) > SwipeGestureListener.SWIPE_VELOCITY_THRESHOLD) {
                this.swipeEvent.onSwipe(diffX > 0);
                return true;
            }
        } catch (Exception e) {
            Log.e("AWAISKING_APP", "", e);
        }
        return false;
    }
}
