package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import java.util.Objects;

import awais.instagrabber.adapters.viewholder.TopicClusterViewHolder;
import awais.instagrabber.databinding.ItemDiscoverTopicBinding;
import awais.instagrabber.repositories.responses.saved.SavedCollection;

public class SavedCollectionsAdapter extends ListAdapter<SavedCollection, TopicClusterViewHolder> {
    private static final DiffUtil.ItemCallback<SavedCollection> DIFF_CALLBACK = new DiffUtil.ItemCallback<SavedCollection>() {
        @Override
        public boolean areItemsTheSame(@NonNull SavedCollection oldItem, @NonNull SavedCollection newItem) {
            return oldItem.getCollectionId().equals(newItem.getCollectionId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull SavedCollection oldItem, @NonNull SavedCollection newItem) {
            if (oldItem.getCoverMediaList() != null && newItem.getCoverMediaList() != null
                && oldItem.getCoverMediaList().size() == newItem.getCoverMediaList().size()) {
                return Objects.equals(oldItem.getCoverMediaList().get(0).getId(), newItem.getCoverMediaList().get(0).getId());
            }
            else if (oldItem.getCoverMedia() != null && newItem.getCoverMedia() != null) {
                return Objects.equals(oldItem.getCoverMedia().getId(), newItem.getCoverMedia().getId());
            }
            return false;
        }
    };

    private final OnCollectionClickListener onCollectionClickListener;

    public SavedCollectionsAdapter(OnCollectionClickListener onCollectionClickListener) {
        super(SavedCollectionsAdapter.DIFF_CALLBACK);
        this.onCollectionClickListener = onCollectionClickListener;
    }

    @NonNull
    @Override
    public TopicClusterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ItemDiscoverTopicBinding binding = ItemDiscoverTopicBinding.inflate(layoutInflater, parent, false);
        return new TopicClusterViewHolder(binding, null, this.onCollectionClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull TopicClusterViewHolder holder, int position) {
        SavedCollection topicCluster = this.getItem(position);
        holder.bind(topicCluster);
    }

    public interface OnCollectionClickListener {
        void onCollectionClick(SavedCollection savedCollection, View root, View cover, View title, int titleColor, int backgroundColor);
    }
}
