package awais.instagrabber.adapters.viewholder;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.adapters.FavoritesAdapter;
import awais.instagrabber.databinding.ItemSearchResultBinding;
import awais.instagrabber.db.entities.Favorite;
import awais.instagrabber.models.enums.FavoriteType;
import awais.instagrabber.utils.Constants;

public class FavoriteViewHolder extends RecyclerView.ViewHolder {
    private static final String TAG = "FavoriteViewHolder";

    private final ItemSearchResultBinding binding;

    public FavoriteViewHolder(@NonNull ItemSearchResultBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
        binding.verified.setVisibility(View.GONE);
    }

    public void bind(Favorite model,
                     FavoritesAdapter.OnFavoriteClickListener clickListener,
                     FavoritesAdapter.OnFavoriteLongClickListener longClickListener) {
        // Log.d(TAG, "bind: " + model);
        if (model == null) return;
        this.itemView.setOnClickListener(v -> {
            if (clickListener == null) return;
            clickListener.onClick(model);
        });
        this.itemView.setOnLongClickListener(v -> {
            if (clickListener == null) return false;
            return longClickListener.onLongClick(model);
        });
        if (model.getType() == FavoriteType.HASHTAG) {
            this.binding.profilePic.setImageURI(Constants.DEFAULT_HASH_TAG_PIC);
        } else {
            this.binding.profilePic.setImageURI(model.getPicUrl());
        }
        this.binding.title.setVisibility(View.VISIBLE);
        this.binding.subtitle.setText(model.getDisplayName());
        String query = model.getQuery();
        switch (model.getType()) {
            case HASHTAG:
                query = "#" + query;
                break;
            case USER:
                query = "@" + query;
                break;
            case LOCATION:
                this.binding.title.setVisibility(View.GONE);
                break;
            default:
                // do nothing
        }
        this.binding.title.setText(query);
    }
}
