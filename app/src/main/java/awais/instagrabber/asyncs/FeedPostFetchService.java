package awais.instagrabber.asyncs;

import java.util.ArrayList;
import java.util.List;

import awais.instagrabber.customviews.helpers.PostFetcher;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.webservices.FeedRepository;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class FeedPostFetchService implements PostFetcher.PostFetchService {
    private static final String TAG = "FeedPostFetchService";
    private final FeedRepository feedRepository;
    private String nextCursor;
    private boolean hasNextPage;

    public FeedPostFetchService() {
        this.feedRepository = FeedRepository.Companion.getInstance();
    }

    @Override
    public void fetch(FetchListener<List<Media>> fetchListener) {
        List<Media> feedModels = new ArrayList<>();
        String cookie = settingsHelper.getString(Constants.COOKIE);
        String csrfToken = CookieUtils.getCsrfTokenFromCookie(cookie);
        String deviceUuid = settingsHelper.getString(Constants.DEVICE_UUID);
        feedModels.clear();
        this.feedRepository.fetchFeed(csrfToken, deviceUuid, this.nextCursor, CoroutineUtilsKt.getContinuation((result, t) -> {
            if (t != null) {
                if (fetchListener != null) {
                    fetchListener.onFailure(t);
                }
                return;
            }
            if (result == null && feedModels.size() > 0) {
                fetchListener.onResult(feedModels);
                return;
            } else if (result == null) return;
            this.nextCursor = result.getNextCursor();
            this.hasNextPage = result.getHasNextPage();

            List<Media> mediaResults = result.getFeedModels();
            feedModels.addAll(mediaResults);

            if (fetchListener != null) {
                fetchListener.onResult(feedModels);
            }
        }));
    }

    @Override
    public void reset() {
        this.nextCursor = null;
    }

    @Override
    public boolean hasNextPage() {
        return this.hasNextPage;
    }
}
