package awais.instagrabber.adapters.viewholder;

import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import awais.instagrabber.adapters.SliderItemsAdapter;
import awais.instagrabber.customviews.drawee.AnimatedZoomableController;
import awais.instagrabber.customviews.drawee.DoubleTapGestureListener;
import awais.instagrabber.databinding.ItemSliderPhotoBinding;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.utils.ResponseBodyUtils;

public class SliderPhotoViewHolder extends SliderItemViewHolder {
    private static final String TAG = "FeedSliderPhotoViewHolder";

    private final ItemSliderPhotoBinding binding;

    public SliderPhotoViewHolder(@NonNull ItemSliderPhotoBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bind(@NonNull Media model,
                     int position,
                     SliderItemsAdapter.SliderCallback sliderCallback) {
        ImageRequest requestBuilder = ImageRequestBuilder
                .newBuilderWithSource(Uri.parse(ResponseBodyUtils.getImageUrl(model)))
                .setLocalThumbnailPreviewsEnabled(true)
                .build();
        this.binding.getRoot()
               .setController(Fresco.newDraweeControllerBuilder()
                                    .setImageRequest(requestBuilder)
                                    .setControllerListener(new BaseControllerListener<ImageInfo>() {
                                        @Override
                                        public void onFailure(String id, Throwable throwable) {
                                            if (sliderCallback != null) {
                                                sliderCallback.onThumbnailLoaded(position);
                                            }
                                        }

                                        @Override
                                        public void onFinalImageSet(String id,
                                                                    ImageInfo imageInfo,
                                                                    Animatable animatable) {
                                            if (sliderCallback != null) {
                                                sliderCallback.onThumbnailLoaded(position);
                                            }
                                        }
                                    })
                                    .setLowResImageRequest(ImageRequest.fromUri(ResponseBodyUtils.getThumbUrl(model)))
                                    .build());
        DoubleTapGestureListener tapListener = new DoubleTapGestureListener(this.binding.getRoot()) {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (sliderCallback != null) {
                    sliderCallback.onItemClicked(position, model, SliderPhotoViewHolder.this.binding.getRoot());
                }
                return super.onSingleTapConfirmed(e);
            }
        };
        this.binding.getRoot().setTapListener(tapListener);
        AnimatedZoomableController zoomableController = AnimatedZoomableController.newInstance();
        zoomableController.setMaxScaleFactor(3f);
        this.binding.getRoot().setZoomableController(zoomableController);
        this.binding.getRoot().setZoomingEnabled(true);
    }
}
