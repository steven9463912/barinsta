package awais.instagrabber.customviews;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.ViewGroup;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;

import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.RoundingParams;

import java.util.HashMap;
import java.util.Map;

import awais.instagrabber.R;

public final class ProfilePicView extends CircularImageView {
    private static final String TAG = "ProfilePicView";

    private Size size;
    private int dimensionPixelSize;

    public ProfilePicView(final Context context, final GenericDraweeHierarchy hierarchy) {
        super(context);
        this.setHierarchy(hierarchy);
        this.size = Size.REGULAR;
        this.updateLayout();
    }

    public ProfilePicView(Context context) {
        super(context);
        this.size = Size.REGULAR;
        this.updateLayout();
    }

    public ProfilePicView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.parseAttrs(context, attrs);
        this.updateLayout();
    }

    public ProfilePicView(Context context,
                          AttributeSet attrs,
                          int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.parseAttrs(context, attrs);
        this.updateLayout();
    }

    private void parseAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ProfilePicView,
                0,
                0);
        try {
            int sizeValue = a.getInt(R.styleable.ProfilePicView_size, Size.REGULAR.getValue());
            this.size = Size.valueOf(sizeValue);
        } finally {
            a.recycle();
        }
    }

    private void updateLayout() {
        @DimenRes int dimenRes;
        switch (this.size) {
            case SMALL:
                dimenRes = R.dimen.profile_pic_size_small;
                break;
            case SMALLER:
                dimenRes = R.dimen.profile_pic_size_smaller;
                break;
            case TINY:
                dimenRes = R.dimen.profile_pic_size_tiny;
                break;
            case LARGE:
                dimenRes = R.dimen.profile_pic_size_large;
                break;
            default:
            case REGULAR:
                dimenRes = R.dimen.profile_pic_size_regular;
                break;
        }
        ViewGroup.LayoutParams layoutParams = this.getLayoutParams();
        if (layoutParams == null) {
            layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        this.dimensionPixelSize = this.getResources().getDimensionPixelSize(dimenRes);
        layoutParams.width = this.dimensionPixelSize;
        layoutParams.height = this.dimensionPixelSize;

        // invalidate();
        // requestLayout();
    }

    public void setSize(Size size) {
        this.size = size;
        this.updateLayout();
    }

    public void setStoriesBorder() {
        // private final int borderSize = 8;
        final int color = Color.GREEN;
        RoundingParams roundingParams = this.getHierarchy().getRoundingParams();
        if (roundingParams == null) {
            roundingParams = RoundingParams.asCircle().setRoundingMethod(RoundingParams.RoundingMethod.BITMAP_ONLY);
        }
        roundingParams.setBorder(color, 5.0f);
        this.getHierarchy().setRoundingParams(roundingParams);
    }

    public enum Size {
        TINY(0),
        SMALL(1),
        REGULAR(2),
        LARGE(3),
        SMALLER(4);

        private final int value;
        private static final Map<Integer, Size> map = new HashMap<>();

        static {
            for (final Size size : values()) {
                Size.map.put(size.value, size);
            }
        }

        Size(int value) {
            this.value = value;
        }

        @NonNull
        public static Size valueOf(int value) {
            Size size = Size.map.get(value);
            return size != null ? size : REGULAR;
        }

        public int getValue() {
            return this.value;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        this.setMeasuredDimension(this.dimensionPixelSize, this.dimensionPixelSize);
    }
}