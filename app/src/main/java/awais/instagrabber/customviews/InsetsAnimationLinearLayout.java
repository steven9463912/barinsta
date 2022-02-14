package awais.instagrabber.customviews;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowInsetsAnimation;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Arrays;

import awais.instagrabber.customviews.helpers.SimpleImeAnimationController;
import awais.instagrabber.utils.ViewUtils;

import static androidx.core.view.ViewCompat.TYPE_TOUCH;

public final class InsetsAnimationLinearLayout extends LinearLayout implements NestedScrollingParent3 {
    private final NestedScrollingParentHelper nestedScrollingParentHelper = new NestedScrollingParentHelper(this);
    private final SimpleImeAnimationController imeAnimController = new SimpleImeAnimationController();
    private final int[] tempIntArray2 = new int[2];
    private final int[] startViewLocation = new int[2];

    private View currentNestedScrollingChild;
    private int dropNextY;
    private boolean scrollImeOffScreenWhenVisible = true;
    private boolean scrollImeOnScreenWhenNotVisible = true;
    private boolean scrollImeOffScreenWhenVisibleOnFling;
    private boolean scrollImeOnScreenWhenNotVisibleOnFling;

    public InsetsAnimationLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public InsetsAnimationLinearLayout(@NonNull final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public final boolean getScrollImeOffScreenWhenVisible() {
        return this.scrollImeOffScreenWhenVisible;
    }

    public final void setScrollImeOffScreenWhenVisible(final boolean scrollImeOffScreenWhenVisible) {
        this.scrollImeOffScreenWhenVisible = scrollImeOffScreenWhenVisible;
    }

    public final boolean getScrollImeOnScreenWhenNotVisible() {
        return this.scrollImeOnScreenWhenNotVisible;
    }

    public final void setScrollImeOnScreenWhenNotVisible(final boolean scrollImeOnScreenWhenNotVisible) {
        this.scrollImeOnScreenWhenNotVisible = scrollImeOnScreenWhenNotVisible;
    }

    public boolean getScrollImeOffScreenWhenVisibleOnFling() {
        return this.scrollImeOffScreenWhenVisibleOnFling;
    }

    public void setScrollImeOffScreenWhenVisibleOnFling(boolean scrollImeOffScreenWhenVisibleOnFling) {
        this.scrollImeOffScreenWhenVisibleOnFling = scrollImeOffScreenWhenVisibleOnFling;
    }

    public boolean getScrollImeOnScreenWhenNotVisibleOnFling() {
        return this.scrollImeOnScreenWhenNotVisibleOnFling;
    }

    public void setScrollImeOnScreenWhenNotVisibleOnFling(boolean scrollImeOnScreenWhenNotVisibleOnFling) {
        this.scrollImeOnScreenWhenNotVisibleOnFling = scrollImeOnScreenWhenNotVisibleOnFling;
    }

    public SimpleImeAnimationController getImeAnimController() {
        return this.imeAnimController;
    }

    @Override
    public boolean onStartNestedScroll(@NonNull View child,
                                       @NonNull View target,
                                       int axes,
                                       int type) {
        return (axes & View.SCROLL_AXIS_VERTICAL) != 0 && type == TYPE_TOUCH;
    }

    @Override
    public void onNestedScrollAccepted(@NonNull View child,
                                       @NonNull View target,
                                       int axes,
                                       int type) {
        this.nestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes, type);
        this.currentNestedScrollingChild = child;
    }

    @Override
    public void onNestedPreScroll(@NonNull View target,
                                  int dx,
                                  int dy,
                                  @NonNull int[] consumed,
                                  int type) {
        if (this.imeAnimController.isInsetAnimationRequestPending()) {
            consumed[0] = dx;
            consumed[1] = dy;
        } else {
            int deltaY = dy;
            if (this.dropNextY != 0) {
                consumed[1] = this.dropNextY;
                deltaY = dy - this.dropNextY;
                this.dropNextY = 0;
            }

            if (deltaY < 0) {
                if (this.imeAnimController.isInsetAnimationInProgress()) {
                    consumed[1] -= this.imeAnimController.insetBy(-deltaY);
                } else if (this.scrollImeOffScreenWhenVisible && !this.imeAnimController.isInsetAnimationRequestPending()) {
                    final WindowInsetsCompat rootWindowInsets = ViewCompat.getRootWindowInsets(this);
                    if (rootWindowInsets != null) {
                        if (rootWindowInsets.isVisible(WindowInsetsCompat.Type.ime())) {
                            this.startControlRequest();
                            consumed[1] = deltaY;
                        }
                    }
                }
            }

        }
    }

    @Override
    public void onNestedScroll(@NonNull View target,
                               int dxConsumed,
                               int dyConsumed,
                               int dxUnconsumed,
                               int dyUnconsumed,
                               int type,
                               @NonNull int[] consumed) {
        if (dyUnconsumed > 0) {
            if (this.imeAnimController.isInsetAnimationInProgress()) {
                consumed[1] = -this.imeAnimController.insetBy(-dyUnconsumed);
            } else if (this.scrollImeOnScreenWhenNotVisible && !this.imeAnimController.isInsetAnimationRequestPending()) {
                final WindowInsetsCompat rootWindowInsets = ViewCompat.getRootWindowInsets(this);
                if (rootWindowInsets != null) {
                    if (!rootWindowInsets.isVisible(WindowInsetsCompat.Type.ime())) {
                        this.startControlRequest();
                        consumed[1] = dyUnconsumed;
                    }
                }
            }
        }

    }

    @Override
    public boolean onNestedFling(@NonNull View target,
                                 float velocityX,
                                 float velocityY,
                                 boolean consumed) {
        if (this.imeAnimController.isInsetAnimationInProgress()) {
            this.imeAnimController.animateToFinish(velocityY);
            return true;
        } else {
            boolean imeVisible = false;
            WindowInsetsCompat rootWindowInsets = ViewCompat.getRootWindowInsets(this);
            if (rootWindowInsets != null && rootWindowInsets.isVisible(WindowInsetsCompat.Type.ime())) {
                imeVisible = true;
            }
            if (velocityY > 0 && this.scrollImeOnScreenWhenNotVisibleOnFling && !imeVisible) {
                this.imeAnimController.startAndFling(this, velocityY);
                return true;
            } else if (velocityY < 0 && this.scrollImeOffScreenWhenVisibleOnFling && imeVisible) {
                this.imeAnimController.startAndFling(this, velocityY);
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public void onStopNestedScroll(@NonNull View target, int type) {
        this.nestedScrollingParentHelper.onStopNestedScroll(target, type);
        if (this.imeAnimController.isInsetAnimationInProgress() && !this.imeAnimController.isInsetAnimationFinishing()) {
            this.imeAnimController.animateToFinish(null);
        }
        this.reset();
    }

    @Override
    public void dispatchWindowInsetsAnimationPrepare(@NonNull WindowInsetsAnimation animation) {
        super.dispatchWindowInsetsAnimationPrepare(animation);
        ViewUtils.suppressLayoutCompat(this, false);
    }

    private void startControlRequest() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return;
        }
        ViewUtils.suppressLayoutCompat(this, true);
        if (this.currentNestedScrollingChild != null) {
            this.currentNestedScrollingChild.getLocationInWindow(this.startViewLocation);
        }
        this.imeAnimController.startControlRequest(this, windowInsetsAnimationControllerCompat -> this.onControllerReady());
    }

    private void onControllerReady() {
        if (this.currentNestedScrollingChild != null) {
            this.imeAnimController.insetBy(0);
            final int[] location = this.tempIntArray2;
            this.currentNestedScrollingChild.getLocationInWindow(location);
            this.dropNextY = location[1] - this.startViewLocation[1];
        }

    }

    private void reset() {
        this.dropNextY = 0;
        Arrays.fill(this.startViewLocation, 0);
        ViewUtils.suppressLayoutCompat(this, false);
    }

    @Override
    public void onNestedScrollAccepted(@NonNull View child,
                                       @NonNull View target,
                                       int axes) {
        this.onNestedScrollAccepted(child, target, axes, TYPE_TOUCH);
    }

    @Override
    public void onNestedScroll(@NonNull View target,
                               int dxConsumed,
                               int dyConsumed,
                               int dxUnconsumed,
                               int dyUnconsumed,
                               int type) {
        this.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, this.tempIntArray2);
    }

    @Override
    public void onStopNestedScroll(@NonNull View target) {
        this.onStopNestedScroll(target, TYPE_TOUCH);
    }
}

