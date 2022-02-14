package awais.instagrabber.customviews;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.emoji.widget.EmojiAppCompatTextView;

import awais.instagrabber.R;

/**
 * https://stackoverflow.com/a/31916731
 */
public class TextViewDrawableSize extends EmojiAppCompatTextView {

    private int mDrawableWidth;
    private int mDrawableHeight;
    private Boolean calledFromInit;

    public TextViewDrawableSize(Context context) {
        this(context, null);
    }

    public TextViewDrawableSize(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextViewDrawableSize(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init(context, attrs, defStyleAttr);
    }

    private void init(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.TextViewDrawableSize, defStyleAttr, 0);

        try {
            this.mDrawableWidth = array.getDimensionPixelSize(R.styleable.TextViewDrawableSize_compoundDrawableWidth, -1);
            this.mDrawableHeight = array.getDimensionPixelSize(R.styleable.TextViewDrawableSize_compoundDrawableHeight, -1);
        } finally {
            array.recycle();
        }

        if (this.mDrawableWidth > 0 || this.mDrawableHeight > 0) {
            this.initCompoundDrawableSize();
        }
    }

    private void initCompoundDrawableSize() {
        Drawable[] drawables = this.getCompoundDrawablesRelative();
        for (final Drawable drawable : drawables) {
            if (drawable == null) {
                continue;
            }

            Rect realBounds = drawable.getBounds();
            final float scaleFactor = drawable.getIntrinsicHeight() / (float) drawable.getIntrinsicWidth();

            float drawableWidth = drawable.getIntrinsicWidth();
            float drawableHeight = drawable.getIntrinsicHeight();

            if (this.mDrawableWidth > 0) {
                // save scale factor of image
                if (drawableWidth > this.mDrawableWidth) {
                    drawableWidth = this.mDrawableWidth;
                    drawableHeight = drawableWidth * scaleFactor;
                }
            }
            if (this.mDrawableHeight > 0) {
                // save scale factor of image
                if (drawableHeight > this.mDrawableHeight) {
                    drawableHeight = this.mDrawableHeight;
                    drawableWidth = drawableHeight / scaleFactor;
                }
            }

            realBounds.right = realBounds.left + Math.round(drawableWidth);
            realBounds.bottom = realBounds.top + Math.round(drawableHeight);

            drawable.setBounds(realBounds);
        }
        this.setCompoundDrawablesRelative(drawables[0], drawables[1], drawables[2], drawables[3]);
    }

    public void setCompoundDrawablesRelativeWithSize(@Nullable Drawable start,
                                                     @Nullable Drawable top,
                                                     @Nullable Drawable end,
                                                     @Nullable Drawable bottom) {
        this.setCompoundDrawablesRelative(start, top, end, bottom);
        this.initCompoundDrawableSize();
    }
}
