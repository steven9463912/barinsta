package awais.instagrabber.customviews.emoji;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Objects;

public class Emoji {
    private final String unicode;
    private final String name;
    private final List<Emoji> variants;
    private GoogleCompatEmojiDrawable drawable;

    public Emoji(String unicode,
                 String name,
                 List<Emoji> variants) {
        this.unicode = unicode;
        this.name = name;
        this.variants = variants;
    }

    public String getUnicode() {
        return this.unicode;
    }

    public void addVariant(Emoji emoji) {
        this.variants.add(emoji);
    }

    public String getName() {
        return this.name;
    }

    public List<Emoji> getVariants() {
        return this.variants;
    }

    public GoogleCompatEmojiDrawable getDrawable() {
        if (this.drawable == null && this.unicode != null) {
            this.drawable = new GoogleCompatEmojiDrawable(this.unicode);
        }
        return this.drawable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        Emoji emoji = (Emoji) o;
        return Objects.equals(this.unicode, emoji.unicode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.unicode);
    }

    @NonNull
    @Override
    public String toString() {
        return "Emoji{" +
                "unicode='" + this.unicode + '\'' +
                ", name='" + this.name + '\'' +
                ", variants=" + this.variants +
                '}';
    }
}
