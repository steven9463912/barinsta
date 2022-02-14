package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;
import androidx.recyclerview.widget.AdapterListUpdateCallback;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.adapters.viewholder.FavoriteViewHolder;
import awais.instagrabber.databinding.ItemFavSectionHeaderBinding;
import awais.instagrabber.databinding.ItemSearchResultBinding;
import awais.instagrabber.db.entities.Favorite;
import awais.instagrabber.models.enums.FavoriteType;

public class FavoritesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final OnFavoriteClickListener clickListener;
    private final OnFavoriteLongClickListener longClickListener;
    private final AsyncListDiffer<FavoriteModelOrHeader> differ;

    private static final DiffUtil.ItemCallback<FavoriteModelOrHeader> diffCallback = new DiffUtil.ItemCallback<FavoriteModelOrHeader>() {
        @Override
        public boolean areItemsTheSame(@NonNull FavoriteModelOrHeader oldItem, @NonNull FavoriteModelOrHeader newItem) {
            final boolean areSame = oldItem.isHeader() && newItem.isHeader();
            if (!areSame) {
                return false;
            }
            if (oldItem.isHeader()) {
                return ObjectsCompat.equals(oldItem.header, newItem.header);
            }
            if (oldItem.model != null && newItem.model != null) {
                return oldItem.model.getId() == newItem.model.getId();
            }
            return false;
        }

        @Override
        public boolean areContentsTheSame(@NonNull FavoriteModelOrHeader oldItem, @NonNull FavoriteModelOrHeader newItem) {
            final boolean areSame = oldItem.isHeader() && newItem.isHeader();
            if (!areSame) {
                return false;
            }
            if (oldItem.isHeader()) {
                return ObjectsCompat.equals(oldItem.header, newItem.header);
            }
            return ObjectsCompat.equals(oldItem.model, newItem.model);
        }
    };

    public FavoritesAdapter(OnFavoriteClickListener clickListener, OnFavoriteLongClickListener longClickListener) {
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
        this.differ = new AsyncListDiffer<>(new AdapterListUpdateCallback(this),
                                       new AsyncDifferConfig.Builder<>(FavoritesAdapter.diffCallback).build());
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == 0) {
            // header
            return new FavSectionViewHolder(ItemFavSectionHeaderBinding.inflate(inflater, parent, false));
        }
        ItemSearchResultBinding binding = ItemSearchResultBinding.inflate(inflater, parent, false);
        return new FavoriteViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (this.getItemViewType(position) == 0) {
            FavoriteModelOrHeader modelOrHeader = this.getItem(position);
            if (!modelOrHeader.isHeader()) return;
            ((FavSectionViewHolder) holder).bind(modelOrHeader.header);
            return;
        }
        ((FavoriteViewHolder) holder).bind(this.getItem(position).model, this.clickListener, this.longClickListener);
    }

    protected FavoriteModelOrHeader getItem(final int position) {
        return this.differ.getCurrentList().get(position);
    }

    @Override
    public int getItemCount() {
        return this.differ.getCurrentList().size();
    }

    @Override
    public int getItemViewType(int position) {
        return this.getItem(position).isHeader() ? 0 : 1;
    }

    public void submitList(@Nullable List<Favorite> list) {
        if (list == null) {
            this.differ.submitList(null);
            return;
        }
        this.differ.submitList(this.sectionAndSort(list));
    }

    public void submitList(@Nullable List<Favorite> list, @Nullable Runnable commitCallback) {
        if (list == null) {
            this.differ.submitList(null, commitCallback);
            return;
        }
        this.differ.submitList(this.sectionAndSort(list), commitCallback);
    }

    @NonNull
    private List<FavoriteModelOrHeader> sectionAndSort(@NonNull List<Favorite> list) {
        List<Favorite> listCopy = new ArrayList<>(list);
        Collections.sort(listCopy, (o1, o2) -> {
            if (o1.getType() == o2.getType()) return 0;
            // keep users at top
            if (o1.getType() == FavoriteType.USER) return -1;
            if (o2.getType() == FavoriteType.USER) return 1;
            // keep locations at bottom
            if (o1.getType() == FavoriteType.LOCATION) return 1;
            if (o2.getType() == FavoriteType.LOCATION) return -1;
            return 0;
        });
        List<FavoriteModelOrHeader> modelOrHeaders = new ArrayList<>();
        for (int i = 0; i < listCopy.size(); i++) {
            Favorite model = listCopy.get(i);
            FavoriteModelOrHeader prev = modelOrHeaders.isEmpty() ? null : modelOrHeaders.get(modelOrHeaders.size() - 1);
            final boolean prevWasSameType = prev != null && prev.model.getType() == model.getType();
            if (prevWasSameType) {
                // just add model
                FavoriteModelOrHeader modelOrHeader = new FavoriteModelOrHeader();
                modelOrHeader.model = model;
                modelOrHeaders.add(modelOrHeader);
                continue;
            }
            // add header and model
            FavoriteModelOrHeader modelOrHeader = new FavoriteModelOrHeader();
            modelOrHeader.header = model.getType();
            modelOrHeaders.add(modelOrHeader);
            modelOrHeader = new FavoriteModelOrHeader();
            modelOrHeader.model = model;
            modelOrHeaders.add(modelOrHeader);
        }
        return modelOrHeaders;
    }

    private static class FavoriteModelOrHeader {
        FavoriteType header;
        Favorite model;

        boolean isHeader() {
            return this.header != null;
        }
    }

    public interface OnFavoriteClickListener {
        void onClick(Favorite model);
    }

    public interface OnFavoriteLongClickListener {
        boolean onLongClick(Favorite model);
    }

    public static class FavSectionViewHolder extends RecyclerView.ViewHolder {
        private final ItemFavSectionHeaderBinding binding;

        public FavSectionViewHolder(@NonNull ItemFavSectionHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(FavoriteType header) {
            if (header == null) return;
            int headerText;
            switch (header) {
                case USER:
                    headerText = R.string.accounts;
                    break;
                case HASHTAG:
                    headerText = R.string.hashtags;
                    break;
                case LOCATION:
                    headerText = R.string.locations;
                    break;
                default:
                    headerText = R.string.unknown;
                    break;
            }
            this.binding.getRoot().setText(headerText);
        }
    }
}
