package awais.instagrabber.customviews.helpers;

import android.util.Log;

import java.util.List;

import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.repositories.responses.Media;

public class PostFetcher {
    private static final String TAG = PostFetcher.class.getSimpleName();

    private final PostFetchService postFetchService;
    private final FetchListener<List<Media>> fetchListener;
    private boolean fetching;

    public PostFetcher(PostFetchService postFetchService,
                       FetchListener<List<Media>> fetchListener) {
        this.postFetchService = postFetchService;
        this.fetchListener = fetchListener;
    }

    public void fetch() {
        if (this.fetching) return;
        this.fetching = true;
        this.postFetchService.fetch(new FetchListener<List<Media>>() {
            @Override
            public void onResult(List<Media> result) {
                PostFetcher.this.fetching = false;
                PostFetcher.this.fetchListener.onResult(result);
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(PostFetcher.TAG, "onFailure: ", t);
            }
        });
    }

    public void reset() {
        this.postFetchService.reset();
    }

    public boolean isFetching() {
        return this.fetching;
    }

    public boolean hasMore() {
        return this.postFetchService.hasNextPage();
    }

    public interface PostFetchService {
        void fetch(FetchListener<List<Media>> fetchListener);

        void reset();

        boolean hasNextPage();
    }
}
