package awais.instagrabber.asyncs;

import java.util.List;

import awais.instagrabber.customviews.helpers.PostFetcher;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.PostsFetchResponse;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.webservices.GraphQLRepository;
import awais.instagrabber.webservices.ProfileRepository;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.Dispatchers;

public class ProfilePostFetchService implements PostFetcher.PostFetchService {
    private static final String TAG = "ProfilePostFetchService";
    private final ProfileRepository profileRepository;
    private final GraphQLRepository graphQLRepository;
    private final User profileModel;
    private final boolean isLoggedIn;
    private String nextMaxId;
    private boolean moreAvailable;

    public ProfilePostFetchService(User profileModel, boolean isLoggedIn) {
        this.profileModel = profileModel;
        this.isLoggedIn = isLoggedIn;
        this.graphQLRepository = isLoggedIn ? null : GraphQLRepository.Companion.getInstance();
        this.profileRepository = isLoggedIn ? ProfileRepository.Companion.getInstance() : null;
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
        if (this.isLoggedIn) this.profileRepository.fetchPosts(this.profileModel.getPk(), this.nextMaxId, cb);
        else this.graphQLRepository.fetchProfilePosts(
                this.profileModel.getPk(),
                30,
                this.nextMaxId,
                this.profileModel,
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
