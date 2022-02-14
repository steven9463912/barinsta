package awais.instagrabber.fragments.imageedit.filters.properties;

import android.graphics.PointF;

import androidx.annotation.StringRes;

/**
 * Min and Max values do not matter here
 */
public class PointFProperty extends Property<PointF> {
    private final int label;
    private final PointF defaultValue;

    public PointFProperty(@StringRes int label,
                          PointF defaultValue) {
        this.label = label;
        this.defaultValue = defaultValue;
    }

    @Override
    public int getLabel() {
        return this.label;
    }

    @Override
    public PointF getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public PointF getMinValue() {
        return this.defaultValue;
    }

    @Override
    public PointF getMaxValue() {
        return this.defaultValue;
    }
}
