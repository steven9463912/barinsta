/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package awais.instagrabber.customviews.drawee;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.graphics.Matrix;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.logging.FLog;


/**
 * ZoomableController that adds animation capabilities to DefaultZoomableController using standard
 * Android animation classes
 */
public class AnimatedZoomableController extends AbstractAnimatedZoomableController {

    private static final Class<?> TAG = AnimatedZoomableController.class;

    private final ValueAnimator mValueAnimator;

    public static AnimatedZoomableController newInstance() {
        return new AnimatedZoomableController(TransformGestureDetector.newInstance());
    }

    @SuppressLint("NewApi")
    public AnimatedZoomableController(final TransformGestureDetector transformGestureDetector) {
        super(transformGestureDetector);
        this.mValueAnimator = ValueAnimator.ofFloat(0, 1);
        this.mValueAnimator.setInterpolator(new DecelerateInterpolator());
    }

    @SuppressLint("NewApi")
    @Override
    public void setTransformAnimated(
            Matrix newTransform, final long durationMs, @Nullable Runnable onAnimationComplete) {
        FLog.v(this.getLogTag(), "setTransformAnimated: duration %d ms", durationMs);
        this.stopAnimation();
        Preconditions.checkArgument(durationMs > 0);
        Preconditions.checkState(!this.isAnimating());
        this.setAnimating(true);
        this.mValueAnimator.setDuration(durationMs);
        this.getTransform().getValues(this.getStartValues());
        newTransform.getValues(this.getStopValues());
        this.mValueAnimator.addUpdateListener(
                new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(final ValueAnimator valueAnimator) {
                        AnimatedZoomableController.this.calculateInterpolation(AnimatedZoomableController.this.getWorkingTransform(), (float) valueAnimator.getAnimatedValue());
                        AnimatedZoomableController.super.setTransform(AnimatedZoomableController.this.getWorkingTransform());
                    }
                });
        this.mValueAnimator.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationCancel(final Animator animation) {
                        FLog.v(AnimatedZoomableController.this.getLogTag(), "setTransformAnimated: animation cancelled");
                        this.onAnimationStopped();
                    }

                    @Override
                    public void onAnimationEnd(final Animator animation) {
                        FLog.v(AnimatedZoomableController.this.getLogTag(), "setTransformAnimated: animation finished");
                        this.onAnimationStopped();
                    }

                    private void onAnimationStopped() {
                        if (onAnimationComplete != null) {
                            onAnimationComplete.run();
                        }
                        AnimatedZoomableController.this.setAnimating(false);
                        AnimatedZoomableController.this.getDetector().restartGesture();
                    }
                });
        this.mValueAnimator.start();
    }

    @SuppressLint("NewApi")
    @Override
    public void stopAnimation() {
        if (!this.isAnimating()) {
            return;
        }
        FLog.v(this.getLogTag(), "stopAnimation");
        this.mValueAnimator.cancel();
        this.mValueAnimator.removeAllUpdateListeners();
        this.mValueAnimator.removeAllListeners();
    }

    @Override
    protected Class<?> getLogTag() {
        return AnimatedZoomableController.TAG;
    }
}
