package awais.instagrabber.customviews;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.transition.ChangeBounds;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;
import androidx.work.Data;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import awais.instagrabber.adapters.FeedAdapterV2;
import awais.instagrabber.customviews.helpers.GridSpacingItemDecoration;
import awais.instagrabber.customviews.helpers.PostFetcher;
import awais.instagrabber.customviews.helpers.RecyclerLazyLoaderAtEdge;
import awais.instagrabber.models.PostsLayoutPreferences;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.viewmodels.MediaViewModel;
import awais.instagrabber.workers.DownloadWorker;

public class PostsRecyclerView extends RecyclerView {
    private static final String TAG = "PostsRecyclerView";

    private StaggeredGridLayoutManager layoutManager;
    private PostsLayoutPreferences layoutPreferences;
    private PostFetcher.PostFetchService postFetchService;
    private Transition transition;
    private ViewModelStoreOwner viewModelStoreOwner;
    private FeedAdapterV2 feedAdapter;
    private LifecycleOwner lifeCycleOwner;
    private MediaViewModel mediaViewModel;
    private boolean initCalled;
    private GridSpacingItemDecoration gridSpacingItemDecoration;
    private RecyclerLazyLoaderAtEdge lazyLoader;
    private FeedAdapterV2.FeedItemCallback feedItemCallback;
    private boolean shouldScrollToTop;
    private FeedAdapterV2.SelectionModeCallback selectionModeCallback;

    private final List<FetchStatusChangeListener> fetchStatusChangeListeners = new ArrayList<>();

    private final RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(this.getContext()) {
        @Override
        protected int getVerticalSnapPreference() {
            return LinearSmoothScroller.SNAP_TO_START;
        }
    };

    public PostsRecyclerView(@NonNull Context context) {
        super(context);
    }

    public PostsRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PostsRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PostsRecyclerView setViewModelStoreOwner(ViewModelStoreOwner owner) {
        if (this.initCalled) {
            throw new IllegalArgumentException("init already called!");
        }
        viewModelStoreOwner = owner;
        return this;
    }

    public PostsRecyclerView setLifeCycleOwner(LifecycleOwner lifeCycleOwner) {
        if (this.initCalled) {
            throw new IllegalArgumentException("init already called!");
        }
        this.lifeCycleOwner = lifeCycleOwner;
        return this;
    }

    public PostsRecyclerView setPostFetchService(PostFetcher.PostFetchService postFetchService) {
        if (this.initCalled) {
            throw new IllegalArgumentException("init already called!");
        }
        this.postFetchService = postFetchService;
        return this;
    }

    public PostsRecyclerView setFeedItemCallback(@NonNull FeedAdapterV2.FeedItemCallback feedItemCallback) {
        this.feedItemCallback = feedItemCallback;
        return this;
    }

    public PostsRecyclerView setSelectionModeCallback(@NonNull FeedAdapterV2.SelectionModeCallback selectionModeCallback) {
        this.selectionModeCallback = selectionModeCallback;
        return this;
    }

    public PostsRecyclerView setLayoutPreferences(PostsLayoutPreferences layoutPreferences) {
        this.layoutPreferences = layoutPreferences;
        if (this.initCalled) {
            if (layoutPreferences == null) return this;
            this.feedAdapter.setLayoutPreferences(layoutPreferences);
            this.updateLayout();
        }
        return this;
    }

    public void init() {
        this.initCalled = true;
        if (this.viewModelStoreOwner == null) {
            throw new IllegalArgumentException("ViewModelStoreOwner cannot be null");
        } else if (this.lifeCycleOwner == null) {
            throw new IllegalArgumentException("LifecycleOwner cannot be null");
        } else if (this.postFetchService == null) {
            throw new IllegalArgumentException("PostFetchService cannot be null");
        }
        if (this.layoutPreferences == null) {
            this.layoutPreferences = PostsLayoutPreferences.builder().build();
            // Utils.settingsHelper.putString(Constants.PREF_POSTS_LAYOUT, layoutPreferences.getJson());
        }
        this.gridSpacingItemDecoration = new GridSpacingItemDecoration(Utils.convertDpToPx(2));
        this.initTransition();
        this.initAdapter();
        this.initLayoutManager();
        this.initSelf();
        this.initDownloadWorkerListener();
    }

    private void initTransition() {
        this.transition = new ChangeBounds();
        this.transition.setDuration(300);
    }

    private void initLayoutManager() {
        this.layoutManager = new StaggeredGridLayoutManager(this.layoutPreferences.getColCount(), StaggeredGridLayoutManager.VERTICAL);
        this.setLayoutManager(this.layoutManager);
    }

    private void initAdapter() {
        this.feedAdapter = new FeedAdapterV2(this.layoutPreferences, this.feedItemCallback, this.selectionModeCallback);
        this.feedAdapter.setStateRestorationPolicy(RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY);
        this.setAdapter(this.feedAdapter);
    }

    private void initSelf() {
        try {
            this.mediaViewModel = new ViewModelProvider(
                    this.viewModelStoreOwner,
                    new MediaViewModel.ViewModelFactory(this.postFetchService)
            ).get(MediaViewModel.class);
        } catch (final Exception e) {
            Log.e(PostsRecyclerView.TAG, "initSelf: ", e);
        }
        if (this.mediaViewModel == null) return;
        LiveData<List<Media>> mediaListLiveData = this.mediaViewModel.getList();
        mediaListLiveData.observe(this.lifeCycleOwner, list -> this.feedAdapter.submitList(list, () -> {
            this.dispatchFetchStatus();
            this.postDelayed(this::fetchMoreIfPossible, 1000);
            if (!this.shouldScrollToTop) return;
            this.shouldScrollToTop = false;
            this.post(() -> this.smoothScrollToPosition(0));
        }));
        if (this.layoutPreferences.getHasGap()) {
            this.addItemDecoration(this.gridSpacingItemDecoration);
        }
        this.setHasFixedSize(true);
        this.setNestedScrollingEnabled(true);
        this.setItemAnimator(null);
        this.lazyLoader = new RecyclerLazyLoaderAtEdge(this.layoutManager, (page) -> {
            if (this.mediaViewModel.hasMore()) {
                this.mediaViewModel.fetch();
                this.dispatchFetchStatus();
            }
        });
        this.addOnScrollListener(this.lazyLoader);
        if (mediaListLiveData.getValue() == null || mediaListLiveData.getValue().isEmpty()) {
            this.mediaViewModel.fetch();
            this.dispatchFetchStatus();
        }
    }

    private void fetchMoreIfPossible() {
        if (!this.mediaViewModel.hasMore()) return;
        if (this.feedAdapter.getItemCount() == 0) return;
        LayoutManager layoutManager = this.getLayoutManager();
        if (!(layoutManager instanceof StaggeredGridLayoutManager)) return;
        int[] itemPositions = ((StaggeredGridLayoutManager) layoutManager).findLastCompletelyVisibleItemPositions(null);
        boolean allNoPosition = Arrays.stream(itemPositions).allMatch(position -> position == RecyclerView.NO_POSITION);
        if (allNoPosition) return;
        boolean match = Arrays.stream(itemPositions).anyMatch(position -> position == this.feedAdapter.getItemCount() - 1);
        if (!match) return;
        this.mediaViewModel.fetch();
        this.dispatchFetchStatus();
    }

    private void initDownloadWorkerListener() {
        WorkManager.getInstance(this.getContext())
                   .getWorkInfosByTagLiveData("download")
                   .observe(this.lifeCycleOwner, workInfoList -> {
                       for (WorkInfo workInfo : workInfoList) {
                           if (workInfo == null) continue;
                           Data progress = workInfo.getProgress();
                           float progressPercent = progress.getFloat(DownloadWorker.PROGRESS, 0);
                           if (progressPercent != 100) continue;
                           String url = progress.getString(DownloadWorker.URL);
                           List<Media> feedModels = this.mediaViewModel.getList().getValue();
                           if (feedModels == null) continue;
                           for (int i = 0; i < feedModels.size(); i++) {
                               Media feedModel = feedModels.get(i);
                               List<String> displayUrls = this.getDisplayUrl(feedModel);
                               if (displayUrls.contains(url)) {
                                   this.feedAdapter.notifyItemChanged(i);
                                   break;
                               }
                           }
                       }
                   });
    }

    private List<String> getDisplayUrl(Media feedModel) {
        List<String> urls = Collections.emptyList();
        if (feedModel == null || feedModel.getType() == null) return urls;
        switch (feedModel.getType()) {
            case MEDIA_TYPE_IMAGE:
            case MEDIA_TYPE_VIDEO:
                urls = Collections.singletonList(ResponseBodyUtils.getImageUrl(feedModel));
                break;
            case MEDIA_TYPE_SLIDER:
                List<Media> sliderItems = feedModel.getCarouselMedia();
                if (sliderItems != null) {
                    ImmutableList.Builder<String> builder = ImmutableList.builder();
                    for (Media child : sliderItems) {
                        builder.add(ResponseBodyUtils.getImageUrl(child));
                    }
                    urls = builder.build();
                }
                break;
            default:
        }
        return urls;
    }

    private void updateLayout() {
        this.post(() -> {
            TransitionManager.beginDelayedTransition(this, this.transition);
            this.feedAdapter.notifyDataSetChanged();
            int itemDecorationCount = this.getItemDecorationCount();
            if (!this.layoutPreferences.getHasGap()) {
                if (itemDecorationCount == 1) {
                    this.removeItemDecoration(this.gridSpacingItemDecoration);
                }
            } else {
                if (itemDecorationCount == 0) {
                    this.addItemDecoration(this.gridSpacingItemDecoration);
                }
            }
            if (this.layoutPreferences.getType() == PostsLayoutPreferences.PostsLayoutType.LINEAR) {
                if (this.layoutManager.getSpanCount() != 1) {
                    this.layoutManager.setSpanCount(1);
                    this.setAdapter(null);
                    this.setAdapter(this.feedAdapter);
                }
            } else {
                final boolean shouldRedraw = this.layoutManager.getSpanCount() == 1;
                this.layoutManager.setSpanCount(this.layoutPreferences.getColCount());
                if (shouldRedraw) {
                    this.setAdapter(null);
                    this.setAdapter(this.feedAdapter);
                }
            }
        });
    }

    public void refresh() {
        this.shouldScrollToTop = true;
        if (this.lazyLoader != null) {
            this.lazyLoader.resetState();
        }
        if (this.mediaViewModel != null) {
            this.mediaViewModel.refresh();
        }
        this.dispatchFetchStatus();
    }

    public boolean isFetching() {
        return this.mediaViewModel != null && this.mediaViewModel.isFetching();
    }

    public PostsRecyclerView addFetchStatusChangeListener(FetchStatusChangeListener fetchStatusChangeListener) {
        if (fetchStatusChangeListener == null) return this;
        this.fetchStatusChangeListeners.add(fetchStatusChangeListener);
        return this;
    }

    public void removeFetchStatusListener(FetchStatusChangeListener fetchStatusChangeListener) {
        if (fetchStatusChangeListener == null) return;
        this.fetchStatusChangeListeners.remove(fetchStatusChangeListener);
    }

    private void dispatchFetchStatus() {
        for (FetchStatusChangeListener listener : this.fetchStatusChangeListeners) {
            listener.onFetchStatusChange(this.isFetching());
        }
    }

    public PostsLayoutPreferences getLayoutPreferences() {
        return this.layoutPreferences;
    }

    public void endSelection() {
        this.feedAdapter.endSelection();
    }

    public interface FetchStatusChangeListener {
        void onFetchStatusChange(boolean fetching);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.lifeCycleOwner = null;
        this.initCalled = false;
    }

    @Override
    public void smoothScrollToPosition(int position) {
        this.smoothScroller.setTargetPosition(position);
        this.layoutManager.startSmoothScroll(this.smoothScroller);
    }
}
