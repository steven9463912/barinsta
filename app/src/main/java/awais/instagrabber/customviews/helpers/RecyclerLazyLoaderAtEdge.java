package awais.instagrabber.customviews.helpers;

import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public final class RecyclerLazyLoaderAtEdge extends RecyclerView.OnScrollListener {

    private final RecyclerView.LayoutManager layoutManager;
    private final LazyLoadListener lazyLoadListener;
    private final boolean atTop;
    private int currentPage;
    private int previousItemCount;
    private boolean loading;

    public RecyclerLazyLoaderAtEdge(@NonNull RecyclerView.LayoutManager layoutManager,
                                    LazyLoadListener lazyLoadListener) {
        this.layoutManager = layoutManager;
        atTop = false;
        this.lazyLoadListener = lazyLoadListener;
    }

    public RecyclerLazyLoaderAtEdge(@NonNull RecyclerView.LayoutManager layoutManager,
                                    boolean atTop,
                                    LazyLoadListener lazyLoadListener) {
        this.layoutManager = layoutManager;
        this.atTop = atTop;
        this.lazyLoadListener = lazyLoadListener;
    }

    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
        super.onScrollStateChanged(recyclerView, newState);
        int itemCount = this.layoutManager.getItemCount();
        if (itemCount > this.previousItemCount) {
            this.loading = false;
        }
        if (!recyclerView.canScrollVertically(this.atTop ? -1 : 1)
                && newState == RecyclerView.SCROLL_STATE_IDLE
                && !this.loading
                && this.lazyLoadListener != null) {
            this.loading = true;
            new Handler().postDelayed(() -> this.lazyLoadListener.onLoadMore(++this.currentPage), 300);
        }
    }

    public int getCurrentPage() {
        return this.currentPage;
    }

    public void resetState() {
        this.currentPage = 0;
        this.previousItemCount = 0;
        this.loading = true;
    }

    public interface LazyLoadListener {
        void onLoadMore(int page);
    }
}