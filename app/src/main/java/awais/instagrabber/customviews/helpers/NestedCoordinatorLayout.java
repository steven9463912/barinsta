package awais.instagrabber.customviews.helpers;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.NestedScrollingChild;
import androidx.core.view.NestedScrollingChildHelper;

public class NestedCoordinatorLayout extends CoordinatorLayout implements NestedScrollingChild {

    private final NestedScrollingChildHelper mChildHelper;

    public NestedCoordinatorLayout(final Context context) {
        super(context);
        this.mChildHelper = new NestedScrollingChildHelper(this);
        this.setNestedScrollingEnabled(true);
    }

    public NestedCoordinatorLayout(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        this.mChildHelper = new NestedScrollingChildHelper(this);
        this.setNestedScrollingEnabled(true);
    }

    public NestedCoordinatorLayout(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mChildHelper = new NestedScrollingChildHelper(this);
        this.setNestedScrollingEnabled(true);
    }

    @Override
    public void onNestedPreScroll(final View target, final int dx, final int dy, final int[] consumed, final int type) {
        final int[][] tConsumed = new int[2][2];
        super.onNestedPreScroll(target, dx, dy, consumed, type);
        this.dispatchNestedPreScroll(dx, dy, tConsumed[1], null);
        consumed[0] = tConsumed[0][0] + tConsumed[1][0];
        consumed[1] = tConsumed[0][1] + tConsumed[1][1];
    }

    @Override
    public void onNestedScroll(final View target, final int dxConsumed, final int dyConsumed, final int dxUnconsumed, final int dyUnconsumed, final int type) {
        super.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type);
        this.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, null);
    }

    @Override
    public void onStopNestedScroll(final View target, final int type) {
        /* Disable the scrolling behavior of our own children */
        super.onStopNestedScroll(target, type);
        /* Disable the scrolling behavior of the parent's other children  */
        this.stopNestedScroll();
    }

    @Override
    public boolean onStartNestedScroll(final View child, final View target, final int nestedScrollAxes, final int type) {
        /* Enable the scrolling behavior of our own children */
        final boolean tHandled = super.onStartNestedScroll(child, target, nestedScrollAxes, type);
        /* Enable the scrolling behavior of the parent's other children  */
        return this.startNestedScroll(nestedScrollAxes) || tHandled;
    }

    @Override
    public boolean onStartNestedScroll(final View child, final View target, final int nestedScrollAxes) {
        /* Enable the scrolling behavior of our own children */
        final boolean tHandled = super.onStartNestedScroll(child, target, nestedScrollAxes);
        /* Enable the scrolling behavior of the parent's other children  */
        return this.startNestedScroll(nestedScrollAxes) || tHandled;
    }

    @Override
    public void onStopNestedScroll(final View target) {
        /* Disable the scrolling behavior of our own children */
        super.onStopNestedScroll(target);
        /* Disable the scrolling behavior of the parent's other children  */
        this.stopNestedScroll();
    }

    @Override
    public void onNestedPreScroll(final View target, final int dx, final int dy, final int[] consumed) {
        final int[][] tConsumed = new int[2][2];
        super.onNestedPreScroll(target, dx, dy, tConsumed[0]);
        this.dispatchNestedPreScroll(dx, dy, tConsumed[1], null);
        consumed[0] = tConsumed[0][0] + tConsumed[1][0];
        consumed[1] = tConsumed[0][1] + tConsumed[1][1];
    }

    @Override
    public void onNestedScroll(final View target, final int dxConsumed, final int dyConsumed,
                               final int dxUnconsumed, final int dyUnconsumed) {
        super.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
        this.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, null);
    }

    @Override
    public boolean onNestedPreFling(final View target, final float velocityX, final float velocityY) {
        final boolean tHandled = super.onNestedPreFling(target, velocityX, velocityY);
        return this.dispatchNestedPreFling(velocityX, velocityY) || tHandled;
    }

    @Override
    public boolean onNestedFling(final View target, final float velocityX, final float velocityY, final boolean consumed) {
        final boolean tHandled = super.onNestedFling(target, velocityX, velocityY, consumed);
        return this.dispatchNestedFling(velocityX, velocityY, consumed) || tHandled;
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return this.mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public void setNestedScrollingEnabled(final boolean enabled) {
        this.mChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean startNestedScroll(final int axes) {
        return this.mChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        this.mChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return this.mChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(final int dxConsumed, final int dyConsumed, final int dxUnconsumed,
                                        final int dyUnconsumed, final int[] offsetInWindow) {
        return this.mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed,
                dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(final int dx, final int dy, final int[] consumed, final int[] offsetInWindow) {
        return this.mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(final float velocityX, final float velocityY, final boolean consumed) {
        return this.mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(final float velocityX, final float velocityY) {
        return this.mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }
}
