/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package awais.instagrabber.customviews.drawee;

import android.graphics.PointF;
import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * Tap gesture listener for double tap to zoom / unzoom and double-tap-and-drag to zoom.
 *
 * @see ZoomableDraweeView#setTapListener(GestureDetector.SimpleOnGestureListener)
 */
public class DoubleTapGestureListener extends GestureDetector.SimpleOnGestureListener {
    private static final int DURATION_MS = 300;
    private static final int DOUBLE_TAP_SCROLL_THRESHOLD = 20;

    private final ZoomableDraweeView mDraweeView;
    private final PointF mDoubleTapViewPoint = new PointF();
    private final PointF mDoubleTapImagePoint = new PointF();
    private float mDoubleTapScale = 1;
    private boolean mDoubleTapScroll;

    public DoubleTapGestureListener(final ZoomableDraweeView zoomableDraweeView) {
        this.mDraweeView = zoomableDraweeView;
    }

    @Override
    public boolean onDoubleTapEvent(final MotionEvent e) {
        final AbstractAnimatedZoomableController zc =
                (AbstractAnimatedZoomableController) this.mDraweeView.getZoomableController();
        final PointF vp = new PointF(e.getX(), e.getY());
        final PointF ip = zc.mapViewToImage(vp);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                this.mDoubleTapViewPoint.set(vp);
                this.mDoubleTapImagePoint.set(ip);
                this.mDoubleTapScale = zc.getScaleFactor();
                break;
            case MotionEvent.ACTION_MOVE:
                this.mDoubleTapScroll = this.mDoubleTapScroll || this.shouldStartDoubleTapScroll(vp);
                if (this.mDoubleTapScroll) {
                    final float scale = this.calcScale(vp);
                    zc.zoomToPoint(scale, this.mDoubleTapImagePoint, this.mDoubleTapViewPoint);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (this.mDoubleTapScroll) {
                    final float scale = this.calcScale(vp);
                    zc.zoomToPoint(scale, this.mDoubleTapImagePoint, this.mDoubleTapViewPoint);
                } else {
                    float maxScale = zc.getMaxScaleFactor();
                    float minScale = zc.getMinScaleFactor();
                    if (zc.getScaleFactor() < (maxScale + minScale) / 2) {
                        zc.zoomToPoint(
                                maxScale, ip, vp, DefaultZoomableController.LIMIT_ALL, DoubleTapGestureListener.DURATION_MS, null);
                    } else {
                        zc.zoomToPoint(
                                minScale, ip, vp, DefaultZoomableController.LIMIT_ALL, DoubleTapGestureListener.DURATION_MS, null);
                    }
                }
                this.mDoubleTapScroll = false;
                break;
        }
        return true;
    }

    private boolean shouldStartDoubleTapScroll(final PointF viewPoint) {
        final double dist =
                Math.hypot(viewPoint.x - this.mDoubleTapViewPoint.x, viewPoint.y - this.mDoubleTapViewPoint.y);
        return dist > DoubleTapGestureListener.DOUBLE_TAP_SCROLL_THRESHOLD;
    }

    private float calcScale(final PointF currentViewPoint) {
        final float dy = (currentViewPoint.y - this.mDoubleTapViewPoint.y);
        final float t = 1 + Math.abs(dy) * 0.001f;
        return (dy < 0) ? this.mDoubleTapScale / t : this.mDoubleTapScale * t;
    }
}
