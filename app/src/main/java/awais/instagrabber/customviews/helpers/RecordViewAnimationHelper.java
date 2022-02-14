package awais.instagrabber.customviews.helpers;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;
import androidx.vectordrawable.graphics.drawable.AnimatorInflaterCompat;

import awais.instagrabber.R;
import awais.instagrabber.customviews.RecordButton;
import awais.instagrabber.customviews.RecordView;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class RecordViewAnimationHelper {
    private static final String TAG = RecordViewAnimationHelper.class.getSimpleName();
    private final Context context;
    private final AnimatedVectorDrawableCompat animatedVectorDrawable;
    private final ImageView basketImg;
    private final ImageView smallBlinkingMic;
    private AlphaAnimation alphaAnimation;
    private RecordView.OnBasketAnimationEnd onBasketAnimationEndListener;
    private boolean isBasketAnimating;
    private boolean isStartRecorded;
    private float micX;
    private float micY;
    private AnimatorSet micAnimation;
    private TranslateAnimation translateAnimation1, translateAnimation2;
    private Handler handler1, handler2;

    public RecordViewAnimationHelper(final Context context, final AppCompatImageView basketImg, final AppCompatImageView smallBlinkingMic) {
        this.context = context;
        this.smallBlinkingMic = smallBlinkingMic;
        this.basketImg = basketImg;
        this.animatedVectorDrawable = AnimatedVectorDrawableCompat.create(context, R.drawable.recv_basket_animated);
    }

    @SuppressLint("RestrictedApi")
    public void animateBasket(final float basketInitialY) {
        this.isBasketAnimating = true;

        this.clearAlphaAnimation(false);

        //save initial x,y values for mic icon
        if (this.micX == 0) {
            this.micX = this.smallBlinkingMic.getX();
            this.micY = this.smallBlinkingMic.getY();
        }

        this.micAnimation = (AnimatorSet) AnimatorInflaterCompat.loadAnimator(this.context, R.animator.delete_mic_animation);
        this.micAnimation.setTarget(this.smallBlinkingMic); // set the view you want to animate

        this.translateAnimation1 = new TranslateAnimation(0, 0, basketInitialY, basketInitialY - 90);
        this.translateAnimation1.setDuration(250);

        this.translateAnimation2 = new TranslateAnimation(0, 0, basketInitialY - 90, basketInitialY);
        this.translateAnimation2.setDuration(350);

        this.micAnimation.start();
        this.basketImg.setImageDrawable(this.animatedVectorDrawable);

        this.handler1 = new Handler();
        this.handler1.postDelayed(() -> {
            this.basketImg.setVisibility(VISIBLE);
            this.basketImg.startAnimation(this.translateAnimation1);
        }, 350);

        this.translateAnimation1.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(final Animation animation) {}

            @Override
            public void onAnimationEnd(final Animation animation) {
                RecordViewAnimationHelper.this.animatedVectorDrawable.start();
                RecordViewAnimationHelper.this.handler2 = new Handler();
                RecordViewAnimationHelper.this.handler2.postDelayed(() -> {
                    RecordViewAnimationHelper.this.basketImg.startAnimation(RecordViewAnimationHelper.this.translateAnimation2);
                    RecordViewAnimationHelper.this.smallBlinkingMic.setVisibility(INVISIBLE);
                    RecordViewAnimationHelper.this.basketImg.setVisibility(INVISIBLE);
                }, 450);
            }

            @Override
            public void onAnimationRepeat(final Animation animation) {}
        });

        this.translateAnimation2.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(final Animation animation) {}

            @Override
            public void onAnimationEnd(final Animation animation) {
                RecordViewAnimationHelper.this.basketImg.setVisibility(INVISIBLE);
                RecordViewAnimationHelper.this.isBasketAnimating = false;
                //if the user pressed the record button while the animation is running
                // then do NOT call on Animation end
                if (RecordViewAnimationHelper.this.onBasketAnimationEndListener != null && !RecordViewAnimationHelper.this.isStartRecorded) {
                    RecordViewAnimationHelper.this.onBasketAnimationEndListener.onAnimationEnd();
                }
            }

            @Override
            public void onAnimationRepeat(final Animation animation) {}
        });
    }

    //if the user started a new Record while the Animation is running
    // then we want to stop the current animation and revert views back to default state
    public void resetBasketAnimation() {
        if (this.isBasketAnimating) {
            this.translateAnimation1.reset();
            this.translateAnimation1.cancel();
            this.translateAnimation2.reset();
            this.translateAnimation2.cancel();
            this.micAnimation.cancel();
            this.smallBlinkingMic.clearAnimation();
            this.basketImg.clearAnimation();
            if (this.handler1 != null) {
                this.handler1.removeCallbacksAndMessages(null);
            }
            if (this.handler2 != null) {
                this.handler2.removeCallbacksAndMessages(null);
            }
            this.basketImg.setVisibility(INVISIBLE);
            this.smallBlinkingMic.setX(this.micX);
            this.smallBlinkingMic.setY(this.micY);
            this.smallBlinkingMic.setVisibility(View.GONE);
            this.isBasketAnimating = false;
        }
    }

    public void clearAlphaAnimation(final boolean hideView) {
        if (this.alphaAnimation != null) {
            this.alphaAnimation.cancel();
            this.alphaAnimation.reset();
        }
        this.smallBlinkingMic.clearAnimation();
        if (hideView) {
            this.smallBlinkingMic.setVisibility(View.GONE);
        }
    }

    public void animateSmallMicAlpha() {
        this.alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
        this.alphaAnimation.setDuration(500);
        this.alphaAnimation.setRepeatMode(Animation.REVERSE);
        this.alphaAnimation.setRepeatCount(Animation.INFINITE);
        this.smallBlinkingMic.startAnimation(this.alphaAnimation);
    }

    public void moveRecordButtonAndSlideToCancelBack(RecordButton recordBtn, final View slideToCancelLayout, final float initialX, final float difX) {
        ValueAnimator positionAnimator = ValueAnimator.ofFloat(recordBtn.getX(), initialX);
        positionAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        positionAnimator.addUpdateListener(animation -> {
            final float x = (Float) animation.getAnimatedValue();
            recordBtn.setX(x);
        });
        recordBtn.stopScale();
        positionAnimator.setDuration(200);
        positionAnimator.start();

        // if the move event was not called ,then the difX will still 0 and there is no need to move it back
        if (difX != 0) {
            final float x = initialX - difX;
            slideToCancelLayout.animate()
                               .x(x)
                               .setDuration(0)
                               .start();
        }
    }

    public void resetSmallMic() {
        this.smallBlinkingMic.setAlpha(1.0f);
        this.smallBlinkingMic.setScaleX(1.0f);
        this.smallBlinkingMic.setScaleY(1.0f);
    }

    public void setOnBasketAnimationEndListener(RecordView.OnBasketAnimationEnd onBasketAnimationEndListener) {
        this.onBasketAnimationEndListener = onBasketAnimationEndListener;

    }

    public void onAnimationEnd() {
        if (onBasketAnimationEndListener != null) {
            onBasketAnimationEndListener.onAnimationEnd();
        }
    }

    //check if the user started a new Record by pressing the RecordButton
    public void setStartRecorded(boolean startRecorded) {
        isStartRecorded = startRecorded;
    }

}
