package awais.instagrabber.asyncs;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import awais.instagrabber.customviews.helpers.PostFetcher;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.discover.TopicalExploreFeedResponse;
import awais.instagrabber.repositories.responses.WrappedMedia;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.webservices.FeedRepository;
import awais.instagrabber.webservices.ServiceCallback;

public class DiscoverPostFetchService implements PostFetcher.PostFetchService {
    private static final String TAG = "DiscoverPostFetchService";
    private final FeedRepository feedRepository;
    private String maxId;
    private boolean moreAvailable;

    public DiscoverPostFetchService() {
        this.feedRepository = FeedRepository.Companion.getInstance();
    }

    @Override
    public void fetch(FetchListener<List<Media>> fetchListener) {
        this.feedRepository.topicalExplore(this.maxId, CoroutineUtilsKt.getContinuation((result, t) -> {
            if (t != null) {
                if (fetchListener != null) {
                    fetchListener.onFailure(t);
                }
                return;
            }
            if (result == null) {
                fetchListener.onFailure(new RuntimeException("result is null"));
                return;
            }
            this.moreAvailable = result.getMoreAvailable();
            this.maxId = result.getNextMaxId();
            List<WrappedMedia> items = result.getItems();
            List<Media> posts;
            if (items == null) {
                posts = Collections.emptyList();
            } else {
                posts = items.stream()
                             .map(WrappedMedia::getMedia)
                             .filter(Objects::nonNull)
                             .collect(Collectors.toList());
            }
            if (fetchListener != null) {
                fetchListener.onResult(posts);
            }
        }));
    }

    @Override
    public void reset() {
        this.maxId = null;
    }

    @Override
    public boolean hasNextPage() {
        return this.moreAvailable;
    }
}
