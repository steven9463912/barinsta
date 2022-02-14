/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package awais.instagrabber.customviews.drawee;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewParent;

import androidx.annotation.Nullable;
import androidx.core.view.ScrollingView;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.logging.FLog;
import com.facebook.drawee.controller.AbstractDraweeController;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.generic.GenericDraweeHierarchyInflater;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.DraweeView;


/**
 * DraweeView that has zoomable capabilities.
 *
 * <p>Once the image loads, pinch-to-zoom and translation gestures are enabled.
 */
public class ZoomableDraweeView extends DraweeView<GenericDraweeHierarchy>
        implements ScrollingView {

    private static final Class<?> TAG = ZoomableDraweeView.class;

    private static final float HUGE_IMAGE_SCALE_FACTOR_THRESHOLD = 1.1f;

    private final RectF mImageBounds = new RectF();
    private final RectF mViewBounds = new RectF();

    private DraweeController mHugeImageController;
    private ZoomableController mZoomableController;
    private GestureDetector mTapGestureDetector;
    private boolean mAllowTouchInterceptionWhileZoomed;

    private boolean mIsDialtoneEnabled;
    private boolean mZoomingEnabled = true;

    private final ControllerListener mControllerListener =
            new BaseControllerListener<Object>() {
                @Override
                public void onFinalImageSet(
                        final String id, @Nullable final Object imageInfo, @Nullable final Animatable animatable) {
                    ZoomableDraweeView.this.onFinalImageSet();
                }

                @Override
                public void onRelease(final String id) {
                    ZoomableDraweeView.this.onRelease();
                }
            };

    private final ZoomableController.Listener mZoomableListener =
            new ZoomableController.Listener() {
                @Override
                public void onTransformBegin(final Matrix transform) {
                    ZoomableDraweeView.this.onTransformBegin(transform);
                }

                @Override
                public void onTransformChanged(final Matrix transform) {
                    ZoomableDraweeView.this.onTransformChanged(transform);
                }

                @Override
                public void onTransformEnd(final Matrix transform) {
                    ZoomableDraweeView.this.onTransformEnd(transform);
                }

                @Override
                public void onTranslationLimited(float offsetLeft, float offsetTop) {
                    ZoomableDraweeView.this.onTranslationLimited(offsetLeft, offsetTop);
                }
            };

    private final GestureListenerWrapper mTapListenerWrapper = new GestureListenerWrapper();

    public ZoomableDraweeView(final Context context, final GenericDraweeHierarchy hierarchy) {
        super(context);
        this.setHierarchy(hierarchy);
        this.init();
    }

    public ZoomableDraweeView(final Context context) {
        super(context);
        this.inflateHierarchy(context, null);
        this.init();
    }

    public ZoomableDraweeView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        this.inflateHierarchy(context, attrs);
        this.init();
    }

    public ZoomableDraweeView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        this.inflateHierarchy(context, attrs);
        this.init();
    }

    protected void inflateHierarchy(final Context context, @Nullable final AttributeSet attrs) {
        final Resources resources = context.getResources();
        final GenericDraweeHierarchyBuilder builder =
                new GenericDraweeHierarchyBuilder(resources)
                        .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER);
        GenericDraweeHierarchyInflater.updateBuilder(builder, context, attrs);
        this.setAspectRatio(builder.getDesiredAspectRatio());
        this.setHierarchy(builder.build());
    }

    private void init() {
        this.mZoomableController = this.createZoomableController();
        this.mZoomableController.setListener(this.mZoomableListener);
        this.mTapGestureDetector = new GestureDetector(this.getContext(), this.mTapListenerWrapper);
    }

    public void setIsDialtoneEnabled(final boolean isDialtoneEnabled) {
        this.mIsDialtoneEnabled = isDialtoneEnabled;
    }

    /**
     * Gets the original image bounds, in view-absolute coordinates.
     *
     * <p>The original image bounds are those reported by the hierarchy. The hierarchy itself may
     * apply scaling on its own (e.g. due to scale type) so the reported bounds are not necessarily
     * the same as the actual bitmap dimensions. In other words, the original image bounds correspond
     * to the image bounds within this view when no zoomable transformation is applied, but including
     * the potential scaling of the hierarchy. Having the actual bitmap dimensions abstracted away
     * from this view greatly simplifies implementation because the actual bitmap may change (e.g.
     * when a high-res image arrives and replaces the previously set low-res image). With proper
     * hierarchy scaling (e.g. FIT_CENTER), this underlying change will not affect this view nor the
     * zoomable transformation in any way.
     */
    protected void getImageBounds(final RectF outBounds) {
        this.getHierarchy().getActualImageBounds(outBounds);
    }

    /**
     * Gets the bounds used to limit the translation, in view-absolute coordinates.
     *
     * <p>These bounds are passed to the zoomable controller in order to limit the translation. The
     * image is attempted to be centered within the limit bounds if the transformed image is smaller.
     * There will be no empty spaces within the limit bounds if the transformed image is bigger. This
     * applies to each dimension (horizontal and vertical) independently.
     *
     * <p>Unless overridden by a subclass, these bounds are same as the view bounds.
     */
    protected void getLimitBounds(final RectF outBounds) {
        outBounds.set(0, 0, this.getWidth(), this.getHeight());
    }

    /**
     * Sets a custom zoomable controller, instead of using the default one.
     */
    public void setZoomableController(final ZoomableController zoomableController) {
        Preconditions.checkNotNull(zoomableController);
        this.mZoomableController.setListener(null);
        this.mZoomableController = zoomableController;
        this.mZoomableController.setListener(this.mZoomableListener);
    }

    /**
     * Gets the zoomable controller.
     *
     * <p>Zoomable controller can be used to zoom to point, or to map point from view to image
     * coordinates for instance.
     */
    public ZoomableController getZoomableController() {
        return this.mZoomableController;
    }

    /**
     * Check whether the parent view can intercept touch events while zoomed. This can be used, for
     * example, to swipe between images in a view pager while zoomed.
     *
     * @return true if touch events can be intercepted
     */
    public boolean allowsTouchInterceptionWhileZoomed() {
        return this.mAllowTouchInterceptionWhileZoomed;
    }

    /**
     * If this is set to true, parent views can intercept touch events while the view is zoomed. For
     * example, this can be used to swipe between images in a view pager while zoomed.
     *
     * @param allowTouchInterceptionWhileZoomed true if the parent needs to intercept touches
     */
    public void setAllowTouchInterceptionWhileZoomed(final boolean allowTouchInterceptionWhileZoomed) {
        this.mAllowTouchInterceptionWhileZoomed = allowTouchInterceptionWhileZoomed;
    }

    /**
     * Sets the tap listener.
     */
    public void setTapListener(final GestureDetector.SimpleOnGestureListener tapListener) {
        this.mTapListenerWrapper.setListener(tapListener);
    }

    /**
     * Sets whether long-press tap detection is enabled. Unfortunately, long-press conflicts with
     * onDoubleTapEvent.
     */
    public void setIsLongpressEnabled(final boolean enabled) {
        this.mTapGestureDetector.setIsLongpressEnabled(enabled);
    }

    public void setZoomingEnabled(final boolean zoomingEnabled) {
        this.mZoomingEnabled = zoomingEnabled;
        this.mZoomableController.setEnabled(zoomingEnabled);
    }

    /**
     * Sets the image controller.
     */
    @Override
    public void setController(@Nullable final DraweeController controller) {
        this.setControllers(controller, null);
    }

    /**
     * Sets the controllers for the normal and huge image.
     *
     * <p>The huge image controller is used after the image gets scaled above a certain threshold.
     *
     * <p>IMPORTANT: in order to avoid a flicker when switching to the huge image, the huge image
     * controller should have the normal-image-uri set as its low-res-uri.
     *
     * @param controller          controller to be initially used
     * @param hugeImageController controller to be used after the client starts zooming-in
     */
    public void setControllers(
            @Nullable final DraweeController controller, @Nullable final DraweeController hugeImageController) {
        this.setControllersInternal(null, null);
        this.mZoomableController.setEnabled(false);
        this.setControllersInternal(controller, hugeImageController);
    }

    private void setControllersInternal(
            @Nullable final DraweeController controller, @Nullable final DraweeController hugeImageController) {
        this.removeControllerListener(this.getController());
        this.addControllerListener(controller);
        this.mHugeImageController = hugeImageController;
        super.setController(controller);
    }

    private void maybeSetHugeImageController() {
        if (this.mHugeImageController != null
                && this.mZoomableController.getScaleFactor() > ZoomableDraweeView.HUGE_IMAGE_SCALE_FACTOR_THRESHOLD) {
            this.setControllersInternal(this.mHugeImageController, null);
        }
    }

    private void removeControllerListener(final DraweeController controller) {
        if (controller instanceof AbstractDraweeController) {
            ((AbstractDraweeController) controller).removeControllerListener(this.mControllerListener);
        }
    }

    private void addControllerListener(final DraweeController controller) {
        if (controller instanceof AbstractDraweeController) {
            ((AbstractDraweeController) controller).addControllerListener(this.mControllerListener);
        }
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        final int saveCount = canvas.save();
        canvas.concat(this.mZoomableController.getTransform());
        try {
            super.onDraw(canvas);
        } catch (final Exception e) {
            final DraweeController controller = this.getController();
            if (controller != null && controller instanceof AbstractDraweeController) {
                final Object callerContext = ((AbstractDraweeController) controller).getCallerContext();
                if (callerContext != null) {
                    throw new RuntimeException(
                            String.format("Exception in onDraw, callerContext=%s", callerContext), e);
                }
            }
            throw e;
        }
        canvas.restoreToCount(saveCount);
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        final int a = event.getActionMasked();
        FLog.v(this.getLogTag(), "onTouchEvent: %d, view %x, received", a, hashCode());
        if (!this.mIsDialtoneEnabled && this.mTapGestureDetector.onTouchEvent(event)) {
            FLog.v(this.getLogTag(),
                   "onTouchEvent: %d, view %x, handled by tap gesture detector",
                   a,
                    hashCode());
            return true;
        }

        if (!this.mIsDialtoneEnabled && this.mZoomableController.onTouchEvent(event)) {
            FLog.v(
                    this.getLogTag(),
                    "onTouchEvent: %d, view %x, handled by zoomable controller",
                    a,
                    hashCode());
            if (!this.mAllowTouchInterceptionWhileZoomed && !this.mZoomableController.isIdentity()) {
                ViewParent parent = this.getParent();
                parent.requestDisallowInterceptTouchEvent(true);
            }
            return true;
        }
        if (super.onTouchEvent(event)) {
            FLog.v(this.getLogTag(), "onTouchEvent: %d, view %x, handled by the super", a, hashCode());
            return true;
        }
        // None of our components reported that they handled the touch event. Upon returning false
        // from this method, our parent won't send us any more events for this gesture. Unfortunately,
        // some components may have started a delayed action, such as a long-press timer, and since we
        // won't receive an ACTION_UP that would cancel that timer, a false event may be triggered.
        // To prevent that we explicitly send one last cancel event when returning false.
        final MotionEvent cancelEvent = MotionEvent.obtain(event);
        cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
        this.mTapGestureDetector.onTouchEvent(cancelEvent);
        this.mZoomableController.onTouchEvent(cancelEvent);
        cancelEvent.recycle();
        return false;
    }

    @Override
    public int computeHorizontalScrollRange() {
        return this.mZoomableController.computeHorizontalScrollRange();
    }

    @Override
    public int computeHorizontalScrollOffset() {
        return this.mZoomableController.computeHorizontalScrollOffset();
    }

    @Override
    public int computeHorizontalScrollExtent() {
        return this.mZoomableController.computeHorizontalScrollExtent();
    }

    @Override
    public int computeVerticalScrollRange() {
        return this.mZoomableController.computeVerticalScrollRange();
    }

    @Override
    public int computeVerticalScrollOffset() {
        return this.mZoomableController.computeVerticalScrollOffset();
    }

    @Override
    public int computeVerticalScrollExtent() {
        return this.mZoomableController.computeVerticalScrollExtent();
    }

    @Override
    protected void onLayout(final boolean changed, final int left, final int top, final int right, final int bottom) {
        FLog.v(this.getLogTag(), "onLayout: view %x", hashCode());
        super.onLayout(changed, left, top, right, bottom);
        this.updateZoomableControllerBounds();
    }

    private void onFinalImageSet() {
        FLog.v(this.getLogTag(), "onFinalImageSet: view %x", hashCode());
        if (!this.mZoomableController.isEnabled() && this.mZoomingEnabled) {
            this.mZoomableController.setEnabled(true);
            this.updateZoomableControllerBounds();
        }
    }

    private void onRelease() {
        FLog.v(this.getLogTag(), "onRelease: view %x", hashCode());
        this.mZoomableController.setEnabled(false);
    }

    protected void onTransformBegin(Matrix transform) {}

    protected void onTransformChanged(final Matrix transform) {
        FLog.v(this.getLogTag(), "onTransformChanged: view %x, transform: %s", hashCode(), transform);
        this.maybeSetHugeImageController();
        this.invalidate();
    }

    protected void onTransformEnd(Matrix transform) {}

    protected void onTranslationLimited(float offsetLeft, float offsetTop) {}

    protected void updateZoomableControllerBounds() {
        this.getImageBounds(this.mImageBounds);
        this.getLimitBounds(this.mViewBounds);
        // Log.d(TAG.getSimpleName(), "updateZoomableControllerBounds: mImageBounds: " + mImageBounds);
        this.mZoomableController.setImageBounds(this.mImageBounds);
        this.mZoomableController.setViewBounds(this.mViewBounds);
        FLog.v(this.getLogTag(),
               "updateZoomableControllerBounds: view %x, view bounds: %s, image bounds: %s",
                hashCode(),
                this.mViewBounds,
                this.mImageBounds);
    }

    protected Class<?> getLogTag() {
        return ZoomableDraweeView.TAG;
    }

    protected ZoomableController createZoomableController() {
        return AnimatedZoomableController.newInstance();
    }
}
