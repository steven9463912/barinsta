package awais.instagrabber.asyncs;

import java.util.List;

import awais.instagrabber.customviews.helpers.PostFetcher;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.repositories.responses.Location;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.PostsFetchResponse;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.webservices.GraphQLRepository;
import awais.instagrabber.webservices.LocationRepository;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.Dispatchers;

public class LocationPostFetchService implements PostFetcher.PostFetchService {
    private final LocationRepository locationRepository;
    private final GraphQLRepository graphQLRepository;
    private final Location locationModel;
    private String nextMaxId;
    private boolean moreAvailable;
    private final boolean isLoggedIn;

    public LocationPostFetchService(Location locationModel, boolean isLoggedIn) {
        this.locationModel = locationModel;
        this.isLoggedIn = isLoggedIn;
        this.locationRepository = isLoggedIn ? LocationRepository.Companion.getInstance() : null;
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
        if (this.isLoggedIn) this.locationRepository.fetchPosts(this.locationModel.getPk(), this.nextMaxId, cb);
        else this.graphQLRepository.fetchLocationPosts(
                this.locationModel.getPk(),
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
