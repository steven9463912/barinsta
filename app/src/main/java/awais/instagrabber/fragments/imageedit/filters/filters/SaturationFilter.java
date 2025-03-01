package awais.instagrabber.fragments.imageedit.filters.filters;

import java.util.Collections;
import java.util.Map;

import awais.instagrabber.R;
import awais.instagrabber.fragments.imageedit.filters.FiltersHelper;
import awais.instagrabber.fragments.imageedit.filters.properties.FloatProperty;
import awais.instagrabber.fragments.imageedit.filters.properties.Property;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSaturationFilter;

public class SaturationFilter extends Filter<GPUImageSaturationFilter> {
    private static final int PROP_SATURATION = 0;

    private final GPUImageSaturationFilter filter;
    private final Map<Integer, Property<?>> properties;

    public SaturationFilter() {
        super(FiltersHelper.FilterType.SATURATION, R.string.saturation);
        this.properties = Collections.singletonMap(
                SaturationFilter.PROP_SATURATION, new FloatProperty(R.string.saturation, 1.0f, 0f, 2.0f)
        );
        this.filter = new GPUImageSaturationFilter((Float) this.getProperty(SaturationFilter.PROP_SATURATION).getDefaultValue());
    }

    @Override
    public Map<Integer, Property<?>> getProperties() {
        return this.properties;
    }

    @Override
    public void adjust(int property, Object value) {
        super.adjust(property, value);
        if (!(value instanceof Float)) return;
        this.filter.setSaturation((Float) value);
    }

    @Override
    public GPUImageSaturationFilter getInstance() {
        return this.filter;
    }
}
