/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package awais.instagrabber.customviews.drawee;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;

import com.facebook.drawee.generic.GenericDraweeHierarchy;

import awais.instagrabber.customviews.VerticalDragHelper;

public class DraggableZoomableDraweeView extends ZoomableDraweeView {
    private static final String TAG = "DraggableZoomableDV";

    private final VerticalDragHelper verticalDragHelper;

    public DraggableZoomableDraweeView(Context context, GenericDraweeHierarchy hierarchy) {
        super(context, hierarchy);
        this.verticalDragHelper = new VerticalDragHelper(this);
    }

    public DraggableZoomableDraweeView(Context context) {
        super(context);
        this.verticalDragHelper = new VerticalDragHelper(this);
    }

    public DraggableZoomableDraweeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.verticalDragHelper = new VerticalDragHelper(this);
    }

    public DraggableZoomableDraweeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.verticalDragHelper = new VerticalDragHelper(this);
    }

    public void setOnVerticalDragListener(@NonNull VerticalDragHelper.OnVerticalDragListener onVerticalDragListener) {
        this.verticalDragHelper.setOnVerticalDragListener(onVerticalDragListener);
    }

    private int lastPointerCount;
    private int lastNewPointerCount;
    private boolean wasTransformCorrected;

    // @Override
    // protected void onTransformEnd(final Matrix transform) {
    //     super.onTransformEnd(transform);
    //     final AnimatedZoomableController zoomableController = (AnimatedZoomableController) getZoomableController();
    //     final TransformGestureDetector detector = zoomableController.getDetector();
    //     lastNewPointerCount = detector.getNewPointerCount();
    //     lastPointerCount = detector.getPointerCount();
    // }
    //
    // @Override
    // protected void onTranslationLimited(final float offsetLeft, final float offsetTop) {
    //     super.onTranslationLimited(offsetLeft, offsetTop);
    //     wasTransformCorrected = offsetTop != 0;
    // }

    // @SuppressLint("ClickableViewAccessibility")
    // @Override
    // public boolean onTouchEvent(final MotionEvent event) {
    //     boolean superResult = false;
    //     superResult = super.onTouchEvent(event);
    // if (verticalDragHelper.isDragging()) {
    //     final boolean onDragTouch = verticalDragHelper.onDragTouch(event);
    //     if (onDragTouch) {
    //         return true;
    //     }
    // }
    // if (!verticalDragHelper.isDragging()) {
    //     superResult = super.onTouchEvent(event);
    //     if (wasTransformCorrected
    //             && (lastPointerCount == 1 || lastPointerCount == 0)
    //             && (lastNewPointerCount == 1 || lastNewPointerCount == 0)) {
    //         final boolean onDragTouch = verticalDragHelper.onDragTouch(event);
    //         if (onDragTouch) {
    //             return true;
    //         }
    //     }
    // }
    // final boolean gestureListenerResult = verticalDragHelper.onGestureTouchEvent(event);
    // if (gestureListenerResult) {
    //     return true;
    // }
    // return superResult;
    // }
}
