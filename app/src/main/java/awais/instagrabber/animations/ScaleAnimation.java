package awais.instagrabber.animations;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

public class ScaleAnimation {

    private final View view;

    public ScaleAnimation(final View view) {
        this.view = view;
    }


    public void start() {
        final AnimatorSet set = new AnimatorSet();
        final ObjectAnimator scaleY = ObjectAnimator.ofFloat(this.view, "scaleY", 2.0f);

        final ObjectAnimator scaleX = ObjectAnimator.ofFloat(this.view, "scaleX", 2.0f);
        set.setDuration(150);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.playTogether(scaleY, scaleX);
        set.start();
    }

    public void stop() {
        final AnimatorSet set = new AnimatorSet();
        final ObjectAnimator scaleY = ObjectAnimator.ofFloat(this.view, "scaleY", 1.0f);
        //        scaleY.setDuration(250);
        //        scaleY.setInterpolator(new DecelerateInterpolator());


        final ObjectAnimator scaleX = ObjectAnimator.ofFloat(this.view, "scaleX", 1.0f);
        //        scaleX.setDuration(250);
        //        scaleX.setInterpolator(new DecelerateInterpolator());


        set.setDuration(150);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.playTogether(scaleY, scaleX);
        set.start();
    }
}
