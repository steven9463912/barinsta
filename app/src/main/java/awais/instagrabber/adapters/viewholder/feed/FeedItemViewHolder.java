package awais.instagrabber.adapters.viewholder.feed;

import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.R;
import awais.instagrabber.adapters.FeedAdapterV2;
import awais.instagrabber.customviews.VerticalImageSpan;
import awais.instagrabber.databinding.ItemFeedTopBinding;
import awais.instagrabber.databinding.LayoutPostViewBottomBinding;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.repositories.responses.Caption;
import awais.instagrabber.repositories.responses.Location;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

import static android.text.TextUtils.TruncateAt.END;

public abstract class FeedItemViewHolder extends RecyclerView.ViewHolder {
    public static final int MAX_LINES_COLLAPSED = 5;
    private final ItemFeedTopBinding topBinding;
    private final LayoutPostViewBottomBinding bottomBinding;
    private final ViewGroup bottomFrame;
    private final FeedAdapterV2.FeedItemCallback feedItemCallback;

    public FeedItemViewHolder(@NonNull ViewGroup root,
                              FeedAdapterV2.FeedItemCallback feedItemCallback) {
        super(root);
        bottomFrame = root;
        topBinding = ItemFeedTopBinding.bind(root);
        bottomBinding = LayoutPostViewBottomBinding.bind(root);
        this.feedItemCallback = feedItemCallback;
    }

    public void bind(Media media) {
        if (media == null) {
            return;
        }
        this.setupProfilePic(media);
        this.bottomBinding.date.setText(media.getDate());
        this.setupComments(media);
        this.setupCaption(media);
        this.setupActions(media);
        if (media.getType() != MediaItemType.MEDIA_TYPE_SLIDER) {
            this.bottomBinding.download.setOnClickListener(v ->
                    this.feedItemCallback.onDownloadClick(media, -1, this.bottomBinding.download)
            );
        }
        this.bindItem(media);
        this.bottomFrame.post(() -> this.setupLocation(media));
    }

    private void setupComments(@NonNull Media feedModel) {
        long commentsCount = feedModel.getCommentCount();
        this.bottomBinding.commentsCount.setText(String.valueOf(commentsCount));
        this.bottomBinding.comment.setOnClickListener(v -> this.feedItemCallback.onCommentsClick(feedModel));
    }

    private void setupProfilePic(@NonNull Media media) {
        User user = media.getUser();
        if (user == null) {
            this.topBinding.profilePic.setVisibility(View.GONE);
            this.topBinding.title.setVisibility(View.GONE);
            this.topBinding.subtitle.setVisibility(View.GONE);
            return;
        }
        this.topBinding.profilePic.setOnClickListener(v -> this.feedItemCallback.onProfilePicClick(media));
        this.topBinding.profilePic.setImageURI(user.getProfilePicUrl());
        this.setupTitle(media);
    }

    private void setupTitle(@NonNull Media media) {
        // final int titleLen = profileModel.getUsername().length() + 1;
        // final SpannableString spannableString = new SpannableString();
        // spannableString.setSpan(new CommentMentionClickSpan(), 0, titleLen, 0);
        User user = media.getUser();
        if (user == null) return;
        this.setUsername(user);
        this.topBinding.title.setOnClickListener(v -> this.feedItemCallback.onNameClick(media));
        String fullName = user.getFullName();
        if (TextUtils.isEmpty(fullName)) {
            this.topBinding.subtitle.setVisibility(View.GONE);
        } else {
            this.topBinding.subtitle.setVisibility(View.VISIBLE);
            this.topBinding.subtitle.setText(fullName);
        }
        this.topBinding.subtitle.setOnClickListener(v -> this.feedItemCallback.onNameClick(media));
    }

    private void setupCaption(Media media) {
        this.bottomBinding.caption.clearOnMentionClickListeners();
        this.bottomBinding.caption.clearOnHashtagClickListeners();
        this.bottomBinding.caption.clearOnURLClickListeners();
        this.bottomBinding.caption.clearOnEmailClickListeners();
        Caption caption = media.getCaption();
        if (caption == null) {
            this.bottomBinding.caption.setVisibility(View.GONE);
            return;
        }
        CharSequence postCaption = caption.getText();
        boolean captionEmpty = TextUtils.isEmpty(postCaption);
        this.bottomBinding.caption.setVisibility(captionEmpty ? View.GONE : View.VISIBLE);
        if (captionEmpty) return;
        this.bottomBinding.caption.setText(postCaption);
        this.bottomBinding.caption.setMaxLines(FeedItemViewHolder.MAX_LINES_COLLAPSED);
        this.bottomBinding.caption.setEllipsize(END);
        this.bottomBinding.caption.setOnClickListener(v -> this.bottomFrame.post(() -> {
            TransitionManager.beginDelayedTransition(this.bottomFrame);
            if (this.bottomBinding.caption.getMaxLines() == FeedItemViewHolder.MAX_LINES_COLLAPSED) {
                this.bottomBinding.caption.setMaxLines(Integer.MAX_VALUE);
                this.bottomBinding.caption.setEllipsize(null);
                return;
            }
            this.bottomBinding.caption.setMaxLines(FeedItemViewHolder.MAX_LINES_COLLAPSED);
            this.bottomBinding.caption.setEllipsize(END);
        }));
        this.bottomBinding.caption.addOnMentionClickListener(autoLinkItem -> this.feedItemCallback.onMentionClick(autoLinkItem.getOriginalText()));
        this.bottomBinding.caption.addOnHashtagListener(autoLinkItem -> this.feedItemCallback.onHashtagClick(autoLinkItem.getOriginalText()));
        this.bottomBinding.caption.addOnEmailClickListener(autoLinkItem -> this.feedItemCallback.onEmailClick(autoLinkItem.getOriginalText()));
        this.bottomBinding.caption.addOnURLClickListener(autoLinkItem -> this.feedItemCallback.onURLClick(autoLinkItem.getOriginalText()));
    }

    private void setupLocation(@NonNull Media media) {
        Location location = media.getLocation();
        if (location == null) {
            this.topBinding.location.setVisibility(View.GONE);
        } else {
            String locationName = location.getName();
            if (TextUtils.isEmpty(locationName)) {
                this.topBinding.location.setVisibility(View.GONE);
            } else {
                this.topBinding.location.setVisibility(View.VISIBLE);
                this.topBinding.location.setText(locationName);
                this.topBinding.location.setOnClickListener(v -> this.feedItemCallback.onLocationClick(media));
            }
        }
    }

    private void setupActions(@NonNull Media media) {
        // temporary - to be set up later
        this.bottomBinding.like.setVisibility(View.GONE);
        this.bottomBinding.save.setVisibility(View.GONE);
        this.bottomBinding.translate.setVisibility(View.GONE);
        this.bottomBinding.share.setVisibility(View.GONE);
    }

    private void setUsername(User user) {
        SpannableStringBuilder sb = new SpannableStringBuilder(user.getUsername());
        int drawableSize = Utils.convertDpToPx(24);
        if (user.isVerified()) {
            Drawable verifiedDrawable = this.itemView.getResources().getDrawable(R.drawable.verified);
            VerticalImageSpan verifiedSpan = null;
            if (verifiedDrawable != null) {
                Drawable drawable = verifiedDrawable.mutate();
                drawable.setBounds(0, 0, drawableSize, drawableSize);
                verifiedSpan = new VerticalImageSpan(drawable);
            }
            try {
                if (verifiedSpan != null) {
                    sb.append("  ");
                    sb.setSpan(verifiedSpan, sb.length() - 1, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } catch (final Exception e) {
                Log.e("FeedItemViewHolder", "setUsername: ", e);
            }
        }
        this.topBinding.title.setText(sb);
    }

    public abstract void bindItem(Media media);
}