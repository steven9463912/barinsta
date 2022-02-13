/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package awais.instagrabber.customviews.drawee;

import android.view.MotionEvent;

/**
 * Component that detects and tracks multiple pointers based on touch events.
 *
 * <p>Each time a pointer gets pressed or released, the current gesture (if any) will end, and a new
 * one will be started (if there are still pressed pointers left). It is guaranteed that the number
 * of pointers within the single gesture will remain the same during the whole gesture.
 */
public class MultiPointerGestureDetector {

    /**
     * The listener for receiving notifications when gestures occur.
     */
    public interface Listener {
        /**
         * A callback called right before the gesture is about to start.
         */
        void onGestureBegin(MultiPointerGestureDetector detector);

        /**
         * A callback called each time the gesture gets updated.
         */
        void onGestureUpdate(MultiPointerGestureDetector detector);

        /**
         * A callback called right after the gesture has finished.
         */
        void onGestureEnd(MultiPointerGestureDetector detector);
    }

    private static final int MAX_POINTERS = 2;

    private boolean mGestureInProgress;
    private int mPointerCount;
    private int mNewPointerCount;
    private final int[] mId = new int[MultiPointerGestureDetector.MAX_POINTERS];
    private final float[] mStartX = new float[MultiPointerGestureDetector.MAX_POINTERS];
    private final float[] mStartY = new float[MultiPointerGestureDetector.MAX_POINTERS];
    private final float[] mCurrentX = new float[MultiPointerGestureDetector.MAX_POINTERS];
    private final float[] mCurrentY = new float[MultiPointerGestureDetector.MAX_POINTERS];

    private Listener mListener;

    public MultiPointerGestureDetector() {
        this.reset();
    }

    /**
     * Factory method that creates a new instance of MultiPointerGestureDetector
     */
    public static MultiPointerGestureDetector newInstance() {
        return new MultiPointerGestureDetector();
    }

    /**
     * Sets the listener.
     *
     * @param listener listener to set
     */
    public void setListener(final Listener listener) {
        this.mListener = listener;
    }

    /**
     * Resets the component to the initial state.
     */
    public void reset() {
        this.mGestureInProgress = false;
        this.mPointerCount = 0;
        for (int i = 0; i < MultiPointerGestureDetector.MAX_POINTERS; i++) {
            this.mId[i] = MotionEvent.INVALID_POINTER_ID;
        }
    }

    /**
     * This method can be overridden in order to perform threshold check or something similar.
     *
     * @return whether or not to start a new gesture
     */
    protected boolean shouldStartGesture() {
        return true;
    }

    /**
     * Starts a new gesture and calls the listener just before starting it.
     */
    private void startGesture() {
        if (!this.mGestureInProgress) {
            if (this.mListener != null) {
                this.mListener.onGestureBegin(this);
            }
            this.mGestureInProgress = true;
        }
    }

    /**
     * Stops the current gesture and calls the listener right after stopping it.
     */
    private void stopGesture() {
        if (this.mGestureInProgress) {
            this.mGestureInProgress = false;
            if (this.mListener != null) {
                this.mListener.onGestureEnd(this);
            }
        }
    }

    /**
     * Gets the index of the i-th pressed pointer. Normally, the index will be equal to i, except in
     * the case when the pointer is released.
     *
     * @return index of the specified pointer or -1 if not found (i.e. not enough pointers are down)
     */
    private int getPressedPointerIndex(final MotionEvent event, int i) {
        int count = event.getPointerCount();
        int action = event.getActionMasked();
        int index = event.getActionIndex();
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            if (i >= index) {
                i++;
            }
        }
        return (i < count) ? i : -1;
    }

    /**
     * Gets the number of pressed pointers (fingers down).
     */
    private static int getPressedPointerCount(final MotionEvent event) {
        int count = event.getPointerCount();
        final int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            count--;
        }
        return count;
    }

    private void updatePointersOnTap(final MotionEvent event) {
        this.mPointerCount = 0;
        for (int i = 0; i < MultiPointerGestureDetector.MAX_POINTERS; i++) {
            final int index = this.getPressedPointerIndex(event, i);
            if (index == -1) {
                this.mId[i] = MotionEvent.INVALID_POINTER_ID;
            } else {
                this.mId[i] = event.getPointerId(index);
                this.mCurrentX[i] = this.mStartX[i] = event.getX(index);
                this.mCurrentY[i] = this.mStartY[i] = event.getY(index);
                this.mPointerCount++;
            }
        }
    }

    private void updatePointersOnMove(final MotionEvent event) {
        for (int i = 0; i < MultiPointerGestureDetector.MAX_POINTERS; i++) {
            final int index = event.findPointerIndex(this.mId[i]);
            if (index != -1) {
                this.mCurrentX[i] = event.getX(index);
                this.mCurrentY[i] = event.getY(index);
            }
        }
    }

    /**
     * Handles the given motion event.
     *
     * @param event event to handle
     * @return whether or not the event was handled
     */
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE: {
                // update pointers
                this.updatePointersOnMove(event);
                // start a new gesture if not already started
                if (!this.mGestureInProgress && this.mPointerCount > 0 && this.shouldStartGesture()) {
                    this.startGesture();
                }
                // notify listener
                if (this.mGestureInProgress && this.mListener != null) {
                    this.mListener.onGestureUpdate(this);
                }
                break;
            }

            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP: {
                // restart gesture whenever the number of pointers changes
                this.mNewPointerCount = MultiPointerGestureDetector.getPressedPointerCount(event);
                this.stopGesture();
                this.updatePointersOnTap(event);
                if (this.mPointerCount > 0 && this.shouldStartGesture()) {
                    this.startGesture();
                }
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                this.mNewPointerCount = 0;
                this.stopGesture();
                this.reset();
                break;
            }
        }
        return true;
    }

    /**
     * Restarts the current gesture (if any).
     */
    public void restartGesture() {
        if (!this.mGestureInProgress) {
            return;
        }
        this.stopGesture();
        for (int i = 0; i < MultiPointerGestureDetector.MAX_POINTERS; i++) {
            this.mStartX[i] = this.mCurrentX[i];
            this.mStartY[i] = this.mCurrentY[i];
        }
        this.startGesture();
    }

    /**
     * Gets whether there is a gesture in progress
     */
    public boolean isGestureInProgress() {
        return this.mGestureInProgress;
    }

    /**
     * Gets the number of pointers after the current gesture
     */
    public int getNewPointerCount() {
        return this.mNewPointerCount;
    }

    /**
     * Gets the number of pointers in the current gesture
     */
    public int getPointerCount() {
        return this.mPointerCount;
    }

    /**
     * Gets the start X coordinates for the all pointers Mutable array is exposed for performance
     * reasons and is not to be modified by the callers.
     */
    public float[] getStartX() {
        return this.mStartX;
    }

    /**
     * Gets the start Y coordinates for the all pointers Mutable array is exposed for performance
     * reasons and is not to be modified by the callers.
     */
    public float[] getStartY() {
        return this.mStartY;
    }

    /**
     * Gets the current X coordinates for the all pointers Mutable array is exposed for performance
     * reasons and is not to be modified by the callers.
     */
    public float[] getCurrentX() {
        return this.mCurrentX;
    }

    /**
     * Gets the current Y coordinates for the all pointers Mutable array is exposed for performance
     * reasons and is not to be modified by the callers.
     */
    public float[] getCurrentY() {
        return this.mCurrentY;
    }
}
