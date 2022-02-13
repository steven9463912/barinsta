package awais.instagrabber.asyncs;

import java.util.List;

import awais.instagrabber.customviews.helpers.PostFetcher;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.repositories.responses.Hashtag;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.PostsFetchResponse;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.webservices.GraphQLRepository;
import awais.instagrabber.webservices.HashtagRepository;
import awais.instagrabber.webservices.ServiceCallback;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.Dispatchers;

public class HashtagPostFetchService implements PostFetcher.PostFetchService {
    private final HashtagRepository hashtagRepository;
    private final GraphQLRepository graphQLRepository;
    private final Hashtag hashtagModel;
    private String nextMaxId;
    private boolean moreAvailable;
    private final boolean isLoggedIn;

    public HashtagPostFetchService(Hashtag hashtagModel, boolean isLoggedIn) {
        this.hashtagModel = hashtagModel;
        this.isLoggedIn = isLoggedIn;
        this.hashtagRepository = isLoggedIn ? HashtagRepository.Companion.getInstance() : null;
        this.graphQLRepository = isLoggedIn ? null : GraphQLRepository.Companion.getInstance();
    }

    @Override
    public void fetch(FetchListener<List<Media>> fetchListener) {
        Continuation<PostsFetchResponse> cb = CoroutineUtilsKt.getContinuation((result, t) -> {
            if (t != null) {
                if (fetchListener != null) {
                    fetchListener.onFailure(t);
                }
                return;
            }
            if (result == null) return;
            this.nextMaxId = result.getNextCursor();
            this.moreAvailable = result.getHasNextPage();
            if (fetchListener != null) {
                fetchListener.onResult(result.getFeedModels());
            }
        }, Dispatchers.getIO());
        if (this.isLoggedIn) this.hashtagRepository.fetchPosts(this.hashtagModel.getName().toLowerCase(), this.nextMaxId, cb);
        else this.graphQLRepository.fetchHashtagPosts(
                this.hashtagModel.getName().toLowerCase(),
                this.nextMaxId,
                cb
        );
    }

    @Override
    public void reset() {
        this.nextMaxId = null;
    }

    @Override
    public boolean hasNextPage() {
        return this.moreAvailable;
    }
}
