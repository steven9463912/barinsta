package awais.instagrabber.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.Locale;

import awais.instagrabber.databinding.DialogCreateBackupBinding;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.ExportImportUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

import static android.app.Activity.RESULT_OK;

public class CreateBackupDialogFragment extends DialogFragment {
    private static final String TAG = CreateBackupDialogFragment.class.getSimpleName();
    private static final int STORAGE_PERM_REQUEST_CODE = 8020;
    private static final DateTimeFormatter BACKUP_FILE_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.US);
    private static final int CREATE_FILE_REQUEST_CODE = 1;


    private final OnResultListener onResultListener;
    private DialogCreateBackupBinding binding;

    public CreateBackupDialogFragment(OnResultListener onResultListener) {
        this.onResultListener = onResultListener;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        this.binding = DialogCreateBackupBinding.inflate(inflater, container, false);
        return this.binding.getRoot();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = this.getDialog();
        if (dialog == null) return;
        Window window = dialog.getWindow();
        if (window == null) return;
        final int height = ViewGroup.LayoutParams.WRAP_CONTENT;
        int width = (int) (Utils.displayMetrics.widthPixels * 0.8);
        window.setLayout(width, height);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.init();
    }

    private void init() {
        this.binding.etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                CreateBackupDialogFragment.this.binding.btnSaveTo.setEnabled(!TextUtils.isEmpty(s));
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        Context context = this.getContext();
        if (context == null) {
            return;
        }
        this.binding.cbPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (TextUtils.isEmpty(this.binding.etPassword.getText())) {
                    this.binding.btnSaveTo.setEnabled(false);
                }
                this.binding.passwordField.setVisibility(View.VISIBLE);
                this.binding.etPassword.requestFocus();
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm == null) return;
                imm.showSoftInput(this.binding.etPassword, InputMethodManager.SHOW_IMPLICIT);
                return;
            }
            this.binding.btnSaveTo.setEnabled(true);
            this.binding.passwordField.setVisibility(View.GONE);
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm == null) return;
            imm.hideSoftInputFromWindow(this.binding.etPassword.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
        });
        this.binding.btnSaveTo.setOnClickListener(v -> this.createFile());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (data == null || data.getData() == null) return;
        if (resultCode != RESULT_OK || requestCode != CreateBackupDialogFragment.CREATE_FILE_REQUEST_CODE) return;
        Context context = this.getContext();
        if (context == null) return;
        Editable passwordText = this.binding.etPassword.getText();
        String password = this.binding.cbPassword.isChecked()
                                        && passwordText != null
                                        && !TextUtils.isEmpty(passwordText.toString())
                                ? passwordText.toString().trim()
                                : null;
        int flags = 0;
        if (this.binding.cbExportFavorites.isChecked()) {
            flags |= ExportImportUtils.FLAG_FAVORITES;
        }
        if (this.binding.cbExportSettings.isChecked()) {
            flags |= ExportImportUtils.FLAG_SETTINGS;
        }
        if (this.binding.cbExportLogins.isChecked()) {
            flags |= ExportImportUtils.FLAG_COOKIES;
        }
        ExportImportUtils.exportData(context, flags, data.getData(), password, result -> {
            if (this.onResultListener != null) {
                this.onResultListener.onResult(result);
            }
            this.dismiss();
        });
    }

    private void createFile() {
        final Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        String fileName = String.format("barinsta_%s.backup", LocalDateTime.now().format(CreateBackupDialogFragment.BACKUP_FILE_DATE_TIME_FORMAT));
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        // Optionally, specify a URI for the directory that should be opened in
        // the system file picker when your app creates the document.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, DownloadUtils.getBackupsDir().getUri());
        }

        this.startActivityForResult(intent, CreateBackupDialogFragment.CREATE_FILE_REQUEST_CODE);
    }


    public interface OnResultListener {
        void onResult(boolean result);
    }
}
