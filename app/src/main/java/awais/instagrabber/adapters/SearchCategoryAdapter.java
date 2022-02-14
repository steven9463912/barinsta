package awais.instagrabber.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

import awais.instagrabber.fragments.search.SearchCategoryFragment;
import awais.instagrabber.models.enums.FavoriteType;

public class SearchCategoryAdapter extends FragmentStateAdapter {

    private final List<FavoriteType> categories;

    public SearchCategoryAdapter(@NonNull Fragment fragment,
                                 @NonNull List<FavoriteType> categories) {
        super(fragment);
        this.categories = categories;

    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return SearchCategoryFragment.newInstance(this.categories.get(position));
    }

    @Override
    public int getItemCount() {
        return this.categories.size();
    }
}
