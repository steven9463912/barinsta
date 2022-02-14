package awais.instagrabber.customviews;

import android.content.Context;
import android.util.AttributeSet;
import android.view.WindowInsets;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

public class InsetsNotifyingLinearLayout extends LinearLayout {
    public InsetsNotifyingLinearLayout(Context context) {
        super(context);
    }

    public InsetsNotifyingLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public InsetsNotifyingLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public InsetsNotifyingLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public WindowInsets onApplyWindowInsets(final WindowInsets insets) {
        final int childCount = this.getChildCount();
        for (int index = 0; index < childCount; index++) {
            this.getChildAt(index).dispatchApplyWindowInsets(insets);
        }
        return insets;
    }
}
