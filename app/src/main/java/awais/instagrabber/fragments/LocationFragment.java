package awais.instagrabber.fragments;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import awais.instagrabber.asyncs.LocationPostFetchService;
import awais.instagrabber.customviews.PrimaryActionModeCallback;
import awais.instagrabber.databinding.FragmentLocationBinding;
import awais.instagrabber.databinding.LayoutLocationDetailsBinding;
import awais.instagrabber.db.entities.Favorite;
import awais.instagrabber.db.repositories.FavoriteRepository;
import awais.instagrabber.dialogs.PostsLayoutPreferencesDialogFragment;
import awais.instagrabber.models.PostsLayoutPreferences;
import awais.instagrabber.models.enums.FavoriteType;
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
import awais.instagrabber.webservices.LocationRepository;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.Dispatchers;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class LocationFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "LocationFragment";

    private MainActivity fragmentActivity;
    private FragmentLocationBinding binding;
    private CoordinatorLayout root;
    private boolean shouldRefresh = true;
    private boolean opening;
    private long locationId;
    private Location locationModel;
    private ActionMode actionMode;
    private GraphQLRepository graphQLRepository;
    private LocationRepository locationRepository;
    private boolean isLoggedIn;
    private Set<Media> selectedFeedModels;
    private PostsLayoutPreferences layoutPreferences = Utils.getPostsLayoutPreferences(Constants.PREF_LOCATION_POSTS_LAYOUT);
    private LayoutLocationDetailsBinding locationDetailsBinding;

    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            LocationFragment.this.binding.posts.endSelection();
        }
    };
    private final PrimaryActionModeCallback multiSelectAction = new PrimaryActionModeCallback(
            R.menu.multi_select_download_menu, new PrimaryActionModeCallback.CallbacksHelper() {
        @Override
        public void onDestroy(ActionMode mode) {
            LocationFragment.this.binding.posts.endSelection();
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode,
                                           MenuItem item) {
            if (item.getItemId() == R.id.action_download) {
                if (selectedFeedModels == null) return false;
                Context context = LocationFragment.this.getContext();
                if (context == null) return false;
                DownloadUtils.download(context, ImmutableList.copyOf(selectedFeedModels));
                LocationFragment.this.binding.posts.endSelection();
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
                NavDirections commentsAction = LocationFragmentDirections.actionToComments(
                        feedModel.getCode(),
                        feedModel.getPk(),
                        user.getPk()
                );
                NavHostFragment.findNavController(LocationFragment.this).navigate(commentsAction);
            } catch (final Exception e) {
                Log.e(LocationFragment.TAG, "onCommentsClick: ", e);
            }
        }

        @Override
        public void onDownloadClick(Media feedModel, int childPosition, View popupLocation) {
            Context context = LocationFragment.this.getContext();
            if (context == null) return;
            DownloadUtils.showDownloadDialog(context, feedModel, childPosition, popupLocation);
        }

        @Override
        public void onHashtagClick(String hashtag) {
            try {
                NavDirections action = LocationFragmentDirections.actionToHashtag(hashtag);
                NavHostFragment.findNavController(LocationFragment.this).navigate(action);
            } catch (final Exception e) {
                Log.e(LocationFragment.TAG, "onHashtagClick: ", e);
            }
        }

        @Override
        public void onLocationClick(Media feedModel) {
            Location location = feedModel.getLocation();
            if (location == null) return;
            NavDirections action = LocationFragmentDirections.actionToLocation(location.getPk());
            NavHostFragment.findNavController(LocationFragment.this).navigate(action);
        }

        @Override
        public void onMentionClick(String mention) {
            LocationFragment.this.navigateToProfile(mention.trim());
        }

        @Override
        public void onNameClick(Media feedModel) {
            User user = feedModel.getUser();
            if (user == null) return;
            LocationFragment.this.navigateToProfile("@" + user.getUsername());
        }

        @Override
        public void onProfilePicClick(Media feedModel) {
            User user = feedModel.getUser();
            if (user == null) return;
            LocationFragment.this.navigateToProfile("@" + user.getUsername());
        }

        @Override
        public void onURLClick(String url) {
            Utils.openURL(LocationFragment.this.getContext(), url);
        }

        @Override
        public void onEmailClick(String emailId) {
            Utils.openEmailAddress(LocationFragment.this.getContext(), emailId);
        }

        private void openPostDialog(@NonNull Media feedModel, int position) {
            if (LocationFragment.this.opening) return;
            User user = feedModel.getUser();
            if (user == null) return;
            if (TextUtils.isEmpty(user.getUsername())) {
                LocationFragment.this.opening = true;
                String code = feedModel.getCode();
                if (code == null) return;
                LocationFragment.this.graphQLRepository.fetchPost(
                        code,
                        CoroutineUtilsKt.getContinuation((media, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                            LocationFragment.this.opening = false;
                            if (throwable != null) {
                                Log.e(LocationFragment.TAG, "Error", throwable);
                                return;
                            }
                            if (media == null) return;
                            this.openPostDialog(media, position);
                        }))
                );
                return;
            }
            LocationFragment.this.opening = true;
            try {
                NavDirections action = LocationFragmentDirections.actionToPost(feedModel, position);
                NavHostFragment.findNavController(LocationFragment.this).navigate(action);
            } catch (final Exception e) {
                Log.e(LocationFragment.TAG, "openPostDialog: ", e);
            }
            LocationFragment.this.opening = false;
        }
    };
    private final FeedAdapterV2.SelectionModeCallback selectionModeCallback = new FeedAdapterV2.SelectionModeCallback() {

        @Override
        public void onSelectionStart() {
            if (!LocationFragment.this.onBackPressedCallback.isEnabled()) {
                OnBackPressedDispatcher onBackPressedDispatcher = LocationFragment.this.fragmentActivity.getOnBackPressedDispatcher();
                LocationFragment.this.onBackPressedCallback.setEnabled(true);
                onBackPressedDispatcher.addCallback(LocationFragment.this.getViewLifecycleOwner(), LocationFragment.this.onBackPressedCallback);
            }
            if (LocationFragment.this.actionMode == null) {
                LocationFragment.this.actionMode = LocationFragment.this.fragmentActivity.startActionMode(LocationFragment.this.multiSelectAction);
            }
        }

        @Override
        public void onSelectionChange(Set<Media> selectedFeedModels) {
            String title = LocationFragment.this.getString(R.string.number_selected, selectedFeedModels.size());
            if (LocationFragment.this.actionMode != null) {
                LocationFragment.this.actionMode.setTitle(title);
            }
            LocationFragment.this.selectedFeedModels = selectedFeedModels;
        }

        @Override
        public void onSelectionEnd() {
            if (LocationFragment.this.onBackPressedCallback.isEnabled()) {
                LocationFragment.this.onBackPressedCallback.setEnabled(false);
                LocationFragment.this.onBackPressedCallback.remove();
            }
            if (LocationFragment.this.actionMode != null) {
                LocationFragment.this.actionMode.finish();
                LocationFragment.this.actionMode = null;
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.fragmentActivity = (MainActivity) this.requireActivity();
        String cookie = settingsHelper.getString(Constants.COOKIE);
        this.isLoggedIn = !TextUtils.isEmpty(cookie) && CookieUtils.getUserIdFromCookie(cookie) > 0;
        this.locationRepository = this.isLoggedIn ? LocationRepository.Companion.getInstance() : null;
        // storiesRepository = StoriesRepository.Companion.getInstance();
        this.graphQLRepository = this.isLoggedIn ? null : GraphQLRepository.Companion.getInstance();
        this.setHasOptionsMenu(true);
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
        this.binding = FragmentLocationBinding.inflate(inflater, container, false);
        this.root = this.binding.getRoot();
        this.locationDetailsBinding = this.binding.header;
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
        LocationFragmentArgs fragmentArgs = LocationFragmentArgs.fromBundle(this.getArguments());
        this.locationId = fragmentArgs.getLocationId();
        this.locationDetailsBinding.favChip.setVisibility(View.GONE);
        this.locationDetailsBinding.btnMap.setVisibility(View.GONE);
        this.setTitle();
        this.fetchLocationModel();
    }

    private void setupPosts() {
        this.binding.posts.setViewModelStoreOwner(this)
                     .setLifeCycleOwner(this)
                     .setPostFetchService(new LocationPostFetchService(this.locationModel, this.isLoggedIn))
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

    private void fetchLocationModel() {
        this.binding.swipeRefreshLayout.setRefreshing(true);
        Continuation<Location> cb = CoroutineUtilsKt.getContinuation((result, t) -> {
            this.locationModel = result;
            AppExecutors.INSTANCE.getMainThread().execute(() -> {
                this.setupLocationDetails();
                this.binding.swipeRefreshLayout.setRefreshing(false);
            });
        }, Dispatchers.getIO());
        if (this.isLoggedIn) this.locationRepository.fetch(this.locationId, cb);
        else this.graphQLRepository.fetchLocation(this.locationId, cb);
    }

    private void setupLocationDetails() {
        if (this.locationModel == null) {
            try {
                Toast.makeText(this.getContext(), R.string.error_loading_location, Toast.LENGTH_SHORT).show();
                this.binding.swipeRefreshLayout.setEnabled(false);
            } catch (final Exception ignored) {}
            return;
        }
        this.setTitle();
        this.setupPosts();
        //        fetchStories();
        long locationId = this.locationModel.getPk();
        // binding.swipeRefreshLayout.setRefreshing(true);
        this.locationDetailsBinding.mainLocationImage.setImageURI("res:/" + R.drawable.ic_location);
        // final String postCount = String.valueOf(locationModel.getChildCommentCount());
        // final SpannableStringBuilder span = new SpannableStringBuilder(getResources().getQuantityString(R.plurals.main_posts_count_inline,
        //                                                                                                 locationModel.getPostCount() > 2000000000L
        //                                                                                                 ? 2000000000
        //                                                                                                 : locationModel.getPostCount().intValue(),
        //                                                                                                 postCount));
        // span.setSpan(new RelativeSizeSpan(1.2f), 0, postCount.length(), 0);
        // span.setSpan(new StyleSpan(Typeface.BOLD), 0, postCount.length(), 0);
        // locationDetailsBinding.mainLocPostCount.setText(span);
        // locationDetailsBinding.mainLocPostCount.setVisibility(View.VISIBLE);
        this.locationDetailsBinding.locationFullName.setText(this.locationModel.getName());
        final CharSequence biography = this.locationModel.getAddress() + "\n" + this.locationModel.getCity();
        // binding.locationBiography.setCaptionIsExpandable(true);
        // binding.locationBiography.setCaptionIsExpanded(true);

        Context context = this.getContext();
        if (context == null) return;
        if (TextUtils.isEmpty(biography)) {
            this.locationDetailsBinding.locationBiography.setVisibility(View.GONE);
        } else {
            this.locationDetailsBinding.locationBiography.setVisibility(View.VISIBLE);
            this.locationDetailsBinding.locationBiography.setText(biography);
            // locationDetailsBinding.locationBiography.addOnHashtagListener(autoLinkItem -> {
            //     final NavController navController = NavHostFragment.findNavController(this);
            //     final Bundle bundle = new Bundle();
            //     final String originalText = autoLinkItem.getOriginalText().trim();
            //     bundle.putString(ARG_HASHTAG, originalText);
            //     navController.navigate(R.id.action_global_hashTagFragment, bundle);
            // });
            // locationDetailsBinding.locationBiography.addOnMentionClickListener(autoLinkItem -> {
            //     final String originalText = autoLinkItem.getOriginalText().trim();
            //     navigateToProfile(originalText);
            // });
            // locationDetailsBinding.locationBiography.addOnEmailClickListener(autoLinkItem -> Utils.openEmailAddress(context,
            //                                                                                                         autoLinkItem.getOriginalText()
            //                                                                                                                     .trim()));
            // locationDetailsBinding.locationBiography
            //         .addOnURLClickListener(autoLinkItem -> Utils.openURL(context, autoLinkItem.getOriginalText().trim()));
            this.locationDetailsBinding.locationBiography.setOnLongClickListener(v -> {
                Utils.copyText(context, biography);
                return true;
            });
        }

        if (!this.locationModel.getGeo().startsWith("geo:0.0,0.0?z=17")) {
            this.locationDetailsBinding.btnMap.setVisibility(View.VISIBLE);
            this.locationDetailsBinding.btnMap.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(this.locationModel.getGeo()));
                    this.startActivity(intent);
                } catch (final ActivityNotFoundException e) {
                    Toast.makeText(context, R.string.no_external_map_app, Toast.LENGTH_LONG).show();
                    Log.e(LocationFragment.TAG, "setupLocationDetails: ", e);
                } catch (final Exception e) {
                    Log.e(LocationFragment.TAG, "setupLocationDetails: ", e);
                }
            });
        } else {
            this.locationDetailsBinding.btnMap.setVisibility(View.GONE);
            this.locationDetailsBinding.btnMap.setOnClickListener(null);
        }

        FavoriteRepository favoriteRepository = FavoriteRepository.Companion.getInstance(context);
        this.locationDetailsBinding.favChip.setVisibility(View.VISIBLE);
        favoriteRepository.getFavorite(
                String.valueOf(locationId),
                FavoriteType.LOCATION,
                CoroutineUtilsKt.getContinuation((favorite, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                    if (throwable != null || favorite == null) {
                        this.locationDetailsBinding.favChip.setChipIconResource(R.drawable.ic_outline_star_plus_24);
                        this.locationDetailsBinding.favChip.setChipIconResource(R.drawable.ic_outline_star_plus_24);
                        this.locationDetailsBinding.favChip.setText(R.string.add_to_favorites);
                        Log.e(LocationFragment.TAG, "setupLocationDetails: ", throwable);
                        return;
                    }
                    this.locationDetailsBinding.favChip.setChipIconResource(R.drawable.ic_star_check_24);
                    this.locationDetailsBinding.favChip.setChipIconResource(R.drawable.ic_star_check_24);
                    this.locationDetailsBinding.favChip.setText(R.string.favorite_short);
                    favoriteRepository.insertOrUpdateFavorite(
                            new Favorite(
                                    favorite.getId(),
                                    String.valueOf(locationId),
                                    FavoriteType.LOCATION,
                                    this.locationModel.getName(),
                                    "res:/" + R.drawable.ic_location,
                                    favorite.getDateAdded()
                            ),
                            CoroutineUtilsKt.getContinuation((unit, throwable1) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                                if (throwable1 != null) {
                                    Log.e(LocationFragment.TAG, "onSuccess: ", throwable1);
                                }
                            }), Dispatchers.getIO())
                    );
                }), Dispatchers.getIO())
        );
        this.locationDetailsBinding.favChip.setOnClickListener(v -> favoriteRepository.getFavorite(
                String.valueOf(locationId),
                FavoriteType.LOCATION,
                CoroutineUtilsKt.getContinuation((favorite, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                    if (throwable != null) {
                        Log.e(LocationFragment.TAG, "setupLocationDetails: ", throwable);
                        return;
                    }
                    if (favorite == null) {
                        favoriteRepository.insertOrUpdateFavorite(
                                new Favorite(
                                        0,
                                        String.valueOf(locationId),
                                        FavoriteType.LOCATION,
                                        this.locationModel.getName(),
                                        "res:/" + R.drawable.ic_location,
                                        LocalDateTime.now()
                                ),
                                CoroutineUtilsKt.getContinuation((unit, throwable1) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                                    if (throwable1 != null) {
                                        Log.e(LocationFragment.TAG, "onDataNotAvailable: ", throwable1);
                                        return;
                                    }
                                    this.locationDetailsBinding.favChip.setText(R.string.favorite_short);
                                    this.locationDetailsBinding.favChip.setChipIconResource(R.drawable.ic_star_check_24);
                                    this.showSnackbar(this.getString(R.string.added_to_favs));
                                }), Dispatchers.getIO())
                        );
                        return;
                    }
                    favoriteRepository.deleteFavorite(
                            String.valueOf(locationId),
                            FavoriteType.LOCATION,
                            CoroutineUtilsKt.getContinuation((unit, throwable1) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                                if (throwable1 != null) {
                                    Log.e(LocationFragment.TAG, "onSuccess: ", throwable1);
                                    return;
                                }
                                this.locationDetailsBinding.favChip.setText(R.string.add_to_favorites);
                                this.locationDetailsBinding.favChip.setChipIconResource(R.drawable.ic_outline_star_plus_24);
                                this.showSnackbar(this.getString(R.string.removed_from_favs));
                            }), Dispatchers.getIO())
                    );
                }), Dispatchers.getIO())
        ));
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
        if (actionBar != null && this.locationModel != null) {
            actionBar.setTitle(this.locationModel.getName());
        }
    }

    private void updateSwipeRefreshState() {
        AppExecutors.INSTANCE.getMainThread().execute(() -> this.binding.swipeRefreshLayout.setRefreshing(this.binding.posts.isFetching()));
    }

    private void navigateToProfile(String username) {
        try {
            NavDirections action = LocationFragmentDirections.actionToProfile().setUsername(username);
            NavHostFragment.findNavController(this).navigate(action);
        } catch (final Exception e) {
            Log.e(LocationFragment.TAG, "navigateToProfile: ", e);
        }
    }

    private void showPostsLayoutPreferences() {
        PostsLayoutPreferencesDialogFragment fragment = new PostsLayoutPreferencesDialogFragment(
                Constants.PREF_LOCATION_POSTS_LAYOUT,
                preferences -> {
                    this.layoutPreferences = preferences;
                    new Handler().postDelayed(() -> this.binding.posts.setLayoutPreferences(preferences), 200);
                });
        fragment.show(this.getChildFragmentManager(), "posts_layout_preferences");
    }
}
