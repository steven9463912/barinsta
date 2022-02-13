package awais.instagrabber.customviews;

import android.content.Context;
import android.text.InputFilter;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.emoji.widget.EmojiTextViewHelper;

import java.util.ArrayList;
import java.util.List;

import io.github.armcha.autolink.AutoLinkItem;
import io.github.armcha.autolink.AutoLinkTextView;
import io.github.armcha.autolink.MODE_EMAIL;
import io.github.armcha.autolink.MODE_HASHTAG;
import io.github.armcha.autolink.MODE_MENTION;
import io.github.armcha.autolink.MODE_URL;
import io.github.armcha.autolink.Mode;

public class RamboTextViewV2 extends AutoLinkTextView {
    private final List<OnMentionClickListener> onMentionClickListeners = new ArrayList<>();
    private final List<OnHashtagClickListener> onHashtagClickListeners = new ArrayList<>();
    private final List<OnURLClickListener> onURLClickListeners = new ArrayList<>();
    private final List<OnEmailClickListener> onEmailClickListeners = new ArrayList<>();

    private EmojiTextViewHelper emojiTextViewHelper;

    public RamboTextViewV2(@NonNull Context context,
                           @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.init();
    }

    private void init() {
        this.getEmojiTextViewHelper().updateTransformationMethod();
        this.addAutoLinkMode(MODE_HASHTAG.INSTANCE, MODE_MENTION.INSTANCE, MODE_EMAIL.INSTANCE, MODE_URL.INSTANCE);
        this.onAutoLinkClick(autoLinkItem -> {
            Mode mode = autoLinkItem.getMode();
            if (mode.equals(MODE_MENTION.INSTANCE)) {
                for (OnMentionClickListener onMentionClickListener : this.onMentionClickListeners) {
                    onMentionClickListener.onMentionClick(autoLinkItem);
                }
                return;
            }
            if (mode.equals(MODE_HASHTAG.INSTANCE)) {
                for (OnHashtagClickListener onHashtagClickListener : this.onHashtagClickListeners) {
                    onHashtagClickListener.onHashtagClick(autoLinkItem);
                }
                return;
            }
            if (mode.equals(MODE_URL.INSTANCE)) {
                for (OnURLClickListener onURLClickListener : this.onURLClickListeners) {
                    onURLClickListener.onURLClick(autoLinkItem);
                }
                return;
            }
            if (mode.equals(MODE_EMAIL.INSTANCE)) {
                for (OnEmailClickListener onEmailClickListener : this.onEmailClickListeners) {
                    onEmailClickListener.onEmailClick(autoLinkItem);
                }
            }
        });
        this.onAutoLinkLongClick(autoLinkItem -> {});
    }

    @Override
    public void setFilters(final InputFilter[] filters) {
        super.setFilters(this.getEmojiTextViewHelper().getFilters(filters));
    }

    @Override
    public void setAllCaps(final boolean allCaps) {
        super.setAllCaps(allCaps);
        this.getEmojiTextViewHelper().setAllCaps(allCaps);
    }


    private EmojiTextViewHelper getEmojiTextViewHelper() {
        if (this.emojiTextViewHelper == null) {
            this.emojiTextViewHelper = new EmojiTextViewHelper(this);
        }
        return this.emojiTextViewHelper;
    }

    public void addOnMentionClickListener(OnMentionClickListener onMentionClickListener) {
        if (onMentionClickListener == null) {
            return;
        }
        this.onMentionClickListeners.add(onMentionClickListener);
    }

    public void removeOnMentionClickListener(OnMentionClickListener onMentionClickListener) {
        if (onMentionClickListener == null) {
            return;
        }
        this.onMentionClickListeners.remove(onMentionClickListener);
    }

    public void clearOnMentionClickListeners() {
        this.onMentionClickListeners.clear();
    }

    public void addOnHashtagListener(OnHashtagClickListener onHashtagClickListener) {
        if (onHashtagClickListener == null) {
            return;
        }
        this.onHashtagClickListeners.add(onHashtagClickListener);
    }

    public void removeOnHashtagListener(OnHashtagClickListener onHashtagClickListener) {
        if (onHashtagClickListener == null) {
            return;
        }
        this.onHashtagClickListeners.remove(onHashtagClickListener);
    }

    public void clearOnHashtagClickListeners() {
        this.onHashtagClickListeners.clear();
    }

    public void addOnURLClickListener(OnURLClickListener onURLClickListener) {
        if (onURLClickListener == null) {
            return;
        }
        this.onURLClickListeners.add(onURLClickListener);
    }

    public void removeOnURLClickListener(OnURLClickListener onURLClickListener) {
        if (onURLClickListener == null) {
            return;
        }
        this.onURLClickListeners.remove(onURLClickListener);
    }

    public void clearOnURLClickListeners() {
        this.onURLClickListeners.clear();
    }

    public void addOnEmailClickListener(OnEmailClickListener onEmailClickListener) {
        if (onEmailClickListener == null) {
            return;
        }
        this.onEmailClickListeners.add(onEmailClickListener);
    }

    public void removeOnEmailClickListener(OnEmailClickListener onEmailClickListener) {
        if (onEmailClickListener == null) {
            return;
        }
        this.onEmailClickListeners.remove(onEmailClickListener);
    }

    public void clearOnEmailClickListeners() {
        this.onEmailClickListeners.clear();
    }

    public void clearAllAutoLinkListeners() {
        this.clearOnMentionClickListeners();
        this.clearOnHashtagClickListeners();
        this.clearOnURLClickListeners();
        this.clearOnEmailClickListeners();
    }

    public interface OnMentionClickListener {
        void onMentionClick(AutoLinkItem autoLinkItem);
    }

    public interface OnHashtagClickListener {
        void onHashtagClick(AutoLinkItem autoLinkItem);
    }

    public interface OnURLClickListener {
        void onURLClick(AutoLinkItem autoLinkItem);
    }

    public interface OnEmailClickListener {
        void onEmailClick(AutoLinkItem autoLinkItem);
    }
}
