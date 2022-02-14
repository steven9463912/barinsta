package awais.instagrabber.repositories.responses.giphy;

import java.util.Objects;

public class GiphyGifImage {
    private final int height;
    private final int width;
    private final long webpSize;
    private final String webp;

    public GiphyGifImage(int height, int width, long webpSize, String webp) {
        this.height = height;
        this.width = width;
        this.webpSize = webpSize;
        this.webp = webp;
    }

    public int getHeight() {
        return this.height;
    }

    public int getWidth() {
        return this.width;
    }

    public long getWebpSize() {
        return this.webpSize;
    }

    public String getWebp() {
        return this.webp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        GiphyGifImage that = (GiphyGifImage) o;
        return this.height == that.height &&
                this.width == that.width &&
                this.webpSize == that.webpSize &&
                Objects.equals(this.webp, that.webp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.height, this.width, this.webpSize, this.webp);
    }

    @Override
    public String toString() {
        return "GiphyGifImage{" +
                "height=" + this.height +
                ", width=" + this.width +
                ", webpSize=" + this.webpSize +
                ", webp='" + this.webp + '\'' +
                '}';
    }
}
