package awais.instagrabber.customviews.helpers;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsAnimationCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;

/**
 * A customized {@link TranslateDeferringInsetsAnimationCallback} for the emoji picker
 */
public class EmojiPickerInsetsAnimationCallback extends WindowInsetsAnimationCompat.Callback {
    private static final String TAG = EmojiPickerInsetsAnimationCallback.class.getSimpleName();

    private final View view;
    private final int persistentInsetTypes;
    private final int deferredInsetTypes;

    private int kbHeight;
    private onKbVisibilityChangeListener listener;
    private boolean shouldTranslate;

    public EmojiPickerInsetsAnimationCallback(View view,
                                              int persistentInsetTypes,
                                              int deferredInsetTypes) {
        this(view, persistentInsetTypes, deferredInsetTypes, Callback.DISPATCH_MODE_STOP);
    }

    public EmojiPickerInsetsAnimationCallback(View view,
                                              int persistentInsetTypes,
                                              int deferredInsetTypes,
                                              int dispatchMode) {
        super(dispatchMode);
        if ((persistentInsetTypes & deferredInsetTypes) != 0) {
            throw new IllegalArgumentException("persistentInsetTypes and deferredInsetTypes can not contain " +
                                                       "any of same WindowInsetsCompat.Type values");
        }
        this.view = view;
        this.persistentInsetTypes = persistentInsetTypes;
        this.deferredInsetTypes = deferredInsetTypes;
    }

    @NonNull
    @Override
    public WindowInsetsCompat onProgress(@NonNull WindowInsetsCompat insets,
                                         @NonNull List<WindowInsetsAnimationCompat> runningAnimations) {
        // onProgress() is called when any of the running animations progress...

        // First we get the insets which are potentially deferred
        Insets typesInset = insets.getInsets(this.deferredInsetTypes);
        // Then we get the persistent inset types which are applied as padding during layout
        Insets otherInset = insets.getInsets(this.persistentInsetTypes);

        // Now that we subtract the two insets, to calculate the difference. We also coerce
        // the insets to be >= 0, to make sure we don't use negative insets.
        Insets subtract = Insets.subtract(typesInset, otherInset);
        Insets diff = Insets.max(subtract, Insets.NONE);

        // The resulting `diff` insets contain the values for us to apply as a translation
        // to the view
        this.view.setTranslationX(diff.left - diff.right);
        this.view.setTranslationY(this.shouldTranslate ? diff.top - diff.bottom : -this.kbHeight);

        return insets;
    }

    @Override
    public void onEnd(@NonNull WindowInsetsAnimationCompat animation) {
        try {
            WindowInsetsCompat rootWindowInsets = ViewCompat.getRootWindowInsets(this.view);
            if (this.kbHeight == 0) {
                if (rootWindowInsets == null) return;
                Insets imeInsets = rootWindowInsets.getInsets(WindowInsetsCompat.Type.ime());
                Insets navBarInsets = rootWindowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
                this.kbHeight = imeInsets.bottom - navBarInsets.bottom;
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) this.view.getLayoutParams();
                if (layoutParams != null) {
                    layoutParams.height = this.kbHeight;
                    layoutParams.setMargins(layoutParams.leftMargin, layoutParams.topMargin, layoutParams.rightMargin, -this.kbHeight);
                }
            }
            this.view.setTranslationX(0f);
            boolean visible = rootWindowInsets != null && rootWindowInsets.isVisible(WindowInsetsCompat.Type.ime());
            float translationY = 0;
            if (!this.shouldTranslate) {
                translationY = -this.kbHeight;
                if (visible) {
                    translationY = 0;
                }
            }
            this.view.setTranslationY(translationY);

            if (this.listener != null && rootWindowInsets != null) {
                this.listener.onChange(visible);
            }
        } finally {
            this.shouldTranslate = true;
        }
    }

    public void setShouldTranslate(boolean shouldTranslate) {
        this.shouldTranslate = shouldTranslate;
    }

    public void setKbVisibilityListener(onKbVisibilityChangeListener listener) {
        this.listener = listener;
    }

    public interface onKbVisibilityChangeListener {
        void onChange(boolean isVisible);
    }
}
