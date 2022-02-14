package awais.instagrabber.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import awais.instagrabber.R;
import awais.instagrabber.adapters.GifItemsAdapter;
import awais.instagrabber.customviews.helpers.TextWatcherAdapter;
import awais.instagrabber.databinding.LayoutGifPickerBinding;
import awais.instagrabber.repositories.responses.giphy.GiphyGif;
import awais.instagrabber.utils.Debouncer;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.viewmodels.GifPickerViewModel;

public class GifPickerBottomDialogFragment extends BottomSheetDialogFragment {
    private static final String TAG = GifPickerBottomDialogFragment.class.getSimpleName();
    private static final int INPUT_DEBOUNCE_INTERVAL = 500;
    private static final String INPUT_KEY = "gif_search_input";

    private LayoutGifPickerBinding binding;
    private GifPickerViewModel viewModel;
    private GifItemsAdapter gifItemsAdapter;
    private OnSelectListener onSelectListener;
    private Debouncer<String> inputDebouncer;

    public static GifPickerBottomDialogFragment newInstance() {
        Bundle args = new Bundle();
        GifPickerBottomDialogFragment fragment = new GifPickerBottomDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setStyle(DialogFragment.STYLE_NORMAL, R.style.ThemeOverlay_Rounded_BottomSheetDialog);
        Debouncer.Callback<String> callback = new Debouncer.Callback<String>() {
            @Override
            public void call(String key) {
                Editable text = GifPickerBottomDialogFragment.this.binding.input.getText();
                if (TextUtils.isEmpty(text)) {
                    GifPickerBottomDialogFragment.this.viewModel.search(null);
                    return;
                }
                GifPickerBottomDialogFragment.this.viewModel.search(text.toString().trim());
            }

            @Override
            public void onError(Throwable t) {
                Log.e(GifPickerBottomDialogFragment.TAG, "onError: ", t);
            }
        };
        this.inputDebouncer = new Debouncer<>(callback, GifPickerBottomDialogFragment.INPUT_DEBOUNCE_INTERVAL);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.binding = LayoutGifPickerBinding.inflate(inflater, container, false);
        this.viewModel = new ViewModelProvider(this).get(GifPickerViewModel.class);
        return this.binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        this.init();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = this.getDialog();
        if (dialog == null) return;
        BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialog;
        View bottomSheetInternal = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheetInternal == null) return;
        bottomSheetInternal.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
        bottomSheetInternal.requestLayout();
    }

    private void init() {
        this.setupList();
        this.setupInput();
        this.setupObservers();
    }

    private void setupList() {
        Context context = this.getContext();
        if (context == null) return;
        this.binding.gifList.setLayoutManager(new GridLayoutManager(context, 3));
        this.binding.gifList.setHasFixedSize(true);
        this.gifItemsAdapter = new GifItemsAdapter(entry -> {
            if (this.onSelectListener == null) return;
            this.onSelectListener.onSelect(entry);
        });
        this.binding.gifList.setAdapter(this.gifItemsAdapter);
    }

    private void setupInput() {
        this.binding.input.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                GifPickerBottomDialogFragment.this.inputDebouncer.call(GifPickerBottomDialogFragment.INPUT_KEY);
            }
        });
    }

    private void setupObservers() {
        this.viewModel.getImages().observe(this.getViewLifecycleOwner(), imagesResource -> {
            if (imagesResource == null) return;
            switch (imagesResource.status) {
                case SUCCESS:
                    this.gifItemsAdapter.submitList(imagesResource.data);
                    break;
                case ERROR:
                    Context context = this.getContext();
                    if (context != null && imagesResource.message != null) {
                        Snackbar.make(context, this.binding.getRoot(), imagesResource.message, BaseTransientBottomBar.LENGTH_LONG).show();
                    }
                    if (context != null && imagesResource.resId != 0) {
                        Snackbar.make(context, this.binding.getRoot(), this.getString(imagesResource.resId), BaseTransientBottomBar.LENGTH_LONG).show();
                    }
                    break;
                case LOADING:
                    break;
            }
        });
    }

    public void setOnSelectListener(OnSelectListener onSelectListener) {
        this.onSelectListener = onSelectListener;
    }

    public interface OnSelectListener {
        void onSelect(GiphyGif giphyGif);
    }
}
