package awais.instagrabber.customviews.helpers;

/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.transition.Transition;
import androidx.transition.TransitionListenerAdapter;
import androidx.transition.TransitionValues;

import java.util.Map;
import java.util.Objects;

import awais.instagrabber.BuildConfig;

/**
 * This transition tracks changes to the text in TextView targets. If the text
 * changes between the start and end scenes, the transition ensures that the
 * starting text stays until the transition ends, at which point it changes
 * to the end text.  This is useful in situations where you want to resize a
 * text view to its new size before displaying the text that goes there.
 */
public class ChangeText extends Transition {
    private static final String LOG_TAG = "TextChange";
    private static final String PROPNAME_TEXT = "android:textchange:text";
    private static final String PROPNAME_TEXT_SELECTION_START =
            "android:textchange:textSelectionStart";
    private static final String PROPNAME_TEXT_SELECTION_END =
            "android:textchange:textSelectionEnd";
    private static final String PROPNAME_TEXT_COLOR = "android:textchange:textColor";
    private int mChangeBehavior = ChangeText.CHANGE_BEHAVIOR_KEEP;
    private boolean crossFade;
    /**
     * Flag specifying that the text in affected/changing TextView targets will keep
     * their original text during the transition, setting it to the final text when
     * the transition ends. This is the default behavior.
     *
     * @see #setChangeBehavior(int)
     */
    public static final int CHANGE_BEHAVIOR_KEEP = 0;
    /**
     * Flag specifying that the text changing animation should first fade
     * out the original text completely. The new text is set on the target
     * view at the end of the fade-out animation. This transition is typically
     * used with a later {@link #CHANGE_BEHAVIOR_IN} transition, allowing more
     * flexibility than the {@link #CHANGE_BEHAVIOR_OUT_IN} by allowing other
     * transitions to be run sequentially or in parallel with these fades.
     *
     * @see #setChangeBehavior(int)
     */
    public static final int CHANGE_BEHAVIOR_OUT = 1;
    /**
     * Flag specifying that the text changing animation should fade in the
     * end text into the affected target view(s). This transition is typically
     * used in conjunction with an earlier {@link #CHANGE_BEHAVIOR_OUT}
     * transition, possibly with other transitions running as well, such as
     * a sequence to fade out, then resize the view, then fade in.
     *
     * @see #setChangeBehavior(int)
     */
    public static final int CHANGE_BEHAVIOR_IN = 2;
    /**
     * Flag specifying that the text changing animation should first fade
     * out the original text completely and then fade in the
     * new text.
     *
     * @see #setChangeBehavior(int)
     */
    public static final int CHANGE_BEHAVIOR_OUT_IN = 3;
    private static final String[] sTransitionProperties = {
            ChangeText.PROPNAME_TEXT,
            ChangeText.PROPNAME_TEXT_SELECTION_START,
            ChangeText.PROPNAME_TEXT_SELECTION_END
    };

    /**
     * Sets the type of changing animation that will be run, one of
     * {@link #CHANGE_BEHAVIOR_KEEP}, {@link #CHANGE_BEHAVIOR_OUT},
     * {@link #CHANGE_BEHAVIOR_IN}, and {@link #CHANGE_BEHAVIOR_OUT_IN}.
     *
     * @param changeBehavior The type of fading animation to use when this
     *                       transition is run.
     * @return this textChange object.
     */
    public ChangeText setChangeBehavior(final int changeBehavior) {
        if (changeBehavior >= ChangeText.CHANGE_BEHAVIOR_KEEP && changeBehavior <= ChangeText.CHANGE_BEHAVIOR_OUT_IN) {
            this.mChangeBehavior = changeBehavior;
        }
        return this;
    }

    public ChangeText setCrossFade(boolean crossFade) {
        this.crossFade = crossFade;
        return this;
    }

    @Override
    public String[] getTransitionProperties() {
        return ChangeText.sTransitionProperties;
    }

    /**
     * Returns the type of changing animation that will be run.
     *
     * @return either {@link #CHANGE_BEHAVIOR_KEEP}, {@link #CHANGE_BEHAVIOR_OUT},
     * {@link #CHANGE_BEHAVIOR_IN}, or {@link #CHANGE_BEHAVIOR_OUT_IN}.
     */
    public int getChangeBehavior() {
        return this.mChangeBehavior;
    }

    private void captureValues(final TransitionValues transitionValues) {
        if (transitionValues.view instanceof TextView) {
            final TextView textview = (TextView) transitionValues.view;
            transitionValues.values.put(ChangeText.PROPNAME_TEXT, textview.getText());
            if (textview instanceof EditText) {
                transitionValues.values.put(ChangeText.PROPNAME_TEXT_SELECTION_START,
                                            textview.getSelectionStart());
                transitionValues.values.put(ChangeText.PROPNAME_TEXT_SELECTION_END,
                                            textview.getSelectionEnd());
            }
            if (this.mChangeBehavior > ChangeText.CHANGE_BEHAVIOR_KEEP) {
                transitionValues.values.put(ChangeText.PROPNAME_TEXT_COLOR, textview.getCurrentTextColor());
            }
        }
    }

    @Override
    public void captureStartValues(@NonNull final TransitionValues transitionValues) {
        this.captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(@NonNull final TransitionValues transitionValues) {
        this.captureValues(transitionValues);
    }

    @Override
    public Animator createAnimator(@NonNull final ViewGroup sceneRoot, final TransitionValues startValues,
                                   final TransitionValues endValues) {
        if (startValues == null || endValues == null ||
                !(startValues.view instanceof TextView) || !(endValues.view instanceof TextView)) {
            return null;
        }
        TextView view = (TextView) endValues.view;
        final Map<String, Object> startVals = startValues.values;
        final Map<String, Object> endVals = endValues.values;
        CharSequence startText = startVals.get(ChangeText.PROPNAME_TEXT) != null ?
                                       (CharSequence) startVals.get(ChangeText.PROPNAME_TEXT) : "";
        CharSequence endText = endVals.get(ChangeText.PROPNAME_TEXT) != null ?
                                     (CharSequence) endVals.get(ChangeText.PROPNAME_TEXT) : "";
        int startSelectionStart;
        final int startSelectionEnd;
        final int endSelectionStart;
        int endSelectionEnd;
        if (view instanceof EditText) {
            startSelectionStart = startVals.get(ChangeText.PROPNAME_TEXT_SELECTION_START) != null ?
                                  (Integer) startVals.get(ChangeText.PROPNAME_TEXT_SELECTION_START) : -1;
            startSelectionEnd = startVals.get(ChangeText.PROPNAME_TEXT_SELECTION_END) != null ?
                                (Integer) startVals.get(ChangeText.PROPNAME_TEXT_SELECTION_END) : startSelectionStart;
            endSelectionStart = endVals.get(ChangeText.PROPNAME_TEXT_SELECTION_START) != null ?
                                (Integer) endVals.get(ChangeText.PROPNAME_TEXT_SELECTION_START) : -1;
            endSelectionEnd = endVals.get(ChangeText.PROPNAME_TEXT_SELECTION_END) != null ?
                              (Integer) endVals.get(ChangeText.PROPNAME_TEXT_SELECTION_END) : endSelectionStart;
        } else {
            startSelectionStart = startSelectionEnd = endSelectionStart = endSelectionEnd = -1;
        }
        if (!Objects.equals(startText, endText)) {
            int startColor;
            int endColor;
            if (this.mChangeBehavior != ChangeText.CHANGE_BEHAVIOR_IN) {
                view.setText(startText);
                if (view instanceof EditText) {
                    this.setSelection(((EditText) view), startSelectionStart, startSelectionEnd);
                }
            }
            final Animator anim;
            if (this.mChangeBehavior == ChangeText.CHANGE_BEHAVIOR_KEEP) {
                startColor = endColor = 0;
                anim = ValueAnimator.ofFloat(0, 1);
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(final Animator animation) {
                        if (Objects.equals(startText, view.getText())) {
                            // Only set if it hasn't been changed since anim started
                            view.setText(endText);
                            if (view instanceof EditText) {
                                ChangeText.this.setSelection(((EditText) view), endSelectionStart, endSelectionEnd);
                            }
                        }
                    }
                });
            } else {
                startColor = (Integer) startVals.get(ChangeText.PROPNAME_TEXT_COLOR);
                endColor = (Integer) endVals.get(ChangeText.PROPNAME_TEXT_COLOR);
                // Fade out start text
                ValueAnimator outAnim = null, inAnim = null;
                if (this.mChangeBehavior == ChangeText.CHANGE_BEHAVIOR_OUT_IN ||
                        this.mChangeBehavior == ChangeText.CHANGE_BEHAVIOR_OUT) {
                    outAnim = ValueAnimator.ofInt(Color.alpha(startColor), 0);
                    outAnim.addUpdateListener(animation -> {
                        final int currAlpha = (Integer) animation.getAnimatedValue();
                        view.setTextColor(currAlpha << 24 | startColor & 0xffffff);
                    });
                    outAnim.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(final Animator animation) {
                            if (Objects.equals(startText, view.getText())) {
                                // Only set if it hasn't been changed since anim started
                                view.setText(endText);
                                if (view instanceof EditText) {
                                    ChangeText.this.setSelection(((EditText) view), endSelectionStart,
                                                 endSelectionEnd);
                                }
                            }
                            // restore opaque alpha and correct end color
                            view.setTextColor(endColor);
                        }
                    });
                }
                if (this.mChangeBehavior == ChangeText.CHANGE_BEHAVIOR_OUT_IN ||
                        this.mChangeBehavior == ChangeText.CHANGE_BEHAVIOR_IN) {
                    inAnim = ValueAnimator.ofInt(0, Color.alpha(endColor));
                    inAnim.addUpdateListener(animation -> {
                        final int currAlpha = (Integer) animation.getAnimatedValue();
                        view.setTextColor(currAlpha << 24 | endColor & 0xffffff);
                    });
                    inAnim.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationCancel(final Animator animation) {
                            // restore opaque alpha and correct end color
                            view.setTextColor(endColor);
                        }
                    });
                }
                if (outAnim != null && inAnim != null) {
                    anim = new AnimatorSet();
                    AnimatorSet animatorSet = (AnimatorSet) anim;
                    if (this.crossFade) {
                        animatorSet.playTogether(outAnim, inAnim);
                    } else {
                        animatorSet.playSequentially(outAnim, inAnim);
                    }
                } else if (outAnim != null) {
                    anim = outAnim;
                } else {
                    // Must be an in-only animation
                    anim = inAnim;
                }
            }
            final TransitionListener transitionListener = new TransitionListenerAdapter() {
                int mPausedColor;

                @Override
                public void onTransitionPause(@NonNull final Transition transition) {
                    if (ChangeText.this.mChangeBehavior != ChangeText.CHANGE_BEHAVIOR_IN) {
                        view.setText(endText);
                        if (view instanceof EditText) {
                            ChangeText.this.setSelection(((EditText) view), endSelectionStart, endSelectionEnd);
                        }
                    }
                    if (ChangeText.this.mChangeBehavior > ChangeText.CHANGE_BEHAVIOR_KEEP) {
                        this.mPausedColor = view.getCurrentTextColor();
                        view.setTextColor(endColor);
                    }
                }

                @Override
                public void onTransitionResume(@NonNull final Transition transition) {
                    if (ChangeText.this.mChangeBehavior != ChangeText.CHANGE_BEHAVIOR_IN) {
                        view.setText(startText);
                        if (view instanceof EditText) {
                            ChangeText.this.setSelection(((EditText) view), startSelectionStart, startSelectionEnd);
                        }
                    }
                    if (ChangeText.this.mChangeBehavior > ChangeText.CHANGE_BEHAVIOR_KEEP) {
                        view.setTextColor(this.mPausedColor);
                    }
                }

                @Override
                public void onTransitionEnd(final Transition transition) {
                    transition.removeListener(this);
                }
            };
            this.addListener(transitionListener);
            if (BuildConfig.DEBUG) {
                Log.d(ChangeText.LOG_TAG, "createAnimator returning " + anim);
            }
            return anim;
        }
        return null;
    }

    private void setSelection(final EditText editText, final int start, final int end) {
        if (start >= 0 && end >= 0) {
            editText.setSelection(start, end);
        }
    }
}
