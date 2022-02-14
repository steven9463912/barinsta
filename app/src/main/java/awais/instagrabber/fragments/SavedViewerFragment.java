package awais.instagrabber.fragments;

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
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.common.collect.ImmutableList;

import java.util.Set;

import awais.instagrabber.R;
import awais.instagrabber.adapters.FeedAdapterV2;
import awais.instagrabber.asyncs.SavedPostFetchService;
import awais.instagrabber.customviews.PrimaryActionModeCallback;
import awais.instagrabber.databinding.FragmentSavedBinding;
import awais.instagrabber.dialogs.PostsLayoutPreferencesDialogFragment;
import awais.instagrabber.fragments.main.ProfileFragmentDirections;
import awais.instagrabber.models.PostsLayoutPreferences;
import awais.instagrabber.models.enums.PostItemType;
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

public final class SavedViewerFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = SavedViewerFragment.class.getSimpleName();

    private FragmentSavedBinding binding;
    private String username;
    private long profileId;
    private ActionMode actionMode;
    private SwipeRefreshLayout root;
    private AppCompatActivity fragmentActivity;
    private boolean isLoggedIn, shouldRefresh = true;
    private PostItemType type;
    private Set<Media> selectedFeedModels;
    private PostsLayoutPreferences layoutPreferences;

    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            SavedViewerFragment.this.binding.posts.endSelection();
        }
    };
    private final PrimaryActionModeCallback multiSelectAction = new PrimaryActionModeCallback(
            R.menu.multi_select_download_menu,
            new PrimaryActionModeCallback.CallbacksHelper() {
                @Override
                public void onDestroy(ActionMode mode) {
                    SavedViewerFragment.this.binding.posts.endSelection();
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    if (item.getItemId() == R.id.action_download) {
                        if (selectedFeedModels == null) return false;
                        Context context = SavedViewerFragment.this.getContext();
                        if (context == null) return false;
                        DownloadUtils.download(context, ImmutableList.copyOf(selectedFeedModels));
                        SavedViewerFragment.this.binding.posts.endSelection();
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
                NavHostFragment.findNavController(SavedViewerFragment.this).navigate(commentsAction);
            } catch (final Exception e) {
                Log.e(SavedViewerFragment.TAG, "onCommentsClick: ", e);
            }
        }

        @Override
        public void onDownloadClick(Media feedModel, int childPosition, View popupLocation) {
            Context context = SavedViewerFragment.this.getContext();
            if (context == null) return;
            DownloadUtils.showDownloadDialog(context, feedModel, childPosition, popupLocation);
        }

        @Override
        public void onHashtagClick(String hashtag) {
            try {
                NavDirections action = ProfileFragmentDirections.actionToHashtag(hashtag);
                NavHostFragment.findNavController(SavedViewerFragment.this).navigate(action);
            } catch (final Exception e) {
                Log.e(SavedViewerFragment.TAG, "onHashtagClick: ", e);
            }
        }

        @Override
        public void onLocationClick(Media feedModel) {
            Location location = feedModel.getLocation();
            if (location == null) return;
            try {
                NavDirections action = ProfileFragmentDirections.actionToLocation(location.getPk());
                NavHostFragment.findNavController(SavedViewerFragment.this).navigate(action);
            } catch (final Exception e) {
                Log.e(SavedViewerFragment.TAG, "onLocationClick: ", e);
            }
        }

        @Override
        public void onMentionClick(String mention) {
            SavedViewerFragment.this.navigateToProfile(mention.trim());
        }

        @Override
        public void onNameClick(Media feedModel) {
            SavedViewerFragment.this.navigateToProfile("@" + feedModel.getUser().getUsername());
        }

        @Override
        public void onProfilePicClick(Media feedModel) {
            User user = feedModel.getUser();
            if (user == null) return;
            SavedViewerFragment.this.navigateToProfile("@" + user.getUsername());
        }

        @Override
        public void onURLClick(String url) {
            Utils.openURL(SavedViewerFragment.this.getContext(), url);
        }

        @Override
        public void onEmailClick(String emailId) {
            Utils.openEmailAddress(SavedViewerFragment.this.getContext(), emailId);
        }

        private void openPostDialog(Media feedModel, int position) {
            try {
                NavDirections action = SavedViewerFragmentDirections.actionToPost(feedModel, position);
                NavHostFragment.findNavController(SavedViewerFragment.this).navigate(action);
            } catch (final Exception e) {
                Log.e(SavedViewerFragment.TAG, "openPostDialog: ", e);
            }
        }
    };
    private final FeedAdapterV2.SelectionModeCallback selectionModeCallback = new FeedAdapterV2.SelectionModeCallback() {

        @Override
        public void onSelectionStart() {
            if (!SavedViewerFragment.this.onBackPressedCallback.isEnabled()) {
                OnBackPressedDispatcher onBackPressedDispatcher = SavedViewerFragment.this.fragmentActivity.getOnBackPressedDispatcher();
                SavedViewerFragment.this.onBackPressedCallback.setEnabled(true);
                onBackPressedDispatcher.addCallback(SavedViewerFragment.this.getViewLifecycleOwner(), SavedViewerFragment.this.onBackPressedCallback);
            }
            if (SavedViewerFragment.this.actionMode == null) {
                SavedViewerFragment.this.actionMode = SavedViewerFragment.this.fragmentActivity.startActionMode(SavedViewerFragment.this.multiSelectAction);
            }
        }

        @Override
        public void onSelectionChange(Set<Media> selectedFeedModels) {
            String title = SavedViewerFragment.this.getString(R.string.number_selected, selectedFeedModels.size());
            if (SavedViewerFragment.this.actionMode != null) {
                SavedViewerFragment.this.actionMode.setTitle(title);
            }
            SavedViewerFragment.this.selectedFeedModels = selectedFeedModels;
        }

        @Override
        public void onSelectionEnd() {
            if (SavedViewerFragment.this.onBackPressedCallback.isEnabled()) {
                SavedViewerFragment.this.onBackPressedCallback.setEnabled(false);
                SavedViewerFragment.this.onBackPressedCallback.remove();
            }
            if (SavedViewerFragment.this.actionMode != null) {
                SavedViewerFragment.this.actionMode.finish();
                SavedViewerFragment.this.actionMode = null;
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.fragmentActivity = (AppCompatActivity) this.getActivity();
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
        this.binding = FragmentSavedBinding.inflate(this.getLayoutInflater(), container, false);
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
    public void onResume() {
        super.onResume();
        this.setTitle();
    }

    @Override
    public void onRefresh() {
        this.binding.posts.refresh();
    }

    private void init() {
        Bundle arguments = this.getArguments();
        if (arguments == null) return;
        SavedViewerFragmentArgs fragmentArgs = SavedViewerFragmentArgs.fromBundle(arguments);
        this.username = fragmentArgs.getUsername();
        this.profileId = fragmentArgs.getProfileId();
        this.type = fragmentArgs.getType();
        this.layoutPreferences = Utils.getPostsLayoutPreferences(this.getPostsLayoutPreferenceKey());
        this.setupPosts();
    }

    private void setupPosts() {
        this.binding.posts.setViewModelStoreOwner(this)
                     .setLifeCycleOwner(this)
                     .setPostFetchService(new SavedPostFetchService(this.profileId, this.type, this.isLoggedIn, null))
                     .setLayoutPreferences(this.layoutPreferences)
                     .addFetchStatusChangeListener(fetching -> this.updateSwipeRefreshState())
                     .setFeedItemCallback(this.feedItemCallback)
                     .setSelectionModeCallback(this.selectionModeCallback)
                     .init();
        this.binding.swipeRefreshLayout.setRefreshing(true);
    }

    @NonNull
    private String getPostsLayoutPreferenceKey() {
        switch (this.type) {
            case LIKED:
                return Constants.PREF_LIKED_POSTS_LAYOUT;
            case TAGGED:
                return Constants.PREF_TAGGED_POSTS_LAYOUT;
            case SAVED:
            default:
                return Constants.PREF_SAVED_POSTS_LAYOUT;
        }
    }

    private void setTitle() {
        ActionBar actionBar = this.fragmentActivity.getSupportActionBar();
        if (actionBar == null) return;
        int titleRes;
        switch (this.type) {
            case LIKED:
                titleRes = R.string.liked;
                break;
            case TAGGED:
                titleRes = R.string.tagged;
                break;
            default:
            case SAVED:
                titleRes = R.string.saved;
                break;
        }
        actionBar.setTitle(titleRes);
        actionBar.setSubtitle(this.username);
    }

    private void updateSwipeRefreshState() {
        AppExecutors.INSTANCE.getMainThread().execute(() ->
                this.binding.swipeRefreshLayout.setRefreshing(this.binding.posts.isFetching())
        );
    }

    private void navigateToProfile(String username) {
        try {
            NavDirections action = SavedViewerFragmentDirections.actionToProfile().setUsername(username);
            NavHostFragment.findNavController(this).navigate(action);
        } catch (final Exception e) {
            Log.e(SavedViewerFragment.TAG, "navigateToProfile: ", e);
        }
    }

    private void showPostsLayoutPreferences() {
        PostsLayoutPreferencesDialogFragment fragment = new PostsLayoutPreferencesDialogFragment(
                this.getPostsLayoutPreferenceKey(),
                preferences -> {
                    this.layoutPreferences = preferences;
                    new Handler().postDelayed(() -> this.binding.posts.setLayoutPreferences(preferences), 200);
                });
        fragment.show(this.getChildFragmentManager(), "posts_layout_preferences");
    }
}