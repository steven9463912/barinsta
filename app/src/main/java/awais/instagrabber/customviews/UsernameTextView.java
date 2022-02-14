package awais.instagrabber.customviews;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatTextView;

import awais.instagrabber.R;
import awais.instagrabber.utils.Utils;

public class UsernameTextView extends AppCompatTextView {
    private static final String TAG = UsernameTextView.class.getSimpleName();

    private final int drawableSize = Utils.convertDpToPx(24);

    private boolean verified;
    private VerticalImageSpan verifiedSpan;

    public UsernameTextView(@NonNull Context context) {
        this(context, null);
    }

    public UsernameTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UsernameTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init();
    }

    private void init() {
        try {
            Drawable verifiedDrawable = AppCompatResources.getDrawable(this.getContext(), R.drawable.verified);
            Drawable drawable = verifiedDrawable.mutate();
            drawable.setBounds(0, 0, this.drawableSize, this.drawableSize);
            this.verifiedSpan = new VerticalImageSpan(drawable);
        } catch (final Exception e) {
            Log.e(UsernameTextView.TAG, "init: ", e);
        }
    }

    public void setUsername(CharSequence username) {
        this.setUsername(username, false);
    }

    public void setUsername(CharSequence username, boolean verified) {
        this.verified = verified;
        SpannableStringBuilder sb = new SpannableStringBuilder(username);
        if (verified) {
            try {
                if (this.verifiedSpan != null) {
                    sb.append("  ");
                    sb.setSpan(this.verifiedSpan, sb.length() - 1, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } catch (final Exception e) {
                Log.e(UsernameTextView.TAG, "bind: ", e);
            }
        }
        setText(sb);
    }

    public boolean isVerified() {
        return this.verified;
    }

    public void setVerified(boolean verified) {
        this.setUsername(this.getText(), verified);
    }
}
