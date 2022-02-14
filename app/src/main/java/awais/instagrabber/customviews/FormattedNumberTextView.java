package awais.instagrabber.customviews;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.transition.ChangeBounds;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import java.time.Duration;

import awais.instagrabber.customviews.helpers.ChangeText;
import awais.instagrabber.utils.NumberUtils;

public class FormattedNumberTextView extends AppCompatTextView {
    private static final String TAG = FormattedNumberTextView.class.getSimpleName();
    private static final Transition TRANSITION;

    private long number = Long.MIN_VALUE;
    private boolean showAbbreviation = true;
    private boolean animateChanges;
    private boolean toggleOnClick = true;
    private boolean autoToggleToAbbreviation = true;
    private long autoToggleTimeoutMs = Duration.ofSeconds(2).toMillis();
    private boolean initDone;

    static {
        TransitionSet transitionSet = new TransitionSet();
        ChangeText changeText = new ChangeText().setChangeBehavior(ChangeText.CHANGE_BEHAVIOR_OUT_IN);
        transitionSet.addTransition(changeText).addTransition(new ChangeBounds());
        TRANSITION = transitionSet;
    }


    public FormattedNumberTextView(@NonNull Context context) {
        super(context);
        this.init();
    }

    public FormattedNumberTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.init();
    }

    public FormattedNumberTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init();
    }

    private void init() {
        if (this.initDone) return;
        this.setupClickToggle();
        this.initDone = true;
    }

    private void setupClickToggle() {
        this.setOnClickListener(null);
    }

    private OnClickListener getWrappedClickListener(@Nullable OnClickListener l) {
        if (!this.toggleOnClick) {
            return l;
        }
        return v -> {
            this.toggleAbbreviation();
            if (l != null) {
                l.onClick(this);
            }
        };
    }

    public void setNumber(long number) {
        if (this.number == number) return;
        this.number = number;
        this.format();
    }

    public void clearNumber() {
        if (this.number == Long.MIN_VALUE) return;
        this.number = Long.MIN_VALUE;
        this.format();
    }

    public void setShowAbbreviation(boolean showAbbreviation) {
        if (this.showAbbreviation && showAbbreviation) return;
        this.showAbbreviation = showAbbreviation;
        this.format();
    }

    public boolean isShowAbbreviation() {
        return this.showAbbreviation;
    }

    private void toggleAbbreviation() {
        if (this.number == Long.MIN_VALUE) return;
        this.setShowAbbreviation(!this.showAbbreviation);
    }

    public void setToggleOnClick(boolean toggleOnClick) {
        this.toggleOnClick = toggleOnClick;
    }

    public boolean isToggleOnClick() {
        return this.toggleOnClick;
    }

    public void setAutoToggleToAbbreviation(boolean autoToggleToAbbreviation) {
        this.autoToggleToAbbreviation = autoToggleToAbbreviation;
    }

    public boolean isAutoToggleToAbbreviation() {
        return this.autoToggleToAbbreviation;
    }

    public void setAutoToggleTimeoutMs(long autoToggleTimeoutMs) {
        this.autoToggleTimeoutMs = autoToggleTimeoutMs;
    }

    public long getAutoToggleTimeoutMs() {
        return this.autoToggleTimeoutMs;
    }

    public void setAnimateChanges(boolean animateChanges) {
        this.animateChanges = animateChanges;
    }

    public boolean isAnimateChanges() {
        return this.animateChanges;
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        super.setOnClickListener(this.getWrappedClickListener(l));
    }

    private void format() {
        this.post(() -> {
            if (this.animateChanges) {
                try {
                    TransitionManager.beginDelayedTransition((ViewGroup) this.getParent(), FormattedNumberTextView.TRANSITION);
                } catch (final Exception e) {
                    Log.e(FormattedNumberTextView.TAG, "format: ", e);
                }
            }
            if (this.number == Long.MIN_VALUE) {
                this.setText(null);
                return;
            }
            if (this.showAbbreviation) {
                this.setText(NumberUtils.abbreviate(this.number, null));
                return;
            }
            this.setText(String.valueOf(this.number));
            if (this.autoToggleToAbbreviation) {
                this.getHandler().postDelayed(() -> this.setShowAbbreviation(true), this.autoToggleTimeoutMs);
            }
        });
    }
}
