package awais.instagrabber.adapters.viewholder.directmessages;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;

import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.generic.RoundingParams;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Objects;

import awais.instagrabber.R;
import awais.instagrabber.adapters.DirectItemsAdapter;
import awais.instagrabber.customviews.DirectItemContextMenu;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmMediaShareBinding;
import awais.instagrabber.models.enums.DirectItemType;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.repositories.responses.Caption;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectItemClip;
import awais.instagrabber.repositories.responses.directmessages.DirectItemFelixShare;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.utils.NullSafePair;
import awais.instagrabber.utils.NumberUtils;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.Utils;

public class DirectItemMediaShareViewHolder extends DirectItemViewHolder {
    private static final String TAG = DirectItemMediaShareViewHolder.class.getSimpleName();

    private final LayoutDmMediaShareBinding binding;
    private final RoundingParams incomingRoundingParams;
    private final RoundingParams outgoingRoundingParams;
    private DirectItemType itemType;
    private Caption caption;

    public DirectItemMediaShareViewHolder(@NonNull LayoutDmBaseBinding baseBinding,
                                          @NonNull LayoutDmMediaShareBinding binding,
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
    public void bindItem(DirectItem item, MessageDirection messageDirection) {
        this.binding.topBg.setBackgroundResource(messageDirection == MessageDirection.INCOMING
                                            ? R.drawable.bg_media_share_top_incoming
                                            : R.drawable.bg_media_share_top_outgoing);
        final Media media = this.getMedia(item);
        if (media == null) return;
        this.itemView.post(() -> {
            this.setupUser(media);
            this.setupCaption(media);
        });
        int index;
        Media toDisplay;
        MediaItemType mediaType = media.getType();
        switch (mediaType) {
            case MEDIA_TYPE_SLIDER:
                toDisplay = media.getCarouselMedia().stream()
                        .filter(m -> media.getCarouselShareChildMediaId() != null &&
                                media.getCarouselShareChildMediaId().equals(m.getId()))
                        .findAny()
                        .orElse(media.getCarouselMedia().get(0));
                index = media.getCarouselMedia().indexOf(toDisplay);
                break;
            default:
                toDisplay = media;
                index = 0;
        }
        this.itemView.post(() -> {
            this.setupTypeIndicator(mediaType);
            this.setupPreview(toDisplay, messageDirection);
        });
        this.itemView.setOnClickListener(v -> this.openMedia(media, index));
    }

    private void setupTypeIndicator(MediaItemType mediaType) {
        boolean showTypeIcon = mediaType == MediaItemType.MEDIA_TYPE_VIDEO || mediaType == MediaItemType.MEDIA_TYPE_SLIDER;
        if (!showTypeIcon) {
            this.binding.typeIcon.setVisibility(View.GONE);
        } else {
            this.binding.typeIcon.setVisibility(View.VISIBLE);
            this.binding.typeIcon.setImageResource(mediaType == MediaItemType.MEDIA_TYPE_VIDEO
                                              ? R.drawable.ic_video_24
                                              : R.drawable.ic_checkbox_multiple_blank_stroke);
        }
    }

    private void setupPreview(@NonNull Media media,
                              MessageDirection messageDirection) {
        String url = ResponseBodyUtils.getThumbUrl(media);
        if (Objects.equals(url, this.binding.mediaPreview.getTag())) {
            return;
        }
        RoundingParams roundingParams = messageDirection == MessageDirection.INCOMING ? this.incomingRoundingParams : this.outgoingRoundingParams;
        this.binding.mediaPreview.setHierarchy(new GenericDraweeHierarchyBuilder(this.itemView.getResources())
                                                  .setActualImageScaleType(ScalingUtils.ScaleType.CENTER_CROP)
                                                  .setRoundingParams(roundingParams)
                                                  .build());
        NullSafePair<Integer, Integer> widthHeight = NumberUtils.calculateWidthHeight(
                media.getOriginalHeight(),
                media.getOriginalWidth(),
                this.mediaImageMaxHeight,
                this.mediaImageMaxWidth
        );
        ViewGroup.LayoutParams layoutParams = this.binding.mediaPreview.getLayoutParams();
        layoutParams.width = widthHeight.first;
        layoutParams.height = widthHeight.second;
        this.binding.mediaPreview.requestLayout();
        this.binding.mediaPreview.setTag(url);
        this.binding.mediaPreview.setImageURI(url);
    }

    private void setupCaption(@NonNull Media media) {
        this.caption = media.getCaption();
        if (this.caption != null) {
            this.binding.caption.setVisibility(View.VISIBLE);
            this.binding.caption.setText(this.caption.getText());
            this.binding.caption.setEllipsize(TextUtils.TruncateAt.END);
            this.binding.caption.setMaxLines(2);
        } else {
            this.binding.caption.setVisibility(View.GONE);
        }
    }

    private void setupUser(@NonNull Media media) {
        User user = media.getUser();
        if (user != null) {
            this.binding.username.setVisibility(View.VISIBLE);
            this.binding.profilePic.setVisibility(View.VISIBLE);
            this.binding.username.setText(user.getUsername());
            this.binding.profilePic.setImageURI(user.getProfilePicUrl());
        } else {
            this.binding.username.setVisibility(View.GONE);
            this.binding.profilePic.setVisibility(View.GONE);
        }
    }

    @Nullable
    private Media getMedia(@NonNull DirectItem item) {
        Media media = null;
        this.itemType = item.getItemType();
        if (this.itemType == DirectItemType.MEDIA_SHARE) {
            media = item.getMediaShare();
        } else if (this.itemType == DirectItemType.CLIP) {
            DirectItemClip clip = item.getClip();
            if (clip == null) return null;
            media = clip.getClip();
        } else if (this.itemType == DirectItemType.FELIX_SHARE) {
            DirectItemFelixShare felixShare = item.getFelixShare();
            if (felixShare == null) return null;
            media = felixShare.getVideo();
        }
        return media;
    }

    @Override
    protected int getReactionsTranslationY() {
        return this.reactionTranslationYType2;
    }

    @Override
    public int getSwipeDirection() {
        if (this.itemType != null && (this.itemType == DirectItemType.CLIP || this.itemType == DirectItemType.FELIX_SHARE)) {
            return ItemTouchHelper.ACTION_STATE_IDLE;
        }
        return super.getSwipeDirection();
    }

    @Override
    protected List<DirectItemContextMenu.MenuItem> getLongClickOptions() {
        ImmutableList.Builder<DirectItemContextMenu.MenuItem> builder = ImmutableList.builder();
        if (this.caption != null && !TextUtils.isEmpty(this.caption.getText())) {
            builder.add(new DirectItemContextMenu.MenuItem(R.id.copy, R.string.copy_caption, item -> {
                Utils.copyText(this.itemView.getContext(), this.caption.getText());
                return null;
            }));
        }
        return builder.build();
    }
}
