package awais.instagrabber.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.collect.ImmutableList;

import java.time.LocalDateTime;
import java.util.Set;

import awais.instagrabber.R;
import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.adapters.FeedAdapterV2;
import awais.instagrabber.asyncs.HashtagPostFetchService;
import awais.instagrabber.customviews.PrimaryActionModeCallback;
import awais.instagrabber.databinding.FragmentHashtagBinding;
import awais.instagrabber.databinding.LayoutHashtagDetailsBinding;
import awais.instagrabber.db.entities.Favorite;
import awais.instagrabber.db.repositories.FavoriteRepository;
import awais.instagrabber.dialogs.PostsLayoutPreferencesDialogFragment;
import awais.instagrabber.models.PostsLayoutPreferences;
import awais.instagrabber.models.enums.FavoriteType;
import awais.instagrabber.repositories.responses.Hashtag;
import awais.instagrabber.repositories.responses.Location;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.webservices.GraphQLRepository;
import awais.instagrabber.webservices.HashtagRepository;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.Dispatchers;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class HashTagFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "HashTagFragment";

    private MainActivity fragmentActivity;
    private FragmentHashtagBinding binding;
    private CoordinatorLayout root;
    private boolean shouldRefresh = true;
    private boolean opening;
    private String hashtag;
    private Hashtag hashtagModel;
    private ActionMode actionMode;
    //    private StoriesRepository storiesRepository;
    private boolean isLoggedIn;
    private HashtagRepository hashtagRepository;
    private GraphQLRepository graphQLRepository;
    //    private boolean storiesFetching;
    private Set<Media> selectedFeedModels;
    private PostsLayoutPreferences layoutPreferences = Utils.getPostsLayoutPreferences(Constants.PREF_HASHTAG_POSTS_LAYOUT);
    private LayoutHashtagDetailsBinding hashtagDetailsBinding;

    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            HashTagFragment.this.binding.posts.endSelection();
        }
    };
    private final PrimaryActionModeCallback multiSelectAction = new PrimaryActionModeCallback(
            R.menu.multi_select_download_menu,
            new PrimaryActionModeCallback.CallbacksHelper() {
                @Override
                public void onDestroy(ActionMode mode) {
                    HashTagFragment.this.binding.posts.endSelection();
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    if (item.getItemId() == R.id.action_download) {
                        if (selectedFeedModels == null) return false;
                        Context context = HashTagFragment.this.getContext();
                        if (context == null) return false;
                        DownloadUtils.download(context, ImmutableList.copyOf(selectedFeedModels));
                        HashTagFragment.this.binding.posts.endSelection();
                        return true;
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
                NavDirections commentsAction = HashTagFragmentDirections.actionToComments(
                        feedModel.getCode(),
                        feedModel.getCode(),
                        user.getPk()
                );
                NavHostFragment.findNavController(HashTagFragment.this).navigate(commentsAction);
            } catch (final Exception e) {
                Log.e(HashTagFragment.TAG, "onCommentsClick: ", e);
            }
        }

        @Override
        public void onDownloadClick(Media feedModel, int childPosition, View popupLocation) {
            Context context = HashTagFragment.this.getContext();
            if (context == null) return;
            DownloadUtils.showDownloadDialog(context, feedModel, childPosition, popupLocation);
        }

        @Override
        public void onHashtagClick(String hashtag) {
            try {
                NavDirections action = HashTagFragmentDirections.actionToHashtag(hashtag);
                NavHostFragment.findNavController(HashTagFragment.this).navigate(action);
            } catch (final Exception e) {
                Log.e(HashTagFragment.TAG, "onHashtagClick: ", e);
            }
        }

        @Override
        public void onLocationClick(Media media) {
            Location location = media.getLocation();
            if (location == null) return;
            try {
                NavDirections action = HashTagFragmentDirections.actionToLocation(location.getPk());
                NavHostFragment.findNavController(HashTagFragment.this).navigate(action);
            } catch (final Exception e) {
                Log.e(HashTagFragment.TAG, "onLocationClick: ", e);
            }
        }

        @Override
        public void onMentionClick(String mention) {
            HashTagFragment.this.navigateToProfile(mention.trim());
        }

        @Override
        public void onNameClick(Media feedModel) {
            User user = feedModel.getUser();
            if (user == null) return;
            HashTagFragment.this.navigateToProfile("@" + user.getUsername());
        }

        @Override
        public void onProfilePicClick(Media feedModel) {
            User user = feedModel.getUser();
            if (user == null) return;
            HashTagFragment.this.navigateToProfile("@" + user.getUsername());
        }

        @Override
        public void onURLClick(String url) {
            Utils.openURL(HashTagFragment.this.getContext(), url);
        }

        @Override
        public void onEmailClick(String emailId) {
            Utils.openEmailAddress(HashTagFragment.this.getContext(), emailId);
        }

        private void openPostDialog(@NonNull Media feedModel, int position) {
            if (HashTagFragment.this.opening) return;
            User user = feedModel.getUser();
            if (user == null) return;
            if (TextUtils.isEmpty(user.getUsername())) {
                // this only happens for anons
                HashTagFragment.this.opening = true;
                String code = feedModel.getCode();
                if (code == null) return;
                HashTagFragment.this.graphQLRepository.fetchPost(code, CoroutineUtilsKt.getContinuation((media, throwable) -> {
                    HashTagFragment.this.opening = false;
                    if (throwable != null) {
                        Log.e(HashTagFragment.TAG, "Error", throwable);
                        return;
                    }
                    if (media == null) return;
                    AppExecutors.INSTANCE.getMainThread().execute(() -> this.openPostDialog(media, position));
                }, Dispatchers.getIO()));
                return;
            }
            HashTagFragment.this.opening = true;
            try {
                NavDirections action = HashTagFragmentDirections.actionToPost(feedModel, position);
                NavHostFragment.findNavController(HashTagFragment.this).navigate(action);
            } catch (final Exception e) {
                Log.e(HashTagFragment.TAG, "openPostDialog: ", e);
            }
            HashTagFragment.this.opening = false;
        }
    };
    private final FeedAdapterV2.SelectionModeCallback selectionModeCallback = new FeedAdapterV2.SelectionModeCallback() {

        @Override
        public void onSelectionStart() {
            if (!HashTagFragment.this.onBackPressedCallback.isEnabled()) {
                OnBackPressedDispatcher onBackPressedDispatcher = HashTagFragment.this.fragmentActivity.getOnBackPressedDispatcher();
                HashTagFragment.this.onBackPressedCallback.setEnabled(true);
                onBackPressedDispatcher.addCallback(HashTagFragment.this.getViewLifecycleOwner(), HashTagFragment.this.onBackPressedCallback);
            }
            if (HashTagFragment.this.actionMode == null) {
                HashTagFragment.this.actionMode = HashTagFragment.this.fragmentActivity.startActionMode(HashTagFragment.this.multiSelectAction);
            }
        }

        @Override
        public void onSelectionChange(Set<Media> selectedFeedModels) {
            String title = HashTagFragment.this.getString(R.string.number_selected, selectedFeedModels.size());
            if (HashTagFragment.this.actionMode != null) {
                HashTagFragment.this.actionMode.setTitle(title);
            }
            HashTagFragment.this.selectedFeedModels = selectedFeedModels;
        }

        @Override
        public void onSelectionEnd() {
            if (HashTagFragment.this.onBackPressedCallback.isEnabled()) {
                HashTagFragment.this.onBackPressedCallback.setEnabled(false);
                HashTagFragment.this.onBackPressedCallback.remove();
            }
            if (HashTagFragment.this.actionMode != null) {
                HashTagFragment.this.actionMode.finish();
                HashTagFragment.this.actionMode = null;
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.fragmentActivity = (MainActivity) this.requireActivity();
        String cookie = settingsHelper.getString(Constants.COOKIE);
        this.isLoggedIn = !TextUtils.isEmpty(cookie) && CookieUtils.getUserIdFromCookie(cookie) > 0;
        this.hashtagRepository = this.isLoggedIn ? HashtagRepository.Companion.getInstance() : null;
        //        storiesRepository = isLoggedIn ? StoriesRepository.Companion.getInstance() : null;
        this.graphQLRepository = this.isLoggedIn ? null : GraphQLRepository.Companion.getInstance();
        this.setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (this.root != null) {
            this.shouldRefresh = false;
            return this.root;
        }
        this.binding = FragmentHashtagBinding.inflate(inflater, container, false);
        this.root = this.binding.getRoot();
        this.hashtagDetailsBinding = this.binding.header;
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
    public void onRefresh() {
        this.binding.posts.refresh();
        //        fetchStories();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.fragmentActivity.setToolbar(this.binding.toolbar, this);
        this.setTitle();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.topic_posts_menu, menu);
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
    public void onStop() {
        super.onStop();
        this.fragmentActivity.resetToolbar(this);
    }

    private void init() {
        if (this.getArguments() == null) return;
        HashTagFragmentArgs fragmentArgs = HashTagFragmentArgs.fromBundle(this.getArguments());
        this.hashtag = fragmentArgs.getHashtag();
        if (this.hashtag.charAt(0) == '#') this.hashtag = this.hashtag.substring(1);
        this.fetchHashtagModel(true);
    }

    private void fetchHashtagModel(boolean init) {
        this.binding.swipeRefreshLayout.setRefreshing(true);
        Continuation<Hashtag> cb = CoroutineUtilsKt.getContinuation((result, t) -> {
            this.hashtagModel = result;
            AppExecutors.INSTANCE.getMainThread().execute(() -> {
                this.setHashtagDetails(init);
                this.binding.swipeRefreshLayout.setRefreshing(false);
            });
        }, Dispatchers.getIO());
        if (this.isLoggedIn) this.hashtagRepository.fetch(this.hashtag, cb);
        else this.graphQLRepository.fetchTag(this.hashtag, cb);
    }

    private void setupPosts() {
        this.binding.posts.setViewModelStoreOwner(this)
                     .setLifeCycleOwner(this)
                     .setPostFetchService(new HashtagPostFetchService(this.hashtagModel, this.isLoggedIn))
                     .setLayoutPreferences(this.layoutPreferences)
                     .addFetchStatusChangeListener(fetching -> this.updateSwipeRefreshState())
                     .setFeedItemCallback(this.feedItemCallback)
                     .setSelectionModeCallback(this.selectionModeCallback)
                     .init();
        // binding.posts.addOnScrollListener(new RecyclerView.OnScrollListener() {
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
    }

    private void setHashtagDetails(boolean init) {
        if (this.hashtagModel == null) {
            try {
                Toast.makeText(this.getContext(), R.string.error_loading_hashtag, Toast.LENGTH_SHORT).show();
                this.binding.swipeRefreshLayout.setEnabled(false);
            } catch (final Exception ignored) {}
            return;
        }
        if (init) {
            this.setTitle();
            this.setupPosts();
        }
        if (this.isLoggedIn) {
            this.hashtagDetailsBinding.btnFollowTag.setVisibility(View.VISIBLE);
            this.hashtagDetailsBinding.btnFollowTag.setText(this.hashtagModel.getFollow()
                                                       ? R.string.unfollow
                                                       : R.string.follow);
            this.hashtagDetailsBinding.btnFollowTag.setChipIconResource(this.hashtagModel.getFollow()
                                                                   ? R.drawable.ic_outline_person_add_disabled_24
                                                                   : R.drawable.ic_outline_person_add_24);
            this.hashtagDetailsBinding.btnFollowTag.setOnClickListener(v -> {
                String cookie = settingsHelper.getString(Constants.COOKIE);
                String csrfToken = CookieUtils.getCsrfTokenFromCookie(cookie);
                long userId = CookieUtils.getUserIdFromCookie(cookie);
                String deviceUuid = settingsHelper.getString(Constants.DEVICE_UUID);
                if (csrfToken != null && userId != 0) {
                    this.hashtagDetailsBinding.btnFollowTag.setClickable(false);
                    this.hashtagRepository.changeFollow(
                            this.hashtagModel.getFollow() ? "unfollow" : "follow",
                            this.hashtag,
                            csrfToken,
                            userId,
                            deviceUuid,
                            CoroutineUtilsKt.getContinuation((result, t) -> {
                                this.hashtagDetailsBinding.btnFollowTag.setClickable(true);
                                if (t != null) {
                                    Log.e(HashTagFragment.TAG, "onFailure: ", t);
                                    String message = t.getMessage();
                                    Snackbar.make(
                                            this.root,
                                            message != null ? message : this.getString(R.string.downloader_unknown_error),
                                            BaseTransientBottomBar.LENGTH_LONG)
                                            .show();
                                    return;
                                }
                                if (result != true) {
                                    Log.e(HashTagFragment.TAG, "onSuccess: result is false");
                                    Snackbar.make(this.root, R.string.downloader_unknown_error, BaseTransientBottomBar.LENGTH_LONG)
                                            .show();
                                    return;
                                }
                                this.fetchHashtagModel(false);
                            })
                    );
                }
            });
        } else {
            this.hashtagDetailsBinding.btnFollowTag.setVisibility(View.GONE);
        }
        this.hashtagDetailsBinding.favChip.setVisibility(View.VISIBLE);
        Context context = this.getContext();
        if (context == null) return;
        String postCount = String.valueOf(this.hashtagModel.getMediaCount());
        SpannableStringBuilder span = new SpannableStringBuilder(this.getResources().getQuantityString(
                R.plurals.main_posts_count_inline,
                this.hashtagModel.getMediaCount() > 2000000000L ? 2000000000
                        : Long.valueOf(this.hashtagModel.getMediaCount()).intValue(),
                postCount)
        );
        span.setSpan(new RelativeSizeSpan(1.2f), 0, postCount.length(), 0);
        span.setSpan(new StyleSpan(Typeface.BOLD), 0, postCount.length(), 0);
        this.hashtagDetailsBinding.mainTagPostCount.setText(span);
        this.hashtagDetailsBinding.mainTagPostCount.setVisibility(View.VISIBLE);
        if (!init) return;
        FavoriteRepository favoriteRepository = FavoriteRepository.Companion.getInstance(context);
        favoriteRepository.getFavorite(
                this.hashtag,
                FavoriteType.HASHTAG,
                CoroutineUtilsKt.getContinuation((favorite, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                    if (throwable != null || favorite == null) {
                        this.hashtagDetailsBinding.favChip.setChipIconResource(R.drawable.ic_outline_star_plus_24);
                        this.hashtagDetailsBinding.favChip.setText(R.string.add_to_favorites);
                        return;
                    }
                    favoriteRepository.insertOrUpdateFavorite(
                            new Favorite(
                                    favorite.getId(),
                                    this.hashtag,
                                    FavoriteType.HASHTAG,
                                    this.hashtagModel.getName(),
                                    "res:/" + R.drawable.ic_hashtag,
                                    favorite.getDateAdded()
                            ),
                            CoroutineUtilsKt.getContinuation((unit, throwable1) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                                if (throwable1 != null) {
                                    Log.e(HashTagFragment.TAG, "onSuccess: ", throwable1);
                                    return;
                                }
                                this.hashtagDetailsBinding.favChip.setChipIconResource(R.drawable.ic_star_check_24);
                                this.hashtagDetailsBinding.favChip.setText(R.string.favorite_short);
                            }), Dispatchers.getIO())
                    );
                }), Dispatchers.getIO())
        );
        this.hashtagDetailsBinding.favChip.setOnClickListener(v -> favoriteRepository.getFavorite(
                this.hashtag,
                FavoriteType.HASHTAG,
                CoroutineUtilsKt.getContinuation((favorite, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                    if (throwable != null) {
                        Log.e(HashTagFragment.TAG, "setHashtagDetails: ", throwable);
                        return;
                    }
                    if (favorite == null) {
                        favoriteRepository.insertOrUpdateFavorite(
                                new Favorite(
                                        0,
                                        this.hashtag,
                                        FavoriteType.HASHTAG,
                                        this.hashtagModel.getName(),
                                        "res:/" + R.drawable.ic_hashtag,
                                        LocalDateTime.now()
                                ),
                                CoroutineUtilsKt.getContinuation((unit, throwable1) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                                    if (throwable1 != null) {
                                        Log.e(HashTagFragment.TAG, "onDataNotAvailable: ", throwable1);
                                        return;
                                    }
                                    this.hashtagDetailsBinding.favChip.setText(R.string.favorite_short);
                                    this.hashtagDetailsBinding.favChip.setChipIconResource(R.drawable.ic_star_check_24);
                                    this.showSnackbar(this.getString(R.string.added_to_favs));
                                }), Dispatchers.getIO())
                        );
                        return;
                    }
                    favoriteRepository.deleteFavorite(
                            this.hashtag,
                            FavoriteType.HASHTAG,
                            CoroutineUtilsKt.getContinuation((unit, throwable1) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                                if (throwable1 != null) {
                                    Log.e(HashTagFragment.TAG, "onSuccess: ", throwable1);
                                    return;
                                }
                                this.hashtagDetailsBinding.favChip.setText(R.string.add_to_favorites);
                                this.hashtagDetailsBinding.favChip.setChipIconResource(R.drawable.ic_outline_star_plus_24);
                                this.showSnackbar(this.getString(R.string.removed_from_favs));
                            }), Dispatchers.getIO())
                    );
                }), Dispatchers.getIO())
                                                         )
        );
        this.hashtagDetailsBinding.mainHashtagImage.setImageURI("res:/" + R.drawable.ic_hashtag);
        //        hashtagDetailsBinding.mainHashtagImage.setOnClickListener(v -> {
        //            if (!hasStories) return;
        //            // show stories
        //            final NavDirections action = HashTagFragmentDirections
        //                    .actionHashtagFragmentToStoryViewerFragment(StoryViewerOptions.forHashtag(hashtagModel.getName()));
        //            NavHostFragment.findNavController(this).navigate(action);
        //        });
    }

    private void showSnackbar(String message) {
        @SuppressLint("ShowToast") Snackbar snackbar = Snackbar.make(this.root, message, BaseTransientBottomBar.LENGTH_LONG);
        snackbar.setAction(R.string.ok, v1 -> snackbar.dismiss())
                .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                .setAnchorView(this.fragmentActivity.getBottomNavView())
                .show();
    }

    private void setTitle() {
        ActionBar actionBar = this.fragmentActivity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle('#' + this.hashtag);
        }
    }

    private void updateSwipeRefreshState() {
        AppExecutors.INSTANCE.getMainThread().execute(() ->
                this.binding.swipeRefreshLayout.setRefreshing(this.binding.posts.isFetching())
        );
    }

    private void navigateToProfile(String username) {
        try {
            NavDirections action = HashTagFragmentDirections.actionToProfile().setUsername(username);
            NavHostFragment.findNavController(this).navigate(action);
        } catch (final Exception e) {
            Log.e(HashTagFragment.TAG, "navigateToProfile: ", e);
        }
    }

    private void showPostsLayoutPreferences() {
        PostsLayoutPreferencesDialogFragment fragment = new PostsLayoutPreferencesDialogFragment(
                Constants.PREF_HASHTAG_POSTS_LAYOUT,
                preferences -> {
                    this.layoutPreferences = preferences;
                    new Handler().postDelayed(() -> this.binding.posts.setLayoutPreferences(preferences), 200);
                });
        fragment.show(this.getChildFragmentManager(), "posts_layout_preferences");
    }
}
