package awais.instagrabber.asyncs;

import java.util.List;

import awais.instagrabber.customviews.helpers.PostFetcher;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.enums.PostItemType;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.PostsFetchResponse;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.webservices.GraphQLRepository;
import awais.instagrabber.webservices.ProfileRepository;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.Dispatchers;

public class SavedPostFetchService implements PostFetcher.PostFetchService {
    private final ProfileRepository profileRepository;
    private final GraphQLRepository graphQLRepository;
    private final long profileId;
    private final PostItemType type;
    private final boolean isLoggedIn;

    private String nextMaxId;
    private final String collectionId;
    private boolean moreAvailable;

    public SavedPostFetchService(long profileId, PostItemType type, boolean isLoggedIn, String collectionId) {
        this.profileId = profileId;
        this.type = type;
        this.isLoggedIn = isLoggedIn;
        this.collectionId = collectionId;
        this.graphQLRepository = isLoggedIn ? null : GraphQLRepository.Companion.getInstance();
        this.profileRepository = isLoggedIn ? ProfileRepository.Companion.getInstance() : null;
    }

    @Override
    public void fetch(FetchListener<List<Media>> fetchListener) {
        Continuation<PostsFetchResponse> callback = CoroutineUtilsKt.getContinuation((result, t) -> {
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
        switch (this.type) {
            case LIKED:
                this.profileRepository.fetchLiked(this.nextMaxId, callback);
                break;
            case TAGGED:
                if (this.isLoggedIn) this.profileRepository.fetchTagged(this.profileId, this.nextMaxId, callback);
                else this.graphQLRepository.fetchTaggedPosts(
                        this.profileId,
                        30,
                        this.nextMaxId,
                        callback
                );
                break;
            case COLLECTION:
            case SAVED:
                this.profileRepository.fetchSaved(this.nextMaxId, this.collectionId, callback);
                break;
        }
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
