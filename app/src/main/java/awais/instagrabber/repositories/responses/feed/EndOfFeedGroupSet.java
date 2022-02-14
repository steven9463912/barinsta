package awais.instagrabber.repositories.responses.feed;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class EndOfFeedGroupSet implements Serializable {
    private final long id;
    private final String activeGroupId;
    private final String connectedGroupId;
    private final String nextMaxId;
    private final String paginationSource;
    private final List<EndOfFeedGroup> groups;

    public EndOfFeedGroupSet(long id,
                             String activeGroupId,
                             String connectedGroupId,
                             String nextMaxId,
                             String paginationSource,
                             List<EndOfFeedGroup> groups) {
        this.id = id;
        this.activeGroupId = activeGroupId;
        this.connectedGroupId = connectedGroupId;
        this.nextMaxId = nextMaxId;
        this.paginationSource = paginationSource;
        this.groups = groups;
    }

    public long getId() {
        return this.id;
    }

    public String getActiveGroupId() {
        return this.activeGroupId;
    }

    public String getConnectedGroupId() {
        return this.connectedGroupId;
    }

    public String getNextMaxId() {
        return this.nextMaxId;
    }

    public String getPaginationSource() {
        return this.paginationSource;
    }

    public List<EndOfFeedGroup> getGroups() {
        return this.groups;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        EndOfFeedGroupSet that = (EndOfFeedGroupSet) o;
        return this.id == that.id &&
                Objects.equals(this.activeGroupId, that.activeGroupId) &&
                Objects.equals(this.connectedGroupId, that.connectedGroupId) &&
                Objects.equals(this.nextMaxId, that.nextMaxId) &&
                Objects.equals(this.paginationSource, that.paginationSource) &&
                Objects.equals(this.groups, that.groups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.activeGroupId, this.connectedGroupId, this.nextMaxId, this.paginationSource, this.groups);
    }
}
