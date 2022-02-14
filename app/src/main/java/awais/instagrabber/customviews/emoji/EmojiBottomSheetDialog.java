package awais.instagrabber.customviews.emoji;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import awais.instagrabber.R;
import awais.instagrabber.customviews.helpers.GridSpacingItemDecoration;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.utils.emoji.EmojiParser;

public class EmojiBottomSheetDialog extends BottomSheetDialogFragment {
    public static final String TAG = EmojiBottomSheetDialog.class.getSimpleName();

    private RecyclerView grid;
    private EmojiPicker.OnEmojiClickListener callback;

    @NonNull
    public static EmojiBottomSheetDialog newInstance() {
        // Bundle args = new Bundle();
        // fragment.setArguments(args);
        return new EmojiBottomSheetDialog();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setStyle(DialogFragment.STYLE_NORMAL, R.style.ThemeOverlay_Rounded_BottomSheetDialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context context = this.getContext();
        if (context == null) return null;
        this.grid = new RecyclerView(context);
        return this.grid;
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

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Fragment parentFragment = this.getParentFragment();
        if (parentFragment instanceof EmojiPicker.OnEmojiClickListener) {
            this.callback = (EmojiPicker.OnEmojiClickListener) parentFragment;
        }
    }

    @Override
    public void onDestroyView() {
        this.grid = null;
        super.onDestroyView();
    }

    private void init() {
        Context context = this.getContext();
        if (context == null) return;
        GridLayoutManager gridLayoutManager = new GridLayoutManager(context, 9);
        this.grid.setLayoutManager(gridLayoutManager);
        this.grid.setHasFixedSize(true);
        this.grid.setClipToPadding(false);
        this.grid.addItemDecoration(new GridSpacingItemDecoration(Utils.convertDpToPx(8)));
        EmojiParser emojiParser = EmojiParser.Companion.getInstance(context);
        EmojiGridAdapter adapter = new EmojiGridAdapter(emojiParser, null, (view, emoji) -> {
            if (this.callback != null) {
                this.callback.onClick(view, emoji);
            }
            this.dismiss();
        }, null);
        this.grid.setAdapter(adapter);
    }
}
