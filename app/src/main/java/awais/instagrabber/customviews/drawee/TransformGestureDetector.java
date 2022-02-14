/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package awais.instagrabber.customviews.drawee;

import android.view.MotionEvent;

/**
 * Component that detects translation, scale and rotation based on touch events.
 *
 * <p>This class notifies its listeners whenever a gesture begins, updates or ends. The instance of
 * this detector is passed to the listeners, so it can be queried for pivot, translation, scale or
 * rotation.
 */
public class TransformGestureDetector implements MultiPointerGestureDetector.Listener {

    /**
     * The listener for receiving notifications when gestures occur.
     */
    public interface Listener {
        /**
         * A callback called right before the gesture is about to start.
         */
        void onGestureBegin(TransformGestureDetector detector);

        /**
         * A callback called each time the gesture gets updated.
         */
        void onGestureUpdate(TransformGestureDetector detector);

        /**
         * A callback called right after the gesture has finished.
         */
        void onGestureEnd(TransformGestureDetector detector);
    }

    private final MultiPointerGestureDetector mDetector;

    private Listener mListener;

    public TransformGestureDetector(final MultiPointerGestureDetector multiPointerGestureDetector) {
        this.mDetector = multiPointerGestureDetector;
        this.mDetector.setListener(this);
    }

    /**
     * Factory method that creates a new instance of TransformGestureDetector
     */
    public static TransformGestureDetector newInstance() {
        return new TransformGestureDetector(MultiPointerGestureDetector.newInstance());
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
        this.mDetector.reset();
    }

    /**
     * Handles the given motion event.
     *
     * @param event event to handle
     * @return whether or not the event was handled
     */
    public boolean onTouchEvent(MotionEvent event) {
        return this.mDetector.onTouchEvent(event);
    }

    @Override
    public void onGestureBegin(final MultiPointerGestureDetector detector) {
        if (this.mListener != null) {
            this.mListener.onGestureBegin(this);
        }
    }

    @Override
    public void onGestureUpdate(final MultiPointerGestureDetector detector) {
        if (this.mListener != null) {
            this.mListener.onGestureUpdate(this);
        }
    }

    @Override
    public void onGestureEnd(final MultiPointerGestureDetector detector) {
        if (this.mListener != null) {
            this.mListener.onGestureEnd(this);
        }
    }

    private float calcAverage(final float[] arr, final int len) {
        float sum = 0;
        for (int i = 0; i < len; i++) {
            sum += arr[i];
        }
        return (len > 0) ? sum / len : 0;
    }

    /**
     * Restarts the current gesture (if any).
     */
    public void restartGesture() {
        this.mDetector.restartGesture();
    }

    /**
     * Gets whether there is a gesture in progress
     */
    public boolean isGestureInProgress() {
        return this.mDetector.isGestureInProgress();
    }

    /**
     * Gets the number of pointers after the current gesture
     */
    public int getNewPointerCount() {
        return this.mDetector.getNewPointerCount();
    }

    /**
     * Gets the number of pointers in the current gesture
     */
    public int getPointerCount() {
        return this.mDetector.getPointerCount();
    }

    /**
     * Gets the X coordinate of the pivot point
     */
    public float getPivotX() {
        return this.calcAverage(this.mDetector.getStartX(), this.mDetector.getPointerCount());
    }

    /**
     * Gets the Y coordinate of the pivot point
     */
    public float getPivotY() {
        return this.calcAverage(this.mDetector.getStartY(), this.mDetector.getPointerCount());
    }

    /**
     * Gets the X component of the translation
     */
    public float getTranslationX() {
        return this.calcAverage(this.mDetector.getCurrentX(), this.mDetector.getPointerCount())
                - this.calcAverage(this.mDetector.getStartX(), this.mDetector.getPointerCount());
    }

    /**
     * Gets the Y component of the translation
     */
    public float getTranslationY() {
        return this.calcAverage(this.mDetector.getCurrentY(), this.mDetector.getPointerCount())
                - this.calcAverage(this.mDetector.getStartY(), this.mDetector.getPointerCount());
    }

    /**
     * Gets the scale
     */
    public float getScale() {
        if (this.mDetector.getPointerCount() < 2) {
            return 1;
        } else {
            final float startDeltaX = this.mDetector.getStartX()[1] - this.mDetector.getStartX()[0];
            final float startDeltaY = this.mDetector.getStartY()[1] - this.mDetector.getStartY()[0];
            final float currentDeltaX = this.mDetector.getCurrentX()[1] - this.mDetector.getCurrentX()[0];
            final float currentDeltaY = this.mDetector.getCurrentY()[1] - this.mDetector.getCurrentY()[0];
            final float startDist = (float) Math.hypot(startDeltaX, startDeltaY);
            final float currentDist = (float) Math.hypot(currentDeltaX, currentDeltaY);
            return currentDist / startDist;
        }
    }

    /**
     * Gets the rotation in radians
     */
    public float getRotation() {
        if (this.mDetector.getPointerCount() < 2) {
            return 0;
        } else {
            final float startDeltaX = this.mDetector.getStartX()[1] - this.mDetector.getStartX()[0];
            final float startDeltaY = this.mDetector.getStartY()[1] - this.mDetector.getStartY()[0];
            final float currentDeltaX = this.mDetector.getCurrentX()[1] - this.mDetector.getCurrentX()[0];
            final float currentDeltaY = this.mDetector.getCurrentY()[1] - this.mDetector.getCurrentY()[0];
            final float startAngle = (float) Math.atan2(startDeltaY, startDeltaX);
            final float currentAngle = (float) Math.atan2(currentDeltaY, currentDeltaX);
            return currentAngle - startAngle;
        }
    }
}
