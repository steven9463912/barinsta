package awais.instagrabber.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.customview.view.AbsSavedState;
import androidx.customview.widget.ViewDragHelper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import awais.instagrabber.BuildConfig;

// exactly same as the LayoutDrawer with some edits
@SuppressLint("RtlHardcoded")
public class MouseDrawer extends ViewGroup {
    @IntDef({ViewDragHelper.STATE_IDLE, ViewDragHelper.STATE_DRAGGING, ViewDragHelper.STATE_SETTLING})
    @Retention(RetentionPolicy.SOURCE)
    private @interface State {}

    @IntDef(value = {Gravity.NO_GRAVITY, Gravity.LEFT, Gravity.RIGHT, GravityCompat.START, GravityCompat.END}, flag = true)
    @Retention(RetentionPolicy.SOURCE)
    public @interface EdgeGravity {}

    ////////////////////////////////////////////////////////////////////////////////////
    private static final boolean CHILDREN_DISALLOW_INTERCEPT = true;
    ////////////////////////////////////////////////////////////////////////////////////
    private final ArrayList<View> mNonDrawerViews = new ArrayList<>();
    private final ViewDragHelper mLeftDragger, mRightDragger;
    private boolean mInLayout, mFirstLayout = true;
    private float mDrawerElevation, mInitialMotionX, mInitialMotionY;
    private int mDrawerState;
    private List<DrawerListener> mListeners;
    private Matrix mChildInvertedMatrix;
    private Rect mChildHitRect;

    public interface DrawerListener {
        void onDrawerSlide(View drawerView, @EdgeGravity int gravity, float slideOffset);
        default void onDrawerOpened(View drawerView, @EdgeGravity int gravity) {}
        default void onDrawerClosed(View drawerView, @EdgeGravity int gravity) {}
        default void onDrawerStateChanged() {}
    }

    public MouseDrawer(@NonNull Context context) {
        this(context, null);
    }

    public MouseDrawer(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MouseDrawer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);

        float density = this.getResources().getDisplayMetrics().density;
        mDrawerElevation = 10 * density;

        final float touchSlopSensitivity = 0.5f; // was 1.0f
        float minFlingVelocity = 400 /* dips per second */ * density;

        ViewDragCallback mLeftCallback = new ViewDragCallback(Gravity.LEFT);
        mLeftDragger = ViewDragHelper.create(this, touchSlopSensitivity, mLeftCallback);
        mLeftDragger.setEdgeTrackingEnabled(ViewDragHelper.EDGE_LEFT);
        mLeftDragger.setMinVelocity(minFlingVelocity);

        ViewDragCallback mRightCallback = new ViewDragCallback(Gravity.RIGHT);
        mRightDragger = ViewDragHelper.create(this, touchSlopSensitivity, mRightCallback);
        mRightDragger.setEdgeTrackingEnabled(ViewDragHelper.EDGE_RIGHT);
        mRightDragger.setMinVelocity(minFlingVelocity);

        try {
            Field edgeSizeField = ViewDragHelper.class.getDeclaredField("mEdgeSize");
            if (!edgeSizeField.isAccessible()) edgeSizeField.setAccessible(true);
            int widthPixels = this.getResources().getDisplayMetrics().widthPixels; // whole screen
            edgeSizeField.set(mLeftDragger, widthPixels / 2);
            edgeSizeField.set(mRightDragger, widthPixels / 2);
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Log.e("AWAISKING_APP", "", e);
        }

        mLeftCallback.setDragger(this.mLeftDragger);
        mRightCallback.setDragger(this.mRightDragger);

        this.setFocusableInTouchMode(true);
        //setMotionEventSplittingEnabled(false);
    }

    public void setDrawerElevation(float elevation) {
        this.mDrawerElevation = elevation;
        for (int i = 0; i < this.getChildCount(); i++) {
            View child = this.getChildAt(i);
            if (this.isDrawerView(child)) ViewCompat.setElevation(child, this.mDrawerElevation);
        }
    }

    public float getDrawerElevation() {
        return Build.VERSION.SDK_INT >= 21 ? this.mDrawerElevation : 0f;
    }

    public void addDrawerListener(@NonNull DrawerListener listener) {
        if (this.mListeners == null) this.mListeners = new ArrayList<>();
        this.mListeners.add(listener);
    }

    private boolean isInBoundsOfChild(float x, float y, View child) {
        if (this.mChildHitRect == null) this.mChildHitRect = new Rect();
        child.getHitRect(this.mChildHitRect);
        return this.mChildHitRect.contains((int) x, (int) y);
    }

    private boolean dispatchTransformedGenericPointerEvent(MotionEvent event, @NonNull View child) {
        boolean handled;
        Matrix childMatrix = child.getMatrix();
        if (!childMatrix.isIdentity()) {
            MotionEvent transformedEvent = this.getTransformedMotionEvent(event, child);
            handled = child.dispatchGenericMotionEvent(transformedEvent);
            transformedEvent.recycle();
        } else {
            float offsetX = this.getScrollX() - child.getLeft();
            float offsetY = this.getScrollY() - child.getTop();
            event.offsetLocation(offsetX, offsetY);
            handled = child.dispatchGenericMotionEvent(event);
            event.offsetLocation(-offsetX, -offsetY);
        }
        return handled;
    }

    @NonNull
    private MotionEvent getTransformedMotionEvent(MotionEvent event, @NonNull View child) {
        float offsetX = this.getScrollX() - child.getLeft();
        float offsetY = this.getScrollY() - child.getTop();
        MotionEvent transformedEvent = MotionEvent.obtain(event);
        transformedEvent.offsetLocation(offsetX, offsetY);
        Matrix childMatrix = child.getMatrix();
        if (!childMatrix.isIdentity()) {
            if (this.mChildInvertedMatrix == null) this.mChildInvertedMatrix = new Matrix();
            childMatrix.invert(this.mChildInvertedMatrix);
            transformedEvent.transform(this.mChildInvertedMatrix);
        }
        return transformedEvent;
    }

    void updateDrawerState(@State int activeState, View activeDrawer) {
        int leftState = this.mLeftDragger.getViewDragState();
        int rightState = this.mRightDragger.getViewDragState();

        int state;
        if (leftState == ViewDragHelper.STATE_DRAGGING || rightState == ViewDragHelper.STATE_DRAGGING)
            state = ViewDragHelper.STATE_DRAGGING;
        else if (leftState == ViewDragHelper.STATE_SETTLING || rightState == ViewDragHelper.STATE_SETTLING)
            state = ViewDragHelper.STATE_SETTLING;
        else state = ViewDragHelper.STATE_IDLE;

        if (activeDrawer != null && activeState == ViewDragHelper.STATE_IDLE) {
            LayoutParams lp = (LayoutParams) activeDrawer.getLayoutParams();
            if (lp.onScreen == 0) this.dispatchOnDrawerClosed(activeDrawer);
            else if (lp.onScreen == 1) this.dispatchOnDrawerOpened(activeDrawer);
        }

        if (state != this.mDrawerState) {
            this.mDrawerState = state;

            if (this.mListeners != null) {
                int listenerCount = this.mListeners.size();
                for (int i = listenerCount - 1; i >= 0; i--) this.mListeners.get(i).onDrawerStateChanged();
            }
        }
    }

    void dispatchOnDrawerClosed(@NonNull View drawerView) {
        LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
        if ((lp.openState & LayoutParams.FLAG_IS_OPENED) == 1) {
            lp.openState = 0;

            if (this.mListeners != null) {
                int listenerCount = this.mListeners.size();
                for (int i = listenerCount - 1; i >= 0; i--) this.mListeners.get(i).onDrawerClosed(drawerView, lp.gravity);
            }
        }
    }

    void dispatchOnDrawerOpened(@NonNull View drawerView) {
        LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
        if ((lp.openState & LayoutParams.FLAG_IS_OPENED) == 0) {
            lp.openState = LayoutParams.FLAG_IS_OPENED;
            if (this.mListeners != null) {
                int listenerCount = this.mListeners.size();
                for (int i = listenerCount - 1; i >= 0; i--) this.mListeners.get(i).onDrawerOpened(drawerView, lp.gravity);
            }
        }
    }

    void setDrawerViewOffset(@NonNull View drawerView, float slideOffset) {
        LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
        if (slideOffset != lp.onScreen) {
            lp.onScreen = slideOffset;

            if (this.mListeners != null) {
                int listenerCount = this.mListeners.size();
                for (int i = listenerCount - 1; i >= 0; i--)
                    this.mListeners.get(i).onDrawerSlide(drawerView, lp.gravity, slideOffset);
            }
        }
    }

    float getDrawerViewOffset(@NonNull View drawerView) {
        return ((LayoutParams) drawerView.getLayoutParams()).onScreen;
    }

    int getDrawerViewAbsoluteGravity(@NonNull View drawerView) {
        int gravity = ((LayoutParams) drawerView.getLayoutParams()).gravity;
        return GravityCompat.getAbsoluteGravity(gravity, ViewCompat.getLayoutDirection(this));
    }

    boolean checkDrawerViewAbsoluteGravity(View drawerView, int checkFor) {
        int absGravity = this.getDrawerViewAbsoluteGravity(drawerView);
        return (absGravity & checkFor) == checkFor;
    }

    void moveDrawerToOffset(View drawerView, float slideOffset) {
        float oldOffset = this.getDrawerViewOffset(drawerView);
        int width = drawerView.getWidth();
        int oldPos = (int) (width * oldOffset);
        int newPos = (int) (width * slideOffset);
        int dx = newPos - oldPos;

        drawerView.offsetLeftAndRight(this.checkDrawerViewAbsoluteGravity(drawerView, Gravity.LEFT) ? dx : -dx);
        this.setDrawerViewOffset(drawerView, slideOffset);
    }

    public View findOpenDrawer() {
        int childCount = this.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = this.getChildAt(i);
            LayoutParams childLp = (LayoutParams) child.getLayoutParams();
            if ((childLp.openState & LayoutParams.FLAG_IS_OPENED) == 1) return child;
        }
        return null;
    }

    public View findDrawerWithGravity(int gravity) {
        int absHorizGravity = GravityCompat.getAbsoluteGravity(gravity, ViewCompat.getLayoutDirection(this)) & Gravity.HORIZONTAL_GRAVITY_MASK;
        int childCount = this.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = this.getChildAt(i);
            int childAbsGravity = this.getDrawerViewAbsoluteGravity(child);
            if ((childAbsGravity & Gravity.HORIZONTAL_GRAVITY_MASK) == absHorizGravity) return child;
        }
        return null;
    }

    @NonNull
    static String gravityToString(@EdgeGravity int gravity) {
        if ((gravity & Gravity.LEFT) == Gravity.LEFT) return "LEFT";
        if ((gravity & Gravity.RIGHT) == Gravity.RIGHT) return "RIGHT";
        return Integer.toHexString(gravity);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mFirstLayout = true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mFirstLayout = true;
    }

    @SuppressLint("WrongConstant")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        this.setMeasuredDimension(widthSize, heightSize);

        boolean hasDrawerOnLeftEdge = false;
        boolean hasDrawerOnRightEdge = false;
        int childCount = this.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = this.getChildAt(i);

            if (child.getVisibility() != View.GONE) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();

                if (this.isContentView(child)) {
                    // Content views get measured at exactly the layout's size.
                    int contentWidthSpec = MeasureSpec.makeMeasureSpec(widthSize - lp.leftMargin - lp.rightMargin, MeasureSpec.EXACTLY);
                    int contentHeightSpec = MeasureSpec.makeMeasureSpec(heightSize - lp.topMargin - lp.bottomMargin, MeasureSpec.EXACTLY);
                    child.measure(contentWidthSpec, contentHeightSpec);

                } else if (this.isDrawerView(child)) {
                    if (Build.VERSION.SDK_INT >= 21 && ViewCompat.getElevation(child) != this.mDrawerElevation)
                        ViewCompat.setElevation(child, this.mDrawerElevation);
                    int childGravity = this.getDrawerViewAbsoluteGravity(child) & Gravity.HORIZONTAL_GRAVITY_MASK;

                    boolean isLeftEdgeDrawer = (childGravity == Gravity.LEFT);
                    if (isLeftEdgeDrawer ? hasDrawerOnLeftEdge : hasDrawerOnRightEdge)
                        throw new IllegalStateException("Child drawer has absolute gravity " + MouseDrawer.gravityToString(childGravity)
                                + " but this MouseDrawer already has a drawer view along that edge");

                    if (isLeftEdgeDrawer) hasDrawerOnLeftEdge = true;
                    else hasDrawerOnRightEdge = true;

                    int drawerWidthSpec = ViewGroup.getChildMeasureSpec(widthMeasureSpec, lp.leftMargin + lp.rightMargin, lp.width);
                    int drawerHeightSpec = ViewGroup.getChildMeasureSpec(heightMeasureSpec, lp.topMargin + lp.bottomMargin, lp.height);
                    child.measure(drawerWidthSpec, drawerHeightSpec);
                } else
                    throw new IllegalStateException("Child " + child + " at index " + i
                            + " does not have a valid layout_gravity - must be Gravity.LEFT, Gravity.RIGHT or Gravity.NO_GRAVITY");
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        this.mInLayout = true;
        int width = right - left;
        int childCount = this.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = this.getChildAt(i);

            if (child.getVisibility() != View.GONE) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();

                if (this.isContentView(child)) {
                    child.layout(lp.leftMargin, lp.topMargin, lp.leftMargin + child.getMeasuredWidth(),
                            lp.topMargin + child.getMeasuredHeight());

                } else { // Drawer, if it wasn't onMeasure would have thrown an exception.
                    int childWidth = child.getMeasuredWidth();
                    int childHeight = child.getMeasuredHeight();
                    int childLeft;
                    float newOffset;

                    if (this.checkDrawerViewAbsoluteGravity(child, Gravity.LEFT)) {
                        childLeft = -childWidth + (int) (childWidth * lp.onScreen);
                        newOffset = (float) (childWidth + childLeft) / childWidth;
                    } else { // Right; onMeasure checked for us.
                        childLeft = width - (int) (childWidth * lp.onScreen);
                        newOffset = (float) (width - childLeft) / childWidth;
                    }

                    boolean changeOffset = newOffset != lp.onScreen;

                    int vgrav = lp.gravity & Gravity.VERTICAL_GRAVITY_MASK;
                    switch (vgrav) {
                        default:
                        case Gravity.TOP:
                            child.layout(childLeft, lp.topMargin, childLeft + childWidth, lp.topMargin + childHeight);
                            break;

                        case Gravity.BOTTOM: {
                            int height = bottom - top;
                            child.layout(childLeft, height - lp.bottomMargin - child.getMeasuredHeight(),
                                    childLeft + childWidth, height - lp.bottomMargin);
                            break;
                        }

                        case Gravity.CENTER_VERTICAL: {
                            int height = bottom - top;
                            int childTop = (height - childHeight) / 2;

                            if (childTop < lp.topMargin) childTop = lp.topMargin;
                            else if (childTop + childHeight > height - lp.bottomMargin)
                                childTop = height - lp.bottomMargin - childHeight;

                            child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
                            break;
                        }
                    }

                    if (changeOffset) this.setDrawerViewOffset(child, newOffset);

                    int newVisibility = lp.onScreen > 0 ? View.VISIBLE : View.INVISIBLE;
                    if (child.getVisibility() != newVisibility) child.setVisibility(newVisibility);
                }
            }
        }
        this.mInLayout = false;
        this.mFirstLayout = false;
    }

    @Override
    public void requestLayout() {
        if (!this.mInLayout) super.requestLayout();
    }

    @Override
    public void computeScroll() {
        boolean leftDraggerSettling = this.mLeftDragger.continueSettling(true);
        boolean rightDraggerSettling = this.mRightDragger.continueSettling(true);
        if (leftDraggerSettling || rightDraggerSettling) this.postInvalidateOnAnimation();
    }

    private static boolean hasOpaqueBackground(@NonNull View v) {
        Drawable bg = v.getBackground();
        if (bg != null) return bg.getOpacity() == PixelFormat.OPAQUE;
        return false;
    }

    @Override
    protected boolean drawChild(@NonNull Canvas canvas, View child, long drawingTime) {
        int height = this.getHeight();
        boolean drawingContent = this.isContentView(child);
        int clipLeft = 0, clipRight = this.getWidth();

        int restoreCount = canvas.save();
        if (drawingContent) {
            int childCount = this.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View v = this.getChildAt(i);
                if (v != child && v.getVisibility() == View.VISIBLE && MouseDrawer.hasOpaqueBackground(v) && this.isDrawerView(v) && v.getHeight() >= height) {
                    if (this.checkDrawerViewAbsoluteGravity(v, Gravity.LEFT)) {
                        int vright = v.getRight();
                        if (vright > clipLeft) clipLeft = vright;
                    } else {
                        int vleft = v.getLeft();
                        if (vleft < clipRight) clipRight = vleft;
                    }
                }
            }
            canvas.clipRect(clipLeft, 0, clipRight, this.getHeight());
        }

        boolean result = super.drawChild(canvas, child, drawingTime);
        canvas.restoreToCount(restoreCount);

        return result;
    }

    boolean isContentView(@NonNull View child) {
        return ((LayoutParams) child.getLayoutParams()).gravity == Gravity.NO_GRAVITY;
    }

    boolean isDrawerView(@NonNull View child) {
        int gravity = ((LayoutParams) child.getLayoutParams()).gravity;
        int absGravity = GravityCompat.getAbsoluteGravity(gravity, ViewCompat.getLayoutDirection(child));
        return (absGravity & Gravity.LEFT) != 0 || (absGravity & Gravity.RIGHT) != 0;
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull MotionEvent ev) {
        int action = ev.getActionMasked();

        // "|" used deliberately here; both methods should be invoked.
        boolean interceptForDrag = this.mLeftDragger.shouldInterceptTouchEvent(ev) | this.mRightDragger.shouldInterceptTouchEvent(ev);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                this.mInitialMotionX = ev.getX();
                this.mInitialMotionY = ev.getY();
                break;

            case MotionEvent.ACTION_MOVE:
                this.mLeftDragger.checkTouchSlop(ViewDragHelper.DIRECTION_ALL);
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                this.closeDrawers(true);
        }

        return interceptForDrag || this.hasPeekingDrawer();
    }

    @Override
    public boolean dispatchGenericMotionEvent(@NonNull MotionEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_CLASS_POINTER) == 0 || event.getAction() == MotionEvent.ACTION_HOVER_EXIT)
            return super.dispatchGenericMotionEvent(event);

        int childrenCount = this.getChildCount();
        if (childrenCount != 0) {
            float x = event.getX();
            float y = event.getY();

            // Walk through children from top to bottom.
            for (int i = childrenCount - 1; i >= 0; i--) {
                View child = this.getChildAt(i);
                if (this.isInBoundsOfChild(x, y, child) && !this.isContentView(child) && this.dispatchTransformedGenericPointerEvent(event, child))
                    return true;
            }
        }

        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        this.mLeftDragger.processTouchEvent(ev);
        this.mRightDragger.processTouchEvent(ev);

        int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                this.mInitialMotionX = ev.getX();
                this.mInitialMotionY = ev.getY();
                break;

            case MotionEvent.ACTION_UP:
                float x = ev.getX();
                float y = ev.getY();

                boolean peekingOnly = true;
                View touchedView = this.mLeftDragger.findTopChildUnder((int) x, (int) y);
                if (touchedView != null && this.isContentView(touchedView)) {
                    float dx = x - this.mInitialMotionX;
                    float dy = y - this.mInitialMotionY;
                    int slop = this.mLeftDragger.getTouchSlop();
                    if (dx * dx + dy * dy < slop * slop) {
                        // Taps close a dimmed open drawer but only if it isn't locked open.
                        View openDrawer = this.findOpenDrawer();
                        if (openDrawer != null) peekingOnly = false;
                    }
                }
                this.closeDrawers(peekingOnly);
                break;

            case MotionEvent.ACTION_CANCEL:
                this.closeDrawers(true);
                break;
        }

        return true;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (MouseDrawer.CHILDREN_DISALLOW_INTERCEPT || (!this.mLeftDragger.isEdgeTouched(ViewDragHelper.EDGE_LEFT) && !this.mRightDragger.isEdgeTouched(ViewDragHelper.EDGE_RIGHT)))
            super.requestDisallowInterceptTouchEvent(disallowIntercept);
        if (disallowIntercept) this.closeDrawers(true);
    }

    public void closeDrawers() {
        this.closeDrawers(false);
    }

    void closeDrawers(boolean peekingOnly) {
        boolean needsInvalidate = false;
        int childCount = this.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = this.getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();

            if (this.isDrawerView(child) && (!peekingOnly || lp.isPeeking)) {
                int childWidth = child.getWidth();

                if (this.checkDrawerViewAbsoluteGravity(child, Gravity.LEFT))
                    needsInvalidate |= this.mLeftDragger.smoothSlideViewTo(child, -childWidth, child.getTop());
                else
                    needsInvalidate |= this.mRightDragger.smoothSlideViewTo(child, this.getWidth(), child.getTop());

                lp.isPeeking = false;
            }
        }

        if (needsInvalidate) this.invalidate();
    }

    public void openDrawer(@NonNull View drawerView, boolean animate) {
        if (this.isDrawerView(drawerView)) {
            LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();

            if (this.mFirstLayout) {
                lp.onScreen = 1.f;
                lp.openState = LayoutParams.FLAG_IS_OPENED;
            } else if (animate) {
                lp.openState |= LayoutParams.FLAG_IS_OPENING;

                if (this.checkDrawerViewAbsoluteGravity(drawerView, Gravity.LEFT))
                    this.mLeftDragger.smoothSlideViewTo(drawerView, 0, drawerView.getTop());
                else
                    this.mRightDragger.smoothSlideViewTo(drawerView, this.getWidth() - drawerView.getWidth(), drawerView.getTop());
            } else {
                this.moveDrawerToOffset(drawerView, 1.f);
                this.updateDrawerState(ViewDragHelper.STATE_IDLE, drawerView);
                drawerView.setVisibility(View.VISIBLE);
            }

            this.invalidate();
            return;
        }
        throw new IllegalArgumentException("View " + drawerView + " is not a sliding drawer");
    }

    public void openDrawer(@NonNull View drawerView) {
        this.openDrawer(drawerView, true);
    }

    // public void openDrawer(@EdgeGravity final int gravity, final boolean animate) {
    //     final View drawerView = findDrawerWithGravity(gravity);
    //     if (drawerView != null) openDrawer(drawerView, animate);
    //     else throw new IllegalArgumentException("No drawer view found with gravity " + gravityToString(gravity));
    // }

    // public void openDrawer(@EdgeGravity final int gravity) {
    //     openDrawer(gravity, true);
    // }

    public void closeDrawer(@NonNull View drawerView) {
        this.closeDrawer(drawerView, true);
    }

    public void closeDrawer(@NonNull View drawerView, boolean animate) {
        if (this.isDrawerView(drawerView)) {
            LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
            if (this.mFirstLayout) {
                lp.onScreen = 0.f;
                lp.openState = 0;
            } else if (animate) {
                lp.openState |= LayoutParams.FLAG_IS_CLOSING;

                if (this.checkDrawerViewAbsoluteGravity(drawerView, Gravity.LEFT))
                    this.mLeftDragger.smoothSlideViewTo(drawerView, -drawerView.getWidth(), drawerView.getTop());
                else
                    this.mRightDragger.smoothSlideViewTo(drawerView, this.getWidth(), drawerView.getTop());
            } else {
                this.moveDrawerToOffset(drawerView, 0.f);
                this.updateDrawerState(ViewDragHelper.STATE_IDLE, drawerView);
                drawerView.setVisibility(View.INVISIBLE);
            }
            this.invalidate();
        } else throw new IllegalArgumentException("View " + drawerView + " is not a sliding drawer");
    }

    // public void closeDrawer(@EdgeGravity final int gravity) {
    //     closeDrawer(gravity, true);
    // }

    // public void closeDrawer(@EdgeGravity final int gravity, final boolean animate) {
    //     final View drawerView = findDrawerWithGravity(gravity);
    //     if (drawerView != null) closeDrawer(drawerView, animate);
    //     else throw new IllegalArgumentException("No drawer view found with gravity " + gravityToString(gravity));
    // }

    public boolean isDrawerOpen(@NonNull View drawer) {
        if (this.isDrawerView(drawer)) return (((LayoutParams) drawer.getLayoutParams()).openState & LayoutParams.FLAG_IS_OPENED) == 1;
        else throw new IllegalArgumentException("View " + drawer + " is not a drawer");
    }

    // public boolean isDrawerOpen(@EdgeGravity final int drawerGravity) {
    //     final View drawerView = findDrawerWithGravity(drawerGravity);
    //     return drawerView != null && isDrawerOpen(drawerView);
    // }

    public boolean isDrawerVisible(@NonNull View drawer) {
        if (this.isDrawerView(drawer)) return ((LayoutParams) drawer.getLayoutParams()).onScreen > 0;
        throw new IllegalArgumentException("View " + drawer + " is not a drawer");
    }

    // public boolean isDrawerVisible(@EdgeGravity final int drawerGravity) {
    //     final View drawerView = findDrawerWithGravity(drawerGravity);
    //     return drawerView != null && isDrawerVisible(drawerView);
    // }

    private boolean hasPeekingDrawer() {
        int childCount = this.getChildCount();
        for (int i = 0; i < childCount; i++) {
            LayoutParams lp = (LayoutParams) this.getChildAt(i).getLayoutParams();
            if (lp.isPeeking) return true;
        }
        return false;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams params) {
        return params instanceof LayoutParams ? new LayoutParams((LayoutParams) params) :
                params instanceof ViewGroup.MarginLayoutParams ? new LayoutParams((MarginLayoutParams) params) : new LayoutParams(params);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams params) {
        return params instanceof LayoutParams && super.checkLayoutParams(params);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(this.getContext(), attrs);
    }

    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        if (this.getDescendantFocusability() != ViewGroup.FOCUS_BLOCK_DESCENDANTS) {
            int childCount = this.getChildCount();
            boolean isDrawerOpen = false;
            for (int i = 0; i < childCount; i++) {
                View child = this.getChildAt(i);
                if (!this.isDrawerView(child)) this.mNonDrawerViews.add(child);
                else if (this.isDrawerOpen(child)) {
                    isDrawerOpen = true;
                    child.addFocusables(views, direction, focusableMode);
                }
            }

            if (!isDrawerOpen) {
                int nonDrawerViewsCount = this.mNonDrawerViews.size();
                for (int i = 0; i < nonDrawerViewsCount; ++i) {
                    View child = this.mNonDrawerViews.get(i);
                    if (child.getVisibility() == View.VISIBLE) child.addFocusables(views, direction, focusableMode);
                }
            }

            this.mNonDrawerViews.clear();
        }
    }

    private boolean hasVisibleDrawer() {
        return this.findVisibleDrawer() != null;
    }

    @Nullable
    final View findVisibleDrawer() {
        int childCount = this.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = this.getChildAt(i);
            if (this.isDrawerView(child) && this.isDrawerVisible(child)) return child;
        }
        return null;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && this.hasVisibleDrawer()) {
            event.startTracking();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            View visibleDrawer = this.findVisibleDrawer();
            if (visibleDrawer != null && this.isDrawerView(visibleDrawer)) this.closeDrawers();
            return visibleDrawer != null;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState ss = (SavedState) state;
            super.onRestoreInstanceState(ss.getSuperState());

            if (ss.openDrawerGravity != Gravity.NO_GRAVITY) {
                View toOpen = this.findDrawerWithGravity(ss.openDrawerGravity);
                if (toOpen != null) this.openDrawer(toOpen);
            }
        } else super.onRestoreInstanceState(state);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        assert superState != null;
        SavedState ss = new SavedState(superState);

        int childCount = this.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = this.getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            // Is the current child fully opened (that is, not closing)?
            boolean isOpenedAndNotClosing = (lp.openState == LayoutParams.FLAG_IS_OPENED);
            // Is the current child opening?
            boolean isClosedAndOpening = (lp.openState == LayoutParams.FLAG_IS_OPENING);
            if (isOpenedAndNotClosing || isClosedAndOpening) {
                // If one of the conditions above holds, save the child's gravity so that we open that child during state restore.
                ss.openDrawerGravity = lp.gravity;
                break;
            }
        }

        return ss;
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        View openDrawer = this.findOpenDrawer();
        if (openDrawer == null) this.isDrawerView(child);
    }

    protected static class SavedState extends AbsSavedState {
        public static final Creator<SavedState> CREATOR = new Parcelable.ClassLoaderCreator<SavedState>() {
            @NonNull
            @Override
            public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in, loader);
            }

            @NonNull
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in, null);
            }

            @NonNull
            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        int openDrawerGravity = Gravity.NO_GRAVITY;

        public SavedState(@NonNull Parcelable superState) {
            super(superState);
        }

        public SavedState(@NonNull Parcel in, @Nullable ClassLoader loader) {
            super(in, loader);
            this.openDrawerGravity = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(this.openDrawerGravity);
        }
    }

    private class ViewDragCallback extends ViewDragHelper.Callback {
        private final int mAbsGravity;
        private ViewDragHelper mDragger;

        ViewDragCallback(int gravity) {
            this.mAbsGravity = gravity;
        }

        public void setDragger(ViewDragHelper dragger) {
            this.mDragger = dragger;
        }

        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            return MouseDrawer.this.isDrawerView(child) && MouseDrawer.this.checkDrawerViewAbsoluteGravity(child, this.mAbsGravity);
        }

        @Override
        public void onViewDragStateChanged(int state) {
            MouseDrawer.this.updateDrawerState(state, this.mDragger.getCapturedView());
        }

        @Override
        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
            float offset;
            int childWidth = changedView.getWidth();

            if (MouseDrawer.this.checkDrawerViewAbsoluteGravity(changedView, Gravity.LEFT)) offset = (float) (childWidth + left) / childWidth;
            else offset = (float) (MouseDrawer.this.getWidth() - left) / childWidth;

            MouseDrawer.this.setDrawerViewOffset(changedView, offset);
            changedView.setVisibility(offset == 0 ? View.INVISIBLE : View.VISIBLE);
            MouseDrawer.this.invalidate();
        }

        @Override
        public void onViewCaptured(@NonNull View capturedChild, int activePointerId) {
            LayoutParams lp = (LayoutParams) capturedChild.getLayoutParams();
            lp.isPeeking = false;
            this.closeOtherDrawer();
        }

        private void closeOtherDrawer() {
            int otherGrav = this.mAbsGravity == Gravity.LEFT ? Gravity.RIGHT : Gravity.LEFT;
            View toClose = MouseDrawer.this.findDrawerWithGravity(otherGrav);
            if (toClose != null) MouseDrawer.this.closeDrawer(toClose);
        }

        @Override
        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
            float offset = MouseDrawer.this.getDrawerViewOffset(releasedChild);
            int childWidth = releasedChild.getWidth();

            int left;
            if (MouseDrawer.this.checkDrawerViewAbsoluteGravity(releasedChild, Gravity.LEFT))
                left = xvel > 0 || (xvel == 0 && offset > 0.5f) ? 0 : -childWidth;
            else {
                int width = MouseDrawer.this.getWidth();
                left = xvel < 0 || (xvel == 0 && offset > 0.5f) ? width - childWidth : width;
            }

            this.mDragger.settleCapturedViewAt(left, releasedChild.getTop());
            MouseDrawer.this.invalidate();
        }

        @Override
        public void onEdgeDragStarted(int edgeFlags, int pointerId) {
            View toCapture;
            if ((edgeFlags & ViewDragHelper.EDGE_LEFT) == ViewDragHelper.EDGE_LEFT)
                toCapture = MouseDrawer.this.findDrawerWithGravity(Gravity.LEFT);
            else toCapture = MouseDrawer.this.findDrawerWithGravity(Gravity.RIGHT);

            if (toCapture != null && MouseDrawer.this.isDrawerView(toCapture)) this.mDragger.captureChildView(toCapture, pointerId);
        }

        @Override
        public int getViewHorizontalDragRange(@NonNull View child) {
            return MouseDrawer.this.isDrawerView(child) ? child.getWidth() : 0;
        }

        @Override
        public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
            if (MouseDrawer.this.checkDrawerViewAbsoluteGravity(child, Gravity.LEFT)) return Math.max(-child.getWidth(), Math.min(left, 0));
            int width = MouseDrawer.this.getWidth();
            return Math.max(width - child.getWidth(), Math.min(left, width));
        }

        @Override
        public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
            return child.getTop();
        }
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        private static final int FLAG_IS_CLOSING = 0x4;
        public static final int FLAG_IS_OPENED = 0x1;
        public static final int FLAG_IS_OPENING = 0x2;
        public int openState;
        @EdgeGravity
        public int gravity = Gravity.NO_GRAVITY;
        public boolean isPeeking;
        public float onScreen;

        public LayoutParams(@NonNull Context c, @Nullable AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs, new int[]{android.R.attr.layout_gravity});
            try {
                gravity = a.getInt(0, Gravity.NO_GRAVITY);
            } finally {
                a.recycle();
            }
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(@NonNull LayoutParams source) {
            super(source);
            gravity = source.gravity;
        }

        public LayoutParams(@NonNull ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(@NonNull ViewGroup.MarginLayoutParams source) {
            super(source);
        }
    }
}