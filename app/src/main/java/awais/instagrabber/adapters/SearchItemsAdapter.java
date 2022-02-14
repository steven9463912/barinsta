package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.AdapterListUpdateCallback;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import awais.instagrabber.R;
import awais.instagrabber.adapters.viewholder.SearchItemViewHolder;
import awais.instagrabber.databinding.ItemFavSectionHeaderBinding;
import awais.instagrabber.databinding.ItemSearchResultBinding;
import awais.instagrabber.fragments.search.SearchCategoryFragment;
import awais.instagrabber.models.enums.FavoriteType;
import awais.instagrabber.repositories.responses.search.SearchItem;

public final class SearchItemsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = SearchItemsAdapter.class.getSimpleName();
    private static final DiffUtil.ItemCallback<SearchItemOrHeader> DIFF_CALLBACK = new DiffUtil.ItemCallback<SearchItemOrHeader>() {
        @Override
        public boolean areItemsTheSame(@NonNull SearchItemOrHeader oldItem, @NonNull SearchItemOrHeader newItem) {
            return Objects.equals(oldItem, newItem);
        }

        @Override
        public boolean areContentsTheSame(@NonNull SearchItemOrHeader oldItem, @NonNull SearchItemOrHeader newItem) {
            return Objects.equals(oldItem, newItem);
        }
    };
    private static final String RECENT = "recent";
    private static final String FAVORITE = "favorite";
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private final SearchCategoryFragment.OnSearchItemClickListener onSearchItemClickListener;
    private final AsyncListDiffer<SearchItemOrHeader> differ;

    public SearchItemsAdapter(final SearchCategoryFragment.OnSearchItemClickListener onSearchItemClickListener) {
        this.differ = new AsyncListDiffer<>(new AdapterListUpdateCallback(this),
                                       new AsyncDifferConfig.Builder<>(SearchItemsAdapter.DIFF_CALLBACK).build());
        this.onSearchItemClickListener = onSearchItemClickListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        if (viewType == SearchItemsAdapter.VIEW_TYPE_HEADER) {
            return new HeaderViewHolder(ItemFavSectionHeaderBinding.inflate(layoutInflater, parent, false));
        }
        ItemSearchResultBinding binding = ItemSearchResultBinding.inflate(layoutInflater, parent, false);
        return new SearchItemViewHolder(binding, this.onSearchItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (this.getItemViewType(position) == SearchItemsAdapter.VIEW_TYPE_HEADER) {
            SearchItemOrHeader searchItemOrHeader = this.getItem(position);
            if (!searchItemOrHeader.isHeader()) return;
            ((HeaderViewHolder) holder).bind(searchItemOrHeader.header);
            return;
        }
        ((SearchItemViewHolder) holder).bind(this.getItem(position).searchItem);
    }

    protected SearchItemOrHeader getItem(final int position) {
        return this.differ.getCurrentList().get(position);
    }

    @Override
    public int getItemCount() {
        return this.differ.getCurrentList().size();
    }

    @Override
    public int getItemViewType(int position) {
        return this.getItem(position).isHeader() ? SearchItemsAdapter.VIEW_TYPE_HEADER : SearchItemsAdapter.VIEW_TYPE_ITEM;
    }

    public void submitList(@Nullable List<SearchItem> list) {
        if (list == null) {
            this.differ.submitList(null);
            return;
        }
        this.differ.submitList(this.sectionAndSort(list));
    }

    public void submitList(@Nullable List<SearchItem> list, @Nullable Runnable commitCallback) {
        if (list == null) {
            this.differ.submitList(null, commitCallback);
            return;
        }
        this.differ.submitList(this.sectionAndSort(list), commitCallback);
    }

    @NonNull
    private List<SearchItemOrHeader> sectionAndSort(@NonNull List<SearchItem> list) {
        boolean containsRecentOrFavorite = list.stream().anyMatch(searchItem -> searchItem.isRecent() || searchItem.isFavorite());
        // Don't do anything if not showing recent results
        if (!containsRecentOrFavorite) {
            return list.stream()
                       .map(SearchItemOrHeader::new)
                       .collect(Collectors.toList());
        }
        List<SearchItem> listCopy = new ArrayList<>(list);
        Collections.sort(listCopy, (o1, o2) -> {
            boolean bothRecent = o1.isRecent() && o2.isRecent();
            if (bothRecent) {
                // Don't sort
                return 0;
            }
            boolean bothFavorite = o1.isFavorite() && o2.isFavorite();
            if (bothFavorite) {
                if (o1.getType() == o2.getType()) return 0;
                // keep users at top
                if (o1.getType() == FavoriteType.USER) return -1;
                if (o2.getType() == FavoriteType.USER) return 1;
                // keep locations at bottom
                if (o1.getType() == FavoriteType.LOCATION) return 1;
                if (o2.getType() == FavoriteType.LOCATION) return -1;
            }
            // keep recents at top
            if (o1.isRecent()) return -1;
            if (o2.isRecent()) return 1;
            return 0;
        });
        List<SearchItemOrHeader> itemOrHeaders = new ArrayList<>();
        for (int i = 0; i < listCopy.size(); i++) {
            SearchItem searchItem = listCopy.get(i);
            SearchItemOrHeader prev = itemOrHeaders.isEmpty() ? null : itemOrHeaders.get(itemOrHeaders.size() - 1);
            final boolean prevWasSameType = prev != null && ((prev.searchItem.isRecent() && searchItem.isRecent())
                    || (prev.searchItem.isFavorite() && searchItem.isFavorite()));
            if (prevWasSameType) {
                // just add the item
                itemOrHeaders.add(new SearchItemOrHeader(searchItem));
                continue;
            }
            // add header and item
            // add header only if search item is recent or favorite
            if (searchItem.isRecent() || searchItem.isFavorite()) {
                itemOrHeaders.add(new SearchItemOrHeader(searchItem.isRecent() ? SearchItemsAdapter.RECENT : SearchItemsAdapter.FAVORITE));
            }
            itemOrHeaders.add(new SearchItemOrHeader(searchItem));
        }
        return itemOrHeaders;
    }

    private static class SearchItemOrHeader {
        String header;
        SearchItem searchItem;

        public SearchItemOrHeader(SearchItem searchItem) {
            this.searchItem = searchItem;
        }

        public SearchItemOrHeader(String header) {
            this.header = header;
        }

        boolean isHeader() {
            return this.header != null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || this.getClass() != o.getClass()) return false;
            SearchItemOrHeader that = (SearchItemOrHeader) o;
            return Objects.equals(this.header, that.header) &&
                    Objects.equals(this.searchItem, that.searchItem);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.header, this.searchItem);
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final ItemFavSectionHeaderBinding binding;

        public HeaderViewHolder(@NonNull ItemFavSectionHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(String header) {
            if (header == null) return;
            int headerText;
            switch (header) {
                case SearchItemsAdapter.RECENT:
                    headerText = R.string.recent;
                    break;
                case SearchItemsAdapter.FAVORITE:
                    headerText = R.string.title_favorites;
                    break;
                default:
                    headerText = R.string.unknown;
                    break;
            }
            this.binding.getRoot().setText(headerText);
        }
    }
}