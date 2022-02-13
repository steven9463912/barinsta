package awais.instagrabber.customviews.emoji;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.utils.emoji.EmojiParser;

public class EmojiCategoryPageViewHolder extends RecyclerView.ViewHolder {
    // private static final String TAG = EmojiCategoryPageViewHolder.class.getSimpleName();

    private final View rootView;
    private final EmojiPicker.OnEmojiClickListener onEmojiClickListener;
    private final EmojiParser emojiParser = EmojiParser.Companion.getInstance(itemView.getContext());

    public EmojiCategoryPageViewHolder(@NonNull final View rootView,
                                       @NonNull final RecyclerView itemView,
                                       final EmojiPicker.OnEmojiClickListener onEmojiClickListener) {
        super(itemView);
        this.rootView = rootView;
        this.onEmojiClickListener = onEmojiClickListener;
    }

    public void bind(EmojiCategory emojiCategory) {
        RecyclerView emojiGrid = (RecyclerView) this.itemView;
        EmojiGridAdapter adapter = new EmojiGridAdapter(
                this.emojiParser,
                emojiCategory.getType(),
                this.onEmojiClickListener,
                (position, view, parent) -> {
                    EmojiVariantPopup emojiVariantPopup = new EmojiVariantPopup(this.rootView, ((view1, emoji) -> {
                        if (this.onEmojiClickListener != null) {
                            this.onEmojiClickListener.onClick(view1, emoji);
                        }
                        EmojiGridAdapter emojiGridAdapter = (EmojiGridAdapter) emojiGrid.getAdapter();
                        if (emojiGridAdapter == null) return;
                        emojiGridAdapter.notifyItemChanged(position);
                    }));
                    emojiVariantPopup.show(view, parent);
                    return true;
                }
        );
        emojiGrid.setAdapter(adapter);
    }
}
