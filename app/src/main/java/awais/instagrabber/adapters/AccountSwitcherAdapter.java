package awais.instagrabber.adapters;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.R;
import awais.instagrabber.databinding.PrefAccountSwitcherBinding;
import awais.instagrabber.db.entities.Account;
import awais.instagrabber.utils.Constants;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class AccountSwitcherAdapter extends ListAdapter<Account, AccountSwitcherAdapter.ViewHolder> {
    private static final String TAG = "AccountSwitcherAdapter";
    private static final DiffUtil.ItemCallback<Account> DIFF_CALLBACK = new DiffUtil.ItemCallback<Account>() {
        @Override
        public boolean areItemsTheSame(@NonNull Account oldItem, @NonNull Account newItem) {
            return oldItem.getUid().equals(newItem.getUid());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Account oldItem, @NonNull Account newItem) {
            return oldItem.getUid().equals(newItem.getUid());
        }
    };

    private final OnAccountClickListener clickListener;
    private final OnAccountLongClickListener longClickListener;

    public AccountSwitcherAdapter(OnAccountClickListener clickListener,
                                  OnAccountLongClickListener longClickListener) {
        super(AccountSwitcherAdapter.DIFF_CALLBACK);
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        PrefAccountSwitcherBinding binding = PrefAccountSwitcherBinding.inflate(layoutInflater, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Account model = this.getItem(position);
        if (model == null) return;
        String cookie = settingsHelper.getString(Constants.COOKIE);
        boolean isCurrent = model.getCookie().equals(cookie);
        holder.bind(model, isCurrent, this.clickListener, this.longClickListener);
    }

    public interface OnAccountClickListener {
        void onAccountClick(Account model, boolean isCurrent);
    }

    public interface OnAccountLongClickListener {
        boolean onAccountLongClick(Account model, boolean isCurrent);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final PrefAccountSwitcherBinding binding;

        public ViewHolder(PrefAccountSwitcherBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.arrowDown.setImageResource(R.drawable.ic_check_24);
        }

        @SuppressLint("SetTextI18n")
        public void bind(Account model,
                         boolean isCurrent,
                         OnAccountClickListener clickListener,
                         OnAccountLongClickListener longClickListener) {
            // Log.d(TAG, model.getFullName());
            this.itemView.setOnClickListener(v -> {
                if (clickListener == null) return;
                clickListener.onAccountClick(model, isCurrent);
            });
            this.itemView.setOnLongClickListener(v -> {
                if (longClickListener == null) return false;
                return longClickListener.onAccountLongClick(model, isCurrent);
            });
            this.binding.profilePic.setImageURI(model.getProfilePic());
            this.binding.username.setText("@" + model.getUsername());
            this.binding.fullName.setTypeface(null);
            String fullName = model.getFullName();
            if (TextUtils.isEmpty(fullName)) {
                this.binding.fullName.setVisibility(View.GONE);
            } else {
                this.binding.fullName.setVisibility(View.VISIBLE);
                this.binding.fullName.setText(fullName);
            }
            if (!isCurrent) {
                this.binding.arrowDown.setVisibility(View.GONE);
                return;
            }
            this.binding.fullName.setTypeface(this.binding.fullName.getTypeface(), Typeface.BOLD);
            this.binding.arrowDown.setVisibility(View.VISIBLE);
        }
    }
}
