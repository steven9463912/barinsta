package awais.instagrabber.adapters.viewholder;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.R;
import awais.instagrabber.databinding.ItemSearchResultBinding;
import awais.instagrabber.fragments.search.SearchCategoryFragment;
import awais.instagrabber.models.enums.FavoriteType;
import awais.instagrabber.repositories.responses.Hashtag;
import awais.instagrabber.repositories.responses.Place;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.search.SearchItem;

public class SearchItemViewHolder extends RecyclerView.ViewHolder {

    private final ItemSearchResultBinding binding;
    private final SearchCategoryFragment.OnSearchItemClickListener onSearchItemClickListener;

    public SearchItemViewHolder(@NonNull final ItemSearchResultBinding binding,
                                final SearchCategoryFragment.OnSearchItemClickListener onSearchItemClickListener) {
        super(binding.getRoot());
        this.binding = binding;
        this.onSearchItemClickListener = onSearchItemClickListener;
    }

    public void bind(SearchItem searchItem) {
        if (searchItem == null) return;
        FavoriteType type = searchItem.getType();
        if (type == null) return;
        final String title;
        final String subtitle;
        final String picUrl;
        boolean isVerified = false;
        switch (type) {
            case USER:
                User user = searchItem.getUser();
                title = "@" + user.getUsername();
                subtitle = user.getFullName();
                picUrl = user.getProfilePicUrl();
                isVerified = user.isVerified();
                break;
            case HASHTAG:
                Hashtag hashtag = searchItem.getHashtag();
                title = "#" + hashtag.getName();
                subtitle = hashtag.getSearchResultSubtitle();
                picUrl = "res:/" + R.drawable.ic_hashtag;
                break;
            case LOCATION:
                Place place = searchItem.getPlace();
                title = place.getTitle();
                subtitle = place.getSubtitle();
                picUrl = "res:/" + R.drawable.ic_location;
                break;
            default:
                return;
        }
        this.itemView.setOnClickListener(v -> {
            if (this.onSearchItemClickListener != null) {
                this.onSearchItemClickListener.onSearchItemClick(searchItem);
            }
        });
        this.binding.delete.setVisibility(searchItem.isRecent() ? View.VISIBLE : View.GONE);
        if (searchItem.isRecent()) {
            this.binding.delete.setEnabled(true);
            this.binding.delete.setOnClickListener(v -> {
                if (this.onSearchItemClickListener != null) {
                    this.binding.delete.setEnabled(false);
                    this.onSearchItemClickListener.onSearchItemDelete(searchItem, type);
                }
            });
        }
        this.binding.title.setText(title);
        this.binding.subtitle.setText(subtitle);
        this.binding.profilePic.setImageURI(picUrl);
        this.binding.verified.setVisibility(isVerified ? View.VISIBLE : View.GONE);
    }
}
