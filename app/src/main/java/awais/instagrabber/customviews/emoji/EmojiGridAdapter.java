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
        public boolean areItemsTheSame(@NonNull Emoji oldItem, @NonNull Emoji newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Emoji oldItem, @NonNull Emoji newItem) {
            return oldItem.equals(newItem);
        }
    };

    private final AsyncListDiffer<Emoji> differ;
    private final OnEmojiLongClickListener onEmojiLongClickListener;
    private final EmojiPicker.OnEmojiClickListener onEmojiClickListener;
    private final EmojiVariantManager emojiVariantManager;
    private final AppExecutors appExecutors;

    public EmojiGridAdapter(@NonNull final EmojiParser emojiParser,
                            final EmojiCategoryType emojiCategoryType,
                            final EmojiPicker.OnEmojiClickListener onEmojiClickListener,
                            final OnEmojiLongClickListener onEmojiLongClickListener) {
        this.onEmojiClickListener = onEmojiClickListener;
        this.onEmojiLongClickListener = onEmojiLongClickListener;
        differ = new AsyncListDiffer<>(new AdapterListUpdateCallback(this),
                                       new AsyncDifferConfig.Builder<>(diffCallback).build());
        final Map<EmojiCategoryType, EmojiCategory> categoryMap = emojiParser.getCategoryMap();
        emojiVariantManager = EmojiVariantManager.getInstance();
        appExecutors = AppExecutors.INSTANCE;
        setHasStableIds(true);
        if (emojiCategoryType == null) {
            // show all if type is null
            differ.submitList(ImmutableList.copyOf(emojiParser.getAllEmojis().values()));
            return;
        }
        final EmojiCategory emojiCategory = categoryMap.get(emojiCategoryType);
        if (emojiCategory == null) {
            differ.submitList(Collections.emptyList());
            return;
        }
        differ.submitList(ImmutableList.copyOf(emojiCategory.getEmojis().values()));
    }

    @NonNull
    @Override
    public EmojiViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        final ItemEmojiGridBinding binding = ItemEmojiGridBinding.inflate(layoutInflater, parent, false);
        return new EmojiViewHolder(binding, onEmojiClickListener, onEmojiLongClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull final EmojiViewHolder holder, final int position) {
        final Emoji emoji = differ.getCurrentList().get(position);
        final String variant = emojiVariantManager.getVariant(emoji.getUnicode());
        if (variant != null) {
            appExecutors.getTasksThread().execute(() -> {
                final Optional<Emoji> first = emoji.getVariants()
                                                   .stream()
                                                   .filter(e -> e.getUnicode().equals(variant))
                                                   .findFirst();
                if (!first.isPresent()) return;
                appExecutors.getMainThread().execute(() -> holder.bind(position, first.get(), emoji));
            });
            return;
        }
        holder.bind(position, emoji, emoji);
    }

    @Override
    public long getItemId(final int position) {
        return differ.getCurrentList().get(position).hashCode();
    }

    @Override
    public int getItemViewType(final int position) {
        return 0;
    }

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    public static class EmojiViewHolder extends RecyclerView.ViewHolder {
        // private final AppExecutors appExecutors = AppExecutors.getInstance();
        private final ItemEmojiGridBinding binding;
        private final EmojiPicker.OnEmojiClickListener onEmojiClickListener;
        private final OnEmojiLongClickListener onEmojiLongClickListener;

        public EmojiViewHolder(@NonNull final ItemEmojiGridBinding binding,
                               final EmojiPicker.OnEmojiClickListener onEmojiClickListener,
                               OnEmojiLongClickListener onEmojiLongClickListener) {
            super(binding.getRoot());
            this.binding = binding;
            this.onEmojiClickListener = onEmojiClickListener;
            this.onEmojiLongClickListener = onEmojiLongClickListener;
        }

        public void bind(int position, Emoji emoji, Emoji parent) {
            this.binding.image.setImageDrawable(null);
            this.binding.indicator.setVisibility(View.GONE);
            this.itemView.setOnLongClickListener(null);
            // itemView.post(() -> {
            this.binding.image.setImageDrawable(emoji.getDrawable());
            boolean hasVariants = !parent.getVariants().isEmpty();
            this.binding.indicator.setVisibility(hasVariants ? View.VISIBLE : View.GONE);
            if (this.onEmojiClickListener != null) {
                this.itemView.setOnClickListener(v -> this.onEmojiClickListener.onClick(v, emoji));
            }
            if (hasVariants && this.onEmojiLongClickListener != null) {
                this.itemView.setOnLongClickListener(v -> this.onEmojiLongClickListener.onLongClick(position, v, parent));
            }
            // });
        }
    }

    public interface OnEmojiLongClickListener {
        boolean onLongClick(int position, View view, Emoji emoji);
    }
}
