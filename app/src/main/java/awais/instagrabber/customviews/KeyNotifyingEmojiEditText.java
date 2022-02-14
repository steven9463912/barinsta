package awais.instagrabber.customviews;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;

import androidx.emoji.widget.EmojiEditText;

public class KeyNotifyingEmojiEditText extends EmojiEditText {
    private OnKeyEventListener onKeyEventListener;

    public KeyNotifyingEmojiEditText(Context context) {
        super(context);
    }

    public KeyNotifyingEmojiEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public KeyNotifyingEmojiEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public KeyNotifyingEmojiEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (this.onKeyEventListener != null) {
            boolean listenerResult = this.onKeyEventListener.onKeyPreIme(keyCode, event);
            if (listenerResult) return true;
        }
        return super.onKeyPreIme(keyCode, event);
    }

    public void setOnKeyEventListener(OnKeyEventListener onKeyEventListener) {
        this.onKeyEventListener = onKeyEventListener;
    }

    public interface OnKeyEventListener {
        boolean onKeyPreIme(int keyCode, KeyEvent keyEvent);
    }
}
