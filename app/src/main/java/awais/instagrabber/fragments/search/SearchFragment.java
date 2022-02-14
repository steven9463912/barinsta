package awais.instagrabber.fragments.search;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Arrays;
import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.adapters.SearchCategoryAdapter;
import awais.instagrabber.customviews.helpers.TextWatcherAdapter;
import awais.instagrabber.databinding.FragmentSearchBinding;
import awais.instagrabber.models.Resource;
import awais.instagrabber.models.enums.FavoriteType;
import awais.instagrabber.repositories.responses.search.SearchItem;
import awais.instagrabber.viewmodels.SearchFragmentViewModel;

import static awais.instagrabber.fragments.settings.PreferenceKeys.PREF_SEARCH_FOCUS_KEYBOARD;
import static awais.instagrabber.utils.Utils.settingsHelper;

public class SearchFragment extends Fragment implements SearchCategoryFragment.OnSearchItemClickListener {
    private static final String TAG = SearchFragment.class.getSimpleName();
    private static final String QUERY = "query";

    private FragmentSearchBinding binding;
    private LinearLayoutCompat root;
    private boolean shouldRefresh = true;
    @Nullable
    private EditText searchInput;
    @Nullable
    private MainActivity mainActivity;
    private SearchFragmentViewModel viewModel;

    private final TextWatcherAdapter textWatcher = new TextWatcherAdapter() {
        @Override
        public void afterTextChanged(Editable s) {
            if (s == null) return;
            SearchFragment.this.viewModel.submitQuery(s.toString().trim());
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentActivity fragmentActivity = this.getActivity();
        if (!(fragmentActivity instanceof MainActivity)) return;
        this.mainActivity = (MainActivity) fragmentActivity;
        this.viewModel = new ViewModelProvider(this.mainActivity).get(SearchFragmentViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (this.root != null) {
            this.shouldRefresh = false;
            return this.root;
        }
        this.binding = FragmentSearchBinding.inflate(inflater, container, false);
        this.root = this.binding.getRoot();
        return this.root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (!this.shouldRefresh) return;
        this.init(savedInstanceState);
        this.shouldRefresh = false;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        String current = this.viewModel.getQuery().getValue();
        if (TextUtils.isEmpty(current)) return;
        outState.putString(SearchFragment.QUERY, current);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (this.mainActivity != null) {
            this.mainActivity.hideSearchView();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.mainActivity != null) {
            this.mainActivity.hideSearchView();
        }
        if (this.searchInput != null) {
            this.searchInput.removeTextChangedListener(this.textWatcher);
            this.searchInput.setText("");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.mainActivity != null) {
            this.mainActivity.showSearchView();
        }
        if (settingsHelper.getBoolean(PREF_SEARCH_FOCUS_KEYBOARD)) {
            if (this.searchInput != null) {
                this.searchInput.requestFocus();
            }
            InputMethodManager imm = (InputMethodManager) this.requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(this.searchInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void init(@Nullable Bundle savedInstanceState) {
        if (this.mainActivity == null) return;
        this.searchInput = this.mainActivity.showSearchView().getEditText();
        this.setupObservers();
        this.setupViewPager();
        this.setupSearchInput(savedInstanceState);
    }

    private void setupObservers() {
        this.viewModel.getQuery().observe(this.getViewLifecycleOwner(), q -> {}); // need to observe, so that getQuery returns proper value
    }

    private void setupSearchInput(@Nullable Bundle savedInstanceState) {
        if (this.searchInput == null) return;
        this.searchInput.removeTextChangedListener(this.textWatcher); // make sure we add only 1 instance of textWatcher
        this.searchInput.addTextChangedListener(this.textWatcher);
        boolean triggerEmptyQuery = true;
        if (savedInstanceState != null) {
            String savedQuery = savedInstanceState.getString(SearchFragment.QUERY);
            if (TextUtils.isEmpty(savedQuery)) return;
            this.searchInput.setText(savedQuery);
            triggerEmptyQuery = false;
        }
        if (settingsHelper.getBoolean(PREF_SEARCH_FOCUS_KEYBOARD)) {
            this.searchInput.requestFocus();
            InputMethodManager imm = (InputMethodManager) this.requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(this.searchInput, InputMethodManager.SHOW_IMPLICIT);
        }
        if (triggerEmptyQuery) {
            this.viewModel.submitQuery("");
        }
    }

    private void setupViewPager() {
        this.binding.pager.setSaveEnabled(false);
        List<FavoriteType> categories = Arrays.asList(FavoriteType.values());
        this.binding.pager.setAdapter(new SearchCategoryAdapter(this, categories));
        TabLayoutMediator mediator = new TabLayoutMediator(this.binding.tabLayout, this.binding.pager, (tab, position) -> {
            try {
                FavoriteType type = categories.get(position);
                int resId;
                switch (type) {
                    case TOP:
                        resId = R.string.top;
                        break;
                    case USER:
                        resId = R.string.accounts;
                        break;
                    case HASHTAG:
                        resId = R.string.hashtags;
                        break;
                    case LOCATION:
                        resId = R.string.locations;
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + type);
                }
                tab.setText(resId);
            } catch (final Exception e) {
                Log.e(SearchFragment.TAG, "setupViewPager: ", e);
            }
        });
        mediator.attach();
    }

    @Override
    public void onSearchItemClick(SearchItem searchItem) {
        if (searchItem == null) return;
        FavoriteType type = searchItem.getType();
        if (type == null) return;
        try {
            if (!searchItem.isFavorite()) {
                this.viewModel.saveToRecentSearches(searchItem); // insert or update recent
            }
            NavDirections action;
            switch (type) {
                case USER:
                    action = SearchFragmentDirections.actionToProfile().setUsername(searchItem.getUser().getUsername());
                    break;
                case HASHTAG:
                    action = SearchFragmentDirections.actionToHashtag(searchItem.getHashtag().getName());
                    break;
                case LOCATION:
                    action = SearchFragmentDirections.actionToLocation(searchItem.getPlace().getLocation().getPk());
                    break;
                default:
                    return;
            }
            NavHostFragment.findNavController(this).navigate(action);
        } catch (final Exception e) {
            Log.e(SearchFragment.TAG, "onSearchItemClick: ", e);
        }
    }

    @Override
    public void onSearchItemDelete(SearchItem searchItem, FavoriteType type) {
        LiveData<Resource<Object>> liveData = this.viewModel.deleteRecentSearch(searchItem);
        if (liveData == null) return;
        liveData.observe(this.getViewLifecycleOwner(), new Observer<Resource<Object>>() {
            @Override
            public void onChanged(Resource<Object> resource) {
                if (resource == null) return;
                switch (resource.status) {
                    case SUCCESS:
                        SearchFragment.this.viewModel.search("", type);
                        SearchFragment.this.viewModel.search("", FavoriteType.TOP);
                        liveData.removeObserver(this);
                        break;
                    case ERROR:
                        Snackbar.make(SearchFragment.this.binding.getRoot(), R.string.error, BaseTransientBottomBar.LENGTH_SHORT).show();
                        liveData.removeObserver(this);
                        break;
                    case LOADING:
                    default:
                        break;
                }
            }
        });
    }
}