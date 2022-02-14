package awais.instagrabber.customviews;

import android.animation.Animator;
import android.graphics.Rect;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.transition.ChangeBounds;
import androidx.transition.ChangeTransform;
import androidx.transition.Transition;
import androidx.transition.TransitionListenerAdapter;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import awais.instagrabber.utils.Utils;

public abstract class SharedElementTransitionDialogFragment extends DialogFragment {
    // private static final String TAG = "SETDialogFragment";
    private static final int DURATION = 200;

    private final Map<Integer, View> startViews = new HashMap<>();
    private final Map<Integer, View> destViews = new HashMap<>();
    private final Map<Integer, ViewBounds> viewBoundsMap = new HashMap<>();
    private final List<Animator> additionalAnimators = new ArrayList<>();
    private final Handler initialBoundsHandler = new Handler();

    private boolean startCalled;
    private boolean startInitiated;
    private int boundsCalculatedCount;

    protected int getAnimationDuration() {
        return SharedElementTransitionDialogFragment.DURATION;
    }

    public void addSharedElement(@NonNull View startView, @NonNull View destView) {
        int key = destView.hashCode();
        this.startViews.put(key, startView);
        this.destViews.put(key, destView);
        this.setupInitialBounds(startView, destView);
        // final View view = getView();
        // if (view == null) return;
        // view.post(() -> {});
    }

    public void startPostponedEnterTransition() {
        this.startCalled = true;
        if (this.startInitiated) return;
        if (this.boundsCalculatedCount < this.startViews.size()) return;
        this.startInitiated = true;
        Set<Integer> keySet = this.startViews.keySet();
        View view = this.getView();
        if (!(view instanceof ViewGroup)) return;
        TransitionSet transitionSet = new TransitionSet()
                .setDuration(SharedElementTransitionDialogFragment.DURATION)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .addTransition(new ChangeBounds())
                .addTransition(new ChangeTransform())
                .addListener(new TransitionListenerAdapter() {
                    @Override
                    public void onTransitionStart(@NonNull Transition transition) {
                        for (final Animator animator : SharedElementTransitionDialogFragment.this.additionalAnimators) {
                            animator.start();
                        }
                    }

                    @Override
                    public void onTransitionEnd(@NonNull Transition transition) {
                        for (Integer key : keySet) {
                            View startView = SharedElementTransitionDialogFragment.this.startViews.get(key);
                            View destView = SharedElementTransitionDialogFragment.this.destViews.get(key);
                            ViewBounds viewBounds = SharedElementTransitionDialogFragment.this.viewBoundsMap.get(key);
                            if (startView == null || destView == null || viewBounds == null) return;
                            SharedElementTransitionDialogFragment.this.onEndSharedElementAnimation(startView, destView, viewBounds);
                        }
                    }
                });
        view.post(() -> {
            TransitionManager.beginDelayedTransition((ViewGroup) view, transitionSet);
            for (Integer key : keySet) {
                View startView = this.startViews.get(key);
                View destView = this.destViews.get(key);
                ViewBounds viewBounds = this.viewBoundsMap.get(key);
                if (startView == null || destView == null || viewBounds == null) return;
                this.onBeforeSharedElementAnimation(startView, destView, viewBounds);
                this.setDestBounds(key);
            }
        });
    }

    private void setDestBounds(int key) {
        View startView = this.startViews.get(key);
        if (startView == null) return;
        View destView = this.destViews.get(key);
        if (destView == null) return;
        ViewBounds viewBounds = this.viewBoundsMap.get(key);
        if (viewBounds == null) return;
        destView.setX((int) viewBounds.getDestX());
        destView.setY((int) viewBounds.getDestY());
        destView.setTranslationX(0);
        destView.setTranslationY(0);
        ViewGroup.LayoutParams layoutParams = destView.getLayoutParams();
        layoutParams.height = viewBounds.getDestHeight();
        layoutParams.width = viewBounds.getDestWidth();
        destView.requestLayout();
    }

    protected void onBeforeSharedElementAnimation(@NonNull View startView,
                                                  @NonNull View destView,
                                                  @NonNull ViewBounds viewBounds) {}

    protected void onEndSharedElementAnimation(@NonNull View startView,
                                               @NonNull View destView,
                                               @NonNull ViewBounds viewBounds) {}

    private void setupInitialBounds(@NonNull View startView, @NonNull View destView) {
        ViewTreeObserver.OnPreDrawListener preDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            private boolean firstPassDone;

            @Override
            public boolean onPreDraw() {
                destView.getViewTreeObserver().removeOnPreDrawListener(this);
                if (!this.firstPassDone) {
                    SharedElementTransitionDialogFragment.this.getViewBounds(startView, destView, this);
                    this.firstPassDone = true;
                    return false;
                }
                int[] location = new int[2];
                startView.getLocationOnScreen(location);
                int initX = location[0];
                int initY = location[1];
                destView.setX(initX);
                destView.setY(initY - Utils.getStatusBarHeight(SharedElementTransitionDialogFragment.this.getContext()));
                SharedElementTransitionDialogFragment.this.boundsCalculatedCount++;
                if (SharedElementTransitionDialogFragment.this.startCalled) {
                    SharedElementTransitionDialogFragment.this.startPostponedEnterTransition();
                }
                return true;
            }
        };
        destView.getViewTreeObserver().addOnPreDrawListener(preDrawListener);
    }

    private void getViewBounds(@NonNull View startView,
                               @NonNull View destView,
                               @NonNull ViewTreeObserver.OnPreDrawListener preDrawListener) {
        ViewBounds viewBounds = new ViewBounds();
        viewBounds.setDestWidth(destView.getWidth());
        viewBounds.setDestHeight(destView.getHeight());
        viewBounds.setDestX(destView.getX());
        viewBounds.setDestY(destView.getY());

        Rect destBounds = new Rect();
        destView.getDrawingRect(destBounds);
        viewBounds.setDestBounds(destBounds);

        ViewGroup.LayoutParams layoutParams = destView.getLayoutParams();

        Rect startBounds = new Rect();
        startView.getDrawingRect(startBounds);
        viewBounds.setStartBounds(startBounds);

        int key = destView.hashCode();
        this.viewBoundsMap.put(key, viewBounds);

        layoutParams.height = startView.getHeight();
        layoutParams.width = startView.getWidth();

        destView.getViewTreeObserver().addOnPreDrawListener(preDrawListener);
        destView.requestLayout();
    }

    // private void animateBounds(@NonNull final View startView,
    //                            @NonNull final View destView,
    //                            @NonNull final ViewBounds viewBounds) {
    //     final ValueAnimator heightAnimator = ObjectAnimator.ofInt(startView.getHeight(), viewBounds.getDestHeight());
    //     final ValueAnimator widthAnimator = ObjectAnimator.ofInt(startView.getWidth(), viewBounds.getDestWidth());
    //     heightAnimator.setDuration(DURATION);
    //     widthAnimator.setDuration(DURATION);
    //     additionalAnimators.add(heightAnimator);
    //     additionalAnimators.add(widthAnimator);
    //     heightAnimator.addUpdateListener(animation -> {
    //         ViewGroup.LayoutParams params = destView.getLayoutParams();
    //         params.height = (int) animation.getAnimatedValue();
    //         destView.requestLayout();
    //     });
    //     widthAnimator.addUpdateListener(animation -> {
    //         ViewGroup.LayoutParams params = destView.getLayoutParams();
    //         params.width = (int) animation.getAnimatedValue();
    //         destView.requestLayout();
    //     });
    //     onBeforeSharedElementAnimation(startView, destView, viewBounds);
    //     final float destX = viewBounds.getDestX();
    //     final float destY = viewBounds.getDestY();
    //     final AnimatorSet animatorSet = new AnimatorSet();
    //     animatorSet.addListener(new AnimatorListenerAdapter() {
    //         @Override
    //         public void onAnimationEnd(final Animator animation) {
    //             animationEnded(startView, destView, viewBounds);
    //         }
    //     });
    //
    //     destView.animate()
    //             .x(destX)
    //             .y(destY)
    //             .setDuration(DURATION)
    //             .withStartAction(() -> {
    //                 if (!additionalAnimatorsStarted && additionalAnimators.size() > 0) {
    //                     additionalAnimatorsStarted = true;
    //                     animatorSet.playTogether(additionalAnimators);
    //                     animatorSet.start();
    //                 }
    //             })
    //             .withEndAction(() -> animationEnded(startView, destView, viewBounds))
    //             .start();
    // }

    // private int endCount = 0;
    // private void animationEnded(final View startView, final View destView, final ViewBounds viewBounds) {
    //     ++endCount;
    //     if (endCount != startViews.size() + 1) return;
    //     onEndSharedElementAnimation(startView, destView, viewBounds);
    // }

    protected void addAnimator(@NonNull Animator animator) {
        this.additionalAnimators.add(animator);
    }

    protected static class ViewBounds {
        private float destY;
        private float destX;
        private int destHeight;
        private int destWidth;
        private Rect startBounds;
        private Rect destBounds;

        public ViewBounds() {}

        public float getDestY() {
            return this.destY;
        }

        public void setDestY(float destY) {
            this.destY = destY;
        }

        public float getDestX() {
            return this.destX;
        }

        public void setDestX(float destX) {
            this.destX = destX;
        }

        public int getDestHeight() {
            return this.destHeight;
        }

        public void setDestHeight(int destHeight) {
            this.destHeight = destHeight;
        }

        public int getDestWidth() {
            return this.destWidth;
        }

        public void setDestWidth(int destWidth) {
            this.destWidth = destWidth;
        }

        public Rect getStartBounds() {
            return this.startBounds;
        }

        public void setStartBounds(Rect startBounds) {
            this.startBounds = startBounds;
        }

        public Rect getDestBounds() {
            return this.destBounds;
        }

        public void setDestBounds(Rect destBounds) {
            this.destBounds = destBounds;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.startViews.clear();
        this.destViews.clear();
        this.viewBoundsMap.clear();
        this.additionalAnimators.clear();
    }
}
