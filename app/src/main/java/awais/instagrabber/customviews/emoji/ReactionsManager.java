package awais.instagrabber.customviews.emoji;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.common.collect.ImmutableList;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.utils.emoji.EmojiParser;

import static awais.instagrabber.utils.Constants.PREF_REACTIONS;

public class ReactionsManager {
    private static final String TAG = ReactionsManager.class.getSimpleName();
    private static final Object LOCK = new Object();

    // private final AppExecutors appExecutors = AppExecutors.INSTANCE;
    private final List<Emoji> reactions = new ArrayList<>();

    private static ReactionsManager instance;

    public static ReactionsManager getInstance(@NonNull Context context) {
        if (ReactionsManager.instance == null) {
            synchronized (ReactionsManager.LOCK) {
                if (ReactionsManager.instance == null) {
                    ReactionsManager.instance = new ReactionsManager(context);
                }
            }
        }
        return ReactionsManager.instance;
    }

    private ReactionsManager(@NonNull Context context) {
        EmojiParser emojiParser = EmojiParser.Companion.getInstance(context);
        String reactionsJson = Utils.settingsHelper.getString(PREF_REACTIONS);
        if (TextUtils.isEmpty(reactionsJson)) {
            ImmutableList<String> list = ImmutableList.of("❤️", "\uD83D\uDE02", "\uD83D\uDE2E", "\uD83D\uDE22", "\uD83D\uDE21", "\uD83D\uDC4D");
            reactionsJson = new JSONArray(list).toString();
        }
        Map<String, Emoji> allEmojis = emojiParser.getAllEmojis();
        try {
            JSONArray reactionsJsonArray = new JSONArray(reactionsJson);
            for (int i = 0; i < reactionsJsonArray.length(); i++) {
                String emojiUnicode = reactionsJsonArray.optString(i);
                if (emojiUnicode == null) continue;
                Emoji emoji = allEmojis.get(emojiUnicode);
                if (emoji == null) continue;
                this.reactions.add(emoji);
            }
        } catch (final JSONException e) {
            Log.e(ReactionsManager.TAG, "ReactionsManager: ", e);
        }
    }

    public List<Emoji> getReactions() {
        return this.reactions;
    }

    // public void setVariant(final String parent, final String variant) {
    //     if (parent == null || variant == null) return;
    //     selectedVariantMap.put(parent, variant);
    //     appExecutors.tasksThread().execute(() -> {
    //         final JSONObject jsonObject = new JSONObject(selectedVariantMap);
    //         final String json = jsonObject.toString();
    //         Utils.settingsHelper.putString(PREF_EMOJI_VARIANTS, json);
    //     });
    // }
}
