package awais.instagrabber.customviews.emoji;

import java.util.HashMap;
import java.util.Map;

public enum EmojiCategoryType {
    SMILEYS_AND_EMOTION("Smileys & Emotion"),
    // PEOPLE_AND_BODY("People & Body"),
    ANIMALS_AND_NATURE("Animals & Nature"),
    FOOD_AND_DRINK("Food & Drink"),
    TRAVEL_AND_PLACES("Travel & Places"),
    ACTIVITIES("Activities"),
    OBJECTS("Objects"),
    SYMBOLS("Symbols"),
    FLAGS("Flags"),
    OTHERS("Others");

    private final String name;

    private static final Map<String, EmojiCategoryType> map = new HashMap<>();

    static {
        for (final EmojiCategoryType type : values()) {
            EmojiCategoryType.map.put(type.name, type);
        }
    }

    EmojiCategoryType(String name) {
        this.name = name;
    }

    public static EmojiCategoryType valueOfName(String name) {
        EmojiCategoryType emojiCategoryType = EmojiCategoryType.map.get(name);
        if (emojiCategoryType == null) {
            return OTHERS;
        }
        return emojiCategoryType;
    }

    public String getName() {
        return this.name;
    }
}
