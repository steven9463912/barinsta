package awais.instagrabber.adapters.viewholder.feed;

import android.net.Uri;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import awais.instagrabber.adapters.FeedAdapterV2;
import awais.instagrabber.databinding.ItemFeedPhotoBinding;
import awais.instagrabber.databinding.LayoutPostViewBottomBinding;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.TextUtils;

public class FeedPhotoViewHolder extends FeedItemViewHolder {
    private static final String TAG = "FeedPhotoViewHolder";

    private final ItemFeedPhotoBinding binding;
    private final FeedAdapterV2.FeedItemCallback feedItemCallback;

    public FeedPhotoViewHolder(@NonNull ItemFeedPhotoBinding binding,
                               FeedAdapterV2.FeedItemCallback feedItemCallback) {
        super(binding.getRoot(), feedItemCallback);
        this.binding = binding;
        this.feedItemCallback = feedItemCallback;
        LayoutPostViewBottomBinding bottom = LayoutPostViewBottomBinding.bind(binding.getRoot());
        bottom.viewsCount.setVisibility(View.GONE);
        // binding.itemFeedBottom.btnMute.setVisibility(View.GONE);
        binding.imageViewer.setAllowTouchInterceptionWhileZoomed(false);
        GenericDraweeHierarchy hierarchy = new GenericDraweeHierarchyBuilder(this.itemView.getContext().getResources())
                .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
                .build();
        binding.imageViewer.setHierarchy(hierarchy);
    }

    @Override
    public void bindItem(Media media) {
        if (media == null) return;
        this.binding.getRoot().post(() -> {
            this.setDimensions(media);
            String thumbnailUrl = ResponseBodyUtils.getThumbUrl(media);
            String url = ResponseBodyUtils.getImageUrl(media);
            if (TextUtils.isEmpty(url)) url = thumbnailUrl;
            ImageRequest requestBuilder = ImageRequestBuilder.newBuilderWithSource(Uri.parse(url))
                                                                   // .setLocalThumbnailPreviewsEnabled(true)
                                                                   // .setProgressiveRenderingEnabled(true)
                                                                   .build();
            this.binding.imageViewer.setController(Fresco.newDraweeControllerBuilder()
                                                    .setImageRequest(requestBuilder)
                                                    .setOldController(this.binding.imageViewer.getController())
                                                    .setLowResImageRequest(ImageRequest.fromUri(thumbnailUrl))
                                                    .build());
            this.binding.imageViewer.setTapListener(new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    if (FeedPhotoViewHolder.this.feedItemCallback != null) {
                        FeedPhotoViewHolder.this.feedItemCallback.onPostClick(media);
                        return true;
                    }
                    return false;
                }
            });
        });
    }

    private void setDimensions(Media feedModel) {
        float aspectRatio = (float) feedModel.getOriginalWidth() / feedModel.getOriginalHeight();
        this.binding.imageViewer.setAspectRatio(aspectRatio);
    }
}
