package awais.instagrabber.customviews.helpers;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;

import com.google.android.material.behavior.HideBottomViewOnScrollBehavior;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class CustomHideBottomViewOnScrollBehavior extends HideBottomViewOnScrollBehavior<BottomNavigationView> {
    private static final String TAG = "CustomHideBottomView";

    public CustomHideBottomViewOnScrollBehavior() {
    }

    public CustomHideBottomViewOnScrollBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout,
                                       @NonNull BottomNavigationView child,
                                       @NonNull View directTargetChild,
                                       @NonNull View target,
                                       int nestedScrollAxes,
                                       int type) {
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL;
    }

    @Override
    public void onNestedPreScroll(@NonNull CoordinatorLayout coordinatorLayout,
                                  @NonNull BottomNavigationView child,
                                  @NonNull View target,
                                  int dx,
                                  int dy,
                                  @NonNull int[] consumed,
                                  int type) {
        if (dy > 0) {
            this.slideDown(child);
        } else if (dy < 0) {
            this.slideUp(child);
        }
    }
}
