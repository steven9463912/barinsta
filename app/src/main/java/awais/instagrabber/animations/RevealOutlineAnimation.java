package awais.instagrabber.animations;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Outline;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewOutlineProvider;

/**
 * A {@link ViewOutlineProvider} that has helper functions to create reveal animations.
 * This class should be extended so that subclasses can define the reveal shape as the
 * animation progresses from 0 to 1.
 */
public abstract class RevealOutlineAnimation extends ViewOutlineProvider {
    protected Rect mOutline;
    protected float mOutlineRadius;

    public RevealOutlineAnimation() {
        this.mOutline = new Rect();
    }

    /**
     * Returns whether elevation should be removed for the duration of the reveal animation.
     */
    abstract boolean shouldRemoveElevationDuringAnimation();

    /**
     * Sets the progress, from 0 to 1, of the reveal animation.
     */
    abstract void setProgress(float progress);

    public ValueAnimator createRevealAnimator(View revealView, final boolean isReversed) {
        final ValueAnimator va =
                isReversed ? ValueAnimator.ofFloat(1f, 0f) : ValueAnimator.ofFloat(0f, 1f);
        float elevation = revealView.getElevation();

        va.addListener(new AnimatorListenerAdapter() {
            private boolean mIsClippedToOutline;
            private ViewOutlineProvider mOldOutlineProvider;

            public void onAnimationStart(final Animator animation) {
                this.mIsClippedToOutline = revealView.getClipToOutline();
                this.mOldOutlineProvider = revealView.getOutlineProvider();

                revealView.setOutlineProvider(RevealOutlineAnimation.this);
                revealView.setClipToOutline(true);
                if (RevealOutlineAnimation.this.shouldRemoveElevationDuringAnimation()) {
                    revealView.setTranslationZ(-elevation);
                }
            }

            public void onAnimationEnd(final Animator animation) {
                revealView.setOutlineProvider(this.mOldOutlineProvider);
                revealView.setClipToOutline(this.mIsClippedToOutline);
                if (RevealOutlineAnimation.this.shouldRemoveElevationDuringAnimation()) {
                    revealView.setTranslationZ(0);
                }
            }

        });

        va.addUpdateListener(v -> {
            final float progress = (Float) v.getAnimatedValue();
            this.setProgress(progress);
            revealView.invalidateOutline();
        });
        return va;
    }

    @Override
    public void getOutline(final View v, final Outline outline) {
        outline.setRoundRect(this.mOutline, this.mOutlineRadius);
    }

    public float getRadius() {
        return this.mOutlineRadius;
    }

    public void getOutline(final Rect out) {
        out.set(this.mOutline);
    }
}
