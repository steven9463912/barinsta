package awais.instagrabber.repositories.responses.giphy;

import androidx.annotation.NonNull;

import java.util.Objects;

public class GiphyGif {
    private final String type;
    private final String id;
    private final String title;
    private final int isSticker;
    private final GiphyGifImages images;

    public GiphyGif(String type, String id, String title, int isSticker, GiphyGifImages images) {
        this.type = type;
        this.id = id;
        this.title = title;
        this.isSticker = isSticker;
        this.images = images;
    }

    public String getType() {
        return this.type;
    }

    public String getId() {
        return this.id;
    }

    public String getTitle() {
        return this.title;
    }

    public boolean isSticker() {
        return this.isSticker ==  1;
    }

    public GiphyGifImages getImages() {
        return this.images;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        GiphyGif giphyGif = (GiphyGif) o;
        return this.isSticker == giphyGif.isSticker &&
                Objects.equals(this.type, giphyGif.type) &&
                Objects.equals(this.id, giphyGif.id) &&
                Objects.equals(this.title, giphyGif.title) &&
                Objects.equals(this.images, giphyGif.images);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.type, this.id, this.title, this.isSticker, this.images);
    }

    @NonNull
    @Override
    public String toString() {
        return "GiphyGif{" +
                "type='" + this.type + '\'' +
                ", id='" + this.id + '\'' +
                ", title='" + this.title + '\'' +
                ", isSticker=" + this.isSticker() +
                ", images=" + this.images +
                '}';
    }
}
