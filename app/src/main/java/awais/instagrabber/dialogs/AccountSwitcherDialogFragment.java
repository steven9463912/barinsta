package awais.instagrabber.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.adapters.AccountSwitcherAdapter;
import awais.instagrabber.databinding.DialogAccountSwitcherBinding;
import awais.instagrabber.db.entities.Account;
import awais.instagrabber.db.repositories.AccountRepository;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.utils.ProcessPhoenix;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import kotlinx.coroutines.Dispatchers;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class AccountSwitcherDialogFragment extends DialogFragment {
    private static final String TAG = AccountSwitcherDialogFragment.class.getSimpleName();

    private AccountRepository accountRepository;

    private OnAddAccountClickListener onAddAccountClickListener;
    private DialogAccountSwitcherBinding binding;

    public AccountSwitcherDialogFragment() {}

    public AccountSwitcherDialogFragment(OnAddAccountClickListener onAddAccountClickListener) {
        this.onAddAccountClickListener = onAddAccountClickListener;
    }

    private final AccountSwitcherAdapter.OnAccountClickListener accountClickListener = (model, isCurrent) -> {
        if (isCurrent) {
            this.dismiss();
            return;
        }
        CookieUtils.setupCookies(model.getCookie());
        settingsHelper.putString(Constants.COOKIE, model.getCookie());
        // final FragmentActivity activity = getActivity();
        // if (activity != null) activity.recreate();
        // dismiss();
        AppExecutors.INSTANCE.getMainThread().execute(() -> {
            Context context = this.getContext();
            if (context == null) return;
            ProcessPhoenix.triggerRebirth(context);
        }, 200);
    };

    private final AccountSwitcherAdapter.OnAccountLongClickListener accountLongClickListener = (model, isCurrent) -> {
        Context context = this.getContext();
        if (context == null) return false;
        if (isCurrent) {
            new AlertDialog.Builder(context)
                    .setMessage(R.string.quick_access_cannot_delete_curr)
                    .setPositiveButton(R.string.ok, null)
                    .show();
            return true;
        }
        new AlertDialog.Builder(context)
                .setMessage(this.getString(R.string.quick_access_confirm_delete, model.getUsername()))
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    if (this.accountRepository == null) return;
                    this.accountRepository.deleteAccount(
                            model,
                            CoroutineUtilsKt.getContinuation((unit, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                                this.dismiss();
                                if (throwable != null) {
                                    Log.e(AccountSwitcherDialogFragment.TAG, "deleteAccount: ", throwable);
                                }
                            }), Dispatchers.getIO())
                    );
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
        this.dismiss();
        return true;
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        this.binding = DialogAccountSwitcherBinding.inflate(inflater, container, false);
        this.binding.accounts.setLayoutManager(new LinearLayoutManager(this.getContext()));
        return this.binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.init();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.accountRepository = AccountRepository.Companion.getInstance(context);
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

    private void init() {
        AccountSwitcherAdapter adapter = new AccountSwitcherAdapter(this.accountClickListener, this.accountLongClickListener);
        this.binding.accounts.setAdapter(adapter);
        if (this.accountRepository == null) return;
        this.accountRepository.getAllAccounts(
                CoroutineUtilsKt.getContinuation((accounts, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                    if (throwable != null) {
                        Log.e(AccountSwitcherDialogFragment.TAG, "init: ", throwable);
                        return;
                    }
                    if (accounts == null) return;
                    String cookie = settingsHelper.getString(Constants.COOKIE);
                    List<Account> copy = new ArrayList<>(accounts);
                    this.sortUserList(cookie, copy);
                    adapter.submitList(copy);
                }), Dispatchers.getIO())
        );
        this.binding.addAccountBtn.setOnClickListener(v -> {
            if (this.onAddAccountClickListener == null) return;
            this.onAddAccountClickListener.onAddAccountClick(this);
        });
    }

    /**
     * Sort the user list by following logic:
     * <ol>
     * <li>Keep currently active account at top.
     * <li>Check if any user does not have a full name.
     * <li>If all have full names, sort by full names.
     * <li>Otherwise, sort by the usernames
     * </ol>
     *
     * @param cookie   active cookie
     * @param allUsers list of users
     */
    private void sortUserList(String cookie, List<Account> allUsers) {
        boolean sortByName = true;
        for (Account user : allUsers) {
            if (TextUtils.isEmpty(user.getFullName())) {
                sortByName = false;
                break;
            }
        }
        boolean finalSortByName = sortByName;
        Collections.sort(allUsers, (o1, o2) -> {
            // keep current account at top
            if (o1.getCookie().equals(cookie)) return -1;
            if (finalSortByName) {
                // sort by full name
                return o1.getFullName().compareTo(o2.getFullName());
            }
            // otherwise sort by username
            return o1.getUsername().compareTo(o2.getUsername());
        });
    }

    public interface OnAddAccountClickListener {
        void onAddAccountClick(AccountSwitcherDialogFragment dialogFragment);
    }
}
