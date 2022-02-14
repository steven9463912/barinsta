package awais.instagrabber.fragments.imageedit.filters.filters;

import androidx.annotation.CallSuper;
import androidx.annotation.StringRes;

import java.util.Map;
import java.util.Set;

import awais.instagrabber.fragments.imageedit.filters.FiltersHelper;
import awais.instagrabber.fragments.imageedit.filters.properties.Property;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;

public abstract class Filter<T extends GPUImageFilter> {
    private final FiltersHelper.FilterType type;
    private final int label;

    public Filter(FiltersHelper.FilterType type, @StringRes int label) {
        this.type = type;
        this.label = label;
    }

    public FiltersHelper.FilterType getType() {
        return this.type;
    }

    @StringRes
    public int getLabel() {
        return this.label;
    }

    public abstract T getInstance();

    public abstract Map<Integer, Property<?>> getProperties();

    public Property<?> getProperty(final int property) {
        return this.getProperties().get(property);
    }

    @CallSuper
    public void adjust(int property, Object value) {
        Property<?> propertyObj = this.getProperty(property);
        propertyObj.setValue(value);
    }

    public void reset() {
        Map<Integer, Property<?>> propertyMap = this.getProperties();
        if (propertyMap == null) return;
        Set<Map.Entry<Integer, Property<?>>> entries = propertyMap.entrySet();
        for (Map.Entry<Integer, Property<?>> entry : entries) {
            this.adjust(entry.getKey(), entry.getValue().getDefaultValue());
        }
    }
}
