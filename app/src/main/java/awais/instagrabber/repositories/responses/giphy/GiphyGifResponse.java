package awais.instagrabber.repositories.responses.giphy;

import androidx.annotation.NonNull;

import java.util.Objects;

public class GiphyGifResponse {
    private final GiphyGifResults results;
    private final String status;

    public GiphyGifResponse(GiphyGifResults results, String status) {
        this.results = results;
        this.status = status;
    }

    public GiphyGifResults getResults() {
        return this.results;
    }

    public String getStatus() {
        return this.status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        GiphyGifResponse that = (GiphyGifResponse) o;
        return Objects.equals(this.results, that.results) &&
                Objects.equals(this.status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.results, this.status);
    }

    @NonNull
    @Override
    public String toString() {
        return "GiphyGifResponse{" +
                "results=" + this.results +
                ", status='" + this.status + '\'' +
                '}';
    }
}
