package awais.instagrabber.customviews.helpers;

import android.graphics.drawable.Animatable;
import android.view.ViewGroup;

import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.view.DraweeView;
import com.facebook.imagepipeline.image.ImageInfo;

import awais.instagrabber.utils.NumberUtils;

public class ImageResizingControllerListener<T extends DraweeView<GenericDraweeHierarchy>> extends BaseControllerListener<ImageInfo> {
    private static final String TAG = "ImageResizingController";

    private final T imageView;
    private final int requiredWidth;

    public ImageResizingControllerListener(T imageView, int requiredWidth) {
        this.imageView = imageView;
        this.requiredWidth = requiredWidth;
    }

    @Override
    public void onIntermediateImageSet(String id, ImageInfo imageInfo) {
        super.onIntermediateImageSet(id, imageInfo);
    }

    public void onFinalImageSet(final String id, final ImageInfo imageInfo, final Animatable animatable) {
        if (imageInfo != null) {
            // updateViewSize(imageInfo);
            int height = imageInfo.getHeight();
            int width = imageInfo.getWidth();
            // final float aspectRatio = ((float) width) / height;
            ViewGroup.LayoutParams layoutParams = this.imageView.getLayoutParams();
            // final int deviceWidth = Utils.displayMetrics.widthPixels;
            int resultingHeight = NumberUtils.getResultingHeight(this.requiredWidth, height, width);
            layoutParams.width = this.requiredWidth;
            layoutParams.height = resultingHeight;
            this.imageView.requestLayout();
        }
    }
}