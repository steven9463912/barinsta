package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import awais.instagrabber.adapters.viewholder.StoryListViewHolder;
import awais.instagrabber.databinding.ItemNotificationBinding;
import awais.instagrabber.repositories.responses.stories.Story;

public final class HighlightStoriesListAdapter extends ListAdapter<Story, StoryListViewHolder> {
    private final OnHighlightStoryClickListener listener;

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

    public HighlightStoriesListAdapter(OnHighlightStoryClickListener listener) {
        super(HighlightStoriesListAdapter.diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public StoryListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ItemNotificationBinding binding = ItemNotificationBinding.inflate(layoutInflater, parent, false);
        return new StoryListViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryListViewHolder holder, int position) {
        Story model = this.getItem(position);
        holder.bind(model, position, this.listener);
    }

    public interface OnHighlightStoryClickListener {
        void onHighlightClick(Story model, int position);

        void onProfileClick(String username);
    }
}
