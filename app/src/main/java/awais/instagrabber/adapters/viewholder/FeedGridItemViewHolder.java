package awais.instagrabber.adapters.viewholder;

import android.content.Context;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.adapters.FeedAdapterV2;
import awais.instagrabber.databinding.ItemFeedGridBinding;
import awais.instagrabber.models.PostsLayoutPreferences;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.TextUtils;

import static awais.instagrabber.models.PostsLayoutPreferences.PostsLayoutType.STAGGERED_GRID;

public class FeedGridItemViewHolder extends RecyclerView.ViewHolder {
    private final ItemFeedGridBinding binding;

    public FeedGridItemViewHolder(@NonNull ItemFeedGridBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bind(int position,
                     @NonNull Media media,
                     @NonNull PostsLayoutPreferences layoutPreferences,
                     FeedAdapterV2.FeedItemCallback feedItemCallback,
                     FeedAdapterV2.AdapterSelectionCallback adapterSelectionCallback,
                     boolean selectionModeActive,
                     boolean selected) {
        this.itemView.setOnClickListener(v -> {
            if (!selectionModeActive && feedItemCallback != null) {
                feedItemCallback.onPostClick(media);
                return;
            }
            if (selectionModeActive && adapterSelectionCallback != null) {
                adapterSelectionCallback.onPostClick(position, media);
            }
        });
        if (adapterSelectionCallback != null) {
            this.itemView.setOnLongClickListener(v -> adapterSelectionCallback.onPostLongClick(position, media));
        }
        this.binding.selectedView.setVisibility(selected ? View.VISIBLE : View.GONE);
        // for rounded borders (clip view to background shape)
        this.itemView.setClipToOutline(layoutPreferences.getHasRoundedCorners());
        if (layoutPreferences.getType() == STAGGERED_GRID) {
            float aspectRatio = (float) media.getOriginalWidth() / media.getOriginalHeight();
            this.binding.postImage.setAspectRatio(aspectRatio);
        } else {
            this.binding.postImage.setAspectRatio(1);
        }
        this.setUserDetails(media, layoutPreferences);
        String thumbnailUrl = null;
        int typeIconRes;
        MediaItemType mediaType = media.getType();
        if (mediaType == null) return;
        switch (mediaType) {
            case MEDIA_TYPE_IMAGE:
                typeIconRes = -1;
                thumbnailUrl = ResponseBodyUtils.getThumbUrl(media);
                break;
            case MEDIA_TYPE_VIDEO:
                thumbnailUrl = ResponseBodyUtils.getThumbUrl(media);
                typeIconRes = R.drawable.exo_icon_play;
                break;
            case MEDIA_TYPE_SLIDER:
                List<Media> sliderItems = media.getCarouselMedia();
                if (sliderItems != null) {
                    Media child = sliderItems.get(0);
                    if (child != null) {
                        thumbnailUrl = ResponseBodyUtils.getThumbUrl(child);
                        if (layoutPreferences.getType() == STAGGERED_GRID) {
                            float childAspectRatio = (float) child.getOriginalWidth() / child.getOriginalHeight();
                            this.binding.postImage.setAspectRatio(childAspectRatio);
                        }
                    }
                }
                typeIconRes = R.drawable.ic_checkbox_multiple_blank_stroke;
                break;
            default:
                typeIconRes = -1;
                thumbnailUrl = null;
        }
        this.setThumbImage(thumbnailUrl);
        if (typeIconRes <= 0) {
            this.binding.typeIcon.setVisibility(View.GONE);
        } else {
            this.binding.typeIcon.setVisibility(View.VISIBLE);
            this.binding.typeIcon.setImageResource(typeIconRes);
        }
        this.binding.downloaded.setVisibility(View.GONE);
        Context context = this.itemView.getContext();
        if (context == null) {
            return;
        }
        AppExecutors.INSTANCE.getTasksThread().execute(() -> {
            List<Boolean> checkList = DownloadUtils.checkDownloaded(media, context);
            if (checkList.isEmpty()) {
                return;
            }
            AppExecutors.INSTANCE.getMainThread().execute(() -> {
                switch (media.getType()) {
                    case MEDIA_TYPE_IMAGE:
                    case MEDIA_TYPE_VIDEO:
                        this.binding.downloaded.setVisibility(checkList.get(0) ? View.VISIBLE : View.GONE);
                        this.binding.downloaded.setImageTintList(ColorStateList.valueOf(this.itemView.getResources().getColor(R.color.green_A400)));
                        break;
                    case MEDIA_TYPE_SLIDER:
                        this.binding.downloaded.setVisibility(checkList.get(0) ? View.VISIBLE : View.GONE);
                        List<Media> carouselMedia = media.getCarouselMedia();
                        boolean allDownloaded = checkList.size() == (carouselMedia == null ? 0 : carouselMedia.size());
                        if (allDownloaded) {
                            allDownloaded = checkList.stream().allMatch(downloaded -> downloaded);
                        }
                        this.binding.downloaded.setImageTintList(ColorStateList.valueOf(this.itemView.getResources().getColor(
                                allDownloaded ? R.color.green_A400 : R.color.yellow_400)));
                        break;
                    default:
                }
            });
        });
    }

    private void setThumbImage(String thumbnailUrl) {
        if (TextUtils.isEmpty(thumbnailUrl)) {
            this.binding.postImage.setController(null);
            return;
        }
        ImageRequest requestBuilder = ImageRequestBuilder.newBuilderWithSource(Uri.parse(thumbnailUrl))
                                                               .setResizeOptions(ResizeOptions.forDimensions(this.binding.postImage.getWidth(),
                                                                       this.binding.postImage.getHeight()))
                                                               .setLocalThumbnailPreviewsEnabled(true)
                                                               .setProgressiveRenderingEnabled(true)
                                                               .build();
        PipelineDraweeControllerBuilder builder = Fresco.newDraweeControllerBuilder()
                                                              .setImageRequest(requestBuilder)
                                                              .setOldController(this.binding.postImage.getController());
        this.binding.postImage.setController(builder.build());
    }

    private void setUserDetails(@NonNull Media media,
                                @NonNull PostsLayoutPreferences layoutPreferences) {
        User user = media.getUser();
        if (layoutPreferences.isAvatarVisible()) {
            if (user == null) {
                this.binding.profilePic.setVisibility(View.GONE);
            } else {
                String profilePicUrl = user.getProfilePicUrl();
                if (TextUtils.isEmpty(profilePicUrl)) {
                    this.binding.profilePic.setVisibility(View.GONE);
                } else {
                    this.binding.profilePic.setVisibility(View.VISIBLE);
                    this.binding.profilePic.setImageURI(profilePicUrl);
                }
            }
            ViewGroup.LayoutParams layoutParams = this.binding.profilePic.getLayoutParams();
            @DimenRes int dimenRes;
            switch (layoutPreferences.getProfilePicSize()) {
                case SMALL:
                    dimenRes = R.dimen.profile_pic_size_small;
                    break;
                case TINY:
                    dimenRes = R.dimen.profile_pic_size_tiny;
                    break;
                default:
                case REGULAR:
                    dimenRes = R.dimen.profile_pic_size_regular;
                    break;
            }
            int dimensionPixelSize = this.itemView.getResources().getDimensionPixelSize(dimenRes);
            layoutParams.width = dimensionPixelSize;
            layoutParams.height = dimensionPixelSize;
            this.binding.profilePic.requestLayout();
        } else {
            this.binding.profilePic.setVisibility(View.GONE);
        }
        if (layoutPreferences.isNameVisible()) {
            if (user == null) {
                this.binding.name.setVisibility(View.GONE);
            } else {
                String username = user.getUsername();
                if (username == null) {
                    this.binding.name.setVisibility(View.GONE);
                } else {
                    this.binding.name.setVisibility(View.VISIBLE);
                    this.binding.name.setText(username);
                }
            }
        } else {
            this.binding.name.setVisibility(View.GONE);
        }
    }
}
