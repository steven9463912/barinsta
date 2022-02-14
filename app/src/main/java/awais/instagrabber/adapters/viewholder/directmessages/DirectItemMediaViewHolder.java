package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.generic.RoundingParams;

import awais.instagrabber.adapters.DirectItemsAdapter;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmMediaBinding;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.utils.NullSafePair;
import awais.instagrabber.utils.NumberUtils;
import awais.instagrabber.utils.ResponseBodyUtils;

public class DirectItemMediaViewHolder extends DirectItemViewHolder {

    private final LayoutDmMediaBinding binding;
    private final RoundingParams incomingRoundingParams;
    private final RoundingParams outgoingRoundingParams;

    public DirectItemMediaViewHolder(@NonNull LayoutDmBaseBinding baseBinding,
                                     @NonNull LayoutDmMediaBinding binding,
                                     User currentUser,
                                     DirectThread thread,
                                     DirectItemsAdapter.DirectItemCallback callback) {
        super(baseBinding, currentUser, thread, callback);
        this.binding = binding;
        this.incomingRoundingParams = RoundingParams.fromCornersRadii(this.dmRadiusSmall, this.dmRadius, this.dmRadius, this.dmRadius);
        this.outgoingRoundingParams = RoundingParams.fromCornersRadii(this.dmRadius, this.dmRadiusSmall, this.dmRadius, this.dmRadius);
        this.setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(DirectItem directItemModel, MessageDirection messageDirection) {
        RoundingParams roundingParams = messageDirection == MessageDirection.INCOMING ? this.incomingRoundingParams : this.outgoingRoundingParams;
        this.binding.mediaPreview.setHierarchy(new GenericDraweeHierarchyBuilder(this.itemView.getResources())
                                                  .setRoundingParams(roundingParams)
                                                  .setActualImageScaleType(ScalingUtils.ScaleType.CENTER_CROP)
                                                  .build());
        Media media = directItemModel.getMedia();
        this.itemView.setOnClickListener(v -> this.openMedia(media, -1));
        MediaItemType modelMediaType = media.getType();
        this.binding.typeIcon.setVisibility(modelMediaType == MediaItemType.MEDIA_TYPE_VIDEO || modelMediaType == MediaItemType.MEDIA_TYPE_SLIDER
                                       ? View.VISIBLE
                                       : View.GONE);
        NullSafePair<Integer, Integer> widthHeight = NumberUtils.calculateWidthHeight(
                media.getOriginalHeight(),
                media.getOriginalWidth(),
                this.mediaImageMaxHeight,
                this.mediaImageMaxWidth
        );
        ViewGroup.LayoutParams layoutParams = this.binding.mediaPreview.getLayoutParams();
        int width = widthHeight.first;
        layoutParams.width = width;
        layoutParams.height = widthHeight.second;
        this.binding.mediaPreview.requestLayout();
        this.binding.bgTime.getLayoutParams().width = width;
        this.binding.bgTime.requestLayout();
        String thumbUrl = ResponseBodyUtils.getThumbUrl(media);
        this.binding.mediaPreview.setImageURI(thumbUrl);
    }

}
