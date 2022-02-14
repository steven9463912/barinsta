package awais.instagrabber.customviews.emoji;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import java.util.Map;
import java.util.Objects;

import awais.instagrabber.R;

public class EmojiCategory {
    private final EmojiCategoryType type;
    private final Map<String, Emoji> emojis;
    @DrawableRes
    private int drawableRes;

    public EmojiCategory(EmojiCategoryType type, Map<String, Emoji> emojis) {
        this.type = type;
        this.emojis = emojis;
    }

    public EmojiCategoryType getType() {
        return this.type;
    }

    public Map<String, Emoji> getEmojis() {
        return this.emojis;
    }

    public int getDrawableRes() {
        if (this.drawableRes == 0) {
            switch (this.type) {
                case SMILEYS_AND_EMOTION:
                    this.drawableRes = R.drawable.ic_round_emoji_emotions_24;
                    break;
                case ANIMALS_AND_NATURE:
                    this.drawableRes = R.drawable.ic_round_emoji_nature_24;
                    break;
                case FOOD_AND_DRINK:
                    this.drawableRes = R.drawable.ic_round_emoji_food_beverage_24;
                    break;
                case TRAVEL_AND_PLACES:
                    this.drawableRes = R.drawable.ic_round_emoji_transportation_24;
                    break;
                case ACTIVITIES:
                    this.drawableRes = R.drawable.ic_round_emoji_events_24;
                    break;
                case OBJECTS:
                    this.drawableRes = R.drawable.ic_round_emoji_objects_24;
                    break;
                case SYMBOLS:
                    this.drawableRes = R.drawable.ic_round_emoji_symbols_24;
                    break;
                case FLAGS:
                    this.drawableRes = R.drawable.ic_round_emoji_flags_24;
                    break;
                case OTHERS:
                    this.drawableRes = R.drawable.ic_round_unknown_24;
                    break;
            }
        }
        return this.drawableRes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        EmojiCategory that = (EmojiCategory) o;
        return this.type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.type);
    }

    @NonNull
    @Override
    public String toString() {
        return "EmojiCategory{" +
                "type=" + this.type +
                ", emojis=" + this.emojis +
                '}';
    }
}
