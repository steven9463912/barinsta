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
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.common.collect.ImmutableList;

import java.util.Set;

import awais.instagrabber.R;
import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.adapters.FeedAdapterV2;
import awais.instagrabber.asyncs.DiscoverPostFetchService;
import awais.instagrabber.customviews.PrimaryActionModeCallback;
import awais.instagrabber.databinding.FragmentDiscoverBinding;
import awais.instagrabber.dialogs.PostsLayoutPreferencesDialogFragment;
import awais.instagrabber.models.PostsLayoutPreferences;
import awais.instagrabber.repositories.responses.Location;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class DiscoverFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "DiscoverFragment";

    private MainActivity fragmentActivity;
    private SwipeRefreshLayout root;
    private FragmentDiscoverBinding binding;
    private ActionMode actionMode;
    private boolean isLoggedIn, shouldRefresh = true;
    private String keyword;
    private Set<Media> selectedFeedModels;
    private PostsLayoutPreferences layoutPreferences = Utils.getPostsLayoutPreferences(Constants.PREF_TOPIC_POSTS_LAYOUT);

    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            DiscoverFragment.this.binding.posts.endSelection();
        }
    };
    private final PrimaryActionModeCallback multiSelectAction = new PrimaryActionModeCallback(
            R.menu.multi_select_download_menu,
            new PrimaryActionModeCallback.CallbacksHelper() {
                @Override
                public void onDestroy(ActionMode mode) {
                    DiscoverFragment.this.binding.posts.endSelection();
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    if (item.getItemId() == R.id.action_download) {
                        if (DiscoverFragment.this.selectedFeedModels == null) return false;
                        Context context = DiscoverFragment.this.getContext();
                        if (context == null) return false;
                        DownloadUtils.download(context, ImmutableList.copyOf(selectedFeedModels));
                        DiscoverFragment.this.binding.posts.endSelection();
                    }
                    return false;
                }
            });
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
            User user = feedModel.getUser();
            if (user == null) return;
            try {
                NavDirections commentsAction = ProfileFragmentDirections.actionToComments(
                        feedModel.getCode(),
                        feedModel.getPk(),
                        user.getPk()
                );
                NavHostFragment.findNavController(DiscoverFragment.this).navigate(commentsAction);
            } catch (final Exception e) {
                Log.e(DiscoverFragment.TAG, "onCommentsClick: ", e);
            }
        }

        @Override
        public void onDownloadClick(Media feedModel, int childPosition, View popupLocation) {
            Context context = DiscoverFragment.this.getContext();
            if (context == null) return;
            DownloadUtils.showDownloadDialog(context, feedModel, childPosition, popupLocation);
        }

        @Override
        public void onHashtagClick(String hashtag) {
            try {
                NavDirections action = ProfileFragmentDirections.actionToHashtag(hashtag);
                NavHostFragment.findNavController(DiscoverFragment.this).navigate(action);
            } catch (final Exception e) {
                Log.e(DiscoverFragment.TAG, "onHashtagClick: ", e);
            }
        }

        @Override
        public void onLocationClick(Media feedModel) {
            Location location = feedModel.getLocation();
            if (location == null) return;
            try {
                NavDirections action = ProfileFragmentDirections.actionToLocation(location.getPk());
                NavHostFragment.findNavController(DiscoverFragment.this).navigate(action);
            } catch (final Exception e) {
                Log.e(DiscoverFragment.TAG, "onLocationClick: ", e);
            }
        }

        @Override
        public void onMentionClick(String mention) {
            DiscoverFragment.this.navigateToProfile(mention.trim());
        }

        @Override
        public void onNameClick(Media feedModel) {
            DiscoverFragment.this.navigateToProfile("@" + feedModel.getUser().getUsername());
        }

        @Override
        public void onProfilePicClick(Media feedModel) {
            User user = feedModel.getUser();
            if (user == null) return;
            DiscoverFragment.this.navigateToProfile("@" + user.getUsername());
        }

        @Override
        public void onURLClick(String url) {
            Utils.openURL(DiscoverFragment.this.getContext(), url);
        }

        @Override
        public void onEmailClick(String emailId) {
            Utils.openEmailAddress(DiscoverFragment.this.getContext(), emailId);
        }

        private void openPostDialog(Media feedModel, int position) {
            try {
                NavDirections action = DiscoverFragmentDirections.actionToPost(feedModel, position);
                NavHostFragment.findNavController(DiscoverFragment.this).navigate(action);
            } catch (final Exception e) {
                Log.e(DiscoverFragment.TAG, "openPostDialog: ", e);
            }
        }
    };
    private final FeedAdapterV2.SelectionModeCallback selectionModeCallback = new FeedAdapterV2.SelectionModeCallback() {

        @Override
        public void onSelectionStart() {
            if (!DiscoverFragment.this.onBackPressedCallback.isEnabled()) {
                OnBackPressedDispatcher onBackPressedDispatcher = DiscoverFragment.this.fragmentActivity.getOnBackPressedDispatcher();
                DiscoverFragment.this.onBackPressedCallback.setEnabled(true);
                onBackPressedDispatcher.addCallback(DiscoverFragment.this.getViewLifecycleOwner(), DiscoverFragment.this.onBackPressedCallback);
            }
            if (DiscoverFragment.this.actionMode == null) {
                DiscoverFragment.this.actionMode = DiscoverFragment.this.fragmentActivity.startActionMode(DiscoverFragment.this.multiSelectAction);
            }
        }

        @Override
        public void onSelectionChange(Set<Media> selectedFeedModels) {
            String title = DiscoverFragment.this.getString(R.string.number_selected, selectedFeedModels.size());
            if (DiscoverFragment.this.actionMode != null) {
                DiscoverFragment.this.actionMode.setTitle(title);
            }
            DiscoverFragment.this.selectedFeedModels = selectedFeedModels;
        }

        @Override
        public void onSelectionEnd() {
            if (DiscoverFragment.this.onBackPressedCallback.isEnabled()) {
                DiscoverFragment.this.onBackPressedCallback.setEnabled(false);
                DiscoverFragment.this.onBackPressedCallback.remove();
            }
            if (DiscoverFragment.this.actionMode != null) {
                DiscoverFragment.this.actionMode.finish();
                DiscoverFragment.this.actionMode = null;
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.fragmentActivity = (MainActivity) this.getActivity();
        this.setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        String cookie = settingsHelper.getString(Constants.COOKIE);
        this.isLoggedIn = !TextUtils.isEmpty(cookie) && CookieUtils.getUserIdFromCookie(cookie) > 0;
        if (this.root != null) {
            this.shouldRefresh = false;
            return this.root;
        }
        this.binding = FragmentDiscoverBinding.inflate(this.getLayoutInflater(), container, false);
        this.root = this.binding.getRoot();
        return this.root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (!this.shouldRefresh) return;
        this.binding.swipeRefreshLayout.setOnRefreshListener(this);
        this.init();
        this.shouldRefresh = false;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.saved_viewer_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.layout) {
            this.showPostsLayoutPreferences();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        this.binding.posts.refresh();
    }

    private void init() {
        Bundle arguments = this.getArguments();
        if (arguments == null) return;
        DiscoverFragmentArgs fragmentArgs = DiscoverFragmentArgs.fromBundle(arguments);
        this.keyword = fragmentArgs.getKeyword();
        this.setupPosts();
    }

    private void setupPosts() {
        this.binding.posts.setViewModelStoreOwner(this)
                .setLifeCycleOwner(this)
                .setPostFetchService(new DiscoverPostFetchService())
                .setLayoutPreferences(this.layoutPreferences)
                .addFetchStatusChangeListener(fetching -> this.updateSwipeRefreshState())
                .setFeedItemCallback(this.feedItemCallback)
                .setSelectionModeCallback(this.selectionModeCallback)
                .init();
        this.binding.swipeRefreshLayout.setRefreshing(true);
    }

    private void updateSwipeRefreshState() {
        AppExecutors.INSTANCE.getMainThread().execute(() ->
                this.binding.swipeRefreshLayout.setRefreshing(this.binding.posts.isFetching())
        );
    }

    private void navigateToProfile(String username) {
        try {
            NavDirections action = DiscoverFragmentDirections.actionToProfile().setUsername(username);
            NavHostFragment.findNavController(this).navigate(action);
        } catch (final Exception e) {
            Log.e(DiscoverFragment.TAG, "navigateToProfile: ", e);
        }
    }

    private void showPostsLayoutPreferences() {
        PostsLayoutPreferencesDialogFragment fragment = new PostsLayoutPreferencesDialogFragment(
                Constants.PREF_TOPIC_POSTS_LAYOUT,
                preferences -> {
                    this.layoutPreferences = preferences;
                    new Handler().postDelayed(() -> this.binding.posts.setLayoutPreferences(preferences), 200);
                });
        fragment.show(this.getChildFragmentManager(), "posts_layout_preferences");
    }
}