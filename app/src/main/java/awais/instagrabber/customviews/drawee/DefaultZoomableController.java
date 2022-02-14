/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package awais.instagrabber.customviews.drawee;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import com.facebook.common.logging.FLog;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Zoomable controller that calculates transformation based on touch events.
 */
public class DefaultZoomableController
        implements ZoomableController, TransformGestureDetector.Listener {

    /**
     * Interface for handling call backs when the image bounds are set.
     */
    public interface ImageBoundsListener {
        void onImageBoundsSet(RectF imageBounds);
    }

    @IntDef(
            flag = true,
            value = {DefaultZoomableController.LIMIT_NONE, DefaultZoomableController.LIMIT_TRANSLATION_X, DefaultZoomableController.LIMIT_TRANSLATION_Y, DefaultZoomableController.LIMIT_SCALE, DefaultZoomableController.LIMIT_ALL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface LimitFlag {}

    public static final int LIMIT_NONE = 0;
    public static final int LIMIT_TRANSLATION_X = 1;
    public static final int LIMIT_TRANSLATION_Y = 2;
    public static final int LIMIT_SCALE = 4;
    public static final int LIMIT_ALL = DefaultZoomableController.LIMIT_TRANSLATION_X | DefaultZoomableController.LIMIT_TRANSLATION_Y | DefaultZoomableController.LIMIT_SCALE;

    private static final float EPS = 1e-3f;

    private static final Class<?> TAG = DefaultZoomableController.class;

    private static final RectF IDENTITY_RECT = new RectF(0, 0, 1, 1);

    private final TransformGestureDetector mGestureDetector;

    @Nullable
    private
    ImageBoundsListener mImageBoundsListener;

    @Nullable
    private
    Listener mListener;

    private boolean mIsEnabled;
    private boolean mIsRotationEnabled;
    private boolean mIsScaleEnabled = true;
    private boolean mIsTranslationEnabled = true;
    private boolean mIsGestureZoomEnabled = true;

    private float mMinScaleFactor = 1.0f;
    private float mMaxScaleFactor = 2.0f;

    // View bounds, in view-absolute coordinates
    private final RectF mViewBounds = new RectF();
    // Non-transformed image bounds, in view-absolute coordinates
    private final RectF mImageBounds = new RectF();
    // Transformed image bounds, in view-absolute coordinates
    private final RectF mTransformedImageBounds = new RectF();

    private final Matrix mPreviousTransform = new Matrix();
    private final Matrix mActiveTransform = new Matrix();
    private final Matrix mActiveTransformInverse = new Matrix();
    private final float[] mTempValues = new float[9];
    private final RectF mTempRect = new RectF();
    private boolean mWasTransformCorrected;

    public static DefaultZoomableController newInstance() {
        return new DefaultZoomableController(TransformGestureDetector.newInstance());
    }

    public DefaultZoomableController(final TransformGestureDetector gestureDetector) {
        this.mGestureDetector = gestureDetector;
        this.mGestureDetector.setListener(this);
    }

    /**
     * Rests the controller.
     */
    public void reset() {
        FLog.v(DefaultZoomableController.TAG, "reset");
        this.mGestureDetector.reset();
        this.mPreviousTransform.reset();
        this.mActiveTransform.reset();
        this.onTransformChanged();
    }

    /**
     * Sets the zoomable listener.
     */
    @Override
    public void setListener(final Listener listener) {
        this.mListener = listener;
    }

    /**
     * Sets whether the controller is enabled or not.
     */
    @Override
    public void setEnabled(final boolean enabled) {
        this.mIsEnabled = enabled;
        if (!enabled) {
            this.reset();
        }
    }

    /**
     * Gets whether the controller is enabled or not.
     */
    @Override
    public boolean isEnabled() {
        return this.mIsEnabled;
    }

    /**
     * Sets whether the rotation gesture is enabled or not.
     */
    public void setRotationEnabled(final boolean enabled) {
        this.mIsRotationEnabled = enabled;
    }

    /**
     * Gets whether the rotation gesture is enabled or not.
     */
    public boolean isRotationEnabled() {
        return this.mIsRotationEnabled;
    }

    /**
     * Sets whether the scale gesture is enabled or not.
     */
    public void setScaleEnabled(final boolean enabled) {
        this.mIsScaleEnabled = enabled;
    }

    /**
     * Gets whether the scale gesture is enabled or not.
     */
    public boolean isScaleEnabled() {
        return this.mIsScaleEnabled;
    }

    /**
     * Sets whether the translation gesture is enabled or not.
     */
    public void setTranslationEnabled(final boolean enabled) {
        this.mIsTranslationEnabled = enabled;
    }

    /**
     * Gets whether the translations gesture is enabled or not.
     */
    public boolean isTranslationEnabled() {
        return this.mIsTranslationEnabled;
    }

    /**
     * Sets the minimum scale factor allowed.
     *
     * <p>Hierarchy's scaling (if any) is not taken into account.
     */
    public void setMinScaleFactor(final float minScaleFactor) {
        this.mMinScaleFactor = minScaleFactor;
    }

    /**
     * Gets the minimum scale factor allowed.
     */
    public float getMinScaleFactor() {
        return this.mMinScaleFactor;
    }

    /**
     * Sets the maximum scale factor allowed.
     *
     * <p>Hierarchy's scaling (if any) is not taken into account.
     */
    public void setMaxScaleFactor(final float maxScaleFactor) {
        this.mMaxScaleFactor = maxScaleFactor;
    }

    /**
     * Gets the maximum scale factor allowed.
     */
    public float getMaxScaleFactor() {
        return this.mMaxScaleFactor;
    }

    /**
     * Sets whether gesture zooms are enabled or not.
     */
    public void setGestureZoomEnabled(final boolean isGestureZoomEnabled) {
        this.mIsGestureZoomEnabled = isGestureZoomEnabled;
    }

    /**
     * Gets whether gesture zooms are enabled or not.
     */
    public boolean isGestureZoomEnabled() {
        return this.mIsGestureZoomEnabled;
    }

    /**
     * Gets the current scale factor.
     */
    @Override
    public float getScaleFactor() {
        return this.getMatrixScaleFactor(this.mActiveTransform);
    }

    /**
     * Sets the image bounds, in view-absolute coordinates.
     */
    @Override
    public void setImageBounds(final RectF imageBounds) {
        if (!imageBounds.equals(this.mImageBounds)) {
            this.mImageBounds.set(imageBounds);
            this.onTransformChanged();
            if (this.mImageBoundsListener != null) {
                this.mImageBoundsListener.onImageBoundsSet(this.mImageBounds);
            }
        }
    }

    /**
     * Gets the non-transformed image bounds, in view-absolute coordinates.
     */
    public RectF getImageBounds() {
        return this.mImageBounds;
    }

    /**
     * Gets the transformed image bounds, in view-absolute coordinates
     */
    private RectF getTransformedImageBounds() {
        return this.mTransformedImageBounds;
    }

    /**
     * Sets the view bounds.
     */
    @Override
    public void setViewBounds(final RectF viewBounds) {
        this.mViewBounds.set(viewBounds);
    }

    /**
     * Gets the view bounds.
     */
    public RectF getViewBounds() {
        return this.mViewBounds;
    }

    /**
     * Sets the image bounds listener.
     */
    public void setImageBoundsListener(@Nullable final ImageBoundsListener imageBoundsListener) {
        this.mImageBoundsListener = imageBoundsListener;
    }

    /**
     * Gets the image bounds listener.
     */
    @Nullable
    public
    ImageBoundsListener getImageBoundsListener() {
        return this.mImageBoundsListener;
    }

    /**
     * Returns true if the zoomable transform is identity matrix.
     */
    @Override
    public boolean isIdentity() {
        return this.isMatrixIdentity(this.mActiveTransform, 1e-3f);
    }

    /**
     * Returns true if the transform was corrected during the last update.
     *
     * <p>We should rename this method to `wasTransformedWithoutCorrection` and just return the
     * internal flag directly. However, this requires interface change and negation of meaning.
     */
    @Override
    public boolean wasTransformCorrected() {
        return this.mWasTransformCorrected;
    }

    /**
     * Gets the matrix that transforms image-absolute coordinates to view-absolute coordinates. The
     * zoomable transformation is taken into account.
     *
     * <p>Internal matrix is exposed for performance reasons and is not to be modified by the callers.
     */
    @Override
    public Matrix getTransform() {
        return this.mActiveTransform;
    }

    /**
     * Gets the matrix that transforms image-relative coordinates to view-absolute coordinates. The
     * zoomable transformation is taken into account.
     */
    public void getImageRelativeToViewAbsoluteTransform(final Matrix outMatrix) {
        outMatrix.setRectToRect(DefaultZoomableController.IDENTITY_RECT, this.mTransformedImageBounds, Matrix.ScaleToFit.FILL);
    }

    /**
     * Maps point from view-absolute to image-relative coordinates. This takes into account the
     * zoomable transformation.
     */
    public PointF mapViewToImage(final PointF viewPoint) {
        final float[] points = this.mTempValues;
        points[0] = viewPoint.x;
        points[1] = viewPoint.y;
        this.mActiveTransform.invert(this.mActiveTransformInverse);
        this.mActiveTransformInverse.mapPoints(points, 0, points, 0, 1);
        this.mapAbsoluteToRelative(points, points, 1);
        return new PointF(points[0], points[1]);
    }

    /**
     * Maps point from image-relative to view-absolute coordinates. This takes into account the
     * zoomable transformation.
     */
    public PointF mapImageToView(final PointF imagePoint) {
        final float[] points = this.mTempValues;
        points[0] = imagePoint.x;
        points[1] = imagePoint.y;
        this.mapRelativeToAbsolute(points, points, 1);
        this.mActiveTransform.mapPoints(points, 0, points, 0, 1);
        return new PointF(points[0], points[1]);
    }

    /**
     * Maps array of 2D points from view-absolute to image-relative coordinates. This does NOT take
     * into account the zoomable transformation. Points are represented by a float array of [x0, y0,
     * x1, y1, ...].
     *
     * @param destPoints destination array (may be the same as source array)
     * @param srcPoints  source array
     * @param numPoints  number of points to map
     */
    private void mapAbsoluteToRelative(final float[] destPoints, final float[] srcPoints, final int numPoints) {
        for (int i = 0; i < numPoints; i++) {
            destPoints[i * 2 + 0] = (srcPoints[i * 2 + 0] - this.mImageBounds.left) / this.mImageBounds.width();
            destPoints[i * 2 + 1] = (srcPoints[i * 2 + 1] - this.mImageBounds.top) / this.mImageBounds.height();
        }
    }

    /**
     * Maps array of 2D points from image-relative to view-absolute coordinates. This does NOT take
     * into account the zoomable transformation. Points are represented by float array of [x0, y0, x1,
     * y1, ...].
     *
     * @param destPoints destination array (may be the same as source array)
     * @param srcPoints  source array
     * @param numPoints  number of points to map
     */
    private void mapRelativeToAbsolute(final float[] destPoints, final float[] srcPoints, final int numPoints) {
        for (int i = 0; i < numPoints; i++) {
            destPoints[i * 2 + 0] = srcPoints[i * 2 + 0] * this.mImageBounds.width() + this.mImageBounds.left;
            destPoints[i * 2 + 1] = srcPoints[i * 2 + 1] * this.mImageBounds.height() + this.mImageBounds.top;
        }
    }

    /**
     * Zooms to the desired scale and positions the image so that the given image point corresponds to
     * the given view point.
     *
     * @param scale      desired scale, will be limited to {min, max} scale factor
     * @param imagePoint 2D point in image's relative coordinate system (i.e. 0 <= x, y <= 1)
     * @param viewPoint  2D point in view's absolute coordinate system
     */
    public void zoomToPoint(final float scale, final PointF imagePoint, final PointF viewPoint) {
        FLog.v(DefaultZoomableController.TAG, "zoomToPoint");
        this.calculateZoomToPointTransform(this.mActiveTransform, scale, imagePoint, viewPoint, DefaultZoomableController.LIMIT_ALL);
        this.onTransformChanged();
    }

    /**
     * Calculates the zoom transformation that would zoom to the desired scale and position the image
     * so that the given image point corresponds to the given view point.
     *
     * @param outTransform the matrix to store the result to
     * @param scale        desired scale, will be limited to {min, max} scale factor
     * @param imagePoint   2D point in image's relative coordinate system (i.e. 0 <= x, y <= 1)
     * @param viewPoint    2D point in view's absolute coordinate system
     * @param limitFlags   whether to limit translation and/or scale.
     * @return whether or not the transform has been corrected due to limitation
     */
    protected boolean calculateZoomToPointTransform(
            final Matrix outTransform,
            final float scale,
            final PointF imagePoint,
            final PointF viewPoint,
            @LimitFlag final int limitFlags) {
        final float[] viewAbsolute = this.mTempValues;
        viewAbsolute[0] = imagePoint.x;
        viewAbsolute[1] = imagePoint.y;
        this.mapRelativeToAbsolute(viewAbsolute, viewAbsolute, 1);
        final float distanceX = viewPoint.x - viewAbsolute[0];
        final float distanceY = viewPoint.y - viewAbsolute[1];
        boolean transformCorrected = false;
        outTransform.setScale(scale, scale, viewAbsolute[0], viewAbsolute[1]);
        transformCorrected |= this.limitScale(outTransform, viewAbsolute[0], viewAbsolute[1], limitFlags);
        outTransform.postTranslate(distanceX, distanceY);
        transformCorrected |= this.limitTranslation(outTransform, limitFlags);
        return transformCorrected;
    }

    /**
     * Sets a new zoom transformation.
     */
    public void setTransform(final Matrix newTransform) {
        FLog.v(DefaultZoomableController.TAG, "setTransform");
        this.mActiveTransform.set(newTransform);
        this.onTransformChanged();
    }

    /**
     * Gets the gesture detector.
     */
    protected TransformGestureDetector getDetector() {
        return this.mGestureDetector;
    }

    /**
     * Notifies controller of the received touch event.
     */
    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        FLog.v(DefaultZoomableController.TAG, "onTouchEvent: action: ", event.getAction());
        if (this.mIsEnabled && this.mIsGestureZoomEnabled) {
            return this.mGestureDetector.onTouchEvent(event);
        }
        return false;
    }

    /* TransformGestureDetector.Listener methods  */

    @Override
    public void onGestureBegin(final TransformGestureDetector detector) {
        FLog.v(DefaultZoomableController.TAG, "onGestureBegin");
        this.mPreviousTransform.set(this.mActiveTransform);
        this.onTransformBegin();
        // We only received a touch down event so far, and so we don't know yet in which direction a
        // future move event will follow. Therefore, if we can't scroll in all directions, we have to
        // assume the worst case where the user tries to scroll out of edge, which would cause
        // transformation to be corrected.
        this.mWasTransformCorrected = !this.canScrollInAllDirection();
    }

    @Override
    public void onGestureUpdate(final TransformGestureDetector detector) {
        FLog.v(DefaultZoomableController.TAG, "onGestureUpdate");
        final boolean transformCorrected = this.calculateGestureTransform(this.mActiveTransform, DefaultZoomableController.LIMIT_ALL);
        this.onTransformChanged();
        if (transformCorrected) {
            this.mGestureDetector.restartGesture();
        }
        // A transformation happened, but was it without correction?
        this.mWasTransformCorrected = transformCorrected;
    }

    @Override
    public void onGestureEnd(final TransformGestureDetector detector) {
        FLog.v(DefaultZoomableController.TAG, "onGestureEnd");
        this.onTransformEnd();
    }

    /**
     * Calculates the zoom transformation based on the current gesture.
     *
     * @param outTransform the matrix to store the result to
     * @param limitTypes   whether to limit translation and/or scale.
     * @return whether or not the transform has been corrected due to limitation
     */
    protected boolean calculateGestureTransform(final Matrix outTransform, @LimitFlag final int limitTypes) {
        final TransformGestureDetector detector = this.mGestureDetector;
        boolean transformCorrected = false;
        outTransform.set(this.mPreviousTransform);
        if (this.mIsRotationEnabled) {
            final float angle = detector.getRotation() * (float) (180 / Math.PI);
            outTransform.postRotate(angle, detector.getPivotX(), detector.getPivotY());
        }
        if (this.mIsScaleEnabled) {
            final float scale = detector.getScale();
            outTransform.postScale(scale, scale, detector.getPivotX(), detector.getPivotY());
        }
        transformCorrected |=
                this.limitScale(outTransform, detector.getPivotX(), detector.getPivotY(), limitTypes);
        if (this.mIsTranslationEnabled) {
            outTransform.postTranslate(detector.getTranslationX(), detector.getTranslationY());
        }
        transformCorrected |= this.limitTranslation(outTransform, limitTypes);
        return transformCorrected;
    }

    private void onTransformBegin() {
        if (this.mListener != null && this.isEnabled()) {
            this.mListener.onTransformBegin(this.mActiveTransform);
        }
    }

    private void onTransformChanged() {
        this.mActiveTransform.mapRect(this.mTransformedImageBounds, this.mImageBounds);
        if (this.mListener != null && this.isEnabled()) {
            this.mListener.onTransformChanged(this.mActiveTransform);
        }
    }

    private void onTransformEnd() {
        if (this.mListener != null && this.isEnabled()) {
            this.mListener.onTransformEnd(this.mActiveTransform);
        }
    }

    /**
     * Keeps the scaling factor within the specified limits.
     *
     * @param pivotX     x coordinate of the pivot point
     * @param pivotY     y coordinate of the pivot point
     * @param limitTypes whether to limit scale.
     * @return whether limiting has been applied or not
     */
    private boolean limitScale(
            final Matrix transform, final float pivotX, final float pivotY, @LimitFlag final int limitTypes) {
        if (!DefaultZoomableController.shouldLimit(limitTypes, DefaultZoomableController.LIMIT_SCALE)) {
            return false;
        }
        final float currentScale = this.getMatrixScaleFactor(transform);
        final float targetScale = this.limit(currentScale, this.mMinScaleFactor, this.mMaxScaleFactor);
        if (targetScale != currentScale) {
            final float scale = targetScale / currentScale;
            transform.postScale(scale, scale, pivotX, pivotY);
            return true;
        }
        return false;
    }

    /**
     * Limits the translation so that there are no empty spaces on the sides if possible.
     *
     * <p>The image is attempted to be centered within the view bounds if the transformed image is
     * smaller. There will be no empty spaces within the view bounds if the transformed image is
     * bigger. This applies to each dimension (horizontal and vertical) independently.
     *
     * @param limitTypes whether to limit translation along the specific axis.
     * @return whether limiting has been applied or not
     */
    private boolean limitTranslation(final Matrix transform, @LimitFlag final int limitTypes) {
        if (!DefaultZoomableController.shouldLimit(limitTypes, DefaultZoomableController.LIMIT_TRANSLATION_X | DefaultZoomableController.LIMIT_TRANSLATION_Y)) {
            return false;
        }
        final RectF b = this.mTempRect;
        b.set(this.mImageBounds);
        transform.mapRect(b);
        boolean shouldLimitX = DefaultZoomableController.shouldLimit(limitTypes, DefaultZoomableController.LIMIT_TRANSLATION_X);
        final float offsetLeft = shouldLimitX
                           ? this.getOffset(b.left, b.right, this.mViewBounds.left, this.mViewBounds.right, this.mImageBounds.centerX())
                           : 0;
        final float offsetTop = DefaultZoomableController.shouldLimit(limitTypes, DefaultZoomableController.LIMIT_TRANSLATION_Y)
                          ? this.getOffset(b.top, b.bottom, this.mViewBounds.top, this.mViewBounds.bottom, this.mImageBounds.centerY())
                          : 0;
        if (this.mListener != null) {
            this.mListener.onTranslationLimited(offsetLeft, offsetTop);
        }
        if (offsetLeft != 0 || offsetTop != 0) {
            transform.postTranslate(offsetLeft, offsetTop);
            return true;
        }
        return false;
    }

    /**
     * Checks whether the specified limit flag is present in the limits provided.
     *
     * <p>If the flag contains multiple flags together using a bitwise OR, this only checks that at
     * least one of the flags is included.
     *
     * @param limits the limits to apply
     * @param flag   the limit flag(s) to check for
     * @return true if the flag (or one of the flags) is included in the limits
     */
    private static boolean shouldLimit(@LimitFlag final int limits, @LimitFlag final int flag) {
        return (limits & flag) != DefaultZoomableController.LIMIT_NONE;
    }

    /**
     * Returns the offset necessary to make sure that: - the image is centered within the limit if the
     * image is smaller than the limit - there is no empty space on left/right if the image is bigger
     * than the limit
     */
    private float getOffset(
            final float imageStart, final float imageEnd, final float limitStart, final float limitEnd, final float limitCenter) {
        final float imageWidth = imageEnd - imageStart;
        final float limitWidth = limitEnd - limitStart;
        final float limitInnerWidth = Math.min(limitCenter - limitStart, limitEnd - limitCenter) * 2;
        // center if smaller than limitInnerWidth
        if (imageWidth < limitInnerWidth) {
            return limitCenter - (imageEnd + imageStart) / 2;
        }
        // to the edge if in between and limitCenter is not (limitLeft + limitRight) / 2
        if (imageWidth < limitWidth) {
            if (limitCenter < (limitStart + limitEnd) / 2) {
                return limitStart - imageStart;
            } else {
                return limitEnd - imageEnd;
            }
        }
        // to the edge if larger than limitWidth and empty space visible
        if (imageStart > limitStart) {
            return limitStart - imageStart;
        }
        if (imageEnd < limitEnd) {
            return limitEnd - imageEnd;
        }
        return 0;
    }

    /**
     * Limits the value to the given min and max range.
     */
    private float limit(final float value, final float min, final float max) {
        return Math.min(Math.max(min, value), max);
    }

    /**
     * Gets the scale factor for the given matrix. This method assumes the equal scaling factor for X
     * and Y axis.
     */
    private float getMatrixScaleFactor(final Matrix transform) {
        transform.getValues(this.mTempValues);
        return this.mTempValues[Matrix.MSCALE_X];
    }

    /**
     * Same as {@code Matrix.isIdentity()}, but with tolerance {@code eps}.
     */
    private boolean isMatrixIdentity(final Matrix transform, final float eps) {
        // Checks whether the given matrix is close enough to the identity matrix:
        //   1 0 0
        //   0 1 0
        //   0 0 1
        // Or equivalently to the zero matrix, after subtracting 1.0f from the diagonal elements:
        //   0 0 0
        //   0 0 0
        //   0 0 0
        transform.getValues(this.mTempValues);
        this.mTempValues[0] -= 1.0f; // m00
        this.mTempValues[4] -= 1.0f; // m11
        this.mTempValues[8] -= 1.0f; // m22
        for (int i = 0; i < 9; i++) {
            if (Math.abs(this.mTempValues[i]) > eps) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether the scroll can happen in all directions. I.e. the image is not on any edge.
     */
    private boolean canScrollInAllDirection() {
        return this.mTransformedImageBounds.left < this.mViewBounds.left - DefaultZoomableController.EPS
                && this.mTransformedImageBounds.top < this.mViewBounds.top - DefaultZoomableController.EPS
                && this.mTransformedImageBounds.right > this.mViewBounds.right + DefaultZoomableController.EPS
                && this.mTransformedImageBounds.bottom > this.mViewBounds.bottom + DefaultZoomableController.EPS;
    }

    @Override
    public int computeHorizontalScrollRange() {
        return (int) this.mTransformedImageBounds.width();
    }

    @Override
    public int computeHorizontalScrollOffset() {
        return (int) (this.mViewBounds.left - this.mTransformedImageBounds.left);
    }

    @Override
    public int computeHorizontalScrollExtent() {
        return (int) this.mViewBounds.width();
    }

    @Override
    public int computeVerticalScrollRange() {
        return (int) this.mTransformedImageBounds.height();
    }

    @Override
    public int computeVerticalScrollOffset() {
        return (int) (this.mViewBounds.top - this.mTransformedImageBounds.top);
    }

    @Override
    public int computeVerticalScrollExtent() {
        return (int) this.mViewBounds.height();
    }

    public Listener getListener() {
        return this.mListener;
    }
}
