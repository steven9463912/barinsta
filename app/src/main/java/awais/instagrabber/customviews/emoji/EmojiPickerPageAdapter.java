package awais.instagrabber.customviews.emoji;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AdapterListUpdateCallback;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.customviews.helpers.GridSpacingItemDecoration;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.utils.emoji.EmojiParser;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class EmojiPickerPageAdapter extends RecyclerView.Adapter<EmojiCategoryPageViewHolder> {

    private static final DiffUtil.ItemCallback<EmojiCategory> diffCallback = new DiffUtil.ItemCallback<EmojiCategory>() {
        @Override
        public boolean areItemsTheSame(@NonNull EmojiCategory oldItem, @NonNull EmojiCategory newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areContentsTheSame(@NonNull EmojiCategory oldItem, @NonNull EmojiCategory newItem) {
            return oldItem.equals(newItem);
        }
    };

    private final View rootView;
    private final EmojiPicker.OnEmojiClickListener onEmojiClickListener;
    private final AsyncListDiffer<EmojiCategory> differ;

    public EmojiPickerPageAdapter(@NonNull final View rootView,
                                  final EmojiPicker.OnEmojiClickListener onEmojiClickListener) {
        this.rootView = rootView;
        this.onEmojiClickListener = onEmojiClickListener;
        this.differ = new AsyncListDiffer<>(new AdapterListUpdateCallback(this),
                                       new AsyncDifferConfig.Builder<>(EmojiPickerPageAdapter.diffCallback).build());
        EmojiParser emojiParser = EmojiParser.Companion.getInstance(rootView.getContext());
        this.differ.submitList(emojiParser.getEmojiCategories());
        this.setHasStableIds(true);
    }

    @NonNull
    @Override
    public EmojiCategoryPageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        RecyclerView emojiGrid = new RecyclerView(context);
        emojiGrid.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        emojiGrid.setLayoutManager(new GridLayoutManager(context, 9));
        emojiGrid.setHasFixedSize(true);
        emojiGrid.setClipToPadding(false);
        emojiGrid.addItemDecoration(new GridSpacingItemDecoration(Utils.convertDpToPx(8)));
        return new EmojiCategoryPageViewHolder(this.rootView, emojiGrid, this.onEmojiClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull EmojiCategoryPageViewHolder holder, int position) {
        EmojiCategory emojiCategory = this.differ.getCurrentList().get(position);
        holder.bind(emojiCategory);
    }

    @Override
    public long getItemId(int position) {
        return this.differ.getCurrentList().get(position).hashCode();
    }

    @Override
    public int getItemCount() {
        return this.differ.getCurrentList().size();
    }
}
