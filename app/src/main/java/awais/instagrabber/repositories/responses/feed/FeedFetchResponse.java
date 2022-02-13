package awais.instagrabber.repositories.responses.feed;

import java.util.List;

import awais.instagrabber.repositories.responses.Media;

public class FeedFetchResponse {
    private final List<Media> items;
    private final int numResults;
    private final boolean moreAvailable;
    private final String nextMaxId;
    private final String status;

    public FeedFetchResponse(List<Media> items,
                             int numResults,
                             boolean moreAvailable,
                             String nextMaxId,
                             String status) {
        this.items = items;
        this.numResults = numResults;
        this.moreAvailable = moreAvailable;
        this.nextMaxId = nextMaxId;
        this.status = status;
    }

    public List<Media> getItems() {
        return this.items;
    }

    public int getNumResults() {
        return this.numResults;
    }

    public boolean isMoreAvailable() {
        return this.moreAvailable;
    }

    public String getNextMaxId() {
        return this.nextMaxId;
    }

    public String getStatus() {
        return this.status;
    }
}
