package awais.instagrabber.customviews;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;

import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.utils.ViewUtils;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class Tooltip extends AppCompatTextView {

    private View anchor;
    private ViewPropertyAnimator animator;
    private boolean showing;

    private final AppExecutors appExecutors = AppExecutors.INSTANCE;
    private final Runnable dismissRunnable = () -> {
        this.animator = this.animate().alpha(0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(final Animator animation) {
                Tooltip.this.setVisibility(View.GONE);
            }
        }).setDuration(300);
        this.animator.start();
    };

    public Tooltip(@NonNull final Context context, @NonNull final ViewGroup parentView, final int backgroundColor, final int textColor) {
        super(context);
        this.setBackgroundDrawable(ViewUtils.createRoundRectDrawable(Utils.convertDpToPx(3), backgroundColor));
        this.setTextColor(textColor);
        this.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        this.setPadding(Utils.convertDpToPx(8), Utils.convertDpToPx(7), Utils.convertDpToPx(8), Utils.convertDpToPx(7));
        this.setGravity(Gravity.CENTER_VERTICAL);
        parentView.addView(this, ViewUtils.createFrame(WRAP_CONTENT, WRAP_CONTENT, Gravity.START | Gravity.TOP, 5, 0, 5, 3));
        this.setVisibility(View.GONE);
    }

    @Override
    protected void onLayout(final boolean changed, final int left, final int top, final int right, final int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        this.updateTooltipPosition();
    }

    private void updateTooltipPosition() {
        if (this.anchor == null) {
            return;
        }
        int top = 0;
        int left = 0;

        final View containerView = (View) this.getParent();
        View view = this.anchor;

        while (view != containerView) {
            top += view.getTop();
            left += view.getLeft();
            view = (View) view.getParent();
        }
        int x = left + this.anchor.getWidth() / 2 - this.getMeasuredWidth() / 2;
        if (x < 0) {
            x = 0;
        } else if (x + this.getMeasuredWidth() > containerView.getMeasuredWidth()) {
            x = containerView.getMeasuredWidth() - this.getMeasuredWidth() - Utils.convertDpToPx(16);
        }
        this.setTranslationX(x);

        final int y = top - this.getMeasuredHeight();
        this.setTranslationY(y);
    }

    public void show(final View anchor) {
        if (anchor == null) {
            return;
        }
        this.anchor = anchor;
        this.updateTooltipPosition();
        this.showing = true;

        this.appExecutors.getMainThread().cancel(this.dismissRunnable);
        this.appExecutors.getMainThread().execute(this.dismissRunnable, 2000);
        if (this.animator != null) {
            this.animator.setListener(null);
            this.animator.cancel();
            this.animator = null;
        }
        if (this.getVisibility() != View.VISIBLE) {
            this.setAlpha(0f);
            this.setVisibility(View.VISIBLE);
            this.animator = this.animate().setDuration(300).alpha(1f).setListener(null);
            this.animator.start();
        }
    }

    public void hide() {
        if (this.showing) {
            if (this.animator != null) {
                this.animator.setListener(null);
                this.animator.cancel();
                this.animator = null;
            }

            this.appExecutors.getMainThread().cancel(this.dismissRunnable);
            this.dismissRunnable.run();
        }
        this.showing = false;
    }
}
