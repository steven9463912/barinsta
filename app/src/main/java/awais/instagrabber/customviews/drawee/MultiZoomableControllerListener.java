/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package awais.instagrabber.customviews.drawee;

import android.graphics.Matrix;

import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of {@link ZoomableController.Listener} that allows multiple child listeners to
 * be added and notified about {@link ZoomableController} events.
 */
public class MultiZoomableControllerListener implements ZoomableController.Listener {

    private final List<ZoomableController.Listener> mListeners = new ArrayList<>();

    @Override
    public synchronized void onTransformBegin(final Matrix transform) {
        for (final ZoomableController.Listener listener : this.mListeners) {
            listener.onTransformBegin(transform);
        }
    }

    @Override
    public synchronized void onTransformChanged(final Matrix transform) {
        for (final ZoomableController.Listener listener : this.mListeners) {
            listener.onTransformChanged(transform);
        }
    }

    @Override
    public synchronized void onTransformEnd(final Matrix transform) {
        for (final ZoomableController.Listener listener : this.mListeners) {
            listener.onTransformEnd(transform);
        }
    }

    @Override
    public void onTranslationLimited(float offsetLeft, float offsetTop) {
        for (final ZoomableController.Listener listener : this.mListeners) {
            listener.onTranslationLimited(offsetLeft, offsetTop);
        }
    }

    public synchronized void addListener(final ZoomableController.Listener listener) {
        this.mListeners.add(listener);
    }

    public synchronized void removeListener(final ZoomableController.Listener listener) {
        this.mListeners.remove(listener);
    }
}
