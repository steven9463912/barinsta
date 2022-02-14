package awais.instagrabber.dialogs;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import awais.instagrabber.databinding.DialogRestoreBackupBinding;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.ExportImportUtils;
import awais.instagrabber.utils.PasswordUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;

import static android.app.Activity.RESULT_OK;

public class RestoreBackupDialogFragment extends DialogFragment {
    private static final String TAG = RestoreBackupDialogFragment.class.getSimpleName();
    private static final int STORAGE_PERM_REQUEST_CODE = 8020;
    private static final int OPEN_FILE_REQUEST_CODE = 1;

    private OnResultListener onResultListener;

    private DialogRestoreBackupBinding binding;
    private boolean isEncrypted;
    private Uri uri;

    public RestoreBackupDialogFragment() {}

    public RestoreBackupDialogFragment(OnResultListener onResultListener) {
        this.onResultListener = onResultListener;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        this.binding = DialogRestoreBackupBinding.inflate(inflater, container, false);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (data == null || data.getData() == null) return;
        if (resultCode != RESULT_OK || requestCode != RestoreBackupDialogFragment.OPEN_FILE_REQUEST_CODE) return;
        Context context = this.getContext();
        if (context == null) return;
        this.isEncrypted = ExportImportUtils.isEncrypted(context, data.getData());
        if (this.isEncrypted) {
            this.binding.passwordGroup.setVisibility(View.VISIBLE);
            this.binding.passwordGroup.post(() -> {
                this.binding.etPassword.requestFocus();
                this.binding.etPassword.post(() -> {
                    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm == null) return;
                    imm.showSoftInput(this.binding.etPassword, InputMethodManager.SHOW_IMPLICIT);
                });
                this.binding.btnRestore.setEnabled(!TextUtils.isEmpty(this.binding.etPassword.getText()));
            });
        } else {
            this.binding.passwordGroup.setVisibility(View.GONE);
            this.binding.btnRestore.setEnabled(true);
        }
        this.uri = data.getData();
        AppExecutors.INSTANCE.getMainThread().execute(() -> {
            Cursor c = null;
            try {
                final String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
                ContentResolver contentResolver = context.getContentResolver();
                c = contentResolver.query(this.uri, projection, null, null, null);
                if (c != null) {
                    while (c.moveToNext()) {
                        String displayName = c.getString(0);
                        this.binding.filePath.setText(displayName);
                    }
                }
            } catch (final Exception e) {
                Log.e(RestoreBackupDialogFragment.TAG, "onActivityResult: ", e);
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        });
    }

    private void init() {
        Context context = this.getContext();
        if (context == null) return;
        this.binding.btnRestore.setEnabled(false);
        this.binding.btnRestore.setOnClickListener(v -> new Handler(Looper.getMainLooper()).post(() -> {
            if (this.uri == null) return;
            int flags = 0;
            if (this.binding.cbFavorites.isChecked()) {
                flags |= ExportImportUtils.FLAG_FAVORITES;
            }
            if (this.binding.cbSettings.isChecked()) {
                flags |= ExportImportUtils.FLAG_SETTINGS;
            }
            if (this.binding.cbAccounts.isChecked()) {
                flags |= ExportImportUtils.FLAG_COOKIES;
            }
            Editable text = this.binding.etPassword.getText();
            if (this.isEncrypted && text == null) return;
            try {
                ExportImportUtils.importData(
                        context,
                        flags,
                        this.uri,
                        !this.isEncrypted ? null : text.toString(),
                        result -> {
                            if (this.onResultListener != null) {
                                this.onResultListener.onResult(result);
                            }
                            this.dismiss();
                        }
                );
            } catch (final PasswordUtils.IncorrectPasswordException e) {
                this.binding.passwordField.setError("Incorrect password");
            }
        }));
        this.binding.etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                RestoreBackupDialogFragment.this.binding.btnRestore.setEnabled(!TextUtils.isEmpty(s));
                RestoreBackupDialogFragment.this.binding.passwordField.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        this.startActivityForResult(intent, RestoreBackupDialogFragment.OPEN_FILE_REQUEST_CODE);

    }

    public interface OnResultListener {
        void onResult(boolean result);
    }
}
