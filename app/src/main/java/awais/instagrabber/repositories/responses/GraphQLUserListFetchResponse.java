package awais.instagrabber.repositories.responses;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Objects;

import awais.instagrabber.utils.TextUtils;

public class GraphQLUserListFetchResponse {
    private String nextMaxId;
    private String status;
    private List<User> items;

    public GraphQLUserListFetchResponse(String nextMaxId,
                                        String status,
                                        List<User> items) {
        this.nextMaxId = nextMaxId;
        this.status = status;
        this.items = items;
    }

    public boolean isMoreAvailable() {
        return !TextUtils.isEmpty(this.nextMaxId);
    }

    public String getNextMaxId() {
        return this.nextMaxId;
    }

    public GraphQLUserListFetchResponse setNextMaxId(String nextMaxId) {
        this.nextMaxId = nextMaxId;
        return this;
    }

    public String getStatus() {
        return this.status;
    }

    public GraphQLUserListFetchResponse setStatus(String status) {
        this.status = status;
        return this;
    }

    public List<User> getItems() {
        return this.items;
    }

    public GraphQLUserListFetchResponse setItems(List<User> items) {
        this.items = items;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        GraphQLUserListFetchResponse that = (GraphQLUserListFetchResponse) o;
        return Objects.equals(this.nextMaxId, that.nextMaxId) &&
                Objects.equals(this.status, that.status) &&
                Objects.equals(this.items, that.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.nextMaxId, this.status, this.items);
    }

    @NonNull
    @Override
    public String toString() {
        return "GraphQLUserListFetchResponse{" +
                "nextMaxId='" + this.nextMaxId + '\'' +
                ", status='" + this.status + '\'' +
                ", items=" + this.items +
                '}';
    }
}
