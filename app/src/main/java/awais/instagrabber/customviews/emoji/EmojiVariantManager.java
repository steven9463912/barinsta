package awais.instagrabber.customviews.emoji;

import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Constants.PREF_EMOJI_VARIANTS;

public class EmojiVariantManager {
    private static final String TAG = EmojiVariantManager.class.getSimpleName();
    private static final Object LOCK = new Object();

    private final AppExecutors appExecutors = AppExecutors.INSTANCE;
    private final Map<String, String> selectedVariantMap = new HashMap<>();

    private static EmojiVariantManager instance;

    public static EmojiVariantManager getInstance() {
        if (EmojiVariantManager.instance == null) {
            synchronized (EmojiVariantManager.LOCK) {
                if (EmojiVariantManager.instance == null) {
                    EmojiVariantManager.instance = new EmojiVariantManager();
                }
            }
        }
        return EmojiVariantManager.instance;
    }

    private EmojiVariantManager() {
        String variantsJson = Utils.settingsHelper.getString(PREF_EMOJI_VARIANTS);
        if (TextUtils.isEmpty(variantsJson)) return;
        try {
            JSONObject variantsJSONObject = new JSONObject(variantsJson);
            Iterator<String> keys = variantsJSONObject.keys();
            keys.forEachRemaining(s -> this.selectedVariantMap.put(s, variantsJSONObject.optString(s)));
        } catch (final JSONException e) {
            Log.e(EmojiVariantManager.TAG, "EmojiVariantManager: ", e);
        }
    }

    @Nullable
    public String getVariant(String parentUnicode) {
        return this.selectedVariantMap.get(parentUnicode);
    }

    public void setVariant(String parent, String variant) {
        if (parent == null || variant == null) return;
        this.selectedVariantMap.put(parent, variant);
        this.appExecutors.getTasksThread().execute(() -> {
            JSONObject jsonObject = new JSONObject(this.selectedVariantMap);
            String json = jsonObject.toString();
            Utils.settingsHelper.putString(PREF_EMOJI_VARIANTS, json);
        });
    }
}
