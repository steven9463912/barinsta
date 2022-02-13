package awais.instagrabber.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.primitives.Booleans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MultiOptionDialogFragment<T extends Serializable> extends DialogFragment {
    private static final String TAG = MultiOptionDialogFragment.class.getSimpleName();

    public enum Type {
        MULTIPLE,
        SINGLE_CHECKED,
        SINGLE
    }

    private Context context;
    private Type type;
    private MultiOptionDialogCallback<T> callback;
    private MultiOptionDialogSingleCallback<T> singleCallback;
    private List<Option<?>> options;

    @NonNull
    public static <E extends Serializable> MultiOptionDialogFragment<E> newInstance(int requestCode,
                                                                                    @StringRes int title,
                                                                                    @NonNull ArrayList<Option<E>> options) {
        return MultiOptionDialogFragment.newInstance(requestCode, title, 0, 0, options, Type.SINGLE);
    }

    @NonNull
    public static <E extends Serializable> MultiOptionDialogFragment<E> newInstance(int requestCode,
                                                                                    @StringRes int title,
                                                                                    @StringRes int positiveButtonText,
                                                                                    @StringRes int negativeButtonText,
                                                                                    @NonNull ArrayList<Option<E>> options,
                                                                                    @NonNull Type type) {
        final Bundle args = new Bundle();
        args.putInt("requestCode", requestCode);
        args.putInt("title", title);
        args.putInt("positiveButtonText", positiveButtonText);
        args.putInt("negativeButtonText", negativeButtonText);
        args.putSerializable("options", options);
        args.putSerializable("type", type);
        final MultiOptionDialogFragment<E> fragment = new MultiOptionDialogFragment<>();
        fragment.setArguments(args);
        return fragment;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        Fragment parentFragment = this.getParentFragment();
        if (parentFragment != null) {
            if (parentFragment instanceof MultiOptionDialogCallback) {
                this.callback = (MultiOptionDialogCallback) parentFragment;
            }
            if (parentFragment instanceof MultiOptionDialogSingleCallback) {
                this.singleCallback = (MultiOptionDialogSingleCallback) parentFragment;
            }
            return;
        }
        FragmentActivity fragmentActivity = this.getActivity();
        if (fragmentActivity instanceof MultiOptionDialogCallback) {
            this.callback = (MultiOptionDialogCallback) fragmentActivity;
        }
        if (fragmentActivity instanceof MultiOptionDialogSingleCallback) {
            this.singleCallback = (MultiOptionDialogSingleCallback) fragmentActivity;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        Bundle arguments = this.getArguments();
        int title = 0;
        int rc = 0;
        if (arguments != null) {
            rc = arguments.getInt("requestCode");
            title = arguments.getInt("title");
            this.type = (Type) arguments.getSerializable("type");
        }
        int requestCode = rc;
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this.context);
        if (title != 0) {
            builder.setTitle(title);
        }
        try {
            //noinspection unchecked
            this.options = arguments != null ? (List<Option<?>>) arguments.getSerializable("options")
                                        : Collections.emptyList();
        } catch (final Exception e) {
            Log.e(MultiOptionDialogFragment.TAG, "onCreateDialog: ", e);
            this.options = Collections.emptyList();
        }
        int negativeButtonText = arguments != null ? arguments.getInt("negativeButtonText", -1) : -1;
        if (negativeButtonText > 0) {
            builder.setNegativeButton(negativeButtonText, (dialog, which) -> {
                if (this.callback != null) {
                    this.callback.onCancel(requestCode);
                    return;
                }
                if (this.singleCallback != null) {
                    this.singleCallback.onCancel(requestCode);
                }
            });
        }
        if (this.type == Type.MULTIPLE || this.type == Type.SINGLE_CHECKED) {
            int positiveButtonText = arguments != null ? arguments.getInt("positiveButtonText", -1) : -1;
            if (positiveButtonText > 0) {
                builder.setPositiveButton(positiveButtonText, (dialog, which) -> {
                    if (this.callback == null || this.options == null || this.options.isEmpty()) return;
                    try {
                        List<T> selected = new ArrayList<>();
                        SparseBooleanArray checkedItemPositions = ((AlertDialog) dialog).getListView().getCheckedItemPositions();
                        for (int i = 0; i < checkedItemPositions.size(); i++) {
                            int position = checkedItemPositions.keyAt(i);
                            boolean checked = checkedItemPositions.get(position);
                            if (!checked) continue;
                            //noinspection unchecked
                            Option<T> option = (Option<T>) this.options.get(position);
                            selected.add(option.value);
                        }
                        this.callback.onMultipleSelect(requestCode, selected);
                    } catch (final Exception e) {
                        Log.e(MultiOptionDialogFragment.TAG, "onCreateDialog: ", e);
                    }
                });
            }
        }
        if (this.type == Type.MULTIPLE) {
            if (this.options != null && !this.options.isEmpty()) {
                String[] items = this.options.stream()
                                              .map(option -> option.label)
                                              .toArray(String[]::new);
                boolean[] checkedItems = Booleans.toArray(this.options.stream()
                                                                       .map(option -> option.checked)
                                                                       .collect(Collectors.toList()));
                builder.setMultiChoiceItems(items, checkedItems, (dialog, which, isChecked) -> {
                    if (this.callback == null) return;
                    try {
                        Option<?> option = this.options.get(which);
                        //noinspection unchecked
                        this.callback.onCheckChange(requestCode, (T) option.value, isChecked);
                    } catch (final Exception e) {
                        Log.e(MultiOptionDialogFragment.TAG, "onCreateDialog: ", e);
                    }
                });
            }
        } else {
            if (this.options != null && !this.options.isEmpty()) {
                String[] items = this.options.stream()
                                              .map(option -> option.label)
                                              .toArray(String[]::new);
                if (this.type == Type.SINGLE_CHECKED) {
                    int index = -1;
                    for (int i = 0; i < this.options.size(); i++) {
                        if (this.options.get(i).checked) {
                            index = i;
                            break;
                        }
                    }
                    builder.setSingleChoiceItems(items, index, (dialog, which) -> {
                        if (this.callback == null) return;
                        try {
                            Option<?> option = this.options.get(which);
                            //noinspection unchecked
                            this.callback.onCheckChange(requestCode, (T) option.value, true);
                        } catch (final Exception e) {
                            Log.e(MultiOptionDialogFragment.TAG, "onCreateDialog: ", e);
                        }
                    });
                } else if (this.type == Type.SINGLE) {
                    builder.setItems(items, (dialog, which) -> {
                        if (this.singleCallback == null) return;
                        try {
                            Option<?> option = this.options.get(which);
                            //noinspection unchecked
                            this.singleCallback.onSelect(requestCode, (T) option.value);
                        } catch (final Exception e) {
                            Log.e(MultiOptionDialogFragment.TAG, "onCreateDialog: ", e);
                        }
                    });
                }
            }
        }
        return builder.create();
    }

    public void setCallback(MultiOptionDialogCallback<T> callback) {
        if (callback == null) return;
        this.callback = callback;
    }

    public void setSingleCallback(MultiOptionDialogSingleCallback<T> callback) {
        if (callback == null) return;
        singleCallback = callback;
    }

    public interface MultiOptionDialogCallback<T> {
        void onSelect(int requestCode, T result);

        void onMultipleSelect(int requestCode, List<T> result);

        void onCheckChange(int requestCode, T item, boolean isChecked);

        void onCancel(int requestCode);
    }

    public interface MultiOptionDialogSingleCallback<T> {
        void onSelect(int requestCode, T result);

        void onCancel(int requestCode);
    }

    public static class Option<T extends Serializable> implements Serializable {
        private final String label;
        private final T value;
        private final boolean checked;

        public Option(String label, T value) {
            this.label = label;
            this.value = value;
            checked = false;
        }

        public Option(String label, T value, boolean checked) {
            this.label = label;
            this.value = value;
            this.checked = checked;
        }

        public String getLabel() {
            return this.label;
        }

        public T getValue() {
            return this.value;
        }

        public boolean isChecked() {
            return this.checked;
        }
    }
}
