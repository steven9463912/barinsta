package awais.instagrabber.adapters.viewholder.directmessages;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;

import com.google.android.material.transition.MaterialFade;
import com.google.common.collect.ImmutableList;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import awais.instagrabber.R;
import awais.instagrabber.adapters.DirectItemsAdapter;
import awais.instagrabber.customviews.DirectItemContextMenu;
import awais.instagrabber.customviews.DirectItemFrameLayout;
import awais.instagrabber.customviews.RamboTextViewV2;
import awais.instagrabber.customviews.helpers.SwipeAndRestoreItemTouchHelperCallback;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.models.enums.DirectItemType;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectItemEmojiReaction;
import awais.instagrabber.repositories.responses.directmessages.DirectItemReactions;
import awais.instagrabber.repositories.responses.directmessages.DirectItemStoryShare;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.utils.DMUtils;
import awais.instagrabber.utils.DeepLinkParser;
import awais.instagrabber.utils.ResponseBodyUtils;

public abstract class DirectItemViewHolder extends RecyclerView.ViewHolder implements SwipeAndRestoreItemTouchHelperCallback.SwipeableViewHolder {
    private static final String TAG = DirectItemViewHolder.class.getSimpleName();
    // private static final List<Integer> THREAD_CHANGING_OPTIONS = ImmutableList.of(R.id.unsend);

    private final LayoutDmBaseBinding binding;
    private final User currentUser;
    private final DirectThread thread;
    private final int groupMessageWidth;
    private final List<Long> userIds;
    private final DirectItemsAdapter.DirectItemCallback callback;
    private final int reactionAdjustMargin;
    private final AccelerateDecelerateInterpolator accelerateDecelerateInterpolator = new AccelerateDecelerateInterpolator();

    protected final int margin;
    protected final int dmRadius;
    protected final int dmRadiusSmall;
    protected final int messageInfoPaddingSmall;
    protected final int mediaImageMaxHeight;
    protected final int windowWidth;
    protected final int mediaImageMaxWidth;
    protected final int reactionTranslationYType1;
    protected final int reactionTranslationYType2;

    private boolean selected;
    private DirectItemsAdapter.DirectItemInternalLongClickListener longClickListener;
    private DirectItem item;
    private ViewPropertyAnimator shrinkGrowAnimator;
    private MessageDirection messageDirection;
    // private View.OnLayoutChangeListener layoutChangeListener;

    public DirectItemViewHolder(@NonNull final LayoutDmBaseBinding binding,
                                @NonNull final User currentUser,
                                @NonNull final DirectThread thread,
                                @NonNull final DirectItemsAdapter.DirectItemCallback callback) {
        super(binding.getRoot());
        this.binding = binding;
        this.currentUser = currentUser;
        this.thread = thread;
        this.callback = callback;
        this.userIds = thread.getUsers()
                        .stream()
                        .map(User::getPk)
                        .collect(Collectors.toList());
        binding.ivProfilePic.setVisibility(thread.isGroup() ? View.VISIBLE : View.GONE);
        binding.ivProfilePic.setOnClickListener(null);
        Resources resources = this.itemView.getResources();
        this.margin = resources.getDimensionPixelSize(R.dimen.dm_message_item_margin);
        int avatarSize = resources.getDimensionPixelSize(R.dimen.dm_message_item_avatar_size);
        this.dmRadius = resources.getDimensionPixelSize(R.dimen.dm_message_card_radius);
        this.dmRadiusSmall = resources.getDimensionPixelSize(R.dimen.dm_message_card_radius_small);
        this.messageInfoPaddingSmall = resources.getDimensionPixelSize(R.dimen.dm_message_info_padding_small);
        this.windowWidth = resources.getDisplayMetrics().widthPixels;
        this.mediaImageMaxHeight = resources.getDimensionPixelSize(R.dimen.dm_media_img_max_height);
        this.reactionAdjustMargin = resources.getDimensionPixelSize(R.dimen.dm_reaction_adjust_margin);
        int groupWidthCorrection = avatarSize + this.messageInfoPaddingSmall * 3;
        this.mediaImageMaxWidth = this.windowWidth - this.margin - (thread.isGroup() ? groupWidthCorrection : this.messageInfoPaddingSmall * 2);
        // messageInfoPaddingSmall is used cuz it's also 4dp, 1 avatar margin + 2 paddings = 3
        this.groupMessageWidth = this.windowWidth - this.margin - groupWidthCorrection;
        this.reactionTranslationYType1 = resources.getDimensionPixelSize(R.dimen.dm_reaction_translation_y_type_1);
        this.reactionTranslationYType2 = resources.getDimensionPixelSize(R.dimen.dm_reaction_translation_y_type_2);
    }

    public void bind(int position, DirectItem item) {
        if (item == null) return;
        this.item = item;
        this.messageDirection = this.isSelf(item) ? MessageDirection.OUTGOING : MessageDirection.INCOMING;
        // Asynchronous binding causes some weird behaviour
        // itemView.post(() -> bindBase(item, messageDirection, position));
        // itemView.post(() -> bindItem(item, messageDirection));
        // itemView.post(() -> setupLongClickListener(position, messageDirection));
        this.bindBase(item, this.messageDirection, position);
        this.bindItem(item, this.messageDirection);
        this.setupLongClickListener(position, this.messageDirection);
    }

    private void bindBase(@NonNull DirectItem item, MessageDirection messageDirection, int position) {
        FrameLayout.LayoutParams containerLayoutParams = (FrameLayout.LayoutParams) this.binding.container.getLayoutParams();
        DirectItemType itemType = item.getItemType() == null ? DirectItemType.UNKNOWN : item.getItemType();
        this.setMessageDirectionGravity(messageDirection, containerLayoutParams);
        this.setGroupUserDetails(item, messageDirection);
        this.setBackground(messageDirection);
        this.setMessageInfo(item, messageDirection);
        switch (itemType) {
            case REEL_SHARE:
            case STORY_SHARE: // i think they could have texts?
//                containerLayoutParams.setMarginStart(0);
//                containerLayoutParams.setMarginEnd(0);
            case TEXT:
            case LINK:
            case UNKNOWN:
                this.binding.messageInfo.setPadding(0, 0, this.dmRadius, this.dmRadiusSmall);
                break;
            default:
                if (this.showMessageInfo()) {
                    this.binding.messageInfo.setPadding(0, 0, this.messageInfoPaddingSmall, this.dmRadiusSmall);
                }
        }
        this.setupReply(item, messageDirection);
        this.setReactions(item, position);
        if (item.getRepliedToMessage() == null && item.getShowForwardAttribution()) {
            this.setForwardInfo(messageDirection);
        }
    }

    private void setBackground(MessageDirection messageDirection) {
        if (this.showBackground()) {
            this.binding.background.setBackgroundResource(messageDirection == MessageDirection.INCOMING ? R.drawable.bg_speech_bubble_incoming
                                                                                                   : R.drawable.bg_speech_bubble_outgoing);
            return;
        }
        this.binding.background.setBackgroundResource(0);
    }

    private void setGroupUserDetails(DirectItem item, MessageDirection messageDirection) {
        if (this.showUserDetailsInGroup()) {
            this.binding.ivProfilePic.setVisibility(messageDirection == MessageDirection.INCOMING && this.thread.isGroup() ? View.VISIBLE : View.GONE);
            this.binding.tvUsername.setVisibility(messageDirection == MessageDirection.INCOMING && this.thread.isGroup() ? View.VISIBLE : View.GONE);
            if (messageDirection == MessageDirection.INCOMING && this.thread.isGroup()) {
                List<User> allUsers = new LinkedList(this.thread.getUsers());
                allUsers.addAll(this.thread.getLeftUsers());
                User user = this.getUser(item.getUserId(), allUsers);
                if (user != null) {
                    this.binding.tvUsername.setText(user.getUsername());
                    this.binding.ivProfilePic.setImageURI(user.getProfilePicUrl());
                }
                final ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) this.binding.chatMessageLayout.getLayoutParams();
                layoutParams.matchConstraintMaxWidth = this.groupMessageWidth;
                this.binding.chatMessageLayout.setLayoutParams(layoutParams);
            }
            return;
        }
        this.binding.ivProfilePic.setVisibility(View.GONE);
        this.binding.tvUsername.setVisibility(View.GONE);
    }

    private void setMessageDirectionGravity(MessageDirection messageDirection,
                                            FrameLayout.LayoutParams containerLayoutParams) {
        if (this.allowMessageDirectionGravity()) {
            containerLayoutParams.setMarginStart(messageDirection == MessageDirection.OUTGOING ? this.margin : 0);
            containerLayoutParams.setMarginEnd(messageDirection == MessageDirection.INCOMING ? this.margin : 0);
            containerLayoutParams.gravity = messageDirection == MessageDirection.INCOMING ? Gravity.START : Gravity.END;
            return;
        }
        containerLayoutParams.gravity = Gravity.CENTER;
    }

    private void setMessageInfo(@NonNull DirectItem item, MessageDirection messageDirection) {
        if (this.showMessageInfo()) {
            this.binding.messageInfo.setVisibility(View.VISIBLE);
            this.binding.deliveryStatus.setVisibility(messageDirection == MessageDirection.OUTGOING ? View.VISIBLE : View.GONE);
            if (item.getDate() != null) {
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
                this.binding.messageTime.setText(dateFormatter.format(item.getDate()));
            }
            if (messageDirection == MessageDirection.OUTGOING) {
                if (item.isPending()) {
                    this.binding.deliveryStatus.setImageResource(R.drawable.ic_check_24);
                } else {
                    boolean read = DMUtils.isRead(item,
                            this.thread.getLastSeenAt(),
                            this.userIds
                    );
                    this.binding.deliveryStatus.setImageResource(R.drawable.ic_check_all_24);
                    ImageViewCompat.setImageTintList(
                            this.binding.deliveryStatus,
                            ColorStateList.valueOf(this.itemView.getResources().getColor(read ? R.color.blue_500 : R.color.grey_500))
                    );
                }
            }
            return;
        }
        this.binding.messageInfo.setVisibility(View.GONE);
    }

    private void setupReply(DirectItem item, MessageDirection messageDirection) {
        if (item.getRepliedToMessage() != null) {
            List<User> allUsers = new LinkedList(this.thread.getUsers());
            allUsers.addAll(this.thread.getLeftUsers());
            this.setReply(item, messageDirection, allUsers);
        } else {
            this.binding.quoteLine.setVisibility(View.GONE);
            this.binding.replyContainer.setVisibility(View.GONE);
            this.binding.replyInfo.setVisibility(View.GONE);
        }
    }

    private void setReply(DirectItem item,
                          MessageDirection messageDirection,
                          List<User> users) {
        DirectItem replied = item.getRepliedToMessage();
        DirectItemType itemType = replied.getItemType();
        Resources resources = this.itemView.getResources();
        String text = null;
        String url = null;
        switch (itemType) {
            case TEXT:
                text = replied.getText();
                break;
            case LINK:
                text = replied.getLink().getText();
                break;
            case PLACEHOLDER:
                text = replied.getPlaceholder().getMessage();
                break;
            case MEDIA:
                url = ResponseBodyUtils.getThumbUrl(replied.getMedia());
                break;
            case RAVEN_MEDIA:
                url = ResponseBodyUtils.getThumbUrl(replied.getVisualMedia().getMedia());
                break;
            case VOICE_MEDIA:
                text = resources.getString(R.string.voice_message);
                break;
            case MEDIA_SHARE:
                Media mediaShare = replied.getMediaShare();
                if (mediaShare.getType() == MediaItemType.MEDIA_TYPE_SLIDER) {
                    mediaShare = mediaShare.getCarouselMedia().get(0);
                }
                url = ResponseBodyUtils.getThumbUrl(mediaShare);
                break;
            case REEL_SHARE:
                text = replied.getReelShare().getText();
                break;
            // Below types cannot be replied to
            // case LIKE:
            //     text = "❤️";
            //     break;
            // case PROFILE:
            //     text = "@" + replied.getProfile().getUsername();
            //     break;
            // case CLIP:
            //     url = ResponseBodyUtils.getThumbUrl(replied.getClip().getClip().getImageVersions2());
            //     break;
            // case FELIX_SHARE:
            //     url = ResponseBodyUtils.getThumbUrl(replied.getFelixShare().getVideo().getImageVersions2());
            //     break;
            // case STORY_SHARE:
            //     final DirectItemMedia media = replied.getStoryShare().getMedia();
            //     if (media == null) break;
            //     url = ResponseBodyUtils.getThumbUrl(media.getImageVersions2());
            //     break;
            // case LOCATION
        }
        if (text == null && url == null) {
            this.binding.quoteLine.setVisibility(View.GONE);
            this.binding.replyContainer.setVisibility(View.GONE);
            this.binding.replyInfo.setVisibility(View.GONE);
            return;
        }
        this.setReplyGravity(messageDirection);
        String info = this.setReplyInfo(item, replied, users, resources);
        this.binding.replyInfo.setVisibility(View.VISIBLE);
        this.binding.replyInfo.setText(info);
        this.binding.quoteLine.setVisibility(View.VISIBLE);
        this.binding.replyContainer.setVisibility(View.VISIBLE);
        if (url != null) {
            this.binding.replyText.setVisibility(View.GONE);
            this.binding.replyImage.setVisibility(View.VISIBLE);
            this.binding.replyImage.setImageURI(url);
            return;
        }
        this.binding.replyImage.setVisibility(View.GONE);
        Drawable background = this.binding.replyText.getBackground().mutate();
        background.setTint(replied.getUserId() != this.currentUser.getPk()
                           ? resources.getColor(R.color.grey_600)
                           : resources.getColor(R.color.deep_purple_400));
        this.binding.replyText.setBackgroundDrawable(background);
        this.binding.replyText.setVisibility(View.VISIBLE);
        this.binding.replyText.setText(text);
    }

    private String setReplyInfo(DirectItem item,
                                DirectItem replied,
                                List<User> users,
                                Resources resources) {
        long repliedToUserId = replied.getUserId();
        if (repliedToUserId == item.getUserId() && item.getUserId() == this.currentUser.getPk()) {
            // User replied to own message
            return resources.getString(R.string.replied_to_yourself);
        }
        if (repliedToUserId == item.getUserId()) {
            // opposite user replied to their own message
            return resources.getString(R.string.replied_to_themself);
        }
        User user = this.getUser(repliedToUserId, users);
        String repliedToUsername = user != null ? user.getUsername() : "";
        if (item.getUserId() == this.currentUser.getPk()) {
            return this.thread.isGroup()
                   ? resources.getString(R.string.replied_you_group, repliedToUsername)
                   : resources.getString(R.string.replied_you);
        }
        if (repliedToUserId == this.currentUser.getPk()) {
            return resources.getString(R.string.replied_to_you);
        }
        return resources.getString(R.string.replied_group, repliedToUsername);
    }

    private void setForwardInfo(MessageDirection direction) {
        this.binding.replyInfo.setVisibility(View.VISIBLE);
        this.binding.replyInfo.setText(direction == MessageDirection.OUTGOING ? R.string.forward_outgoing : R.string.forward_incoming);
    }

    private void setReplyGravity(MessageDirection messageDirection) {
        boolean isIncoming = messageDirection == MessageDirection.INCOMING;
        ConstraintLayout.LayoutParams quoteLineLayoutParams = (ConstraintLayout.LayoutParams) this.binding.quoteLine.getLayoutParams();
        ConstraintLayout.LayoutParams replyContainerLayoutParams = (ConstraintLayout.LayoutParams) this.binding.replyContainer.getLayoutParams();
        ConstraintLayout.LayoutParams replyInfoLayoutParams = (ConstraintLayout.LayoutParams) this.binding.replyInfo.getLayoutParams();
        int profilePicId = this.binding.ivProfilePic.getId();
        int replyContainerId = this.binding.replyContainer.getId();
        int quoteLineId = this.binding.quoteLine.getId();
        quoteLineLayoutParams.startToEnd = isIncoming ? profilePicId : replyContainerId;
        quoteLineLayoutParams.endToStart = isIncoming ? replyContainerId : ConstraintLayout.LayoutParams.UNSET;
        quoteLineLayoutParams.endToEnd = isIncoming ? ConstraintLayout.LayoutParams.UNSET : ConstraintLayout.LayoutParams.PARENT_ID;
        replyContainerLayoutParams.startToEnd = isIncoming ? quoteLineId : profilePicId;
        replyContainerLayoutParams.endToEnd = isIncoming ? ConstraintLayout.LayoutParams.PARENT_ID : ConstraintLayout.LayoutParams.UNSET;
        replyContainerLayoutParams.endToStart = isIncoming ? ConstraintLayout.LayoutParams.UNSET : quoteLineId;
        replyInfoLayoutParams.startToEnd = isIncoming ? quoteLineId : ConstraintLayout.LayoutParams.UNSET;
        replyInfoLayoutParams.endToStart = isIncoming ? ConstraintLayout.LayoutParams.UNSET : quoteLineId;
    }

    private void setReactions(DirectItem item, int position) {
        this.binding.getRoot().post(() -> {
            final MaterialFade materialFade = new MaterialFade();
            materialFade.addTarget(this.binding.emojis);
            TransitionManager.beginDelayedTransition(this.binding.getRoot(), materialFade);
            DirectItemReactions reactions = item.getReactions();
            List<DirectItemEmojiReaction> emojis = reactions != null ? reactions.getEmojis() : null;
            if (emojis == null || emojis.isEmpty()) {
                this.binding.container.setPadding(this.messageInfoPaddingSmall, this.messageInfoPaddingSmall, this.messageInfoPaddingSmall, 0);
                this.binding.reactionsWrapper.setVisibility(View.GONE);
                return;
            }
            this.binding.reactionsWrapper.setVisibility(View.VISIBLE);
            this.binding.reactionsWrapper.setTranslationY(this.getReactionsTranslationY());
            this.binding.container.setPadding(this.messageInfoPaddingSmall, this.messageInfoPaddingSmall, this.messageInfoPaddingSmall, this.reactionAdjustMargin);
            this.binding.emojis.setEmojis(emojis.stream()
                                           .map(DirectItemEmojiReaction::getEmoji)
                                           .collect(Collectors.toList()));
            // binding.emojis.setEmojis(ImmutableList.of("😣",
            //                                           "😖",
            //                                           "😫",
            //                                           "😩",
            //                                           "🥺",
            //                                           "😢",
            //                                           "😭",
            //                                           "😤",
            //                                           "😠",
            //                                           "😡",
            //                                           "🤬"));
            this.binding.emojis.setOnClickListener(v -> this.callback.onReactionClick(item, position));
            // final List<DirectUser> reactedUsers = emojis.stream()
            //                                             .map(DirectItemEmojiReaction::getSenderId)
            //                                             .distinct()
            //                                             .map(userId -> getUser(userId, users))
            //                                             .collect(Collectors.toList());
            // for (final DirectUser user : reactedUsers) {
            //     if (user == null) continue;
            //     final ProfilePicView profilePicView = new ProfilePicView(itemView.getContext());
            //     profilePicView.setSize(ProfilePicView.Size.TINY);
            //     profilePicView.setImageURI(user.getProfilePicUrl());
            //     binding.reactions.addView(profilePicView);
            // }
        });
    }

    protected boolean isSelf(DirectItem directItem) {
        return directItem.getUserId() == this.currentUser.getPk();
    }

    public void setItemView(View view) {
        binding.message.addView(view);
    }

    public abstract void bindItem(DirectItem directItemModel, MessageDirection messageDirection);

    @Nullable
    protected User getUser(long userId, List<User> users) {
        if (userId == this.currentUser.getPk()) {
            return this.currentUser;
        }
        if (users == null) return null;
        for (User user : users) {
            if (userId != user.getPk()) continue;
            return user;
        }
        return null;
    }

    protected boolean allowMessageDirectionGravity() {
        return true;
    }

    protected boolean showUserDetailsInGroup() {
        return true;
    }

    protected boolean showBackground() {
        return false;
    }

    protected boolean showMessageInfo() {
        return true;
    }

    protected boolean allowLongClick() {
        return true;
    }

    protected boolean allowReaction() {
        return true;
    }

    protected boolean canForward() {
        return true;
    }

    protected List<DirectItemContextMenu.MenuItem> getLongClickOptions() {
        return null;
    }

    protected int getReactionsTranslationY() {
        return this.reactionTranslationYType1;
    }

    @CallSuper
    public void cleanup() {
        // if (layoutChangeListener != null) {
        //     binding.container.removeOnLayoutChangeListener(layoutChangeListener);
        // }
    }

    protected void setupRamboTextListeners(@NonNull RamboTextViewV2 textView) {
        textView.addOnHashtagListener(autoLinkItem -> this.callback.onHashtagClick(autoLinkItem.getOriginalText().trim()));
        textView.addOnMentionClickListener(autoLinkItem -> this.openProfile(autoLinkItem.getOriginalText().trim()));
        textView.addOnEmailClickListener(autoLinkItem -> this.callback.onEmailClick(autoLinkItem.getOriginalText().trim()));
        textView.addOnURLClickListener(autoLinkItem -> this.openURL(autoLinkItem.getOriginalText().trim()));
    }

    protected void openProfile(String username) {
        this.callback.onMentionClick(username);
    }

    protected void openLocation(long locationId) {
        this.callback.onLocationClick(locationId);
    }

    protected void openURL(String url) {
        this.callback.onURLClick(url);
    }

    protected void openMedia(Media media, int index) {
        this.callback.onMediaClick(media, index);
    }

    protected void openStory(DirectItemStoryShare storyShare) {
        this.callback.onStoryClick(storyShare);
    }

    protected void handleDeepLink(String deepLinkText) {
        if (deepLinkText == null) return;
        DeepLinkParser.DeepLink deepLink = DeepLinkParser.parse(deepLinkText);
        if (deepLink == null) return;
        switch (deepLink.getType()) {
            case USER:
                this.callback.onMentionClick(deepLink.getValue());
                break;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupLongClickListener(int position, MessageDirection messageDirection) {
        if (!this.allowLongClick()) return;
        this.binding.getRoot().setOnItemLongClickListener(new DirectItemFrameLayout.OnItemLongClickListener() {
            @Override
            public void onLongClickStart(View view) {
                DirectItemViewHolder.this.itemView.post(() -> DirectItemViewHolder.this.shrink());
            }

            @Override
            public void onLongClickCancel(View view) {
                DirectItemViewHolder.this.itemView.post(() -> DirectItemViewHolder.this.grow());
            }

            @Override
            public void onLongClick(View view, float x, float y) {
                // if (longClickListener == null) return false;
                // longClickListener.onLongClick(position, this);
                DirectItemViewHolder.this.itemView.post(() -> DirectItemViewHolder.this.grow());
                DirectItemViewHolder.this.setSelected(true);
                DirectItemViewHolder.this.showLongClickOptions(new Point((int) x, (int) y), messageDirection);
            }
        });
    }

    private void showLongClickOptions(Point location, MessageDirection messageDirection) {
        List<DirectItemContextMenu.MenuItem> longClickOptions = this.getLongClickOptions();
        ImmutableList.Builder<DirectItemContextMenu.MenuItem> builder = ImmutableList.builder();
        if (longClickOptions != null) {
            builder.addAll(longClickOptions);
        }
        if (this.canForward()) {
            builder.add(new DirectItemContextMenu.MenuItem(R.id.forward, R.string.forward));
        }
        if (this.thread.getInputMode() != 1 && messageDirection == MessageDirection.OUTGOING) {
            builder.add(new DirectItemContextMenu.MenuItem(R.id.unsend, R.string.dms_inbox_unsend));
        }
        boolean showReactions = this.thread.getInputMode() != 1 && this.allowReaction();
        ImmutableList<DirectItemContextMenu.MenuItem> menuItems = builder.build();
        if (!showReactions && menuItems.isEmpty()) return;
        DirectItemContextMenu menu = new DirectItemContextMenu(this.itemView.getContext(), showReactions, menuItems);
        menu.setOnDismissListener(() -> this.setSelected(false));
        menu.setOnReactionClickListener(emoji -> this.callback.onReaction(this.item, emoji));
        menu.setOnOptionSelectListener((itemId, cb) -> this.callback.onOptionSelect(this.item, itemId, cb));
        menu.setOnAddReactionListener(() -> {
            menu.dismiss();
            this.itemView.postDelayed(() -> this.callback.onAddReactionListener(this.item), 300);
        });
        menu.show(this.itemView, location);
    }

    public void setLongClickListener(final DirectItemsAdapter.DirectItemInternalLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    private void shrink() {
        if (this.shrinkGrowAnimator != null) {
            this.shrinkGrowAnimator.cancel();
        }
        this.shrinkGrowAnimator = this.itemView.animate()
                                     .scaleX(0.8f)
                                     .scaleY(0.8f)
                                     .setInterpolator(this.accelerateDecelerateInterpolator)
                                     .setDuration(ViewConfiguration.getLongPressTimeout() - ViewConfiguration.getTapTimeout());
        this.shrinkGrowAnimator.start();
    }

    private void grow() {
        if (this.shrinkGrowAnimator != null) {
            this.shrinkGrowAnimator.cancel();
        }
        this.shrinkGrowAnimator = this.itemView.animate()
                                     .scaleX(1f)
                                     .scaleY(1f)
                                     .setInterpolator(this.accelerateDecelerateInterpolator)
                                     .setDuration(200)
                                     .withEndAction(() -> this.shrinkGrowAnimator = null);
        this.shrinkGrowAnimator.start();
    }

    @Override
    public int getSwipeDirection() {
        if (this.item == null || this.messageDirection == null) return ItemTouchHelper.ACTION_STATE_IDLE;
        return this.messageDirection == MessageDirection.OUTGOING ? ItemTouchHelper.START : ItemTouchHelper.END;
    }

    public enum MessageDirection {
        INCOMING,
        OUTGOING
    }
}
