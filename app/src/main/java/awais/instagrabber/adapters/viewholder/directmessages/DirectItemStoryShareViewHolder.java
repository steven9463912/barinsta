package awais.instagrabber.adapters.viewholder.directmessages;

import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;

import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.generic.RoundingParams;

import awais.instagrabber.R;
import awais.instagrabber.adapters.DirectItemsAdapter;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmStoryShareBinding;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectItemStoryShare;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.utils.NullSafePair;
import awais.instagrabber.utils.NumberUtils;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.TextUtils;

public class DirectItemStoryShareViewHolder extends DirectItemViewHolder {

    private final LayoutDmStoryShareBinding binding;

    public DirectItemStoryShareViewHolder(@NonNull LayoutDmBaseBinding baseBinding,
                                          @NonNull LayoutDmStoryShareBinding binding,
                                          User currentUser,
                                          DirectThread thread,
                                          DirectItemsAdapter.DirectItemCallback callback) {
        super(baseBinding, currentUser, thread, callback);
        this.binding = binding;
        this.setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(DirectItem item, MessageDirection messageDirection) {
        Resources resources = this.itemView.getResources();
        int format = R.string.story_share;
        String reelType = item.getStoryShare().getReelType();
        if (reelType == null || item.getStoryShare().getMedia() == null) {
            this.setExpiredStoryInfo(item);
            return;
        }
        if (reelType.equals("highlight_reel")) {
            format = R.string.story_share_highlight;
        }
        User user = item.getStoryShare().getMedia().getUser();
        String info = resources.getString(format, user != null ? user.getUsername() : "");
        this.binding.shareInfo.setText(info);
        this.binding.text.setVisibility(View.GONE);
        this.binding.ivMediaPreview.setController(null);
        DirectItemStoryShare storyShare = item.getStoryShare();
        if (storyShare == null) return;
        this.setText(storyShare);
        Media media = storyShare.getMedia();
        this.setupPreview(messageDirection, media);
        this.itemView.setOnClickListener(v -> this.openStory(storyShare));
    }

    private void setupPreview(MessageDirection messageDirection, Media storyShareMedia) {
        MediaItemType mediaType = storyShareMedia.getType();
        this.binding.typeIcon.setVisibility(mediaType == MediaItemType.MEDIA_TYPE_VIDEO ? View.VISIBLE : View.GONE);
        RoundingParams roundingParams = messageDirection == MessageDirection.INCOMING
                                              ? RoundingParams.fromCornersRadii(this.dmRadiusSmall, this.dmRadius, this.dmRadius, this.dmRadius)
                                              : RoundingParams.fromCornersRadii(this.dmRadius, this.dmRadiusSmall, this.dmRadius, this.dmRadius);
        this.binding.ivMediaPreview.setHierarchy(new GenericDraweeHierarchyBuilder(this.itemView.getResources())
                                                    .setRoundingParams(roundingParams)
                                                    .setActualImageScaleType(ScalingUtils.ScaleType.CENTER_CROP)
                                                    .build());
        NullSafePair<Integer, Integer> widthHeight = NumberUtils.calculateWidthHeight(
                storyShareMedia.getOriginalHeight(),
                storyShareMedia.getOriginalWidth(),
                this.mediaImageMaxHeight,
                this.mediaImageMaxWidth
        );
        ViewGroup.LayoutParams layoutParams = this.binding.ivMediaPreview.getLayoutParams();
        layoutParams.width = widthHeight.first;
        layoutParams.height = widthHeight.second;
        this.binding.ivMediaPreview.requestLayout();
        String thumbUrl = ResponseBodyUtils.getThumbUrl(storyShareMedia);
        this.binding.ivMediaPreview.setImageURI(thumbUrl);
    }

    private void setText(DirectItemStoryShare storyShare) {
        String text = storyShare.getText();
        if (!TextUtils.isEmpty(text)) {
            this.binding.text.setText(text);
            this.binding.text.setVisibility(View.VISIBLE);
            return;
        }
        this.binding.text.setVisibility(View.GONE);
    }

    private void setExpiredStoryInfo(DirectItem item) {
        this.binding.shareInfo.setText(item.getStoryShare().getTitle());
        this.binding.text.setVisibility(View.VISIBLE);
        this.binding.text.setText(item.getStoryShare().getMessage());
        this.binding.ivMediaPreview.setVisibility(View.GONE);
        this.binding.typeIcon.setVisibility(View.GONE);
    }

    @Override
    protected boolean canForward() {
        return false;
    }

    @Override
    public int getSwipeDirection() {
        return ItemTouchHelper.ACTION_STATE_IDLE;
    }
}
