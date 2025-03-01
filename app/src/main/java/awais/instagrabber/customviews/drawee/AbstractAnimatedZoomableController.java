/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package awais.instagrabber.customviews.drawee;

import android.graphics.Matrix;
import android.graphics.PointF;

import androidx.annotation.Nullable;

import com.facebook.common.logging.FLog;

/**
 * Abstract class for ZoomableController that adds animation capabilities to
 * DefaultZoomableController.
 */
public abstract class AbstractAnimatedZoomableController extends DefaultZoomableController {

    private boolean mIsAnimating;
    private final float[] mStartValues = new float[9];
    private final float[] mStopValues = new float[9];
    private final float[] mCurrentValues = new float[9];
    private final Matrix mNewTransform = new Matrix();
    private final Matrix mWorkingTransform = new Matrix();

    public AbstractAnimatedZoomableController(final TransformGestureDetector transformGestureDetector) {
        super(transformGestureDetector);
    }

    @Override
    public void reset() {
        FLog.v(this.getLogTag(), "reset");
        this.stopAnimation();
        this.mWorkingTransform.reset();
        this.mNewTransform.reset();
        super.reset();
    }

    /**
     * Returns true if the zoomable transform is identity matrix, and the controller is idle.
     */
    @Override
    public boolean isIdentity() {
        return !this.isAnimating() && super.isIdentity();
    }

    /**
     * Zooms to the desired scale and positions the image so that the given image point corresponds to
     * the given view point.
     *
     * <p>If this method is called while an animation or gesture is already in progress, the current
     * animation or gesture will be stopped first.
     *
     * @param scale      desired scale, will be limited to {min, max} scale factor
     * @param imagePoint 2D point in image's relative coordinate system (i.e. 0 <= x, y <= 1)
     * @param viewPoint  2D point in view's absolute coordinate system
     */
    @Override
    public void zoomToPoint(final float scale, final PointF imagePoint, final PointF viewPoint) {
        this.zoomToPoint(scale, imagePoint, viewPoint, DefaultZoomableController.LIMIT_ALL, 0, null);
    }

    /**
     * Zooms to the desired scale and positions the image so that the given image point corresponds to
     * the given view point.
     *
     * <p>If this method is called while an animation or gesture is already in progress, the current
     * animation or gesture will be stopped first.
     *
     * @param scale               desired scale, will be limited to {min, max} scale factor
     * @param imagePoint          2D point in image's relative coordinate system (i.e. 0 <= x, y <= 1)
     * @param viewPoint           2D point in view's absolute coordinate system
     * @param limitFlags          whether to limit translation and/or scale.
     * @param durationMs          length of animation of the zoom, or 0 if no animation desired
     * @param onAnimationComplete code to run when the animation completes. Ignored if durationMs=0
     */
    public void zoomToPoint(
            final float scale,
            final PointF imagePoint,
            final PointF viewPoint,
            @LimitFlag final int limitFlags,
            final long durationMs,
            @Nullable final Runnable onAnimationComplete) {
        FLog.v(this.getLogTag(), "zoomToPoint: duration %d ms", durationMs);
        this.calculateZoomToPointTransform(this.mNewTransform, scale, imagePoint, viewPoint, limitFlags);
        this.setTransform(this.mNewTransform, durationMs, onAnimationComplete);
    }

    /**
     * Sets a new zoomable transformation and animates to it if desired.
     *
     * <p>If this method is called while an animation or gesture is already in progress, the current
     * animation or gesture will be stopped first.
     *
     * @param newTransform        new transform to make active
     * @param durationMs          duration of the animation, or 0 to not animate
     * @param onAnimationComplete code to run when the animation completes. Ignored if durationMs=0
     */
    public void setTransform(
            final Matrix newTransform, final long durationMs, @Nullable final Runnable onAnimationComplete) {
        FLog.v(this.getLogTag(), "setTransform: duration %d ms", durationMs);
        if (durationMs <= 0) {
            this.setTransformImmediate(newTransform);
        } else {
            this.setTransformAnimated(newTransform, durationMs, onAnimationComplete);
        }
    }

    private void setTransformImmediate(Matrix newTransform) {
        FLog.v(this.getLogTag(), "setTransformImmediate");
        this.stopAnimation();
        this.mWorkingTransform.set(newTransform);
        setTransform(newTransform);
        this.getDetector().restartGesture();
    }

    protected boolean isAnimating() {
        return this.mIsAnimating;
    }

    protected void setAnimating(final boolean isAnimating) {
        this.mIsAnimating = isAnimating;
    }

    protected float[] getStartValues() {
        return this.mStartValues;
    }

    protected float[] getStopValues() {
        return this.mStopValues;
    }

    protected Matrix getWorkingTransform() {
        return this.mWorkingTransform;
    }

    @Override
    public void onGestureBegin(final TransformGestureDetector detector) {
        FLog.v(this.getLogTag(), "onGestureBegin");
        this.stopAnimation();
        super.onGestureBegin(detector);
    }

    @Override
    public void onGestureUpdate(final TransformGestureDetector detector) {
        FLog.v(this.getLogTag(), "onGestureUpdate %s", this.isAnimating() ? "(ignored)" : "");
        if (this.isAnimating()) {
            return;
        }
        super.onGestureUpdate(detector);
    }

    protected void calculateInterpolation(final Matrix outMatrix, final float fraction) {
        for (int i = 0; i < 9; i++) {
            this.mCurrentValues[i] = (1 - fraction) * this.mStartValues[i] + fraction * this.mStopValues[i];
        }
        outMatrix.setValues(this.mCurrentValues);
    }

    public abstract void setTransformAnimated(
            Matrix newTransform, long durationMs, @Nullable Runnable onAnimationComplete);

    protected abstract void stopAnimation();

    protected abstract Class<?> getLogTag();
}
