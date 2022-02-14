package awais.instagrabber.animations;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

public class ResizeAnimation extends Animation {
    private static final String TAG = "ResizeAnimation";

    final View view;
    final int startHeight;
    final int targetHeight;
    final int startWidth;
    final int targetWidth;

    public ResizeAnimation(View view,
                           int startHeight,
                           int startWidth,
                           int targetHeight,
                           int targetWidth) {
        this.view = view;
        this.startHeight = startHeight;
        this.targetHeight = targetHeight;
        this.startWidth = startWidth;
        this.targetWidth = targetWidth;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        // Log.d(TAG, "applyTransformation: interpolatedTime: " + interpolatedTime);
        this.view.getLayoutParams().height = (int) (this.startHeight + (this.targetHeight - this.startHeight) * interpolatedTime);
        this.view.getLayoutParams().width = (int) (this.startWidth + (this.targetWidth - this.startWidth) * interpolatedTime);
        this.view.requestLayout();
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);
    }

    @Override
    public boolean willChangeBounds() {
        return true;
    }
}
