package awais.instagrabber.adapters;

import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.databinding.ItemStoryBinding;
import awais.instagrabber.repositories.responses.stories.StoryMedia;
import awais.instagrabber.utils.ResponseBodyUtils;

public final class StoriesAdapter extends ListAdapter<StoryMedia, StoriesAdapter.StoryViewHolder> {
    private final OnItemClickListener onItemClickListener;

    private static final DiffUtil.ItemCallback<StoryMedia> diffCallback = new DiffUtil.ItemCallback<StoryMedia>() {
        @Override
        public boolean areItemsTheSame(@NonNull StoryMedia oldItem, @NonNull StoryMedia newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull StoryMedia oldItem, @NonNull StoryMedia newItem) {
            return oldItem.getId().equals(newItem.getId());
        }
    };

    public StoriesAdapter(OnItemClickListener onItemClickListener) {
        super(StoriesAdapter.diffCallback);
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ItemStoryBinding binding = ItemStoryBinding.inflate(layoutInflater, parent, false);
        return new StoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        StoryMedia storyMedia = this.getItem(position);
        holder.bind(storyMedia, position, this.onItemClickListener);
    }

    public static final class StoryViewHolder extends RecyclerView.ViewHolder {
        private final ItemStoryBinding binding;

        public StoryViewHolder(ItemStoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(StoryMedia model,
                         int position,
                         OnItemClickListener clickListener) {
            if (model == null) return;
            model.setPosition(position);

            this.itemView.setTag(model);
            this.itemView.setOnClickListener(v -> {
                if (clickListener == null) return;
                clickListener.onItemClick(model, position);
            });

            this.binding.selectedView.setVisibility(model.isCurrentSlide() ? View.VISIBLE : View.GONE);
            this.binding.icon.setImageURI(ResponseBodyUtils.getThumbUrl(model));
        }
    }

    public void paginate(int newIndex) {
        List<StoryMedia> list = this.getCurrentList();
        for (int i = 0; i < list.size(); i++) {
            StoryMedia item = list.get(i);
            if (!item.isCurrentSlide() && i != newIndex) continue;
            item.setCurrentSlide(i == newIndex);
            this.notifyItemChanged(i, item);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(StoryMedia storyModel, int position);
    }
}