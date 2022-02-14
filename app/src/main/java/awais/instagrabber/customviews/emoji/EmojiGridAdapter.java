package awais.instagrabber.customviews.emoji;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AdapterListUpdateCallback;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import awais.instagrabber.databinding.ItemEmojiGridBinding;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.emoji.EmojiParser;

public class EmojiGridAdapter extends RecyclerView.Adapter<EmojiGridAdapter.EmojiViewHolder> {
    private static final String TAG = EmojiGridAdapter.class.getSimpleName();

    private static final DiffUtil.ItemCallback<Emoji> diffCallback = new DiffUtil.ItemCallback<Emoji>() {
        @Override
        public boolean areItemsTheSame(@NonNull final Emoji oldItem, @NonNull final Emoji newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areContentsTheSame(@NonNull final Emoji oldItem, @NonNull final Emoji newItem) {
            return oldItem.equals(newItem);
        }
    };

    private final AsyncListDiffer<Emoji> differ;
    private final OnEmojiLongClickListener onEmojiLongClickListener;
    private final EmojiPicker.OnEmojiClickListener onEmojiClickListener;
    private final EmojiVariantManager emojiVariantManager;
    private final AppExecutors appExecutors;

    public EmojiGridAdapter(@NonNull EmojiParser emojiParser,
                            EmojiCategoryType emojiCategoryType,
                            EmojiPicker.OnEmojiClickListener onEmojiClickListener,
                            OnEmojiLongClickListener onEmojiLongClickListener) {
        this.onEmojiClickListener = onEmojiClickListener;
        this.onEmojiLongClickListener = onEmojiLongClickListener;
        this.differ = new AsyncListDiffer<>(new AdapterListUpdateCallback(this),
                                       new AsyncDifferConfig.Builder<>(EmojiGridAdapter.diffCallback).build());
        Map<EmojiCategoryType, EmojiCategory> categoryMap = emojiParser.getCategoryMap();
        this.emojiVariantManager = EmojiVariantManager.getInstance();
        this.appExecutors = AppExecutors.INSTANCE;
        this.setHasStableIds(true);
        if (emojiCategoryType == null) {
            // show all if type is null
            this.differ.submitList(ImmutableList.copyOf(emojiParser.getAllEmojis().values()));
            return;
        }
        EmojiCategory emojiCategory = categoryMap.get(emojiCategoryType);
        if (emojiCategory == null) {
            this.differ.submitList(Collections.emptyList());
            return;
        }
        this.differ.submitList(ImmutableList.copyOf(emojiCategory.getEmojis().values()));
    }

    @NonNull
    @Override
    public EmojiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ItemEmojiGridBinding binding = ItemEmojiGridBinding.inflate(layoutInflater, parent, false);
        return new EmojiViewHolder(binding, this.onEmojiClickListener, this.onEmojiLongClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull EmojiViewHolder holder, int position) {
        Emoji emoji = this.differ.getCurrentList().get(position);
        String variant = this.emojiVariantManager.getVariant(emoji.getUnicode());
        if (variant != null) {
            this.appExecutors.getTasksThread().execute(() -> {
                Optional<Emoji> first = emoji.getVariants()
                                                   .stream()
                                                   .filter(e -> e.getUnicode().equals(variant))
                                                   .findFirst();
                if (!first.isPresent()) return;
                this.appExecutors.getMainThread().execute(() -> holder.bind(position, first.get(), emoji));
            });
            return;
        }
        holder.bind(position, emoji, emoji);
    }

    @Override
    public long getItemId(int position) {
        return this.differ.getCurrentList().get(position).hashCode();
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getItemCount() {
        return this.differ.getCurrentList().size();
    }

    public static class EmojiViewHolder extends RecyclerView.ViewHolder {
        // private final AppExecutors appExecutors = AppExecutors.getInstance();
        private final ItemEmojiGridBinding binding;
        private final EmojiPicker.OnEmojiClickListener onEmojiClickListener;
        private final OnEmojiLongClickListener onEmojiLongClickListener;

        public EmojiViewHolder(@NonNull ItemEmojiGridBinding binding,
                               EmojiPicker.OnEmojiClickListener onEmojiClickListener,
                               final OnEmojiLongClickListener onEmojiLongClickListener) {
            super(binding.getRoot());
            this.binding = binding;
            this.onEmojiClickListener = onEmojiClickListener;
            this.onEmojiLongClickListener = onEmojiLongClickListener;
        }

        public void bind(final int position, final Emoji emoji, final Emoji parent) {
            binding.image.setImageDrawable(null);
            binding.indicator.setVisibility(View.GONE);
            itemView.setOnLongClickListener(null);
            // itemView.post(() -> {
            binding.image.setImageDrawable(emoji.getDrawable());
            final boolean hasVariants = !parent.getVariants().isEmpty();
            binding.indicator.setVisibility(hasVariants ? View.VISIBLE : View.GONE);
            if (onEmojiClickListener != null) {
                itemView.setOnClickListener(v -> onEmojiClickListener.onClick(v, emoji));
            }
            if (hasVariants && onEmojiLongClickListener != null) {
                itemView.setOnLongClickListener(v -> onEmojiLongClickListener.onLongClick(position, v, parent));
            }
            // });
        }
    }

    public interface OnEmojiLongClickListener {
        boolean onLongClick(int position, View view, Emoji emoji);
    }
}
