package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import java.util.Objects;

import awais.instagrabber.adapters.viewholder.CommentViewHolder;
import awais.instagrabber.databinding.ItemCommentBinding;
import awais.instagrabber.models.Comment;

public final class CommentsAdapter extends ListAdapter<Comment, CommentViewHolder> {
    private static final DiffUtil.ItemCallback<Comment> DIFF_CALLBACK = new DiffUtil.ItemCallback<Comment>() {
        @Override
        public boolean areItemsTheSame(@NonNull Comment oldItem, @NonNull Comment newItem) {
            return Objects.equals(oldItem.getPk(), newItem.getPk());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Comment oldItem, @NonNull Comment newItem) {
            return Objects.equals(oldItem, newItem);
        }
    };

    private final boolean showingReplies;
    private final CommentCallback commentCallback;
    private final long currentUserId;

    public CommentsAdapter(long currentUserId,
                           boolean showingReplies,
                           CommentCallback commentCallback) {
        super(CommentsAdapter.DIFF_CALLBACK);
        this.showingReplies = showingReplies;
        this.currentUserId = currentUserId;
        this.commentCallback = commentCallback;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ItemCommentBinding binding = ItemCommentBinding.inflate(layoutInflater, parent, false);
        return new CommentViewHolder(binding, this.currentUserId, this.commentCallback);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = this.getItem(position);
        holder.bind(comment, this.showingReplies && position == 0, this.showingReplies && position != 0);
    }

    public interface CommentCallback {
        void onClick(Comment comment);

        void onHashtagClick(String hashtag);

        void onMentionClick(String mention);

        void onURLClick(String url);

        void onEmailClick(String emailAddress);

        void onLikeClick(Comment comment, boolean liked, boolean isReply);

        void onRepliesClick(Comment comment);

        void onViewLikes(Comment comment);

        void onTranslate(Comment comment);

        void onDelete(Comment comment, boolean isReply);
    }
}