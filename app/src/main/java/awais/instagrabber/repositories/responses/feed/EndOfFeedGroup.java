package awais.instagrabber.repositories.responses.feed;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import awais.instagrabber.repositories.responses.Media;

public class EndOfFeedGroup implements Serializable {
    private final String id;
    private final String title;
    private final String nextMaxId;
    private final List<Media> feedItems;

    public EndOfFeedGroup(String id, String title, String nextMaxId, List<Media> feedItems) {
        this.id = id;
        this.title = title;
        this.nextMaxId = nextMaxId;
        this.feedItems = feedItems;
    }

    public String getId() {
        return this.id;
    }

    public String getTitle() {
        return this.title;
    }

    public String getNextMaxId() {
        return this.nextMaxId;
    }

    public List<Media> getFeedItems() {
        return this.feedItems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        EndOfFeedGroup that = (EndOfFeedGroup) o;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.title, that.title) &&
                Objects.equals(this.nextMaxId, that.nextMaxId) &&
                Objects.equals(this.feedItems, that.feedItems);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.title, this.nextMaxId, this.feedItems);
    }
}
