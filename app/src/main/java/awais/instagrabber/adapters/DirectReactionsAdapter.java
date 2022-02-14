package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import java.util.List;

import awais.instagrabber.adapters.viewholder.directmessages.DirectReactionViewHolder;
import awais.instagrabber.databinding.LayoutDmUserItemBinding;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectItemEmojiReaction;

public final class DirectReactionsAdapter extends ListAdapter<DirectItemEmojiReaction, DirectReactionViewHolder> {

    private static final DiffUtil.ItemCallback<DirectItemEmojiReaction> DIFF_CALLBACK = new DiffUtil.ItemCallback<DirectItemEmojiReaction>() {
        @Override
        public boolean areItemsTheSame(@NonNull DirectItemEmojiReaction oldItem, @NonNull DirectItemEmojiReaction newItem) {
            return oldItem.getSenderId() == newItem.getSenderId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull DirectItemEmojiReaction oldItem, @NonNull DirectItemEmojiReaction newItem) {
            return oldItem.getEmoji().equals(newItem.getEmoji());
        }
    };

    private final long viewerId;
    private final List<User> users;
    private final String itemId;
    private final OnReactionClickListener onReactionClickListener;

    public DirectReactionsAdapter(long viewerId,
                                  List<User> users,
                                  String itemId,
                                  OnReactionClickListener onReactionClickListener) {
        super(DirectReactionsAdapter.DIFF_CALLBACK);
        this.viewerId = viewerId;
        this.users = users;
        this.itemId = itemId;
        this.onReactionClickListener = onReactionClickListener;
        this.setHasStableIds(true);
    }

    @NonNull
    @Override
    public DirectReactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        LayoutDmUserItemBinding binding = LayoutDmUserItemBinding.inflate(layoutInflater, parent, false);
        return new DirectReactionViewHolder(binding, this.viewerId, this.itemId, this.onReactionClickListener);

    }

    @Override
    public void onBindViewHolder(@NonNull DirectReactionViewHolder holder, int position) {
        DirectItemEmojiReaction reaction = this.getItem(position);
        if (reaction == null) return;
        holder.bind(reaction, this.getUser(reaction.getSenderId()));
    }

    @Override
    public long getItemId(int position) {
        return this.getItem(position).getSenderId();
    }

    @Nullable
    private User getUser(long pk) {
        return this.users.stream()
                    .filter(user -> user.getPk() == pk)
                    .findFirst()
                    .orElse(null);
    }

    public interface OnReactionClickListener {
        void onReactionClick(String itemId, DirectItemEmojiReaction reaction);
    }
}