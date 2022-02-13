package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import awais.instagrabber.adapters.viewholder.FeedStoryViewHolder;
import awais.instagrabber.databinding.ItemHighlightBinding;
import awais.instagrabber.repositories.responses.stories.Story;

public final class FeedStoriesAdapter extends ListAdapter<Story, FeedStoryViewHolder> {
    private final OnFeedStoryClickListener listener;

    private static final DiffUtil.ItemCallback<Story> diffCallback = new DiffUtil.ItemCallback<Story>() {
        @Override
        public boolean areItemsTheSame(@NonNull Story oldItem, @NonNull Story newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Story oldItem, @NonNull Story newItem) {
            return oldItem.getId().equals(newItem.getId()) && oldItem.getSeen() == newItem.getSeen();
        }
    };

    public FeedStoriesAdapter(OnFeedStoryClickListener listener) {
        super(FeedStoriesAdapter.diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public FeedStoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ItemHighlightBinding binding = ItemHighlightBinding.inflate(layoutInflater, parent, false);
        return new FeedStoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedStoryViewHolder holder, int position) {
        Story model = this.getItem(position);
        holder.bind(model, position, this.listener);
    }

    public interface OnFeedStoryClickListener {
        void onFeedStoryClick(Story model, int position);

        void onFeedStoryLongClick(Story model, int position);
    }
}
