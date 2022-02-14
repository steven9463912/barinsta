/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package awais.instagrabber.customviews.drawee;

import android.view.GestureDetector;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Gesture listener that allows multiple child listeners to be added and notified about gesture
 * events.
 *
 * <p>NOTE: The order of the listeners is important. Listeners can consume gesture events. For
 * example, if one of the child listeners consumes {@link #onLongPress(MotionEvent)} (the listener
 * returned true), subsequent listeners will not be notified about the event any more since it has
 * been consumed.
 */
public class MultiGestureListener extends GestureDetector.SimpleOnGestureListener {

    private final List<GestureDetector.SimpleOnGestureListener> mListeners = new ArrayList<>();

    /**
     * Adds a listener to the multi gesture listener.
     *
     * <p>NOTE: The order of the listeners is important since gesture events can be consumed.
     *
     * @param listener the listener to be added
     */
    public synchronized void addListener(final GestureDetector.SimpleOnGestureListener listener) {
        this.mListeners.add(listener);
    }

    /**
     * Removes the given listener so that it will not be notified about future events.
     *
     * <p>NOTE: The order of the listeners is important since gesture events can be consumed.
     *
     * @param listener the listener to remove
     */
    public synchronized void removeListener(final GestureDetector.SimpleOnGestureListener listener) {
        this.mListeners.remove(listener);
    }

    @Override
    public synchronized boolean onSingleTapUp(final MotionEvent e) {
        int size = this.mListeners.size();
        for (int i = 0; i < size; i++) {
            if (this.mListeners.get(i).onSingleTapUp(e)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized void onLongPress(final MotionEvent e) {
        int size = this.mListeners.size();
        for (int i = 0; i < size; i++) {
            this.mListeners.get(i).onLongPress(e);
        }
    }

    @Override
    public synchronized boolean onScroll(
            final MotionEvent e1, final MotionEvent e2, final float distanceX, final float distanceY) {
        int size = this.mListeners.size();
        for (int i = 0; i < size; i++) {
            if (this.mListeners.get(i).onScroll(e1, e2, distanceX, distanceY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean onFling(
            final MotionEvent e1, final MotionEvent e2, final float velocityX, final float velocityY) {
        int size = this.mListeners.size();
        for (int i = 0; i < size; i++) {
            if (this.mListeners.get(i).onFling(e1, e2, velocityX, velocityY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized void onShowPress(final MotionEvent e) {
        int size = this.mListeners.size();
        for (int i = 0; i < size; i++) {
            this.mListeners.get(i).onShowPress(e);
        }
    }

    @Override
    public synchronized boolean onDown(final MotionEvent e) {
        int size = this.mListeners.size();
        for (int i = 0; i < size; i++) {
            if (this.mListeners.get(i).onDown(e)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean onDoubleTap(final MotionEvent e) {
        int size = this.mListeners.size();
        for (int i = 0; i < size; i++) {
            if (this.mListeners.get(i).onDoubleTap(e)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean onDoubleTapEvent(final MotionEvent e) {
        int size = this.mListeners.size();
        for (int i = 0; i < size; i++) {
            if (this.mListeners.get(i).onDoubleTapEvent(e)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean onSingleTapConfirmed(final MotionEvent e) {
        int size = this.mListeners.size();
        for (int i = 0; i < size; i++) {
            if (this.mListeners.get(i).onSingleTapConfirmed(e)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean onContextClick(final MotionEvent e) {
        int size = this.mListeners.size();
        for (int i = 0; i < size; i++) {
            if (this.mListeners.get(i).onContextClick(e)) {
                return true;
            }
        }
        return false;
    }
}
