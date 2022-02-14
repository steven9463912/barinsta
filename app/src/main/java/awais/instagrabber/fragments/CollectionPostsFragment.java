package awais.instagrabber.fragments;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.transition.ChangeBounds;
import androidx.transition.TransitionInflater;
import androidx.transition.TransitionSet;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.image.ImageInfo;
import com.google.common.collect.ImmutableList;

import java.util.Set;

import awais.instagrabber.R;
import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.adapters.FeedAdapterV2;
import awais.instagrabber.asyncs.SavedPostFetchService;
import awais.instagrabber.customviews.PrimaryActionModeCallback;
import awais.instagrabber.databinding.FragmentCollectionPostsBinding;
import awais.instagrabber.dialogs.PostsLayoutPreferencesDialogFragment;
import awais.instagrabber.models.PostsLayoutPreferences;
import awais.instagrabber.models.enums.PostItemType;
import awais.instagrabber.repositories.responses.Location;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.saved.SavedCollection;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.webservices.CollectionService;
import awais.instagrabber.webservices.ServiceCallback;

public class CollectionPostsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "CollectionPostsFragment";

    private MainActivity fragmentActivity;
    private FragmentCollectionPostsBinding binding;
    private CoordinatorLayout root;
    private boolean shouldRefresh = true;
    private SavedCollection savedCollection;
    private ActionMode actionMode;
    private Set<Media> selectedFeedModels;
    private CollectionService collectionService;
    private PostsLayoutPreferences layoutPreferences = Utils.getPostsLayoutPreferences(Constants.PREF_SAVED_POSTS_LAYOUT);

    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            CollectionPostsFragment.this.binding.posts.endSelection();
        }
    };
    private final PrimaryActionModeCallback multiSelectAction = new PrimaryActionModeCallback(
            R.menu.saved_collection_select_menu, new PrimaryActionModeCallback.CallbacksHelper() {
        @Override
        public void onDestroy(ActionMode mode) {
            CollectionPostsFragment.this.binding.posts.endSelection();
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode,
                                           MenuItem item) {
            if (item.getItemId() == R.id.action_download) {
                if (selectedFeedModels == null) return false;
                Context context = CollectionPostsFragment.this.getContext();
                if (context == null) return false;
                DownloadUtils.download(context, ImmutableList.copyOf(selectedFeedModels));
                CollectionPostsFragment.this.binding.posts.endSelection();
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
                NavDirections commentsAction = CollectionPostsFragmentDirections.actionToComments(
                        feedModel.getCode(),
                        feedModel.getPk(),
                        user.getPk()
                );
                NavHostFragment.findNavController(CollectionPostsFragment.this).navigate(commentsAction);
            } catch (final Exception e) {
                Log.e(CollectionPostsFragment.TAG, "onCommentsClick: ", e);
            }
        }

        @Override
        public void onDownloadClick(Media feedModel, int childPosition, View popupLocation) {
            Context context = CollectionPostsFragment.this.getContext();
            if (context == null) return;
            DownloadUtils.showDownloadDialog(context, feedModel, childPosition, popupLocation);
        }

        @Override
        public void onHashtagClick(String hashtag) {
            try {
                NavDirections action = CollectionPostsFragmentDirections.actionToHashtag(hashtag);
                NavHostFragment.findNavController(CollectionPostsFragment.this).navigate(action);
            } catch (final Exception e) {
                Log.e(CollectionPostsFragment.TAG, "onHashtagClick: ", e);
            }
        }

        @Override
        public void onLocationClick(Media feedModel) {
            Location location = feedModel.getLocation();
            if (location == null) return;
            try {
                NavDirections action = CollectionPostsFragmentDirections.actionToLocation(location.getPk());
                NavHostFragment.findNavController(CollectionPostsFragment.this).navigate(action);
            } catch (final Exception e) {
                Log.e(CollectionPostsFragment.TAG, "onLocationClick: ", e);
            }
        }

        @Override
        public void onMentionClick(String mention) {
            CollectionPostsFragment.this.navigateToProfile(mention.trim());
        }

        @Override
        public void onNameClick(Media feedModel) {
            User user = feedModel.getUser();
            if (user == null) return;
            CollectionPostsFragment.this.navigateToProfile("@" + user.getUsername());
        }

        @Override
        public void onProfilePicClick(Media feedModel) {
            User user = feedModel.getUser();
            if (user == null) return;
            CollectionPostsFragment.this.navigateToProfile("@" + user.getUsername());
        }

        @Override
        public void onURLClick(String url) {
            Utils.openURL(CollectionPostsFragment.this.getContext(), url);
        }

        @Override
        public void onEmailClick(String emailId) {
            Utils.openEmailAddress(CollectionPostsFragment.this.getContext(), emailId);
        }

        private void openPostDialog(Media feedModel, int position) {
            try {
                NavDirections action = CollectionPostsFragmentDirections.actionToPost(feedModel, position);
                NavHostFragment.findNavController(CollectionPostsFragment.this).navigate(action);
            } catch (final Exception e) {
                Log.e(CollectionPostsFragment.TAG, "openPostDialog: ", e);
            }
        }
    };
    private final FeedAdapterV2.SelectionModeCallback selectionModeCallback = new FeedAdapterV2.SelectionModeCallback() {

        @Override
        public void onSelectionStart() {
            if (!CollectionPostsFragment.this.onBackPressedCallback.isEnabled()) {
                OnBackPressedDispatcher onBackPressedDispatcher = CollectionPostsFragment.this.fragmentActivity.getOnBackPressedDispatcher();
                CollectionPostsFragment.this.onBackPressedCallback.setEnabled(true);
                onBackPressedDispatcher.addCallback(CollectionPostsFragment.this.getViewLifecycleOwner(), CollectionPostsFragment.this.onBackPressedCallback);
            }
            if (CollectionPostsFragment.this.actionMode == null) {
                CollectionPostsFragment.this.actionMode = CollectionPostsFragment.this.fragmentActivity.startActionMode(CollectionPostsFragment.this.multiSelectAction);
            }
        }

        @Override
        public void onSelectionChange(Set<Media> selectedFeedModels) {
            String title = CollectionPostsFragment.this.getString(R.string.number_selected, selectedFeedModels.size());
            if (CollectionPostsFragment.this.actionMode != null) {
                CollectionPostsFragment.this.actionMode.setTitle(title);
            }
            CollectionPostsFragment.this.selectedFeedModels = selectedFeedModels;
        }

        @Override
        public void onSelectionEnd() {
            if (CollectionPostsFragment.this.onBackPressedCallback.isEnabled()) {
                CollectionPostsFragment.this.onBackPressedCallback.setEnabled(false);
                CollectionPostsFragment.this.onBackPressedCallback.remove();
            }
            if (CollectionPostsFragment.this.actionMode != null) {
                CollectionPostsFragment.this.actionMode.finish();
                CollectionPostsFragment.this.actionMode = null;
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.fragmentActivity = (MainActivity) this.requireActivity();
        TransitionSet transitionSet = new TransitionSet();
        Context context = this.getContext();
        if (context == null) return;
        transitionSet.addTransition(new ChangeBounds())
                     .addTransition(TransitionInflater.from(context).inflateTransition(android.R.transition.move))
                     .setDuration(200);
        this.setSharedElementEnterTransition(transitionSet);
        this.postponeEnterTransition();
        this.setHasOptionsMenu(true);
        String cookie = Utils.settingsHelper.getString(Constants.COOKIE);
        long userId = CookieUtils.getUserIdFromCookie(cookie);
        String deviceUuid = Utils.settingsHelper.getString(Constants.DEVICE_UUID);
        String csrfToken = CookieUtils.getCsrfTokenFromCookie(cookie);
        this.collectionService = CollectionService.getInstance(deviceUuid, csrfToken, userId);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (this.root != null) {
            this.shouldRefresh = false;
            return this.root;
        }
        this.binding = FragmentCollectionPostsBinding.inflate(inflater, container, false);
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
        inflater.inflate(R.menu.collection_posts_menu, menu);
        MenuItem deleteMenu = menu.findItem(R.id.delete);
        if (deleteMenu != null)
            deleteMenu.setVisible(this.savedCollection.getCollectionType().equals("MEDIA"));
        MenuItem editMenu = menu.findItem(R.id.edit);
        if (editMenu != null)
            editMenu.setVisible(this.savedCollection.getCollectionType().equals("MEDIA"));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.layout) {
            this.showPostsLayoutPreferences();
            return true;
        } else if (item.getItemId() == R.id.delete) {
            Context context = this.getContext();
            if (context == null) return false;
            new AlertDialog.Builder(context)
                    .setTitle(R.string.are_you_sure)
                    .setMessage(R.string.delete_collection_note)
                    .setPositiveButton(R.string.confirm, (d, w) -> this.collectionService.deleteCollection(
                            this.savedCollection.getCollectionId(),
                            new ServiceCallback<String>() {
                                @Override
                                public void onSuccess(String result) {
                                    SavedCollectionsFragment.pleaseRefresh = true;
                                    NavHostFragment.findNavController(CollectionPostsFragment.this).navigateUp();
                                }

                                @Override
                                public void onFailure(Throwable t) {
                                    Log.e(CollectionPostsFragment.TAG, "Error deleting collection", t);
                                    try {
                                        Context context = CollectionPostsFragment.this.getContext();
                                        if (context == null) return;
                                        Toast.makeText(context, t.getMessage(), Toast.LENGTH_SHORT).show();
                                    } catch (Throwable ignored) {}
                                }
                            }))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } else if (item.getItemId() == R.id.edit) {
            Context context = this.getContext();
            if (context == null) return false;
            EditText input = new EditText(context);
            new AlertDialog.Builder(context)
                    .setTitle(R.string.edit_collection)
                    .setView(input)
                    .setPositiveButton(R.string.confirm, (d, w) -> this.collectionService.editCollectionName(
                            this.savedCollection.getCollectionId(),
                            input.getText().toString(),
                            new ServiceCallback<String>() {
                                @Override
                                public void onSuccess(String result) {
                                    CollectionPostsFragment.this.binding.collapsingToolbarLayout.setTitle(input.getText().toString());
                                    SavedCollectionsFragment.pleaseRefresh = true;
                                }

                                @Override
                                public void onFailure(Throwable t) {
                                    Log.e(CollectionPostsFragment.TAG, "Error editing collection", t);
                                    try {
                                        Context context = CollectionPostsFragment.this.getContext();
                                        if (context == null) return;
                                        Toast.makeText(context, t.getMessage(), Toast.LENGTH_SHORT).show();
                                    } catch (Throwable ignored) {}
                                }
                            }))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.fragmentActivity.setToolbar(this.binding.toolbar, this);
    }

    @Override
    public void onRefresh() {
        this.binding.posts.refresh();
    }

    @Override
    public void onStop() {
        super.onStop();
        this.fragmentActivity.resetToolbar(this);
    }

    private void init() {
        if (this.getArguments() == null) return;
        CollectionPostsFragmentArgs fragmentArgs = CollectionPostsFragmentArgs.fromBundle(this.getArguments());
        this.savedCollection = fragmentArgs.getSavedCollection();
        this.setupToolbar(fragmentArgs.getTitleColor(), fragmentArgs.getBackgroundColor());
        this.setupPosts();
    }

    private void setupToolbar(int titleColor, int backgroundColor) {
        if (this.savedCollection == null) {
            return;
        }
        this.binding.cover.setTransitionName("collection-" + this.savedCollection.getCollectionId());
        this.fragmentActivity.setToolbar(this.binding.toolbar, this);
        this.binding.collapsingToolbarLayout.setTitle(this.savedCollection.getCollectionName());
        int collapsedTitleTextColor = ColorUtils.setAlphaComponent(titleColor, 0xFF);
        int expandedTitleTextColor = ColorUtils.setAlphaComponent(titleColor, 0x99);
        this.binding.collapsingToolbarLayout.setExpandedTitleColor(expandedTitleTextColor);
        this.binding.collapsingToolbarLayout.setCollapsedTitleTextColor(collapsedTitleTextColor);
        this.binding.collapsingToolbarLayout.setContentScrimColor(backgroundColor);
        Drawable navigationIcon = this.binding.toolbar.getNavigationIcon();
        Drawable overflowIcon = this.binding.toolbar.getOverflowIcon();
        if (navigationIcon != null && overflowIcon != null) {
            Drawable navDrawable = navigationIcon.mutate();
            Drawable overflowDrawable = overflowIcon.mutate();
            navDrawable.setAlpha(0xFF);
            overflowDrawable.setAlpha(0xFF);
            ArgbEvaluator argbEvaluator = new ArgbEvaluator();
            this.binding.appBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
                int totalScrollRange = appBarLayout.getTotalScrollRange();
                float current = totalScrollRange + verticalOffset;
                float fraction = current / totalScrollRange;
                int tempColor = (int) argbEvaluator.evaluate(fraction, collapsedTitleTextColor, expandedTitleTextColor);
                navDrawable.setColorFilter(tempColor, PorterDuff.Mode.SRC_ATOP);
                overflowDrawable.setColorFilter(tempColor, PorterDuff.Mode.SRC_ATOP);

            });
        }
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.TRANSPARENT, backgroundColor});
        this.binding.background.setBackground(gd);
        this.setupCover();
    }

    private void setupCover() {
        Media coverMedia = this.savedCollection.getCoverMediaList() == null
                ? this.savedCollection.getCoverMedia()
                : this.savedCollection.getCoverMediaList().get(0);
        String coverUrl = ResponseBodyUtils.getImageUrl(coverMedia);
        DraweeController controller = Fresco
                .newDraweeControllerBuilder()
                .setOldController(this.binding.cover.getController())
                .setUri(coverUrl)
                .setControllerListener(new BaseControllerListener<ImageInfo>() {

                    @Override
                    public void onFailure(String id, Throwable throwable) {
                        super.onFailure(id, throwable);
                        CollectionPostsFragment.this.startPostponedEnterTransition();
                    }

                    @Override
                    public void onFinalImageSet(String id,
                                                @Nullable ImageInfo imageInfo,
                                                @Nullable Animatable animatable) {
                        CollectionPostsFragment.this.startPostponedEnterTransition();
                    }
                })
                .build();
        this.binding.cover.setController(controller);
    }

    private void setupPosts() {
        this.binding.posts.setViewModelStoreOwner(this)
                     .setLifeCycleOwner(this)
                     .setPostFetchService(new SavedPostFetchService(0, PostItemType.COLLECTION, true, this.savedCollection.getCollectionId()))
                     .setLayoutPreferences(this.layoutPreferences)
                     .addFetchStatusChangeListener(fetching -> this.updateSwipeRefreshState())
                     .setFeedItemCallback(this.feedItemCallback)
                     .setSelectionModeCallback(this.selectionModeCallback)
                     .init();
    }

    private void updateSwipeRefreshState() {
        AppExecutors.INSTANCE.getMainThread().execute(() ->
                this.binding.swipeRefreshLayout.setRefreshing(this.binding.posts.isFetching())
        );
    }

    private void navigateToProfile(String username) {
        try {
            NavDirections action = CollectionPostsFragmentDirections.actionToProfile().setUsername(username);
            NavHostFragment.findNavController(this).navigate(action);
        } catch (final Exception e) {
            Log.e(CollectionPostsFragment.TAG, "navigateToProfile: ", e);
        }
    }

    private void showPostsLayoutPreferences() {
        PostsLayoutPreferencesDialogFragment fragment = new PostsLayoutPreferencesDialogFragment(
                Constants.PREF_SAVED_POSTS_LAYOUT,
                preferences -> {
                    this.layoutPreferences = preferences;
                    new Handler().postDelayed(() -> this.binding.posts.setLayoutPreferences(preferences), 200);
                });
        fragment.show(this.getChildFragmentManager(), "posts_layout_preferences");
    }
}
