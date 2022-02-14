package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.Gravity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.generic.RoundingParams;
import com.google.common.collect.ImmutableList;

import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.adapters.DirectItemsAdapter;
import awais.instagrabber.customviews.DirectItemContextMenu;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmReelShareBinding;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectItemReelShare;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

public class DirectItemReelShareViewHolder extends DirectItemViewHolder {

    private final LayoutDmReelShareBinding binding;
    private String type;

    public DirectItemReelShareViewHolder(@NonNull LayoutDmBaseBinding baseBinding,
                                         @NonNull LayoutDmReelShareBinding binding,
                                         User currentUser,
                                         DirectThread thread,
                                         DirectItemsAdapter.DirectItemCallback callback) {
        super(baseBinding, currentUser, thread, callback);
        this.binding = binding;
        this.setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(DirectItem item, MessageDirection messageDirection) {
        DirectItemReelShare reelShare = item.getReelShare();
        this.type = reelShare.getType();
        if (this.type == null) return;
        boolean isSelf = this.isSelf(item);
        Media media = reelShare.getMedia();
        if (media == null) return;
        User user = media.getUser();
        if (user == null) return;
        boolean expired = media.getType() == null;
        if (expired) {
            this.binding.preview.setVisibility(View.GONE);
            this.binding.typeIcon.setVisibility(View.GONE);
            this.binding.quoteLine.setVisibility(View.GONE);
            this.binding.reaction.setVisibility(View.GONE);
        } else {
            this.binding.preview.setVisibility(View.VISIBLE);
            this.binding.typeIcon.setVisibility(View.VISIBLE);
            this.binding.quoteLine.setVisibility(View.VISIBLE);
            this.binding.reaction.setVisibility(View.VISIBLE);
        }
        this.setGravity(messageDirection, expired);
        if (this.type.equals("reply")) {
            this.setReply(messageDirection, reelShare, isSelf);
        }
        if (this.type.equals("reaction")) {
            this.setReaction(messageDirection, reelShare, isSelf, expired);
        }
        if (this.type.equals("mention")) {
            this.setMention(isSelf);
        }
        if (!expired) {
            this.setPreview(media);
            this.itemView.setOnClickListener(v -> this.openMedia(media, -1));
        }
    }

    private void setGravity(MessageDirection messageDirection, boolean expired) {
        boolean isIncoming = messageDirection == MessageDirection.INCOMING;
        this.binding.shareInfo.setGravity(isIncoming ? Gravity.START : Gravity.END);
        if (!expired) {
            this.binding.quoteLine.setVisibility(isIncoming ? View.VISIBLE : View.GONE);
            this.binding.quoteLineEnd.setVisibility(isIncoming ? View.GONE : View.VISIBLE);
        }
        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) this.binding.quoteLine.getLayoutParams();
        layoutParams.horizontalBias = isIncoming ? 0 : 1;
        ConstraintLayout.LayoutParams messageLayoutParams = (ConstraintLayout.LayoutParams) this.binding.message.getLayoutParams();
        messageLayoutParams.startToStart = isIncoming ? ConstraintLayout.LayoutParams.PARENT_ID : ConstraintLayout.LayoutParams.UNSET;
        messageLayoutParams.endToEnd = isIncoming ? ConstraintLayout.LayoutParams.UNSET : ConstraintLayout.LayoutParams.PARENT_ID;
        messageLayoutParams.setMarginStart(isIncoming ? this.messageInfoPaddingSmall : 0);
        messageLayoutParams.setMarginEnd(isIncoming ? 0 : this.messageInfoPaddingSmall);
        ConstraintLayout.LayoutParams reactionLayoutParams = (ConstraintLayout.LayoutParams) this.binding.reaction.getLayoutParams();
        int previewId = this.binding.preview.getId();
        if (isIncoming) {
            reactionLayoutParams.startToEnd = previewId;
            reactionLayoutParams.endToEnd = previewId;
            reactionLayoutParams.startToStart = ConstraintLayout.LayoutParams.UNSET;
            reactionLayoutParams.endToStart = ConstraintLayout.LayoutParams.UNSET;
        } else {
            reactionLayoutParams.startToStart = previewId;
            reactionLayoutParams.endToStart = previewId;
            reactionLayoutParams.startToEnd = ConstraintLayout.LayoutParams.UNSET;
            reactionLayoutParams.endToEnd = ConstraintLayout.LayoutParams.UNSET;
        }
    }

    private void setReply(MessageDirection messageDirection,
                          DirectItemReelShare reelShare,
                          boolean isSelf) {
        int info = isSelf ? R.string.replied_story_outgoing : R.string.replied_story_incoming;
        this.binding.shareInfo.setText(info);
        this.binding.reaction.setVisibility(View.GONE);
        String text = reelShare.getText();
        if (TextUtils.isEmpty(text)) {
            this.binding.message.setVisibility(View.GONE);
            return;
        }
        this.setMessage(messageDirection, text);
    }

    private void setReaction(MessageDirection messageDirection,
                             DirectItemReelShare reelShare,
                             boolean isSelf,
                             boolean expired) {
        int info = isSelf ? R.string.reacted_story_outgoing : R.string.reacted_story_incoming;
        this.binding.shareInfo.setText(info);
        this.binding.message.setVisibility(View.GONE);
        String text = reelShare.getText();
        if (TextUtils.isEmpty(text)) {
            this.binding.reaction.setVisibility(View.GONE);
            return;
        }
        if (expired) {
            this.setMessage(messageDirection, text);
            return;
        }
        this.binding.reaction.setVisibility(View.VISIBLE);
        this.binding.reaction.setText(text);
    }

    private void setMention(boolean isSelf) {
        int info = isSelf ? R.string.mentioned_story_outgoing : R.string.mentioned_story_incoming;
        this.binding.shareInfo.setText(info);
        this.binding.message.setVisibility(View.GONE);
        this.binding.reaction.setVisibility(View.GONE);
    }

    private void setMessage(MessageDirection messageDirection, String text) {
        this.binding.message.setVisibility(View.VISIBLE);
        this.binding.message.setBackgroundResource(messageDirection == MessageDirection.INCOMING
                                              ? R.drawable.bg_speech_bubble_incoming
                                              : R.drawable.bg_speech_bubble_outgoing);
        this.binding.message.setText(text);
    }

    private void setPreview(Media media) {
        MediaItemType mediaType = media.getType();
        if (mediaType == null) return;
        this.binding.typeIcon.setVisibility(mediaType == MediaItemType.MEDIA_TYPE_VIDEO || mediaType == MediaItemType.MEDIA_TYPE_SLIDER
                                       ? View.VISIBLE : View.GONE);
        RoundingParams roundingParams = RoundingParams.fromCornersRadii(this.dmRadiusSmall, this.dmRadiusSmall, this.dmRadiusSmall, this.dmRadiusSmall);
        this.binding.preview.setHierarchy(new GenericDraweeHierarchyBuilder(this.itemView.getResources())
                                             .setRoundingParams(roundingParams)
                                             .build());
        String thumbUrl = ResponseBodyUtils.getThumbUrl(media);
        this.binding.preview.setImageURI(thumbUrl);
    }

    @Override
    protected boolean canForward() {
        return false;
    }

    @Override
    protected List<DirectItemContextMenu.MenuItem> getLongClickOptions() {
        ImmutableList.Builder<DirectItemContextMenu.MenuItem> builder = ImmutableList.builder();
        if ("reply".equals(this.type)) {
            builder.add(new DirectItemContextMenu.MenuItem(R.id.copy, R.string.copy_reply, item -> {
                DirectItemReelShare reelShare = item.getReelShare();
                if (reelShare == null) return null;
                String text = reelShare.getText();
                if (TextUtils.isEmpty(text)) return null;
                Utils.copyText(this.itemView.getContext(), text);
                return null;
            }));
        }
        return builder.build();
    }
}
