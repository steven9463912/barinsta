package awais.instagrabber.customviews;

import android.content.Context;
import android.util.AttributeSet;
import android.view.WindowInsets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

public class InsetsNotifyingCoordinatorLayout extends CoordinatorLayout {

    public InsetsNotifyingCoordinatorLayout(@NonNull Context context) {
        super(context);
    }

    public InsetsNotifyingCoordinatorLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public InsetsNotifyingCoordinatorLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
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
