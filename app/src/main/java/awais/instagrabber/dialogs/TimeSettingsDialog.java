package awais.instagrabber.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import awais.instagrabber.R;
import awais.instagrabber.databinding.DialogTimeSettingsBinding;
import awais.instagrabber.utils.DateUtils;
import awais.instagrabber.utils.LocaleUtils;
import awais.instagrabber.utils.TextUtils;

public final class TimeSettingsDialog extends DialogFragment implements AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener,
        View.OnClickListener, TextWatcher {
    private DialogTimeSettingsBinding binding;
    private final LocalDateTime magicDate;
    private DateTimeFormatter currentFormat;
    private String selectedFormat;
    private final boolean customDateTimeFormatEnabled;
    private final String customDateTimeFormat;
    private final String dateTimeSelection;
    private final boolean swapDateTimeEnabled;
    private final OnConfirmListener onConfirmListener;

    public TimeSettingsDialog(boolean customDateTimeFormatEnabled,
                              String customDateTimeFormat,
                              String dateTimeSelection,
                              boolean swapDateTimeEnabled,
                              OnConfirmListener onConfirmListener) {
        this.customDateTimeFormatEnabled = customDateTimeFormatEnabled;
        this.customDateTimeFormat = customDateTimeFormat;
        this.dateTimeSelection = dateTimeSelection;
        this.swapDateTimeEnabled = swapDateTimeEnabled;
        this.onConfirmListener = onConfirmListener;
        this.magicDate = LocalDateTime.ofInstant(
                Instant.now(),
                ZoneId.systemDefault()
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.binding = DialogTimeSettingsBinding.inflate(inflater, container, false);

        this.binding.cbCustomFormat.setOnCheckedChangeListener(this);
        this.binding.cbCustomFormat.setChecked(this.customDateTimeFormatEnabled);
        this.binding.cbSwapTimeDate.setChecked(this.swapDateTimeEnabled);
        this.binding.customFormatEditText.setText(this.customDateTimeFormat);

        String[] dateTimeFormat = this.dateTimeSelection.split(";"); // output = time;separator;date
        this.binding.spTimeFormat.setSelection(Integer.parseInt(dateTimeFormat[0]));
        this.binding.spSeparator.setSelection(Integer.parseInt(dateTimeFormat[1]));
        this.binding.spDateFormat.setSelection(Integer.parseInt(dateTimeFormat[2]));

        this.binding.cbSwapTimeDate.setOnCheckedChangeListener(this);

        this.refreshTimeFormat();

        this.binding.spTimeFormat.setOnItemSelectedListener(this);
        this.binding.spDateFormat.setOnItemSelectedListener(this);
        this.binding.spSeparator.setOnItemSelectedListener(this);

        this.binding.customFormatEditText.addTextChangedListener(this);
        this.binding.btnConfirm.setOnClickListener(this);
        this.binding.customFormatField.setEndIconOnClickListener(this);

        return this.binding.getRoot();
    }

    private void refreshTimeFormat() {
        boolean isCustom = this.binding.cbCustomFormat.isChecked();
        if (isCustom) {
            Editable text = this.binding.customFormatEditText.getText();
            if (text != null) {
                this.selectedFormat = text.toString();
            }
        } else {
            String sepStr = String.valueOf(this.binding.spSeparator.getSelectedItem());
            String timeStr = String.valueOf(this.binding.spTimeFormat.getSelectedItem());
            String dateStr = String.valueOf(this.binding.spDateFormat.getSelectedItem());

            boolean isSwapTime = this.binding.cbSwapTimeDate.isChecked();
            boolean isBlankSeparator = this.binding.spSeparator.getSelectedItemPosition() <= 0;

            this.selectedFormat = (isSwapTime ? dateStr : timeStr)
                    + (isBlankSeparator ? " " : " '" + sepStr + "' ")
                    + (isSwapTime ? timeStr : dateStr);
        }

        this.binding.btnConfirm.setEnabled(true);
        try {
            this.currentFormat = DateTimeFormatter.ofPattern(this.selectedFormat, LocaleUtils.getCurrentLocale());
            if (isCustom) {
                boolean valid = !TextUtils.isEmpty(this.selectedFormat) && DateUtils.checkFormatterValid(this.currentFormat);
                this.binding.customFormatField.setError(valid ? null : this.getString(R.string.invalid_format));
                if (!valid) {
                    this.binding.btnConfirm.setEnabled(false);
                }
            }
            this.binding.timePreview.setText(this.magicDate.format(this.currentFormat));
        } catch (final Exception e) {
            this.binding.btnConfirm.setEnabled(false);
            this.binding.timePreview.setText(null);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
        this.refreshTimeFormat();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == this.binding.cbCustomFormat) {
            this.binding.customFormatField.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            this.binding.customFormatField.setEnabled(isChecked);

            this.binding.spTimeFormat.setEnabled(!isChecked);
            this.binding.spDateFormat.setEnabled(!isChecked);
            this.binding.spSeparator.setEnabled(!isChecked);
            this.binding.cbSwapTimeDate.setEnabled(!isChecked);
        }
        this.refreshTimeFormat();
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        this.refreshTimeFormat();
    }

    @Override
    public void onClick(View v) {
        if (v == this.binding.btnConfirm) {
            if (this.onConfirmListener != null) {
                this.onConfirmListener.onConfirm(
                        this.binding.cbCustomFormat.isChecked(),
                        this.binding.spTimeFormat.getSelectedItemPosition(),
                        this.binding.spSeparator.getSelectedItemPosition(),
                        this.binding.spDateFormat.getSelectedItemPosition(),
                        this.selectedFormat,
                        this.binding.cbSwapTimeDate.isChecked());
            }
            this.dismiss();
        } else if (v == this.binding.customFormatField.findViewById(R.id.text_input_end_icon)) {
            this.binding.customPanel.setVisibility(
                    this.binding.customPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE
            );

        }
    }

    public interface OnConfirmListener {
        void onConfirm(boolean isCustomFormat,
                       int spTimeFormatSelectedItemPosition,
                       int spSeparatorSelectedItemPosition,
                       int spDateFormatSelectedItemPosition,
                       String selectedFormat,
                       boolean swapDateTime);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) { }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void afterTextChanged(Editable s) { }

    @Override
    public void onResume() {
        super.onResume();
        Dialog dialog = this.getDialog();
        if (dialog == null) return;
        Window window = dialog.getWindow();
        if (window == null) return;
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        window.setAttributes(params);
    }
}