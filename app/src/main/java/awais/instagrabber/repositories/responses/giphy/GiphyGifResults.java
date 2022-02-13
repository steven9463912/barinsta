package awais.instagrabber.repositories.responses.giphy;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Objects;

public class GiphyGifResults {
    private final List<GiphyGif> giphyGifs;
    private final List<GiphyGif> giphy;

    public GiphyGifResults(List<GiphyGif> giphyGifs, List<GiphyGif> giphy) {
        this.giphyGifs = giphyGifs;
        this.giphy = giphy;
    }

    public List<GiphyGif> getGiphyGifs() {
        return this.giphyGifs;
    }

    public List<GiphyGif> getGiphy() {
        return this.giphy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        GiphyGifResults that = (GiphyGifResults) o;
        return Objects.equals(this.giphyGifs, that.giphyGifs) &&
                Objects.equals(this.giphy, that.giphy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.giphyGifs, this.giphy);
    }

    @NonNull
    @Override
    public String toString() {
        return "GiphyGifResults{" +
                "giphyGifs=" + this.giphyGifs +
                ", giphy=" + this.giphy +
                '}';
    }
}
