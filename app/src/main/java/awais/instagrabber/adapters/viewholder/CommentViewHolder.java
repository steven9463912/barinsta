package awais.instagrabber.adapters.viewholder;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.Menu;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.R;
import awais.instagrabber.adapters.CommentsAdapter;
import awais.instagrabber.customviews.ProfilePicView;
import awais.instagrabber.databinding.ItemCommentBinding;
import awais.instagrabber.models.Comment;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.utils.Utils;

public final class CommentViewHolder extends RecyclerView.ViewHolder {

    private final ItemCommentBinding binding;
    private final long currentUserId;
    private final CommentsAdapter.CommentCallback commentCallback;
    @ColorInt
    private int parentCommentHighlightColor;
    private PopupMenu optionsPopup;

    public CommentViewHolder(@NonNull final ItemCommentBinding binding,
                             final long currentUserId,
                             final CommentsAdapter.CommentCallback commentCallback) {
        super(binding.getRoot());
        this.binding = binding;
        this.currentUserId = currentUserId;
        this.commentCallback = commentCallback;
        Context context = this.itemView.getContext();
        if (context == null) return;
        Resources.Theme theme = context.getTheme();
        if (theme == null) return;
        TypedValue typedValue = new TypedValue();
        boolean resolved = theme.resolveAttribute(R.attr.parentCommentHighlightColor, typedValue, true);
        if (resolved) {
            this.parentCommentHighlightColor = typedValue.data;
        }
    }

    public void bind(Comment comment, boolean isReplyParent, boolean isReply) {
        if (comment == null) return;
        this.itemView.setOnClickListener(v -> {
            if (this.commentCallback != null) {
                this.commentCallback.onClick(comment);
            }
        });
        if (isReplyParent && this.parentCommentHighlightColor != 0) {
            this.itemView.setBackgroundColor(this.parentCommentHighlightColor);
        } else {
            this.itemView.setBackgroundColor(this.itemView.getResources().getColor(android.R.color.transparent));
        }
        this.setupCommentText(comment, isReply);
        this.binding.date.setText(comment.getDateTime());
        this.setLikes(comment, isReply);
        this.setReplies(comment, isReply);
        this.setUser(comment, isReply);
        this.setupOptions(comment, isReply);
    }

    private void setupCommentText(@NonNull Comment comment, boolean isReply) {
        this.binding.comment.clearOnURLClickListeners();
        this.binding.comment.clearOnHashtagClickListeners();
        this.binding.comment.clearOnMentionClickListeners();
        this.binding.comment.clearOnEmailClickListeners();
        this.binding.comment.setText(comment.getText());
        this.binding.comment.setTextSize(TypedValue.COMPLEX_UNIT_SP, isReply ? 12 : 14);
        this.binding.comment.addOnHashtagListener(autoLinkItem -> {
            String originalText = autoLinkItem.getOriginalText();
            if (this.commentCallback == null) return;
            this.commentCallback.onHashtagClick(originalText);
        });
        this.binding.comment.addOnMentionClickListener(autoLinkItem -> {
            String originalText = autoLinkItem.getOriginalText();
            if (this.commentCallback == null) return;
            this.commentCallback.onMentionClick(originalText);

        });
        this.binding.comment.addOnEmailClickListener(autoLinkItem -> {
            String originalText = autoLinkItem.getOriginalText();
            if (this.commentCallback == null) return;
            this.commentCallback.onEmailClick(originalText);
        });
        this.binding.comment.addOnURLClickListener(autoLinkItem -> {
            String originalText = autoLinkItem.getOriginalText();
            if (this.commentCallback == null) return;
            this.commentCallback.onURLClick(originalText);
        });
        this.binding.comment.setOnLongClickListener(v -> {
            Utils.copyText(this.itemView.getContext(), comment.getText());
            return true;
        });
        this.binding.comment.setOnClickListener(v -> this.commentCallback.onClick(comment));
    }

    private void setUser(@NonNull Comment comment, boolean isReply) {
        User user = comment.getUser();
        if (user == null) return;
        this.binding.username.setUsername(user.getUsername(), user.isVerified());
        this.binding.username.setTextAppearance(this.itemView.getContext(), isReply ? R.style.TextAppearance_MaterialComponents_Subtitle2
                                                                          : R.style.TextAppearance_MaterialComponents_Subtitle1);
        this.binding.username.setOnClickListener(v -> {
            if (this.commentCallback == null) return;
            this.commentCallback.onMentionClick("@" + user.getUsername());
        });
        this.binding.profilePic.setImageURI(user.getProfilePicUrl());
        this.binding.profilePic.setSize(isReply ? ProfilePicView.Size.SMALLER : ProfilePicView.Size.SMALL);
        this.binding.profilePic.setOnClickListener(v -> {
            if (this.commentCallback == null) return;
            this.commentCallback.onMentionClick("@" + user.getUsername());
        });
    }

    private void setLikes(@NonNull Comment comment, boolean isReply) {
        this.binding.likes.setText(String.valueOf(comment.getCommentLikeCount()));
        this.binding.likes.setOnLongClickListener(v -> {
            if (this.commentCallback == null) return false;
            this.commentCallback.onViewLikes(comment);
            return true;
        });
        if (this.currentUserId == 0) { // not logged in
            this.binding.likes.setOnClickListener(v -> {
                if (this.commentCallback == null) return;
                this.commentCallback.onViewLikes(comment);
            });
            return;
        }
        boolean liked = comment.getLiked();
        int resId = liked ? R.drawable.ic_like : R.drawable.ic_not_liked;
        this.binding.likes.setCompoundDrawablesRelativeWithSize(ContextCompat.getDrawable(this.itemView.getContext(), resId), null, null, null);
        this.binding.likes.setOnClickListener(v -> {
            if (this.commentCallback == null) return;
            // toggle like
            this.commentCallback.onLikeClick(comment, !liked, isReply);
        });
    }

    private void setReplies(@NonNull Comment comment, boolean isReply) {
        int replies = comment.getChildCommentCount();
        this.binding.replies.setVisibility(View.VISIBLE);
        String text = isReply ? "" : String.valueOf(replies);
        this.binding.replies.setText(text);
        this.binding.replies.setOnClickListener(v -> {
            if (this.commentCallback == null) return;
            this.commentCallback.onRepliesClick(comment);
        });
    }

    private void setupOptions(Comment comment, boolean isReply) {
        this.binding.options.setOnClickListener(v -> {
            if (this.optionsPopup == null) {
                this.createOptionsPopupMenu(comment, isReply);
            }
            if (this.optionsPopup == null) return;
            this.optionsPopup.show();
        });
    }

    private void createOptionsPopupMenu(Comment comment, boolean isReply) {
        if (this.optionsPopup == null) {
            ContextThemeWrapper themeWrapper = new ContextThemeWrapper(this.itemView.getContext(), R.style.popupMenuStyle);
            this.optionsPopup = new PopupMenu(themeWrapper, this.binding.options);
        } else {
            this.optionsPopup.getMenu().clear();
        }
        this.optionsPopup.getMenuInflater().inflate(R.menu.comment_options_menu, this.optionsPopup.getMenu());
        User user = comment.getUser();
        if (this.currentUserId == 0 || user == null || user.getPk() != this.currentUserId) {
            Menu menu = this.optionsPopup.getMenu();
            menu.removeItem(R.id.delete);
        }
        this.optionsPopup.setOnMenuItemClickListener(item -> {
            if (this.commentCallback == null) return false;
            final int itemId = item.getItemId();
            if (itemId == R.id.translate) {
                this.commentCallback.onTranslate(comment);
                return true;
            }
            if (itemId == R.id.delete) {
                this.commentCallback.onDelete(comment, isReply);
            }
            return true;
        });
    }

    // private void setupReply(final Comment comment) {
    //     if (!isLoggedIn) {
    //         binding.reply.setVisibility(View.GONE);
    //         return;
    //     }
    //     binding.reply.setOnClickListener(v -> {
    //         if (commentCallback == null) return;
    //         // toggle like
    //         commentCallback.onReplyClick(comment);
    //     });
    // }
}