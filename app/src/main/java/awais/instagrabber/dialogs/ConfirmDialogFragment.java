package awais.instagrabber.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import awais.instagrabber.R;

public class ConfirmDialogFragment extends DialogFragment {
    private Context context;
    private ConfirmDialogFragmentCallback callback;

    private final int defaultPositiveButtonText = R.string.ok;
    // private final int defaultNegativeButtonText = R.string.cancel;

    @NonNull
    public static ConfirmDialogFragment newInstance(int requestCode,
                                                    @StringRes int title,
                                                    @NonNull CharSequence message,
                                                    @StringRes int positiveText,
                                                    @StringRes int negativeText,
                                                    @StringRes int neutralText) {
        return ConfirmDialogFragment.newInstance(requestCode, title, 0, message, positiveText, negativeText, neutralText);
    }

    @NonNull
    public static ConfirmDialogFragment newInstance(int requestCode,
                                                    @StringRes int title,
                                                    @StringRes int messageResId,
                                                    @StringRes int positiveText,
                                                    @StringRes int negativeText,
                                                    @StringRes int neutralText) {
        return ConfirmDialogFragment.newInstance(requestCode, title, messageResId, null, positiveText, negativeText, neutralText);
    }

    @NonNull
    private static ConfirmDialogFragment newInstance(int requestCode,
                                                     @StringRes int title,
                                                     @StringRes int messageResId,
                                                     @Nullable CharSequence message,
                                                     @StringRes int positiveText,
                                                     @StringRes int negativeText,
                                                     @StringRes int neutralText) {
        final Bundle args = new Bundle();
        args.putInt("requestCode", requestCode);
        if (title != 0) {
            args.putInt("title", title);
        }
        if (messageResId != 0) {
            args.putInt("messageResId", messageResId);
        } else if (message != null) {
            args.putCharSequence("message", message);
        }
        if (positiveText != 0) {
            args.putInt("positive", positiveText);
        }
        if (negativeText != 0) {
            args.putInt("negative", negativeText);
        }
        if (neutralText != 0) {
            args.putInt("neutral", neutralText);
        }
        final ConfirmDialogFragment fragment = new ConfirmDialogFragment();
        fragment.setArguments(args);
        return fragment;

    }

    public ConfirmDialogFragment() {}

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        Fragment parentFragment = this.getParentFragment();
        if (parentFragment instanceof ConfirmDialogFragmentCallback) {
            this.callback = (ConfirmDialogFragmentCallback) parentFragment;
            return;
        }
        FragmentActivity fragmentActivity = this.getActivity();
        if (fragmentActivity instanceof ConfirmDialogFragmentCallback) {
            this.callback = (ConfirmDialogFragmentCallback) fragmentActivity;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle arguments = this.getArguments();
        int title = 0;
        int messageResId = 0;
        CharSequence message = null;
        int neutralButtonText = 0;
        int negativeButtonText = 0;

        int positiveButtonText;
        int requestCode;
        if (arguments != null) {
            title = arguments.getInt("title", 0);
            messageResId = arguments.getInt("messageResId", 0);
            message = arguments.getCharSequence("message", null);
            positiveButtonText = arguments.getInt("positive", this.defaultPositiveButtonText);
            negativeButtonText = arguments.getInt("negative", 0);
            neutralButtonText = arguments.getInt("neutral", 0);
            requestCode = arguments.getInt("requestCode", 0);
        } else {
            requestCode = 0;
            positiveButtonText = this.defaultPositiveButtonText;
        }
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this.context)
                .setPositiveButton(positiveButtonText, (d, w) -> {
                    if (this.callback == null) return;
                    this.callback.onPositiveButtonClicked(requestCode);
                });
        if (title != 0) {
            builder.setTitle(title);
        }
        if (messageResId != 0) {
            builder.setMessage(messageResId);
        } else if (message != null) {
            builder.setMessage(message);
        }
        if (negativeButtonText != 0) {
            builder.setNegativeButton(negativeButtonText, (dialog, which) -> {
                if (this.callback == null) return;
                this.callback.onNegativeButtonClicked(requestCode);
            });
        }
        if (neutralButtonText != 0) {
            builder.setNeutralButton(neutralButtonText, (dialog, which) -> {
                if (this.callback == null) return;
                this.callback.onNeutralButtonClicked(requestCode);
            });
        }
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = this.getDialog();
        if (dialog == null) return;
        TextView view = dialog.findViewById(android.R.id.message);
        view.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public interface ConfirmDialogFragmentCallback {
        void onPositiveButtonClicked(int requestCode);

        void onNegativeButtonClicked(int requestCode);

        void onNeutralButtonClicked(int requestCode);
    }
}
