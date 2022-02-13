package awais.instagrabber.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.emoji.widget.EmojiAppCompatTextView;

import java.util.List;
import java.util.stream.Collectors;

public class ReactionEmojiTextView extends EmojiAppCompatTextView {
    private static final String TAG = ReactionEmojiTextView.class.getSimpleName();

    private final SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();

    private String count = "";
    private SpannableString ellipsisSpannable;
    private String distinctEmojis;

    public ReactionEmojiTextView(Context context) {
        super(context);
        this.init();
    }

    public ReactionEmojiTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.init();
    }

    public ReactionEmojiTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init();
    }

    private void init() {
        this.ellipsisSpannable = new SpannableString(this.count);
    }

    @SuppressLint("SetTextI18n")
    public void setEmojis(@NonNull List<String> emojis) {
        this.count = String.valueOf(emojis.size());
        this.distinctEmojis = emojis.stream()
                               .distinct()
                               .collect(Collectors.joining());
        this.ellipsisSpannable = new SpannableString(this.count);
        this.setText(this.distinctEmojis + (emojis.size() > 1 ? this.count : ""));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        CharSequence text = this.getText();
        if (text == null) return;
        int measuredWidth = this.getMeasuredWidth();
        float availableTextWidth = measuredWidth - this.getCompoundPaddingLeft() - this.getCompoundPaddingRight();
        CharSequence ellipsizedText = TextUtils.ellipsize(text, this.getPaint(), availableTextWidth, this.getEllipsize());
        if (!ellipsizedText.toString().equals(text.toString())) {
            // If the ellipsizedText is different than the original text, this means that it didn't fit and got indeed ellipsized.
            // Calculate the new availableTextWidth by taking into consideration the size of the custom ellipsis, too.
            availableTextWidth = (availableTextWidth - this.getPaint().measureText(this.count));
            ellipsizedText = TextUtils.ellipsize(text, this.getPaint(), availableTextWidth, this.getEllipsize());
            int defaultEllipsisStart = ellipsizedText.toString().indexOf(this.getDefaultEllipsis());
            int defaultEllipsisEnd = defaultEllipsisStart + 1;
            this.spannableStringBuilder.clear();
            // Update the text with the ellipsized version and replace the default ellipsis with the custom one.
            SpannableStringBuilder replace = this.spannableStringBuilder.append(ellipsizedText)
                                                                         .replace(defaultEllipsisStart, defaultEllipsisEnd, this.ellipsisSpannable);
            this.setText(replace);
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private char getDefaultEllipsis() {
        return '…';
    }

}
