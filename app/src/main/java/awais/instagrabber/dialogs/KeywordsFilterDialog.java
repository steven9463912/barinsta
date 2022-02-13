package awais.instagrabber.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;

import awais.instagrabber.R;
import awais.instagrabber.adapters.KeywordsFilterAdapter;
import awais.instagrabber.databinding.DialogKeywordsFilterBinding;
import awais.instagrabber.fragments.settings.PreferenceKeys;
import awais.instagrabber.utils.SettingsHelper;
import awais.instagrabber.utils.Utils;

public final class KeywordsFilterDialog extends DialogFragment {

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
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        DialogKeywordsFilterBinding dialogKeywordsFilterBinding = DialogKeywordsFilterBinding.inflate(inflater, container, false);
        this.init(dialogKeywordsFilterBinding, this.getContext());
        dialogKeywordsFilterBinding.btnOK.setOnClickListener(view -> dismiss());
        return dialogKeywordsFilterBinding.getRoot();
    }

    private void init(final DialogKeywordsFilterBinding dialogKeywordsFilterBinding, final Context context){
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        RecyclerView recyclerView = dialogKeywordsFilterBinding.recyclerKeyword;
        recyclerView.setLayoutManager(linearLayoutManager);

        SettingsHelper settingsHelper = new SettingsHelper(context);
        ArrayList<String> items = new ArrayList<>(settingsHelper.getStringSet(PreferenceKeys.KEYWORD_FILTERS));
        KeywordsFilterAdapter adapter = new KeywordsFilterAdapter(context, items);
        recyclerView.setAdapter(adapter);

        EditText editText = dialogKeywordsFilterBinding.editText;

        dialogKeywordsFilterBinding.btnAdd.setOnClickListener(view ->{
            String s = editText.getText().toString();
            if(s.isEmpty()) return;
            if(items.contains(s)) {
                editText.setText("");
                return;
            }
            items.add(s.toLowerCase());
            settingsHelper.putStringSet(PreferenceKeys.KEYWORD_FILTERS, new HashSet<>(items));
            adapter.notifyItemInserted(items.size());
            String message = context.getString(R.string.added_keywords, s);
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            editText.setText("");
        });
    }
}
