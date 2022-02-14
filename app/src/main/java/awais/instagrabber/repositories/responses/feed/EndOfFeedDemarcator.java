package awais.instagrabber.repositories.responses.feed;

import java.io.Serializable;
import java.util.Objects;

public class EndOfFeedDemarcator implements Serializable {
    private final long id;
    private final EndOfFeedGroupSet groupSet;

    public EndOfFeedDemarcator(long id, EndOfFeedGroupSet groupSet) {
        this.id = id;
        this.groupSet = groupSet;
    }

    public long getId() {
        return this.id;
    }

    public EndOfFeedGroupSet getGroupSet() {
        return this.groupSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        EndOfFeedDemarcator that = (EndOfFeedDemarcator) o;
        return this.id == that.id &&
                Objects.equals(this.groupSet, that.groupSet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.groupSet);
    }
}
