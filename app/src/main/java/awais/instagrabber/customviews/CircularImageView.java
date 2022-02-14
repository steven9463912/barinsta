package awais.instagrabber.customviews;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.generic.GenericDraweeHierarchyInflater;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.view.SimpleDraweeView;

import awais.instagrabber.R;

public class CircularImageView extends SimpleDraweeView {
    public CircularImageView(final Context context, final GenericDraweeHierarchy hierarchy) {
        super(context);
        this.setHierarchy(hierarchy);
    }

    public CircularImageView(Context context) {
        super(context);
        this.inflateHierarchy(context, null);
    }

    public CircularImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.inflateHierarchy(context, attrs);
    }

    public CircularImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.inflateHierarchy(context, attrs);
    }

    protected void inflateHierarchy(final Context context, @Nullable final AttributeSet attrs) {
        final Resources resources = context.getResources();
        RoundingParams roundingParams = RoundingParams.asCircle();
        final GenericDraweeHierarchyBuilder builder = new GenericDraweeHierarchyBuilder(resources)
                .setRoundingParams(roundingParams)
                .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER);
        GenericDraweeHierarchyInflater.updateBuilder(builder, context, attrs);
        this.setAspectRatio(builder.getDesiredAspectRatio());
        this.setHierarchy(builder.build());
        this.setBackgroundResource(R.drawable.shape_oval_light);
    }

    /* types: 0 clear, 1 green (feed bestie / has story), 2 red (live) */
    public void setStoriesBorder(int type) {
        // private final int borderSize = 8;
        int color = type == 2 ? Color.RED : Color.GREEN;
        RoundingParams roundingParams = this.getHierarchy().getRoundingParams();
        if (roundingParams == null) {
            roundingParams = RoundingParams.asCircle().setRoundingMethod(RoundingParams.RoundingMethod.BITMAP_ONLY);
        }
        roundingParams.setBorder(color, type == 0 ? 0f : 5.0f);
        this.getHierarchy().setRoundingParams(roundingParams);
    }
}