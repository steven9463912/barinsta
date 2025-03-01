package awais.instagrabber.fragments.imageedit.filters.custom;

import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageHueFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSaturationFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSepiaToneFilter;

public class GPUImage1977Filter extends GPUImageFilterGroup {
    public GPUImage1977Filter() {
        this.addFilter(new GPUImageSepiaToneFilter(0.35f));
        this.addFilter(new GPUImageHueFilter(-30f));
        this.addFilter(new GPUImageSaturationFilter(1.4f));
    }
}
