package awais.instagrabber.fragments.imageedit.filters.custom;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageContrastFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageHueFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageOverlayBlendFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSaturationFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSepiaToneFilter;

public class GPUImageClarendonFilter extends GPUImageFilterGroup {
    public GPUImageClarendonFilter() {
        this.addFilter(new GPUImageBrightnessFilter(0.15f));
        this.addFilter(new GPUImageContrastFilter(1.25f));
        this.addFilter(new GPUImageSaturationFilter(1.15f));
        this.addFilter(new GPUImageSepiaToneFilter(0.15f));
        this.addFilter(new GPUImageHueFilter(5));
        GPUImageOverlayBlendFilter blendFilter = new GPUImageOverlayBlendFilter();
        Bitmap bitmap = Bitmap.createBitmap(5, 5, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.argb((int) (0.4 * 255), 127, 187, 227));
        blendFilter.setBitmap(bitmap);
        this.addFilter(blendFilter);
    }
}
