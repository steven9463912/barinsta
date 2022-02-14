package awais.instagrabber.fragments.imageedit.filters.filters;

import java.util.Collections;
import java.util.Map;

import awais.instagrabber.R;
import awais.instagrabber.fragments.imageedit.filters.FiltersHelper;
import awais.instagrabber.fragments.imageedit.filters.properties.FloatProperty;
import awais.instagrabber.fragments.imageedit.filters.properties.Property;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSepiaToneFilter;

public class SepiaToneFilter extends Filter<GPUImageSepiaToneFilter> {
    private static final int PROP_INTENSITY = 0;

    private final GPUImageSepiaToneFilter filter;
    private final Map<Integer, Property<?>> properties;

    public SepiaToneFilter() {
        super(FiltersHelper.FilterType.SEPIA, R.string.sepia);
        this.properties = Collections.singletonMap(
                SepiaToneFilter.PROP_INTENSITY, new FloatProperty(-1, 1f, 1f, 10.0f)
        );
        this.filter = new GPUImageSepiaToneFilter((Float) this.getProperty(SepiaToneFilter.PROP_INTENSITY).getDefaultValue());
    }

    @Override
    public Map<Integer, Property<?>> getProperties() {
        return this.properties;
    }

    @Override
    public void adjust(int property, Object value) {
        super.adjust(property, value);
        if (!(value instanceof Float)) return;
        this.filter.setIntensity((Float) value);
    }

    @Override
    public GPUImageSepiaToneFilter getInstance() {
        return this.filter;
    }
}
