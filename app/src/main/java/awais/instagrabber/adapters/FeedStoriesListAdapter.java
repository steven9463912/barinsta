package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import awais.instagrabber.adapters.viewholder.StoryListViewHolder;
import awais.instagrabber.databinding.ItemNotificationBinding;
import awais.instagrabber.repositories.responses.stories.Story;
import awais.instagrabber.utils.TextUtils;

public final class FeedStoriesListAdapter extends ListAdapter<Story, StoryListViewHolder> implements Filterable {
    private final OnFeedStoryClickListener listener;
    private List<Story> list;

    private final Filter filter = new Filter() {
        @NonNull
        @Override
        protected FilterResults performFiltering(CharSequence filter) {
            String query = TextUtils.isEmpty(filter) ? null : filter.toString().toLowerCase();
            List<Story> filteredList = FeedStoriesListAdapter.this.list;
            if (FeedStoriesListAdapter.this.list != null && query != null) {
                filteredList = FeedStoriesListAdapter.this.list.stream()
                                   .filter(feedStoryModel -> feedStoryModel.getUser()
                                                                           .getUsername()
                                                                           .toLowerCase()
                                                                           .contains(query))
                                   .collect(Collectors.toList());
            }
            FilterResults filterResults = new FilterResults();
            filterResults.count = filteredList != null ? filteredList.size() : 0;
            filterResults.values = filteredList;
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            //noinspection unchecked
            FeedStoriesListAdapter.this.submitList((List<Story>) results.values, true);
        }
    };

    private static final DiffUtil.ItemCallback<Story> diffCallback = new DiffUtil.ItemCallback<Story>() {
        @Override
        public boolean areItemsTheSame(@NonNull Story oldItem, @NonNull Story newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Story oldItem, @NonNull Story newItem) {
            return oldItem.getId().equals(newItem.getId()) && Objects.equals(oldItem.getSeen(), newItem.getSeen());
        }
    };

    public FeedStoriesListAdapter(OnFeedStoryClickListener listener) {
        super(FeedStoriesListAdapter.diffCallback);
        this.listener = listener;
    }

    @Override
    public Filter getFilter() {
        return this.filter;
    }

    private void submitList(@Nullable List<Story> list, boolean isFiltered) {
        if (!isFiltered) {
            this.list = list;
        }
        super.submitList(list);
    }

    @Override
    public void submitList(List<Story> list) {
        this.submitList(list, false);
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
        holder.bind(model, this.listener);
    }

    public interface OnFeedStoryClickListener {
        void onFeedStoryClick(Story model);

        void onProfileClick(String username);
    }
}
