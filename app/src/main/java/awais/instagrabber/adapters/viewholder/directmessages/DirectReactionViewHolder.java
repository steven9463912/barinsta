package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.R;
import awais.instagrabber.adapters.DirectReactionsAdapter;
import awais.instagrabber.customviews.emoji.Emoji;
import awais.instagrabber.databinding.LayoutDmUserItemBinding;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectItemEmojiReaction;
import awais.instagrabber.utils.emoji.EmojiParser;

public class DirectReactionViewHolder extends RecyclerView.ViewHolder {
    private final LayoutDmUserItemBinding binding;
    private final long viewerId;
    private final String itemId;
    private final DirectReactionsAdapter.OnReactionClickListener onReactionClickListener;
    private final EmojiParser emojiParser;

    public DirectReactionViewHolder(final LayoutDmUserItemBinding binding,
                                    final long viewerId,
                                    final String itemId,
                                    final DirectReactionsAdapter.OnReactionClickListener onReactionClickListener) {
        super(binding.getRoot());
        this.binding = binding;
        this.viewerId = viewerId;
        this.itemId = itemId;
        this.onReactionClickListener = onReactionClickListener;
        binding.info.setVisibility(View.GONE);
        binding.secondaryImage.setVisibility(View.VISIBLE);
        this.emojiParser = EmojiParser.Companion.getInstance(this.itemView.getContext());
    }

    public void bind(DirectItemEmojiReaction reaction,
                     @Nullable User user) {
        this.itemView.setOnClickListener(v -> {
            if (this.onReactionClickListener == null) return;
            this.onReactionClickListener.onReactionClick(this.itemId, reaction);
        });
        this.setUser(user);
        this.setReaction(reaction);
    }

    private void setReaction(DirectItemEmojiReaction reaction) {
        Emoji emoji = this.emojiParser.getEmoji(reaction.getEmoji());
        if (emoji == null) {
            this.binding.secondaryImage.setImageDrawable(null);
            return;
        }
        this.binding.secondaryImage.setImageDrawable(emoji.getDrawable());
    }

    private void setUser(User user) {
        if (user == null) {
            this.binding.fullName.setText("");
            this.binding.username.setText("");
            this.binding.profilePic.setImageURI((String) null);
            return;
        }
        this.binding.fullName.setText(user.getFullName());
        if (user.getPk() == this.viewerId) {
            this.binding.username.setText(R.string.tap_to_remove);
        } else {
            this.binding.username.setText(user.getUsername());
        }
        this.binding.profilePic.setImageURI(user.getProfilePicUrl());
    }
}
