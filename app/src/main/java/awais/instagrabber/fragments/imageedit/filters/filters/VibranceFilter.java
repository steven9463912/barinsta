package awais.instagrabber.fragments.imageedit.filters.filters;

import java.util.Collections;
import java.util.Map;

import awais.instagrabber.R;
import awais.instagrabber.fragments.imageedit.filters.FiltersHelper;
import awais.instagrabber.fragments.imageedit.filters.properties.FloatProperty;
import awais.instagrabber.fragments.imageedit.filters.properties.Property;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageVibranceFilter;

public class VibranceFilter extends Filter<GPUImageVibranceFilter> {
    private static final int PROP_VIBRANCE = 0;

    private final GPUImageVibranceFilter filter;
    private final Map<Integer, Property<?>> properties;

    public VibranceFilter() {
        super(FiltersHelper.FilterType.VIBRANCE, R.string.vibrance);
        this.properties = Collections.singletonMap(
                VibranceFilter.PROP_VIBRANCE, new FloatProperty(R.string.vibrance, 0f, -1.2f, 1.2f)
        );
        this.filter = new GPUImageVibranceFilter((Float) this.getProperty(VibranceFilter.PROP_VIBRANCE).getDefaultValue());
    }

    @Override
    public Map<Integer, Property<?>> getProperties() {
        return this.properties;
    }

    @Override
    public void adjust(int property, Object value) {
        super.adjust(property, value);
        if (!(value instanceof Float)) return;
        this.filter.setVibrance((Float) value);
    }

    @Override
    public GPUImageVibranceFilter getInstance() {
        return this.filter;
    }
}
