package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import awais.instagrabber.adapters.viewholder.TopicClusterViewHolder;
import awais.instagrabber.databinding.ItemDiscoverTopicBinding;
import awais.instagrabber.repositories.responses.discover.TopicCluster;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.utils.ResponseBodyUtils;

public class DiscoverTopicsAdapter extends ListAdapter<TopicCluster, TopicClusterViewHolder> {
    private static final DiffUtil.ItemCallback<TopicCluster> DIFF_CALLBACK = new DiffUtil.ItemCallback<TopicCluster>() {
        @Override
        public boolean areItemsTheSame(@NonNull TopicCluster oldItem, @NonNull TopicCluster newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull TopicCluster oldItem, @NonNull TopicCluster newItem) {
            String oldThumbUrl = ResponseBodyUtils.getThumbUrl(oldItem.getCoverMedia());
            return oldThumbUrl != null && oldThumbUrl.equals(ResponseBodyUtils.getThumbUrl(newItem.getCoverMedia()))
                    && oldItem.getTitle().equals(newItem.getTitle());
        }
    };

    private final OnTopicClickListener onTopicClickListener;

    public DiscoverTopicsAdapter(OnTopicClickListener onTopicClickListener) {
        super(DiscoverTopicsAdapter.DIFF_CALLBACK);
        this.onTopicClickListener = onTopicClickListener;
    }

    @NonNull
    @Override
    public TopicClusterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ItemDiscoverTopicBinding binding = ItemDiscoverTopicBinding.inflate(layoutInflater, parent, false);
        return new TopicClusterViewHolder(binding, this.onTopicClickListener, null);
    }

    @Override
    public void onBindViewHolder(@NonNull TopicClusterViewHolder holder, int position) {
        TopicCluster topicCluster = this.getItem(position);
        holder.bind(topicCluster);
    }

    public interface OnTopicClickListener {
        void onTopicClick(TopicCluster topicCluster, View cover, int titleColor, int backgroundColor);

        void onTopicLongClick(Media coverMedia);
    }
}
