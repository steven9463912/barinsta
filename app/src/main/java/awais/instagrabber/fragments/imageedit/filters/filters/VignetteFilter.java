package awais.instagrabber.fragments.imageedit.filters.filters;

import android.graphics.Color;
import android.graphics.PointF;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

import awais.instagrabber.R;
import awais.instagrabber.fragments.imageedit.filters.FiltersHelper;
import awais.instagrabber.fragments.imageedit.filters.properties.ColorProperty;
import awais.instagrabber.fragments.imageedit.filters.properties.FloatProperty;
import awais.instagrabber.fragments.imageedit.filters.properties.PointFProperty;
import awais.instagrabber.fragments.imageedit.filters.properties.Property;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageVignetteFilter;

public class VignetteFilter extends Filter<GPUImageVignetteFilter> {
    private static final int PROP_CENTER = 0;
    private static final int PROP_COLOR = 1;
    private static final int PROP_START = 2;
    private static final int PROP_END = 3;

    private final GPUImageVignetteFilter filter;
    private final Map<Integer, Property<?>> properties;

    public VignetteFilter() {
        super(FiltersHelper.FilterType.VIGNETTE, R.string.vignette);
        this.properties = ImmutableMap.of(
                VignetteFilter.PROP_CENTER, new PointFProperty(R.string.center, new PointF(0.5f, 0.5f)),
                VignetteFilter.PROP_COLOR, new ColorProperty(R.string.color, Color.BLACK),
                VignetteFilter.PROP_START, new FloatProperty(R.string.start, 0.3f),
                VignetteFilter.PROP_END, new FloatProperty(R.string.end, 0.75f)
        );
        this.filter = new GPUImageVignetteFilter(
                (PointF) this.getProperty(VignetteFilter.PROP_CENTER).getDefaultValue(),
                this.getFloatArrayFromColor((Integer) this.getProperty(VignetteFilter.PROP_COLOR).getDefaultValue()),
                (Float) this.getProperty(VignetteFilter.PROP_START).getDefaultValue(),
                (Float) this.getProperty(VignetteFilter.PROP_END).getDefaultValue()
        );
    }

    @Override
    public Map<Integer, Property<?>> getProperties() {
        return this.properties;
    }

    @Override
    public void adjust(int property, Object value) {
        super.adjust(property, value);
        switch (property) {
            case VignetteFilter.PROP_CENTER:
                this.filter.setVignetteCenter((PointF) value);
                return;
            case VignetteFilter.PROP_COLOR:
                int color = (int) value;
                this.filter.setVignetteColor(this.getFloatArrayFromColor(color));
                return;
            case VignetteFilter.PROP_START:
                this.filter.setVignetteStart((float) value);
                return;
            case VignetteFilter.PROP_END:
                this.filter.setVignetteEnd((float) value);
                return;
            default:
        }
    }

    private float[] getFloatArrayFromColor(int color) {
        return new float[]{Color.red(color) / 255f, Color.green(color) / 255f, Color.blue(color) / 255f};
    }

    @Override
    public GPUImageVignetteFilter getInstance() {
        return this.filter;
    }
}
