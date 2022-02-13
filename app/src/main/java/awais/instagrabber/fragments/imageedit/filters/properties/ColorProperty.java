package awais.instagrabber.fragments.imageedit.filters.properties;

import androidx.annotation.StringRes;

/**
 * Min and Max values do not matter here
 */
public class ColorProperty extends Property<Integer> {
    private final int label;
    private final int defaultValue;

    public ColorProperty(@StringRes int label,
                         int defaultValue) {
        this.label = label;
        this.defaultValue = defaultValue;
    }

    @Override
    public int getLabel() {
        return this.label;
    }

    @Override
    public Integer getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public Integer getMinValue() {
        return this.defaultValue;
    }

    @Override
    public Integer getMaxValue() {
        return this.defaultValue;
    }
}
