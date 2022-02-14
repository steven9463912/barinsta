package awais.instagrabber.repositories.responses.giphy;

import androidx.annotation.NonNull;

import java.util.Objects;

import awais.instagrabber.repositories.responses.AnimatedMediaFixedHeight;

public class GiphyGifImages {
    private final AnimatedMediaFixedHeight fixedHeight;

    public GiphyGifImages(AnimatedMediaFixedHeight fixedHeight) {
        this.fixedHeight = fixedHeight;
    }

    public AnimatedMediaFixedHeight getFixedHeight() {
        return this.fixedHeight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        GiphyGifImages that = (GiphyGifImages) o;
        return Objects.equals(this.fixedHeight, that.fixedHeight);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.fixedHeight);
    }

    @NonNull
    @Override
    public String toString() {
        return "GiphyGifImages{" +
                "fixedHeight=" + this.fixedHeight +
                '}';
    }
}
