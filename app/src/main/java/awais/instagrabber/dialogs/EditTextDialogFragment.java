package awais.instagrabber.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import awais.instagrabber.R;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

public class EditTextDialogFragment extends DialogFragment {

    private final int margin;
    private final int topMargin;

    private Context context;
    private EditTextDialogFragmentCallback callback;

    public static EditTextDialogFragment newInstance(@StringRes int title,
                                                     @StringRes int positiveText,
                                                     @StringRes int negativeText,
                                                     @Nullable String initialText) {
        final Bundle args = new Bundle();
        args.putInt("title", title);
        args.putInt("positive", positiveText);
        args.putInt("negative", negativeText);
        args.putString("initial", initialText);
        final EditTextDialogFragment fragment = new EditTextDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public EditTextDialogFragment() {
        this.margin = Utils.convertDpToPx(20);
        this.topMargin = Utils.convertDpToPx(8);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            this.callback = (EditTextDialogFragmentCallback) this.getParentFragment();
        } catch (final ClassCastException e) {
            throw new ClassCastException("Calling fragment must implement EditTextDialogFragmentCallback interface");
        }
        this.context = context;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle arguments = this.getArguments();
        int title = -1;
        int positiveButtonText = R.string.ok;
        int negativeButtonText = R.string.cancel;
        String initialText = null;
        if (arguments != null) {
            title = arguments.getInt("title", -1);
            positiveButtonText = arguments.getInt("positive", R.string.ok);
            negativeButtonText = arguments.getInt("negative", R.string.cancel);
            initialText = arguments.getString("initial", null);
        }
        AppCompatEditText input = new AppCompatEditText(this.context);
        if (!TextUtils.isEmpty(initialText)) {
            input.setText(initialText);
        }
        FrameLayout container = new FrameLayout(this.context);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                                   ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.leftMargin = this.margin;
        layoutParams.rightMargin = this.margin;
        layoutParams.topMargin = this.topMargin;
        input.setLayoutParams(layoutParams);
        container.addView(input);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this.context)
                .setView(container)
                .setPositiveButton(positiveButtonText, (d, w) -> {
                    String string = input.getText() != null ? input.getText().toString() : "";
                    if (this.callback != null) {
                        this.callback.onPositiveButtonClicked(string);
                    }
                })
                .setNegativeButton(negativeButtonText, (dialog, which) -> {
                    if (this.callback != null) {
                        this.callback.onNegativeButtonClicked();
                    }
                });
        if (title > 0) {
            builder.setTitle(title);
        }
        return builder.create();
    }

    public interface EditTextDialogFragmentCallback {
        void onPositiveButtonClicked(String text);

        void onNegativeButtonClicked();
    }
}
