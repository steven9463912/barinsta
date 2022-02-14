package awais.instagrabber.customviews.helpers;

import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import awais.instagrabber.interfaces.LazyLoadListener;

/**
 * thanks to nesquena's <a href="https://gist.github.com/nesquena/d09dc68ff07e845cc622">EndlessRecyclerViewScrollListener</a>
 */
public final class RecyclerLazyLoader extends RecyclerView.OnScrollListener {
    /**
     * The current offset index of data you have loaded
     */
    private int currentPage;
    /**
     * The total number of items in the data set after the last load
     */
    private int previousTotalItemCount;
    /**
     * <code>true</code> if we are still waiting for the last set of data to load.
     */
    private boolean loading = true;
    /**
     * The minimum amount of items to have below your current scroll position before loading more.
     */
    private final int visibleThreshold;
    private final LazyLoadListener lazyLoadListener;
    private final RecyclerView.LayoutManager layoutManager;

    public RecyclerLazyLoader(@NonNull RecyclerView.LayoutManager layoutManager,
                              LazyLoadListener lazyLoadListener,
                              int threshold) {
        this.layoutManager = layoutManager;
        this.lazyLoadListener = lazyLoadListener;
        if (threshold > 0) {
            visibleThreshold = threshold;
            return;
        }
        if (layoutManager instanceof GridLayoutManager) {
            visibleThreshold = 5 * Math.max(3, ((GridLayoutManager) layoutManager).getSpanCount());
        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            visibleThreshold = 4 * Math.max(3, ((StaggeredGridLayoutManager) layoutManager).getSpanCount());
        } else if (layoutManager instanceof LinearLayoutManager) {
            visibleThreshold = ((LinearLayoutManager) layoutManager).getReverseLayout() ? 4 : 8;
        } else {
            visibleThreshold = 5;
        }
    }

    public RecyclerLazyLoader(@NonNull RecyclerView.LayoutManager layoutManager,
                              LazyLoadListener lazyLoadListener) {
        this(layoutManager, lazyLoadListener, -1);
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        int totalItemCount = this.layoutManager.getItemCount();

        if (totalItemCount < this.previousTotalItemCount) {
            this.currentPage = 0;
            this.previousTotalItemCount = totalItemCount;
            if (totalItemCount == 0) this.loading = true;
        }

        if (this.loading && totalItemCount > this.previousTotalItemCount) {
            this.loading = false;
            this.previousTotalItemCount = totalItemCount;
        }

        int lastVisibleItemPosition;
        if (this.layoutManager instanceof GridLayoutManager) {
            GridLayoutManager layoutManager = (GridLayoutManager) this.layoutManager;
            lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
        } else if (this.layoutManager instanceof StaggeredGridLayoutManager) {
            StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) this.layoutManager;
            int spanCount = layoutManager.getSpanCount();
            int[] lastVisibleItemPositions = layoutManager.findLastVisibleItemPositions(null);
            lastVisibleItemPosition = 0;
            for (int itemPosition : lastVisibleItemPositions) {
                if (itemPosition > lastVisibleItemPosition) {
                    lastVisibleItemPosition = itemPosition;
                }
            }
        } else {
            LinearLayoutManager layoutManager = (LinearLayoutManager) this.layoutManager;
            lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
        }

        if (!this.loading && lastVisibleItemPosition + this.visibleThreshold > totalItemCount) {
            this.loading = true;
            if (this.lazyLoadListener != null) {
                new Handler().postDelayed(() -> this.lazyLoadListener.onLoadMore(++this.currentPage, totalItemCount), 200);
            }
        }
    }

    public int getCurrentPage() {
        return this.currentPage;
    }

    public void resetState() {
        currentPage = 0;
        previousTotalItemCount = 0;
        loading = true;
    }
}