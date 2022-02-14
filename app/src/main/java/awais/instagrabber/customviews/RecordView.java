package awais.instagrabber.customviews;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.drawable.DrawableCompat;

import java.io.IOException;

import awais.instagrabber.R;
import awais.instagrabber.customviews.helpers.RecordViewAnimationHelper;
import awais.instagrabber.databinding.RecordViewLayoutBinding;
import awais.instagrabber.utils.Utils;

/**
 * Created by Devlomi on 24/08/2017.
 */

public class RecordView extends RelativeLayout {
    private static final String TAG = RecordView.class.getSimpleName();

    public static final int DEFAULT_CANCEL_BOUNDS = 8; //8dp
    // private ImageView smallBlinkingMic;
    // private ImageView basketImg;
    // private Chronometer counterTime;
    // private TextView slideToCancel;
    // private LinearLayout slideToCancelLayout;
    private float initialX;
    private float basketInitialY;
    private float difX;
    private float cancelBounds = RecordView.DEFAULT_CANCEL_BOUNDS;
    private long startTime;
    private final Context context;
    private OnRecordListener onRecordListener;
    private boolean isSwiped;
    private boolean isLessThanMinAllowed;
    private boolean isSoundEnabled = true;
    private int RECORD_START = R.raw.record_start;
    private int RECORD_FINISHED = R.raw.record_finished;
    private int RECORD_ERROR = R.raw.record_error;
    private RecordViewAnimationHelper recordViewAnimationHelper;
    private RecordViewLayoutBinding binding;
    private int minMillis = 1000;


    public RecordView(final Context context) {
        super(context);
        this.context = context;
        this.init(context, null, -1, -1);
    }


    public RecordView(final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.init(context, attrs, -1, -1);
    }

    public RecordView(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        this.init(context, attrs, defStyleAttr, -1);
    }

    private void init(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        this.binding = RecordViewLayoutBinding.inflate(LayoutInflater.from(context), this, false);
        this.addView(this.binding.getRoot());
        this.hideViews(true);
        if (attrs != null && defStyleAttr == -1 && defStyleRes == -1) {
            final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RecordView, -1, -1);
            final int slideArrowResource = typedArray.getResourceId(R.styleable.RecordView_slide_to_cancel_arrow, -1);
            final String slideToCancelText = typedArray.getString(R.styleable.RecordView_slide_to_cancel_text);
            final int slideToCancelTextColor = typedArray.getResourceId(R.styleable.RecordView_slide_to_cancel_text_color, -1);
            final int slideMarginRight = (int) typedArray.getDimension(R.styleable.RecordView_slide_to_cancel_margin_right, 30);
            final int counterTimeColor = typedArray.getResourceId(R.styleable.RecordView_counter_time_color, -1);
            final int arrowColor = typedArray.getResourceId(R.styleable.RecordView_slide_to_cancel_arrow_color, -1);
            final int cancelBounds = typedArray.getDimensionPixelSize(R.styleable.RecordView_slide_to_cancel_bounds, -1);
            if (cancelBounds != -1) {
                this.setCancelBounds(cancelBounds, false);//don't convert it to pixels since it's already in pixels
            }
            if (slideToCancelText != null) {
                this.setSlideToCancelText(slideToCancelText);
            }
            if (slideToCancelTextColor != -1) {
                this.setSlideToCancelTextColor(this.getResources().getColor(slideToCancelTextColor));
            }
            if (slideArrowResource != -1) {
                this.setSlideArrowDrawable(slideArrowResource);
            }
            if (arrowColor != -1) {
                this.setSlideToCancelArrowColor(this.getResources().getColor(arrowColor));
            }
            if (counterTimeColor != -1) {
                this.setCounterTimeColor(this.getResources().getColor(counterTimeColor));
            }
            this.setMarginRight(slideMarginRight, true);
            typedArray.recycle();
        }
        this.recordViewAnimationHelper = new RecordViewAnimationHelper(context, this.binding.basketImg, this.binding.glowingMic);
    }

    private void hideViews(final boolean hideSmallMic) {
        this.binding.slideToCancel.setVisibility(View.GONE);
        this.binding.basketImg.setVisibility(View.GONE);
        this.binding.counterTv.setVisibility(View.GONE);
        if (hideSmallMic) {
            this.binding.glowingMic.setVisibility(View.GONE);
        }
    }

    private void showViews() {
        this.binding.slideToCancel.setVisibility(View.VISIBLE);
        this.binding.glowingMic.setVisibility(View.VISIBLE);
        this.binding.counterTv.setVisibility(View.VISIBLE);
    }

    private boolean isLessThanMin(final long time) {
        return time <= this.minMillis;
    }

    private void playSound(final int soundRes) {
        if (!this.isSoundEnabled) return;
        if (soundRes == 0) return;
        try {
            MediaPlayer player = new MediaPlayer();
            final AssetFileDescriptor afd = this.context.getResources().openRawResourceFd(soundRes);
            if (afd == null) return;
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            player.prepare();
            player.start();
            player.setOnCompletionListener(MediaPlayer::release);
            player.setLooping(false);
        } catch (final IOException e) {
            Log.e(RecordView.TAG, "playSound", e);
        }
    }

    protected void onActionDown(final RecordButton recordBtn, final MotionEvent motionEvent) {
        if (this.onRecordListener != null) {
            this.onRecordListener.onStart();
        }
        this.recordViewAnimationHelper.setStartRecorded(true);
        this.recordViewAnimationHelper.resetBasketAnimation();
        this.recordViewAnimationHelper.resetSmallMic();
        recordBtn.startScale();
        // slideToCancelLayout.startShimmerAnimation();

        this.initialX = recordBtn.getX();
        this.basketInitialY = this.binding.basketImg.getY() + 90;
        // playSound(RECORD_START);
        this.showViews();

        this.recordViewAnimationHelper.animateSmallMicAlpha();
        this.binding.counterTv.setBase(SystemClock.elapsedRealtime());
        this.startTime = System.currentTimeMillis();
        this.binding.counterTv.start();
        this.isSwiped = false;
    }

    protected void onActionMove(final RecordButton recordBtn, final MotionEvent motionEvent, boolean forceCancel) {
        final long time = System.currentTimeMillis() - this.startTime;
        if (this.isSwiped) return;
        //Swipe To Cancel
        if (forceCancel || (this.binding.slideToCancel.getX() != 0 && this.binding.slideToCancel.getX() <= this.binding.counterTv.getRight() + this.cancelBounds)) {
            //if the time was less than one second then do not start basket animation
            if (this.isLessThanMin(time)) {
                this.hideViews(true);
                this.recordViewAnimationHelper.clearAlphaAnimation(false);
                if (this.onRecordListener != null) {
                    this.onRecordListener.onLessThanMin();
                }
                this.recordViewAnimationHelper.onAnimationEnd();
            } else {
                this.hideViews(false);
                this.recordViewAnimationHelper.animateBasket(this.basketInitialY);
            }
            this.recordViewAnimationHelper.moveRecordButtonAndSlideToCancelBack(recordBtn, this.binding.slideToCancel, this.initialX, this.difX);
            this.binding.counterTv.stop();
            // slideToCancelLayout.stopShimmerAnimation();
            this.isSwiped = true;
            this.recordViewAnimationHelper.setStartRecorded(false);
            if (this.onRecordListener != null) {
                this.onRecordListener.onCancel();
            }
            return;
        }
        //if statement is to Prevent Swiping out of bounds
        if (!(motionEvent.getRawX() < this.initialX)) return;
        recordBtn.animate()
                 .x(motionEvent.getRawX())
                 .setDuration(0)
                 .start();
        if (this.difX == 0) {
            this.difX = (this.initialX - this.binding.slideToCancel.getX());
        }
        this.binding.slideToCancel.animate()
                             .x(motionEvent.getRawX() - this.difX)
                             .setDuration(0)
                             .start();
    }

    protected void onActionUp(final RecordButton recordBtn) {
        long elapsedTime = System.currentTimeMillis() - this.startTime;
        if (!this.isLessThanMinAllowed && this.isLessThanMin(elapsedTime) && !this.isSwiped) {
            if (this.onRecordListener != null) {
                this.onRecordListener.onLessThanMin();
            }
            this.recordViewAnimationHelper.setStartRecorded(false);
            // playSound(RECORD_ERROR);
        } else {
            if (this.onRecordListener != null && !this.isSwiped) {
                this.onRecordListener.onFinish(elapsedTime);
            }
            this.recordViewAnimationHelper.setStartRecorded(false);
            if (!this.isSwiped) {
                // playSound(RECORD_FINISHED);
            }
        }
        //if user has swiped then do not hide SmallMic since it will be hidden after swipe Animation
        this.hideViews(!this.isSwiped);
        if (!this.isSwiped) {
            this.recordViewAnimationHelper.clearAlphaAnimation(true);
        }
        this.recordViewAnimationHelper.moveRecordButtonAndSlideToCancelBack(recordBtn, this.binding.slideToCancel, this.initialX, this.difX);
        this.binding.counterTv.stop();
        // slideToCancelLayout.stopShimmerAnimation();
    }

    private void setMarginRight(final int marginRight, final boolean convertToDp) {
        final ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) this.binding.slideToCancel.getLayoutParams();
        if (convertToDp) {
            layoutParams.rightMargin = Utils.convertDpToPx(marginRight);
        } else {
            layoutParams.rightMargin = marginRight;
        }
        this.binding.slideToCancel.setLayoutParams(layoutParams);
    }

    private void setSlideArrowDrawable(@DrawableRes int slideArrowResource) {
        final Drawable slideArrow = AppCompatResources.getDrawable(this.getContext(), slideArrowResource);
        // Log.d(TAG, "setSlideArrowDrawable: slideArrow: " + slideArrow);
        if (slideArrow == null) return;
        slideArrow.setBounds(0, 0, slideArrow.getIntrinsicWidth(), slideArrow.getIntrinsicHeight());
        this.binding.slideToCancel.setCompoundDrawablesRelative(slideArrow, null, null, null);
    }

    public void setOnRecordListener(final OnRecordListener onRecordListener) {
        this.onRecordListener = onRecordListener;
    }

    public void setOnBasketAnimationEndListener(final OnBasketAnimationEnd onBasketAnimationEndListener) {
        this.recordViewAnimationHelper.setOnBasketAnimationEndListener(onBasketAnimationEndListener);
    }

    public void setSoundEnabled(final boolean isEnabled) {
        this.isSoundEnabled = isEnabled;
    }

    public void setLessThanMinAllowed(final boolean isAllowed) {
        this.isLessThanMinAllowed = isAllowed;
    }

    public void setSlideToCancelText(final String text) {
        this.binding.slideToCancel.setText(text);
    }

    public void setSlideToCancelTextColor(final int color) {
        this.binding.slideToCancel.setTextColor(color);
    }

    public void setSmallMicColor(final int color) {
        this.binding.glowingMic.setColorFilter(color);
    }

    public void setSmallMicIcon(final int icon) {
        this.binding.glowingMic.setImageResource(icon);
    }

    public void setSlideMarginRight(final int marginRight) {
        this.setMarginRight(marginRight, true);
    }

    public void setCustomSounds(final int startSound, final int finishedSound, final int errorSound) {
        //0 means do not play sound
        this.RECORD_START = startSound;
        this.RECORD_FINISHED = finishedSound;
        this.RECORD_ERROR = errorSound;
    }

    public float getCancelBounds() {
        return this.cancelBounds;
    }

    public void setCancelBounds(final float cancelBounds) {
        this.setCancelBounds(cancelBounds, true);
    }

    //set Chronometer color
    public void setCounterTimeColor(@ColorInt final int color) {
        this.binding.counterTv.setTextColor(color);
    }

    public void setSlideToCancelArrowColor(@ColorInt final int color) {
        Drawable drawable = this.binding.slideToCancel.getCompoundDrawablesRelative()[0];
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable.mutate(), color);
        this.binding.slideToCancel.setCompoundDrawablesRelative(drawable, null, null, null);
    }

    private void setCancelBounds(final float cancelBounds, final boolean convertDpToPixel) {
        this.cancelBounds = convertDpToPixel ? Utils.convertDpToPx(cancelBounds) : cancelBounds;
    }

    public void setMinMillis(int minMillis) {
        this.minMillis = minMillis;
    }

    public void cancelRecording(RecordButton recordBtn) {
        this.onActionMove(recordBtn, null, true);
    }

    public interface OnBasketAnimationEnd {
        void onAnimationEnd();
    }

    public interface OnRecordListener {
        void onStart();

        void onCancel();

        void onFinish(long recordTime);

        void onLessThanMin();
    }
}


