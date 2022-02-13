package awais.instagrabber.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import awais.instagrabber.R;
import awais.instagrabber.adapters.FeedStoriesListAdapter;
import awais.instagrabber.adapters.HighlightStoriesListAdapter;
import awais.instagrabber.customviews.helpers.RecyclerLazyLoader;
import awais.instagrabber.databinding.FragmentStoryListViewerBinding;
import awais.instagrabber.repositories.requests.StoryViewerOptions;
import awais.instagrabber.repositories.responses.stories.ArchiveResponse;
import awais.instagrabber.repositories.responses.stories.Story;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.viewmodels.ArchivesViewModel;
import awais.instagrabber.viewmodels.FeedStoriesViewModel;
import awais.instagrabber.webservices.ServiceCallback;
import awais.instagrabber.webservices.StoriesRepository;
import kotlinx.coroutines.Dispatchers;

public final class StoryListViewerFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "StoryListViewerFragment";

    private AppCompatActivity fragmentActivity;
    private FragmentStoryListViewerBinding binding;
    private SwipeRefreshLayout root;
    private boolean shouldRefresh = true;
    private boolean firstRefresh = true;
    private FeedStoriesViewModel feedStoriesViewModel;
    private ArchivesViewModel archivesViewModel;
    private StoriesRepository storiesRepository;
    private Context context;
    private String type;
    private String endCursor;
    private FeedStoriesListAdapter adapter;

    private final FeedStoriesListAdapter.OnFeedStoryClickListener clickListener = new FeedStoriesListAdapter.OnFeedStoryClickListener() {
        @Override
        public void onFeedStoryClick(Story model) {
            if (model == null) return;
            List<Story> feedStoryModels = StoryListViewerFragment.this.feedStoriesViewModel.getList().getValue();
            if (feedStoryModels == null) return;
            int position = Iterables.indexOf(feedStoryModels, feedStoryModel -> feedStoryModel != null
                    && Objects.equals(feedStoryModel.getId(), model.getId()));
            try {
                NavDirections action = StoryListViewerFragmentDirections.actionToStory(StoryViewerOptions.forFeedStoryPosition(position));
                NavHostFragment.findNavController(StoryListViewerFragment.this).navigate(action);
            } catch (final Exception e) {
                Log.e(StoryListViewerFragment.TAG, "onFeedStoryClick: ", e);
            }
        }

        @Override
        public void onProfileClick(String username) {
            StoryListViewerFragment.this.openProfile(username);
        }
    };

    private final HighlightStoriesListAdapter.OnHighlightStoryClickListener archiveClickListener = new HighlightStoriesListAdapter.OnHighlightStoryClickListener() {
        @Override
        public void onHighlightClick(Story model, int position) {
            if (model == null) return;
            try {
                NavDirections action = StoryListViewerFragmentDirections.actionToStory(StoryViewerOptions.forStoryArchive(position));
                NavHostFragment.findNavController(StoryListViewerFragment.this).navigate(action);
            } catch (final Exception e) {
                Log.e(StoryListViewerFragment.TAG, "onHighlightClick: ", e);
            }
        }

        @Override
        public void onProfileClick(String username) {
            StoryListViewerFragment.this.openProfile(username);
        }
    };

    private final ServiceCallback<ArchiveResponse> cb = new ServiceCallback<ArchiveResponse>() {
        @Override
        public void onSuccess(ArchiveResponse result) {
            StoryListViewerFragment.this.binding.swipeRefreshLayout.setRefreshing(false);
            if (result == null) {
                try {
                    Context context = StoryListViewerFragment.this.getContext();
                    Toast.makeText(context, R.string.empty_list, Toast.LENGTH_SHORT).show();
                } catch (final Exception ignored) {}
            } else {
                StoryListViewerFragment.this.endCursor = result.getMaxId();
                List<Story> models = StoryListViewerFragment.this.archivesViewModel.getList().getValue();
                List<Story> modelsCopy = models == null ? new ArrayList<>() : new ArrayList<>(models);
                if (result.getItems() != null) modelsCopy.addAll(result.getItems());
                StoryListViewerFragment.this.archivesViewModel.getList().postValue(modelsCopy);
            }
        }

        @Override
        public void onFailure(Throwable t) {
            Log.e(StoryListViewerFragment.TAG, "Error", t);
            try {
                Context context = StoryListViewerFragment.this.getContext();
                Toast.makeText(context, t.getMessage(), Toast.LENGTH_SHORT).show();
            } catch (final Exception ignored) {}
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.fragmentActivity = (AppCompatActivity) this.requireActivity();
        this.context = this.getContext();
        if (this.context == null) return;
        Bundle args = this.getArguments();
        if (args == null) return;
        StoryListViewerFragmentArgs fragmentArgs = StoryListViewerFragmentArgs.fromBundle(args);
        this.type = fragmentArgs.getType();
        this.setHasOptionsMenu(this.type.equals("feed"));
        this.storiesRepository = StoriesRepository.Companion.getInstance();
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (this.root != null) {
            this.shouldRefresh = false;
            return this.root;
        }
        this.binding = FragmentStoryListViewerBinding.inflate(this.getLayoutInflater());
        this.root = this.binding.getRoot();
        return this.root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (!this.shouldRefresh) return;
        this.init();
        this.shouldRefresh = false;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.search, menu);
        MenuItem menuSearch = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) menuSearch.getActionView();
        searchView.setQueryHint(this.getResources().getString(R.string.action_search));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                if (StoryListViewerFragment.this.adapter != null) {
                    StoryListViewerFragment.this.adapter.getFilter().filter(query);
                }
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        ActionBar actionBar = this.fragmentActivity.getSupportActionBar();
        if (actionBar != null) actionBar.setTitle(this.type.equals("feed") ? R.string.feed_stories : R.string.action_archive);
    }

    @Override
    public void onDestroy() {
        if (this.archivesViewModel != null) this.archivesViewModel.getList().postValue(null);
        super.onDestroy();
    }

    private void init() {
        Context context = this.getContext();
        this.binding.swipeRefreshLayout.setOnRefreshListener(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        ActionBar actionBar = this.fragmentActivity.getSupportActionBar();
        if (this.type.equals("feed")) {
            if (actionBar != null) actionBar.setTitle(R.string.feed_stories);
            this.feedStoriesViewModel = new ViewModelProvider(this.fragmentActivity).get(FeedStoriesViewModel.class);
            this.adapter = new FeedStoriesListAdapter(this.clickListener);
            this.binding.rvStories.setLayoutManager(layoutManager);
            this.binding.rvStories.setAdapter(this.adapter);
            this.feedStoriesViewModel.getList().observe(this.getViewLifecycleOwner(), list -> {
                if (list == null) {
                    this.adapter.submitList(Collections.emptyList());
                    return;
                }
                this.adapter.submitList(list);
            });
        } else {
            if (actionBar != null) actionBar.setTitle(R.string.action_archive);
            RecyclerLazyLoader lazyLoader = new RecyclerLazyLoader(layoutManager, (page, totalItemsCount) -> {
                if (!TextUtils.isEmpty(this.endCursor)) this.onRefresh();
                this.endCursor = null;
            });
            this.binding.rvStories.addOnScrollListener(lazyLoader);
            this.archivesViewModel = new ViewModelProvider(this.fragmentActivity).get(ArchivesViewModel.class);
            HighlightStoriesListAdapter adapter = new HighlightStoriesListAdapter(this.archiveClickListener);
            this.binding.rvStories.setLayoutManager(layoutManager);
            this.binding.rvStories.setAdapter(adapter);
            this.archivesViewModel.getList().observe(this.getViewLifecycleOwner(), adapter::submitList);
        }
        this.onRefresh();
    }

    @Override
    public void onRefresh() {
        this.binding.swipeRefreshLayout.setRefreshing(true);
        if (this.type.equals("feed") && this.firstRefresh) {
            this.binding.swipeRefreshLayout.setRefreshing(false);
            List<Story> value = this.feedStoriesViewModel.getList().getValue();
            if (value != null) {
                this.adapter.submitList(value);
            }
            this.firstRefresh = false;
        } else if (this.type.equals("feed")) {
            this.storiesRepository.getFeedStories(
                    CoroutineUtilsKt.getContinuation((feedStoryModels, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                        if (throwable != null) {
                            Log.e(StoryListViewerFragment.TAG, "failed", throwable);
                            Toast.makeText(this.context, throwable.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        //noinspection unchecked
                        this.feedStoriesViewModel.getList().postValue((List<Story>) feedStoryModels);
                        //noinspection unchecked
                        this.adapter.submitList((List<Story>) feedStoryModels);
                        this.binding.swipeRefreshLayout.setRefreshing(false);
                    }), Dispatchers.getIO())
            );
        } else if (this.type.equals("archive")) {
            this.storiesRepository.fetchArchive(
                    this.endCursor,
                    CoroutineUtilsKt.getContinuation((archiveFetchResponse, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                        if (throwable != null) {
                            this.cb.onFailure(throwable);
                            return;
                        }
                        this.cb.onSuccess(archiveFetchResponse);
                    }), Dispatchers.getIO())
            );
        }
    }

    private void openProfile(String username) {
        try {
            NavDirections action = StoryListViewerFragmentDirections.actionToProfile().setUsername(username);
            NavHostFragment.findNavController(this).navigate(action);
        } catch (final Exception e) {
            Log.e(StoryListViewerFragment.TAG, "openProfile: ", e);
        }
    }
}