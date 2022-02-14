package awais.instagrabber.fragments.imageedit.filters.custom;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageMultiplyBlendFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSaturationFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSepiaToneFilter;

public class GPUImageAdenFilter extends GPUImageFilterGroup {
    public GPUImageAdenFilter() {
        this.addFilter(new GPUImageSepiaToneFilter(0.2f));
        this.addFilter(new GPUImageBrightnessFilter(0.125f));
        this.addFilter(new GPUImageSaturationFilter(1.4f));
        GPUImageMultiplyBlendFilter blendFilter = new GPUImageMultiplyBlendFilter();
        Bitmap bitmap = Bitmap.createBitmap(5, 5, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.argb((int) (0.1 * 255), 125, 105, 24));
        blendFilter.setBitmap(bitmap);
        this.addFilter(blendFilter);
    }
}
