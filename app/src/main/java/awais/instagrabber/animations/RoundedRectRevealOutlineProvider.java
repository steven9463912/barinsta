/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package awais.instagrabber.animations;

import android.graphics.Rect;

/**
 * A {@link RevealOutlineAnimation} that provides an outline that interpolates between two radii
 * and two {@link Rect}s.
 * <p>
 * An example usage of this provider is an outline that starts out as a circle and ends
 * as a rounded rectangle.
 */
public class RoundedRectRevealOutlineProvider extends RevealOutlineAnimation {
    private final float mStartRadius;
    private final float mEndRadius;

    private final Rect mStartRect;
    private final Rect mEndRect;

    public RoundedRectRevealOutlineProvider(final float startRadius, final float endRadius, final Rect startRect, final Rect endRect) {
        this.mStartRadius = startRadius;
        this.mEndRadius = endRadius;
        this.mStartRect = startRect;
        this.mEndRect = endRect;
    }

    @Override
    public boolean shouldRemoveElevationDuringAnimation() {
        return false;
    }

    @Override
    public void setProgress(final float progress) {
        this.mOutlineRadius = (1 - progress) * this.mStartRadius + progress * this.mEndRadius;

        this.mOutline.left = (int) ((1 - progress) * this.mStartRect.left + progress * this.mEndRect.left);
        this.mOutline.top = (int) ((1 - progress) * this.mStartRect.top + progress * this.mEndRect.top);
        this.mOutline.right = (int) ((1 - progress) * this.mStartRect.right + progress * this.mEndRect.right);
        this.mOutline.bottom = (int) ((1 - progress) * this.mStartRect.bottom + progress * this.mEndRect.bottom);
    }
}
