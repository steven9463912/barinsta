package awais.instagrabber.customviews;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.PopupWindow;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.util.Pair;

import java.util.List;
import java.util.function.Function;

import awais.instagrabber.R;
import awais.instagrabber.animations.RoundedRectRevealOutlineProvider;
import awais.instagrabber.customviews.emoji.Emoji;
import awais.instagrabber.customviews.emoji.ReactionsManager;
import awais.instagrabber.databinding.LayoutDirectItemOptionsBinding;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;

import static android.view.View.MeasureSpec.makeMeasureSpec;

public class DirectItemContextMenu extends PopupWindow {
    private static final String TAG = DirectItemContextMenu.class.getSimpleName();
    private static final int DO_NOT_UPDATE_FLAG = -1;
    private static final int DURATION = 300;

    private final Context context;
    private final boolean showReactions;
    private final ReactionsManager reactionsManager;
    private final int emojiSize;
    private final int emojiMargin;
    private final int emojiMarginHalf;
    private final Rect startRect = new Rect();
    private final Rect endRect = new Rect();
    private final TimeInterpolator revealInterpolator = new AccelerateDecelerateInterpolator();
    private final AnimatorListenerAdapter exitAnimationListener;
    private final TypedValue selectableItemBackgroundBorderless;
    private final TypedValue selectableItemBackground;
    private final int dividerHeight;
    private final int optionHeight;
    private final int optionPadding;
    private final int addAdjust;
    private final boolean hasOptions;
    private final List<MenuItem> options;
    private final int widthWithoutReactions;

    private AnimatorSet openCloseAnimator;
    private Point location;
    private Point point;
    private OnReactionClickListener onReactionClickListener;
    private OnOptionSelectListener onOptionSelectListener;
    private OnAddReactionClickListener onAddReactionListener;

    public DirectItemContextMenu(@NonNull Context context, boolean showReactions, List<MenuItem> options) {
        super(context);
        this.context = context;
        this.showReactions = showReactions;
        this.options = options;
        if (!showReactions && (options == null || options.isEmpty())) {
            throw new IllegalArgumentException("showReactions is set false and options are empty");
        }
        this.reactionsManager = ReactionsManager.getInstance(context);
        Resources resources = context.getResources();
        this.emojiSize = resources.getDimensionPixelSize(R.dimen.reaction_picker_emoji_size);
        this.emojiMargin = resources.getDimensionPixelSize(R.dimen.reaction_picker_emoji_margin);
        this.emojiMarginHalf = this.emojiMargin / 2;
        this.addAdjust = resources.getDimensionPixelSize(R.dimen.reaction_picker_add_padding_adjustment);
        this.dividerHeight = resources.getDimensionPixelSize(R.dimen.horizontal_divider_height);
        this.optionHeight = resources.getDimensionPixelSize(R.dimen.reaction_picker_option_height);
        this.optionPadding = resources.getDimensionPixelSize(R.dimen.dm_message_card_radius);
        this.widthWithoutReactions = resources.getDimensionPixelSize(R.dimen.dm_item_context_min_width);
        this.exitAnimationListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                DirectItemContextMenu.this.openCloseAnimator = null;
                DirectItemContextMenu.this.point = null;
                DirectItemContextMenu.this.getContentView().post(DirectItemContextMenu.super::dismiss);
            }
        };
        this.selectableItemBackgroundBorderless = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, this.selectableItemBackgroundBorderless, true);
        this.selectableItemBackground = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, this.selectableItemBackground, true);
        this.hasOptions = options != null && !options.isEmpty();
    }

    public void show(@NonNull final View rootView, @NonNull Point location) {
        View content = this.createContentView();
        content.measure(makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        this.setup(content);
        // rootView.getParent().requestDisallowInterceptTouchEvent(true);
        // final Point correctedLocation = new Point(location.x, location.y - emojiSize * 2);
        this.location = location;
        this.showAtLocation(rootView, Gravity.TOP | Gravity.START, location.x, location.y);
        // fixPopupLocation(popupWindow, correctedLocation);
        this.animateOpen();
    }

    private void setup(View content) {
        this.setContentView(content);
        this.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        this.setFocusable(true);
        this.setOutsideTouchable(true);
        this.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        this.setBackgroundDrawable(null);
    }

    public void setOnOptionSelectListener(OnOptionSelectListener onOptionSelectListener) {
        this.onOptionSelectListener = onOptionSelectListener;
    }

    public void setOnReactionClickListener(OnReactionClickListener onReactionClickListener) {
        this.onReactionClickListener = onReactionClickListener;
    }

    public void setOnAddReactionListener(OnAddReactionClickListener onAddReactionListener) {
        this.onAddReactionListener = onAddReactionListener;
    }

    private void animateOpen() {
        View contentView = this.getContentView();
        contentView.setVisibility(View.INVISIBLE);
        contentView.post(() -> {
            AnimatorSet openAnim = new AnimatorSet();
            // Rectangular reveal.
            ValueAnimator revealAnim = this.createOpenCloseOutlineProvider().createRevealAnimator(contentView, false);
            revealAnim.setDuration(DirectItemContextMenu.DURATION);
            revealAnim.setInterpolator(this.revealInterpolator);

            final ValueAnimator fadeIn = ValueAnimator.ofFloat(0, 1);
            fadeIn.setDuration(DirectItemContextMenu.DURATION);
            fadeIn.setInterpolator(this.revealInterpolator);
            fadeIn.addUpdateListener(anim -> {
                final float alpha = (float) anim.getAnimatedValue();
                contentView.setAlpha(revealAnim.isStarted() ? alpha : 0);
            });
            openAnim.play(fadeIn);
            openAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(final Animator animation) {
                    contentView.setAlpha(1f);
                    DirectItemContextMenu.this.openCloseAnimator = null;
                }
            });

            this.openCloseAnimator = openAnim;
            openAnim.playSequentially(revealAnim);
            contentView.setVisibility(View.VISIBLE);
            openAnim.start();
        });
    }

    protected void animateClose() {
        this.endRect.setEmpty();
        if (this.openCloseAnimator != null) {
            this.openCloseAnimator.cancel();
        }
        View contentView = this.getContentView();
        AnimatorSet closeAnim = new AnimatorSet();
        // Rectangular reveal (reversed).
        ValueAnimator revealAnim = this.createOpenCloseOutlineProvider().createRevealAnimator(contentView, true);
        revealAnim.setDuration(DirectItemContextMenu.DURATION);
        revealAnim.setInterpolator(this.revealInterpolator);
        closeAnim.play(revealAnim);

        final ValueAnimator fadeOut = ValueAnimator.ofFloat(contentView.getAlpha(), 0);
        fadeOut.setDuration(DirectItemContextMenu.DURATION);
        fadeOut.setInterpolator(this.revealInterpolator);
        fadeOut.addUpdateListener(anim -> {
            final float alpha = (float) anim.getAnimatedValue();
            contentView.setAlpha(revealAnim.isStarted() ? alpha : contentView.getAlpha());
        });
        closeAnim.playTogether(fadeOut);
        closeAnim.addListener(this.exitAnimationListener);
        this.openCloseAnimator = closeAnim;
        closeAnim.start();
    }

    private RoundedRectRevealOutlineProvider createOpenCloseOutlineProvider() {
        View contentView = this.getContentView();
        int radius = this.context.getResources().getDimensionPixelSize(R.dimen.dm_message_card_radius_small);
        // Log.d(TAG, "createOpenCloseOutlineProvider: " + locationOnScreen(contentView) + " " + contentView.getMeasuredWidth() + " " + contentView
        //         .getMeasuredHeight());
        if (this.point == null) {
            this.point = this.locationOnScreen(contentView);
        }
        int left = this.location.x - this.point.x;
        int top = this.location.y - this.point.y;
        this.startRect.set(left, top, left, top);
        this.endRect.set(0, 0, contentView.getMeasuredWidth(), contentView.getMeasuredHeight());
        return new RoundedRectRevealOutlineProvider(radius, radius, this.startRect, this.endRect);
    }

    public void dismiss() {
        this.animateClose();
    }

    private View createContentView() {
        LayoutInflater layoutInflater = LayoutInflater.from(this.context);
        LayoutDirectItemOptionsBinding binding = LayoutDirectItemOptionsBinding.inflate(layoutInflater, null, false);
        Pair<View, View> firstLastEmojiView = null;
        if (this.showReactions) {
            firstLastEmojiView = this.addReactions(layoutInflater, binding.container);
        }
        if (this.hasOptions) {
            View divider = null;
            if (this.showReactions) {
                if (firstLastEmojiView == null) {
                    throw new IllegalStateException("firstLastEmojiView is null even though reactions were added");
                }
                // add divider if reactions were added
                divider = this.addDivider(binding.container,
                                     firstLastEmojiView.first.getId(),
                                     firstLastEmojiView.first.getId(),
                                     firstLastEmojiView.second.getId());
                ((ConstraintLayout.LayoutParams) firstLastEmojiView.first.getLayoutParams()).bottomToTop = divider.getId();
            }
            this.addOptions(layoutInflater, binding.container, divider);
        }
        return binding.getRoot();
    }

    private Pair<View, View> addReactions(LayoutInflater layoutInflater, ConstraintLayout container) {
        List<Emoji> reactions = this.reactionsManager.getReactions();
        AppCompatImageView prevSquareImageView = null;
        View firstImageView = null;
        View lastImageView = null;
        for (int i = 0; i < reactions.size(); i++) {
            Emoji reaction = reactions.get(i);
            AppCompatImageView imageView = this.getEmojiImageView();
            ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) imageView.getLayoutParams();
            if (i == 0 && !this.hasOptions) {
                // only connect bottom to parent bottom if there are no options
                layoutParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
            }
            if (i == 0) {
                layoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
                layoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
                firstImageView = imageView;
                layoutParams.setMargins(this.emojiMargin, this.emojiMargin, this.emojiMarginHalf, this.emojiMargin);
            } else {
                layoutParams.startToEnd = prevSquareImageView.getId();
                ConstraintLayout.LayoutParams prevViewLayoutParams = (ConstraintLayout.LayoutParams) prevSquareImageView.getLayoutParams();
                prevViewLayoutParams.endToStart = imageView.getId();
                // always connect the other image view's top and bottom to the first image view top and bottom
                layoutParams.topToTop = firstImageView.getId();
                layoutParams.bottomToBottom = firstImageView.getId();
                layoutParams.setMargins(this.emojiMarginHalf, this.emojiMargin, this.emojiMarginHalf, this.emojiMargin);
            }
            imageView.setImageDrawable(reaction.getDrawable());
            imageView.setOnClickListener(view -> {
                if (this.onReactionClickListener != null) {
                    this.onReactionClickListener.onClick(reaction);
                }
                this.dismiss();
            });
            container.addView(imageView);
            prevSquareImageView = imageView;
        }
        // add the + icon
        if (prevSquareImageView != null) {
            AppCompatImageView imageView = this.getEmojiImageView();
            ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) imageView.getLayoutParams();
            layoutParams.topToTop = firstImageView.getId();
            layoutParams.bottomToBottom = firstImageView.getId();
            layoutParams.startToEnd = prevSquareImageView.getId();
            ConstraintLayout.LayoutParams prevViewLayoutParams = (ConstraintLayout.LayoutParams) prevSquareImageView.getLayoutParams();
            prevViewLayoutParams.endToStart = imageView.getId();
            layoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            layoutParams.setMargins(this.emojiMarginHalf - this.addAdjust, this.emojiMargin - this.addAdjust, this.emojiMargin - this.addAdjust, this.emojiMargin - this.addAdjust);
            imageView.setImageResource(R.drawable.ic_add);
            imageView.setOnClickListener(view -> {
                if (this.onAddReactionListener != null) {
                    this.onAddReactionListener.onAdd();
                }
                this.dismiss();
            });
            lastImageView = imageView;
            container.addView(imageView);
        }
        return new Pair<>(firstImageView, lastImageView);
    }

    @NonNull
    private AppCompatImageView getEmojiImageView() {
        AppCompatImageView imageView = new AppCompatImageView(this.context);
        ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(this.emojiSize, this.emojiSize);
        imageView.setBackgroundResource(this.selectableItemBackgroundBorderless.resourceId);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setId(View.generateViewId());
        imageView.setLayoutParams(layoutParams);
        return imageView;
    }

    private void addOptions(LayoutInflater layoutInflater,
                            ConstraintLayout container,
                            @Nullable View divider) {
        View prevOptionView = null;
        if (!this.showReactions) {
            container.getLayoutParams().width = this.widthWithoutReactions;
        }
        for (int i = 0; i < this.options.size(); i++) {
            MenuItem menuItem = this.options.get(i);
            AppCompatTextView textView = this.getTextView();
            ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) textView.getLayoutParams();
            layoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            layoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            if (i == 0) {
                if (divider != null) {
                    layoutParams.topToBottom = divider.getId();
                    ((ConstraintLayout.LayoutParams) divider.getLayoutParams()).bottomToTop = textView.getId();
                } else {
                    // if divider is null mean reactions were not added, so connect top to top of parent
                    layoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
                    layoutParams.topMargin = this.emojiMargin; // material design spec (https://material.io/components/menus#specs)
                }
            } else {
                layoutParams.topToBottom = prevOptionView.getId();
                ConstraintLayout.LayoutParams prevLayoutParams = (ConstraintLayout.LayoutParams) prevOptionView.getLayoutParams();
                prevLayoutParams.bottomToTop = textView.getId();
            }
            if (i == this.options.size() - 1) {
                layoutParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
                layoutParams.bottomMargin = this.emojiMargin; // material design spec (https://material.io/components/menus#specs)
            }
            textView.setText(this.context.getString(menuItem.getTitleRes()));
            textView.setOnClickListener(v -> {
                if (this.onOptionSelectListener != null) {
                    this.onOptionSelectListener.onSelect(menuItem.getItemId(), menuItem.getCallback());
                }
                this.dismiss();
            });
            container.addView(textView);
            prevOptionView = textView;
        }
    }

    private AppCompatTextView getTextView() {
        AppCompatTextView textView = new AppCompatTextView(this.context);
        textView.setId(View.generateViewId());
        textView.setBackgroundResource(this.selectableItemBackground.resourceId);
        textView.setGravity(Gravity.CENTER_VERTICAL);
        textView.setPaddingRelative(this.optionPadding, 0, this.optionPadding, 0);
        textView.setTextAppearance(this.context, R.style.TextAppearance_MaterialComponents_Body1);
        ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                this.optionHeight);
        textView.setLayoutParams(layoutParams);
        return textView;
    }

    private View addDivider(ConstraintLayout container,
                            int topViewId,
                            int startViewId,
                            int endViewId) {
        View dividerView = new View(this.context);
        dividerView.setId(View.generateViewId());
        dividerView.setBackgroundResource(R.drawable.pref_list_divider_material);
        ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                this.dividerHeight);
        layoutParams.topToBottom = topViewId;
        layoutParams.startToStart = startViewId;
        layoutParams.endToEnd = endViewId;
        dividerView.setLayoutParams(layoutParams);
        container.addView(dividerView);
        return dividerView;
    }

    @NonNull
    private Point locationOnScreen(@NonNull View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        return new Point(location[0], location[1]);
    }

    public static class MenuItem {
        @IdRes
        private final int itemId;
        @StringRes
        private final int titleRes;

        /**
         * Callback function
         */
        private final Function<DirectItem, Void> callback;

        public MenuItem(@IdRes int itemId, @StringRes int titleRes) {
            this(itemId, titleRes, null);
        }

        public MenuItem(@IdRes int itemId, @StringRes int titleRes, @Nullable Function<DirectItem, Void> callback) {
            this.itemId = itemId;
            this.titleRes = titleRes;
            this.callback = callback;
        }

        public int getItemId() {
            return this.itemId;
        }

        public int getTitleRes() {
            return this.titleRes;
        }

        public Function<DirectItem, Void> getCallback() {
            return this.callback;
        }
    }

    public interface OnOptionSelectListener {
        void onSelect(int itemId, @Nullable Function<DirectItem, Void> callback);
    }

    public interface OnReactionClickListener {
        void onClick(Emoji emoji);
    }

    public interface OnAddReactionClickListener {
        void onAdd();
    }

    // @NonNull
    // private Rect getGlobalVisibleRect(@NonNull final View view) {
    //     final Rect rect = new Rect();
    //     view.getGlobalVisibleRect(rect);
    //     return rect;
    // }

    // private void fixPopupLocation(@NonNull final PopupWindow popupWindow, @NonNull final Point desiredLocation) {
    //     popupWindow.getContentView().post(() -> {
    //         final Point actualLocation = locationOnScreen(popupWindow.getContentView());
    //
    //         if (!(actualLocation.x == desiredLocation.x && actualLocation.y == desiredLocation.y)) {
    //             final int differenceX = actualLocation.x - desiredLocation.x;
    //             final int differenceY = actualLocation.y - desiredLocation.y;
    //
    //             final int fixedOffsetX;
    //             final int fixedOffsetY;
    //
    //             if (actualLocation.x > desiredLocation.x) {
    //                 fixedOffsetX = desiredLocation.x - differenceX;
    //             } else {
    //                 fixedOffsetX = desiredLocation.x + differenceX;
    //             }
    //
    //             if (actualLocation.y > desiredLocation.y) {
    //                 fixedOffsetY = desiredLocation.y - differenceY;
    //             } else {
    //                 fixedOffsetY = desiredLocation.y + differenceY;
    //             }
    //
    //             popupWindow.update(fixedOffsetX, fixedOffsetY, DO_NOT_UPDATE_FLAG, DO_NOT_UPDATE_FLAG);
    //         }
    //     });
    // }
}

