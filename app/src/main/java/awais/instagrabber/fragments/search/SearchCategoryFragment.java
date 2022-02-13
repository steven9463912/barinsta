package awais.instagrabber.fragments.search;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import awais.instagrabber.adapters.SearchItemsAdapter;
import awais.instagrabber.models.Resource;
import awais.instagrabber.models.enums.FavoriteType;
import awais.instagrabber.repositories.responses.search.SearchItem;
import awais.instagrabber.viewmodels.SearchFragmentViewModel;

public class SearchCategoryFragment extends Fragment {
    private static final String TAG = SearchCategoryFragment.class.getSimpleName();
    private static final String ARG_TYPE = "type";


    @Nullable
    private SwipeRefreshLayout swipeRefreshLayout;
    @Nullable
    private RecyclerView list;
    private SearchFragmentViewModel viewModel;
    private FavoriteType type;
    private SearchItemsAdapter searchItemsAdapter;
    @Nullable
    private OnSearchItemClickListener onSearchItemClickListener;
    private boolean skipViewRefresh;
    private String prevQuery;

    @NonNull
    public static SearchCategoryFragment newInstance(@NonNull FavoriteType type) {
        SearchCategoryFragment fragment = new SearchCategoryFragment();
        Bundle args = new Bundle();
        args.putSerializable(SearchCategoryFragment.ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    public SearchCategoryFragment() {}

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Fragment parentFragment = this.getParentFragment();
        if (!(parentFragment instanceof OnSearchItemClickListener)) return;
        this.onSearchItemClickListener = (OnSearchItemClickListener) parentFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentActivity fragmentActivity = this.getActivity();
        if (fragmentActivity == null) return;
        this.viewModel = new ViewModelProvider(fragmentActivity).get(SearchFragmentViewModel.class);
        Bundle args = this.getArguments();
        if (args == null) {
            Log.e(SearchCategoryFragment.TAG, "onCreate: arguments are null");
            return;
        }
        Serializable typeSerializable = args.getSerializable(SearchCategoryFragment.ARG_TYPE);
        if (!(typeSerializable instanceof FavoriteType)) {
            Log.e(SearchCategoryFragment.TAG, "onCreate: type not a FavoriteType");
            return;
        }
        this.type = (FavoriteType) typeSerializable;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context context = this.getContext();
        if (context == null) return null;
        this.skipViewRefresh = false;
        if (this.swipeRefreshLayout != null) {
            this.skipViewRefresh = true;
            return this.swipeRefreshLayout;
        }
        this.swipeRefreshLayout = new SwipeRefreshLayout(context);
        this.swipeRefreshLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        this.list = new RecyclerView(context);
        this.list.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        this.swipeRefreshLayout.addView(this.list);
        return this.swipeRefreshLayout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (this.skipViewRefresh) return;
        this.setupList();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Log.d(TAG, "onResume: type: " + type);
        this.setupObservers();
        String currentQuery = this.viewModel.getQuery().getValue();
        if (this.prevQuery != null && currentQuery != null && !Objects.equals(this.prevQuery, currentQuery)) {
            this.viewModel.search(currentQuery, this.type);
        }
        this.prevQuery = null;
    }

    private void setupList() {
        if (this.list == null || this.swipeRefreshLayout == null) return;
        Context context = this.getContext();
        if (context == null) return;
        this.list.setLayoutManager(new LinearLayoutManager(context));
        this.searchItemsAdapter = new SearchItemsAdapter(this.onSearchItemClickListener);
        this.list.setAdapter(this.searchItemsAdapter);
        this.swipeRefreshLayout.setOnRefreshListener(() -> {
            String currentQuery = this.viewModel.getQuery().getValue();
            if (currentQuery == null) currentQuery = "";
            this.viewModel.search(currentQuery, this.type);
        });
    }

    private void setupObservers() {
        this.viewModel.getQuery().observe(this.getViewLifecycleOwner(), q -> {
            if (!this.isVisible() || Objects.equals(this.prevQuery, q)) return;
            this.viewModel.search(q, this.type);
            this.prevQuery = q;
        });
        LiveData<Resource<List<SearchItem>>> resultsLiveData = this.getResultsLiveData();
        if (resultsLiveData != null) {
            resultsLiveData.observe(this.getViewLifecycleOwner(), this::onResults);
        }
    }

    private void onResults(Resource<List<SearchItem>> listResource) {
        if (listResource == null) return;
        switch (listResource.status) {
            case SUCCESS:
                if (this.searchItemsAdapter != null) {
                    this.searchItemsAdapter.submitList(listResource.data);
                }
                if (this.swipeRefreshLayout != null) {
                    this.swipeRefreshLayout.setRefreshing(false);
                }
                break;
            case ERROR:
                if (this.searchItemsAdapter != null) {
                    this.searchItemsAdapter.submitList(Collections.emptyList());
                }
                if (this.swipeRefreshLayout != null) {
                    this.swipeRefreshLayout.setRefreshing(false);
                }
                break;
            case LOADING:
                if (this.swipeRefreshLayout != null) {
                    this.swipeRefreshLayout.setRefreshing(true);
                }
                break;
            default:
                break;
        }
    }

    @Nullable
    private LiveData<Resource<List<SearchItem>>> getResultsLiveData() {
        switch (this.type) {
            case TOP:
                return this.viewModel.getTopResults();
            case USER:
                return this.viewModel.getUserResults();
            case HASHTAG:
                return this.viewModel.getHashtagResults();
            case LOCATION:
                return this.viewModel.getLocationResults();
        }
        return null;
    }

    public interface OnSearchItemClickListener {
        void onSearchItemClick(SearchItem searchItem);

        void onSearchItemDelete(SearchItem searchItem, FavoriteType type);
    }
}
