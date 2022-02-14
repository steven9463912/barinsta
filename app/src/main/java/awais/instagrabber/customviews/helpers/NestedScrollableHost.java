package awais.instagrabber.customviews.helpers;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import static androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL;

public class NestedScrollableHost extends FrameLayout {

    private final int touchSlop;
    private float initialX;
    private float initialY;

    public NestedScrollableHost(@NonNull Context context) {
        this(context, null);
    }

    public NestedScrollableHost(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        this.handleInterceptTouchEvent(ev);
        return super.onInterceptTouchEvent(ev);
    }

    private void handleInterceptTouchEvent(MotionEvent e) {
        if (this.getParentViewPager() == null) return;
        int orientation = this.getParentViewPager().getOrientation();
        // Early return if child can't scroll in same direction as parent
        if (!this.canChildScroll(orientation, -1f) && !this.canChildScroll(orientation, 1f)) return;

        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            this.initialX = e.getX();
            this.initialY = e.getY();
            this.getParent().requestDisallowInterceptTouchEvent(true);
        } else if (e.getAction() == MotionEvent.ACTION_MOVE) {
            float dx = e.getX() - this.initialX;
            float dy = e.getY() - this.initialY;
            boolean isVpHorizontal = orientation == ORIENTATION_HORIZONTAL;

            // assuming ViewPager2 touch-slop is 2x touch-slop of child
            float scaledDx = Math.abs(dx) * (isVpHorizontal ? .5f : 1f);
            float scaledDy = Math.abs(dy) * (isVpHorizontal ? 1f : .5f);

            if (scaledDx > this.touchSlop || scaledDy > this.touchSlop) {
                if (isVpHorizontal == (scaledDy > scaledDx)) {
                    // Gesture is perpendicular, allow all parents to intercept
                    this.getParent().requestDisallowInterceptTouchEvent(false);
                } else {
                    // Gesture is parallel, query child if movement in that direction is possible
                    // Child can scroll, disallow all parents to intercept
                    // Child cannot scroll, allow all parents to intercept
                    this.getParent().requestDisallowInterceptTouchEvent(this.canChildScroll(orientation, (isVpHorizontal ? dx : dy)));
                }
            }
        }
    }

    private boolean canChildScroll(int orientation, float delta) {
        int direction = -(int) Math.signum(delta);
        View child = this.getChild();
        if (child == null) return false;
        ViewPager2 viewPagerChild = null;
        if (child instanceof ViewPager2) {
            viewPagerChild = (ViewPager2) child;
        }

        final boolean canScroll;
        switch (orientation) {
            case 0:
                canScroll = child.canScrollHorizontally(direction);
                break;
            case 1:
                canScroll = child.canScrollVertically(direction);
                break;
            default:
                throw new IllegalArgumentException();
        }
        if (!canScroll || viewPagerChild == null || viewPagerChild.getAdapter() == null)
            return canScroll;
        // check if viewpager has reached its limits and decide accordingly
        return (direction < 0 && viewPagerChild.getCurrentItem() > 0)
                || (direction > 0 && viewPagerChild.getCurrentItem() < viewPagerChild.getAdapter().getItemCount() - 1);
    }

    public ViewPager2 getParentViewPager() {
        View v = (View) this.getParent();
        while (v != null && !(v instanceof ViewPager2)) {
            v = (View) v.getParent();
        }
        return (ViewPager2) v;
    }

    public View getChild() {
        return this.getChildCount() > 0 ? this.getChildAt(0) : null;
    }
}
