/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package awais.instagrabber.customviews.drawee;

import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * Wrapper for SimpleOnGestureListener as GestureDetector does not allow changing its listener.
 */
public class GestureListenerWrapper extends GestureDetector.SimpleOnGestureListener {

    private GestureDetector.SimpleOnGestureListener mDelegate;

    public GestureListenerWrapper() {
        this.mDelegate = new GestureDetector.SimpleOnGestureListener();
    }

    public void setListener(final GestureDetector.SimpleOnGestureListener listener) {
        this.mDelegate = listener;
    }

    @Override
    public void onLongPress(final MotionEvent e) {
        this.mDelegate.onLongPress(e);
    }

    @Override
    public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX, final float distanceY) {
        return this.mDelegate.onScroll(e1, e2, distanceX, distanceY);
    }

    @Override
    public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX, final float velocityY) {
        return this.mDelegate.onFling(e1, e2, velocityX, velocityY);
    }

    @Override
    public void onShowPress(final MotionEvent e) {
        this.mDelegate.onShowPress(e);
    }

    @Override
    public boolean onDown(final MotionEvent e) {
        return this.mDelegate.onDown(e);
    }

    @Override
    public boolean onDoubleTap(final MotionEvent e) {
        return this.mDelegate.onDoubleTap(e);
    }

    @Override
    public boolean onDoubleTapEvent(final MotionEvent e) {
        return this.mDelegate.onDoubleTapEvent(e);
    }

    @Override
    public boolean onSingleTapConfirmed(final MotionEvent e) {
        return this.mDelegate.onSingleTapConfirmed(e);
    }

    @Override
    public boolean onSingleTapUp(final MotionEvent e) {
        return this.mDelegate.onSingleTapUp(e);
    }
}
