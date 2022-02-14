package awais.instagrabber.fragments.imageedit.filters.properties;

import androidx.annotation.StringRes;

public class FloatProperty extends Property<Float> {

    private final int label;
    private final float defaultValue;
    private final float minValue;
    private final float maxValue;

    public FloatProperty(@StringRes int label,
                         float defaultValue,
                         float minValue,
                         float maxValue) {

        this.label = label;
        this.defaultValue = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public FloatProperty(@StringRes int label, float value) {
        this.label = label;
        defaultValue = value;
        minValue = value;
        maxValue = value;
    }

    @Override
    public int getLabel() {
        return this.label;
    }

    @Override
    public Float getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public Float getMinValue() {
        return this.minValue;
    }

    @Override
    public Float getMaxValue() {
        return this.maxValue;
    }
}
