package awais.instagrabber.fragments.imageedit.filters.filters;

import java.util.Collections;
import java.util.Map;

import awais.instagrabber.R;
import awais.instagrabber.fragments.imageedit.filters.FiltersHelper;
import awais.instagrabber.fragments.imageedit.filters.properties.Property;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;

public class NormalFilter extends Filter<GPUImageFilter> {
    private final GPUImageFilter filter;
    private final Map<Integer, Property<?>> properties;

    public NormalFilter() {
        super(FiltersHelper.FilterType.NORMAL, R.string.normal);
        this.properties = Collections.emptyMap();
        this.filter = new GPUImageFilter();
    }

    @Override
    public Map<Integer, Property<?>> getProperties() {
        return this.properties;
    }

    @Override
    public GPUImageFilter getInstance() {
        return this.filter;
    }
}
