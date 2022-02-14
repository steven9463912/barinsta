package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import awais.instagrabber.adapters.viewholder.HighlightViewHolder;
import awais.instagrabber.databinding.ItemHighlightBinding;
import awais.instagrabber.repositories.responses.stories.Story;

public final class HighlightsAdapter extends ListAdapter<Story, HighlightViewHolder> {

    private final OnHighlightClickListener clickListener;

    private static final DiffUtil.ItemCallback<Story> diffCallback = new DiffUtil.ItemCallback<Story>() {
        @Override
        public boolean areItemsTheSame(@NonNull Story oldItem, @NonNull Story newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Story oldItem, @NonNull Story newItem) {
            return oldItem.getId().equals(newItem.getId());
        }
    };

    public HighlightsAdapter(OnHighlightClickListener clickListener) {
        super(HighlightsAdapter.diffCallback);
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public HighlightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ItemHighlightBinding binding = ItemHighlightBinding.inflate(layoutInflater, parent, false);
        return new HighlightViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HighlightViewHolder holder, int position) {
        Story highlightModel = this.getItem(position);
        if (this.clickListener != null) {
            holder.itemView.setOnClickListener(v -> this.clickListener.onHighlightClick(highlightModel, position));
        }
        holder.bind(highlightModel);
    }

    public interface OnHighlightClickListener {
        void onHighlightClick(Story model, int position);
    }
}
