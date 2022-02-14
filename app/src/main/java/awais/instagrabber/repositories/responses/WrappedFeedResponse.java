package awais.instagrabber.repositories.responses;

import java.util.List;

public class WrappedFeedResponse {
    private final int numResults;
    private final String nextMaxId;
    private final boolean moreAvailable;
    private final String status;
    private final List<WrappedMedia> items;

    public WrappedFeedResponse(int numResults,
                               String nextMaxId,
                               boolean moreAvailable,
                               String status,
                               List<WrappedMedia> items) {
        this.numResults = numResults;
        this.nextMaxId = nextMaxId;
        this.moreAvailable = moreAvailable;
        this.status = status;
        this.items = items;
    }

    public int getNumResults() {
        return this.numResults;
    }

    public String getNextMaxId() {
        return this.nextMaxId;
    }

    public boolean isMoreAvailable() {
        return this.moreAvailable;
    }

    public String getStatus() {
        return this.status;
    }

    public List<WrappedMedia> getItems() {
        return this.items;
    }
}
