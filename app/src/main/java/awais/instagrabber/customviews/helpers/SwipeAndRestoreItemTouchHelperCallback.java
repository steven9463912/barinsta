package awais.instagrabber.customviews.helpers;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.R;
import awais.instagrabber.utils.Utils;

/**
 * Thanks to https://github.com/izjumovfs/SwipeToReply/blob/master/swipetoreply/src/main/java/com/capybaralabs/swipetoreply/SwipeController.java
 */
public class SwipeAndRestoreItemTouchHelperCallback extends ItemTouchHelper.Callback {
    private static final String TAG = "SwipeRestoreCallback";

    private final float swipeThreshold;
    private final float swipeAutoCancelThreshold;
    private final OnSwipeListener onSwipeListener;
    private final Drawable replyIcon;
    // private final Drawable replyIconBackground;
    private final int replyIconShowThreshold;
    private final float replyIconMaxTranslation;
    private final Rect replyIconBounds = new Rect();
    private final float replyIconXOffset;
    private final int replyIconSize;

    private boolean mSwipeBack;
    private boolean hasVibrated;

    public SwipeAndRestoreItemTouchHelperCallback(Context context, OnSwipeListener onSwipeListener) {
        this.onSwipeListener = onSwipeListener;
        this.swipeThreshold = Utils.displayMetrics.widthPixels * 0.25f;
        this.swipeAutoCancelThreshold = this.swipeThreshold + Utils.convertDpToPx(5);
        this.replyIcon = AppCompatResources.getDrawable(context, R.drawable.ic_round_reply_24);
        if (this.replyIcon == null) {
            throw new IllegalArgumentException("reply icon is null");
        }
        this.replyIcon.setTint(context.getResources().getColor(R.color.white)); //todo need to update according to theme
        this.replyIconShowThreshold = Utils.convertDpToPx(24);
        this.replyIconMaxTranslation = this.swipeThreshold - this.replyIconShowThreshold;
        // Log.d(TAG, "replyIconShowThreshold: " + replyIconShowThreshold + ", swipeThreshold: " + swipeThreshold);
        this.replyIconSize = this.replyIconShowThreshold; // Utils.convertDpToPx(24);
        this.replyIconXOffset = this.swipeThreshold * 0.25f /*Utils.convertDpToPx(20)*/;
    }

    @Override
    public int getMovementFlags(@NonNull final RecyclerView recyclerView, @NonNull final RecyclerView.ViewHolder viewHolder) {
        if (!(viewHolder instanceof SwipeableViewHolder)) {
            return Callback.makeMovementFlags(ItemTouchHelper.ACTION_STATE_IDLE, ItemTouchHelper.ACTION_STATE_IDLE);
        }
        return Callback.makeMovementFlags(ItemTouchHelper.ACTION_STATE_IDLE, ((SwipeableViewHolder) viewHolder).getSwipeDirection());
    }

    @Override
    public boolean onMove(@NonNull final RecyclerView recyclerView,
                          @NonNull final RecyclerView.ViewHolder viewHolder,
                          @NonNull final RecyclerView.ViewHolder viewHolder1) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder, final int i) {}

    @Override
    public int convertToAbsoluteDirection(final int flags, final int layoutDirection) {
        if (this.mSwipeBack) {
            this.mSwipeBack = false;
            return 0;
        }
        return super.convertToAbsoluteDirection(flags, layoutDirection);
    }

    @Override
    public void onChildDraw(@NonNull final Canvas c,
                            @NonNull final RecyclerView recyclerView,
                            @NonNull final RecyclerView.ViewHolder viewHolder,
                            final float dX,
                            final float dY,
                            final int actionState,
                            final boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            this.setTouchListener(recyclerView, viewHolder);
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        this.drawReplyButton(c, viewHolder);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setTouchListener(final RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        recyclerView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                if (Math.abs(viewHolder.itemView.getTranslationX()) >= this.swipeAutoCancelThreshold) {
                    if (!this.hasVibrated) {
                        viewHolder.itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS,
                                                                  HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                        this.hasVibrated = true;
                    }
                    //     MotionEvent cancelEvent = MotionEvent.obtain(event);
                    //     cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                    //     recyclerView.dispatchTouchEvent(cancelEvent);
                    //     cancelEvent.recycle();
                }
            }
            this.mSwipeBack = event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP;
            if (this.mSwipeBack) {
                this.hasVibrated = false;
                if (Math.abs(viewHolder.itemView.getTranslationX()) >= this.swipeThreshold) {
                    if (this.onSwipeListener != null) {
                        this.onSwipeListener.onSwipe(viewHolder.getBindingAdapterPosition(), viewHolder);
                    }
                }
            }
            return false;
        });
    }

    public interface SwipeableViewHolder {
        int getSwipeDirection();
    }

    public interface OnSwipeListener {
        void onSwipe(int adapterPosition, RecyclerView.ViewHolder viewHolder);
    }

    private void drawReplyButton(final Canvas canvas, RecyclerView.ViewHolder viewHolder) {
        if (!(viewHolder instanceof SwipeableViewHolder)) return;
        int swipeDirection = ((SwipeableViewHolder) viewHolder).getSwipeDirection();
        if (swipeDirection != ItemTouchHelper.START && swipeDirection != ItemTouchHelper.END) return;
        View view = viewHolder.itemView;
        final float translationX = view.getTranslationX();
        boolean show = false;
        float progress;
        float translationXAbs = Math.abs(translationX);
        if (translationXAbs >= this.replyIconShowThreshold) {
            show = true;
        }
        if (show) {
            // replyIconShowThreshold -> swipeThreshold <=> progress 0 -> 1
            float replyIconTranslation = translationXAbs - this.replyIconShowThreshold;
            progress = replyIconTranslation / this.replyIconMaxTranslation;
            if (progress > 1) {
                progress = 1f;
            }
            if (progress < 0) {
                progress = 0;
            }
            // Log.d(TAG, /*"translationX: " + translationX + ",  replyIconTranslation: " + replyIconTranslation +*/ "progress: " + progress);
        } else {
            progress = 0f;
            // Log.d(TAG, /*"translationX: " + translationX + ",  replyIconTranslation: " + 0 +*/ "progress: " + progress);
        }
        if (progress > 0) {
            // calculate the reply icon y position, then offset top, bottom with icon size
            int y = view.getTop() + (view.getMeasuredHeight() / 2);
            int tempIconSize = (int) (this.replyIconSize * progress);
            int tempIconSizeHalf = tempIconSize / 2;
            int xOffset = (int) (this.replyIconXOffset * progress);
            int left;
            if (swipeDirection == ItemTouchHelper.END) {
                // draw arrow of left side
                left = xOffset;
            } else {
                // draw arrow of right side
                left = view.getMeasuredWidth() - xOffset - tempIconSize;
            }
            int right = tempIconSize + left;
            this.replyIconBounds.set(left, y - tempIconSizeHalf, right, y + tempIconSizeHalf);
            this.replyIcon.setBounds(this.replyIconBounds);
            this.replyIcon.draw(canvas);
        }
    }

}
