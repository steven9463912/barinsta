package awais.instagrabber.fragments.main;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import awais.instagrabber.R;
import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.adapters.FeedAdapterV2;
import awais.instagrabber.adapters.FeedStoriesAdapter;
import awais.instagrabber.asyncs.FeedPostFetchService;
import awais.instagrabber.customviews.PrimaryActionModeCallback;
import awais.instagrabber.databinding.FragmentFeedBinding;
import awais.instagrabber.dialogs.PostsLayoutPreferencesDialogFragment;
import awais.instagrabber.fragments.settings.PreferenceKeys;
import awais.instagrabber.models.PostsLayoutPreferences;
import awais.instagrabber.repositories.requests.StoryViewerOptions;
import awais.instagrabber.repositories.responses.Location;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.stories.Story;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.viewmodels.FeedStoriesViewModel;
import awais.instagrabber.webservices.StoriesRepository;
import kotlinx.coroutines.Dispatchers;

public class FeedFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "FeedFragment";

    private MainActivity fragmentActivity;
    private CoordinatorLayout root;
    private FragmentFeedBinding binding;
    private StoriesRepository storiesRepository;
    private boolean shouldRefresh = true;
    private FeedStoriesViewModel feedStoriesViewModel;
    private boolean storiesFetching;
    private ActionMode actionMode;
    private Set<Media> selectedFeedModels;
    private PostsLayoutPreferences layoutPreferences = Utils.getPostsLayoutPreferences(Constants.PREF_POSTS_LAYOUT);
    private MenuItem storyListMenu;

    private final FeedStoriesAdapter feedStoriesAdapter = new FeedStoriesAdapter(
            new FeedStoriesAdapter.OnFeedStoryClickListener() {
                @Override
                public void onFeedStoryClick(final Story model, final int position) {
                    NavController navController = NavHostFragment.findNavController(FeedFragment.this);
                    if (FeedFragment.this.isSafeToNavigate(navController)) {
                        try {
                            NavDirections action = FeedFragmentDirections.actionToStory(StoryViewerOptions.forFeedStoryPosition(position));
                            navController.navigate(action);
                        } catch (final Exception e) {
                            Log.e(FeedFragment.TAG, "onFeedStoryClick: ", e);
                        }
                    }
                }

                @Override
                public void onFeedStoryLongClick(final Story model, final int position) {
                    User user = model.getUser();
                    if (user == null) return;
                    FeedFragment.this.navigateToProfile("@" + user.getUsername());
                }
            }
    );

    private final FeedAdapterV2.FeedItemCallback feedItemCallback = new FeedAdapterV2.FeedItemCallback() {
        @Override
        public void onPostClick(Media feedModel) {
            this.openPostDialog(feedModel, -1);
        }

        @Override
        public void onSliderClick(Media feedModel, int position) {
            this.openPostDialog(feedModel, position);
        }

        @Override
        public void onCommentsClick(Media feedModel) {
            try {
                User user = feedModel.getUser();
                if (user == null) return;
                NavDirections commentsAction = FeedFragmentDirections.actionToComments(
                        feedModel.getCode(),
                        feedModel.getPk(),
                        user.getPk()
                );
                NavHostFragment.findNavController(FeedFragment.this).navigate(commentsAction);
            } catch (final Exception e) {
                Log.e(FeedFragment.TAG, "onCommentsClick: ", e);
            }
        }

        @Override
        public void onDownloadClick(Media feedModel, int childPosition, View popupLocation) {
            Context context = FeedFragment.this.getContext();
            if (context == null) return;
            DownloadUtils.showDownloadDialog(context, feedModel, childPosition, popupLocation);
        }

        @Override
        public void onHashtagClick(String hashtag) {
            try {
                NavDirections action = FeedFragmentDirections.actionToHashtag(hashtag);
                NavHostFragment.findNavController(FeedFragment.this).navigate(action);
            } catch (final Exception e) {
                Log.e(FeedFragment.TAG, "onHashtagClick: ", e);
            }
        }

        @Override
        public void onLocationClick(Media feedModel) {
            Location location = feedModel.getLocation();
            if (location == null) return;
            try {
                NavDirections action = FeedFragmentDirections.actionToLocation(location.getPk());
                NavHostFragment.findNavController(FeedFragment.this).navigate(action);
            } catch (final Exception e) {
                Log.e(FeedFragment.TAG, "onLocationClick: ", e);
            }
        }

        @Override
        public void onMentionClick(String mention) {
            FeedFragment.this.navigateToProfile(mention.trim());
        }

        @Override
        public void onNameClick(Media feedModel) {
            if (feedModel.getUser() == null) return;
            FeedFragment.this.navigateToProfile("@" + feedModel.getUser().getUsername());
        }

        @Override
        public void onProfilePicClick(Media feedModel) {
            if (feedModel.getUser() == null) return;
            FeedFragment.this.navigateToProfile("@" + feedModel.getUser().getUsername());
        }

        @Override
        public void onURLClick(String url) {
            Utils.openURL(FeedFragment.this.getContext(), url);
        }

        @Override
        public void onEmailClick(String emailId) {
            Utils.openEmailAddress(FeedFragment.this.getContext(), emailId);
        }

        private void openPostDialog(Media feedModel, int position) {
            try {
                NavDirections action = FeedFragmentDirections.actionToPost(feedModel, position);
                NavHostFragment.findNavController(FeedFragment.this).navigate(action);
            } catch (final Exception e) {
                Log.e(FeedFragment.TAG, "openPostDialog: ", e);
            }
        }
    };
    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            FeedFragment.this.binding.feedRecyclerView.endSelection();
        }
    };
    private final PrimaryActionModeCallback multiSelectAction = new PrimaryActionModeCallback(
            R.menu.multi_select_download_menu,
            new PrimaryActionModeCallback.CallbacksHelper() {
                @Override
                public void onDestroy(ActionMode mode) {
                    FeedFragment.this.binding.feedRecyclerView.endSelection();
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    if (item.getItemId() == R.id.action_download) {
                        if (selectedFeedModels == null) return false;
                        Context context = FeedFragment.this.getContext();
                        if (context == null) return false;
                        DownloadUtils.download(context, ImmutableList.copyOf(selectedFeedModels));
                        FeedFragment.this.binding.feedRecyclerView.endSelection();
                        return true;
                    }
                    return false;
                }
            });
    private final FeedAdapterV2.SelectionModeCallback selectionModeCallback = new FeedAdapterV2.SelectionModeCallback() {

        @Override
        public void onSelectionStart() {
            if (!FeedFragment.this.onBackPressedCallback.isEnabled()) {
                OnBackPressedDispatcher onBackPressedDispatcher = FeedFragment.this.fragmentActivity.getOnBackPressedDispatcher();
                FeedFragment.this.onBackPressedCallback.setEnabled(true);
                onBackPressedDispatcher.addCallback(FeedFragment.this.getViewLifecycleOwner(), FeedFragment.this.onBackPressedCallback);
            }
            if (FeedFragment.this.actionMode == null) {
                FeedFragment.this.actionMode = FeedFragment.this.fragmentActivity.startActionMode(FeedFragment.this.multiSelectAction);
            }
        }

        @Override
        public void onSelectionChange(Set<Media> selectedFeedModels) {
            String title = FeedFragment.this.getString(R.string.number_selected, selectedFeedModels.size());
            if (FeedFragment.this.actionMode != null) {
                FeedFragment.this.actionMode.setTitle(title);
            }
            FeedFragment.this.selectedFeedModels = selectedFeedModels;
        }

        @Override
        public void onSelectionEnd() {
            if (FeedFragment.this.onBackPressedCallback.isEnabled()) {
                FeedFragment.this.onBackPressedCallback.setEnabled(false);
                FeedFragment.this.onBackPressedCallback.remove();
            }
            if (FeedFragment.this.actionMode != null) {
                FeedFragment.this.actionMode.finish();
                FeedFragment.this.actionMode = null;
            }
        }
    };

    private void navigateToProfile(String username) {
        try {
            NavDirections action = FeedFragmentDirections.actionToProfile().setUsername(username);
            NavHostFragment.findNavController(this).navigate(action);
        } catch (final Exception e) {
            Log.e(FeedFragment.TAG, "navigateToProfile: ", e);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.fragmentActivity = (MainActivity) this.requireActivity();
        this.storiesRepository = StoriesRepository.Companion.getInstance();
        this.setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        if (this.root != null) {
            this.shouldRefresh = false;
            return this.root;
        }
        this.binding = FragmentFeedBinding.inflate(inflater, container, false);
        this.root = this.binding.getRoot();
        return this.root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (!this.shouldRefresh) return;
        this.binding.feedSwipeRefreshLayout.setOnRefreshListener(this);
        /*
        FabAnimation.init(binding.fabCamera);
        FabAnimation.init(binding.fabStory);
        binding.fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRotate = FabAnimation.rotateFab(v, !isRotate);
                if (isRotate) {
                    FabAnimation.showIn(binding.fabCamera);
                    FabAnimation.showIn(binding.fabStory);
                }
                else {
                    FabAnimation.showOut(binding.fabCamera);
                    FabAnimation.showOut(binding.fabStory);
                }
            }
        });
         */
        this.setupFeedStories();
        this.setupFeed();
        this.shouldRefresh = false;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.feed_menu, menu);
        this.storyListMenu = menu.findItem(R.id.storyList);
        this.storyListMenu.setVisible(!this.storiesFetching);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.storyList) {
            try {
                NavDirections action = FeedFragmentDirections.actionToStoryList("feed");
                NavHostFragment.findNavController(this).navigate(action);
            } catch (final Exception e) {
                Log.e(FeedFragment.TAG, "onOptionsItemSelected: ", e);
            }
        } else if (item.getItemId() == R.id.layout) {
            this.showPostsLayoutPreferences();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        this.binding.feedRecyclerView.refresh();
        this.fetchStories();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.fragmentActivity.setToolbar(this.binding.toolbar, this);
    }

    @Override
    public void onStop() {
        super.onStop();
        this.fragmentActivity.resetToolbar(this);
    }

    private void setupFeed() {
        this.binding.feedRecyclerView.setViewModelStoreOwner(this)
                                .setLifeCycleOwner(this)
                                .setPostFetchService(new FeedPostFetchService())
                                .setLayoutPreferences(this.layoutPreferences)
                                .addFetchStatusChangeListener(fetching -> this.updateSwipeRefreshState())
                                .setFeedItemCallback(this.feedItemCallback)
                                .setSelectionModeCallback(this.selectionModeCallback)
                                .init();
        // binding.feedRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
        //     @Override
        //     public void onScrolled(@NonNull final RecyclerView recyclerView, final int dx, final int dy) {
        //         super.onScrolled(recyclerView, dx, dy);
        //         final boolean canScrollVertically = recyclerView.canScrollVertically(-1);
        //         final MotionScene.Transition transition = root.getTransition(R.id.transition);
        //         if (transition != null) {
        //             transition.setEnable(!canScrollVertically);
        //         }
        //     }
        // });
        // if (shouldAutoPlay) {
        //     videoAwareRecyclerScroller = new VideoAwareRecyclerScroller();
        //     binding.feedRecyclerView.addOnScrollListener(videoAwareRecyclerScroller);
        // }
    }

    private void updateSwipeRefreshState() {
        AppExecutors.INSTANCE.getMainThread().execute(() -> this.binding.feedSwipeRefreshLayout
                .setRefreshing(this.binding.feedRecyclerView.isFetching() || this.storiesFetching)
        );
    }

    private void setupFeedStories() {
        if (this.storyListMenu != null) this.storyListMenu.setVisible(false);
        this.feedStoriesViewModel = new ViewModelProvider(this.fragmentActivity).get(FeedStoriesViewModel.class);
        Context context = this.getContext();
        if (context == null) return;
        RecyclerView storiesRecyclerView = this.binding.header;
        storiesRecyclerView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false));
        storiesRecyclerView.setAdapter(this.feedStoriesAdapter);
        this.feedStoriesViewModel.getList().observe(this.fragmentActivity, this.feedStoriesAdapter::submitList);
        this.fetchStories();
    }

    private void fetchStories() {
        if (this.storiesFetching) return;
        // final String cookie = settingsHelper.getString(Constants.COOKIE);
        this.storiesFetching = true;
        this.updateSwipeRefreshState();
        this.storiesRepository.getFeedStories(
                CoroutineUtilsKt.getContinuation((feedStoryModels, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                    if (throwable != null) {
                        Log.e(FeedFragment.TAG, "failed", throwable);
                        this.storiesFetching = false;
                        this.updateSwipeRefreshState();
                        return;
                    }
                    this.storiesFetching = false;
                    //noinspection unchecked
                    if (Utils.settingsHelper.getBoolean(PreferenceKeys.HIDE_MUTED_REELS)) {
                        this.feedStoriesViewModel.getList().postValue(feedStoryModels
                                .stream()
                                .filter(s -> s.getMuted() != true)
                                .collect(Collectors.toList()));
                    }
                    this.feedStoriesViewModel.getList().postValue((List<Story>) feedStoryModels);
                    if (this.storyListMenu != null) this.storyListMenu.setVisible(true);
                    this.updateSwipeRefreshState();
                }), Dispatchers.getIO())
        );
    }

    private void showPostsLayoutPreferences() {
        PostsLayoutPreferencesDialogFragment fragment = new PostsLayoutPreferencesDialogFragment(
                Constants.PREF_POSTS_LAYOUT,
                preferences -> {
                    this.layoutPreferences = preferences;
                    new Handler().postDelayed(() -> this.binding.feedRecyclerView.setLayoutPreferences(preferences), 200);
                }
        );
        fragment.show(this.getChildFragmentManager(), "posts_layout_preferences");
    }

    public void scrollToTop() {
        if (this.binding != null) {
            this.binding.feedRecyclerView.smoothScrollToPosition(0);
            // binding.storiesContainer.setExpanded(true);
        }
    }

    private boolean isSafeToNavigate(NavController navController) {
        return navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == R.id.feedFragment;
    }
}
