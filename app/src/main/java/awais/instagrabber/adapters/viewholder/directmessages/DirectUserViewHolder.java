package awais.instagrabber.adapters.viewholder.directmessages;

import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.R;
import awais.instagrabber.adapters.DirectUsersAdapter;
import awais.instagrabber.customviews.VerticalImageSpan;
import awais.instagrabber.databinding.LayoutDmUserItemBinding;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.utils.Utils;

public class DirectUserViewHolder extends RecyclerView.ViewHolder {
    private static final String TAG = DirectUserViewHolder.class.getSimpleName();

    private final LayoutDmUserItemBinding binding;
    private final DirectUsersAdapter.OnDirectUserClickListener onClickListener;
    private final DirectUsersAdapter.OnDirectUserLongClickListener onLongClickListener;
    private final int drawableSize;

    private VerticalImageSpan verifiedSpan;

    public DirectUserViewHolder(@NonNull final LayoutDmUserItemBinding binding,
                                final DirectUsersAdapter.OnDirectUserClickListener onClickListener,
                                final DirectUsersAdapter.OnDirectUserLongClickListener onLongClickListener) {
        super(binding.getRoot());
        this.binding = binding;
        this.onClickListener = onClickListener;
        this.onLongClickListener = onLongClickListener;
        this.drawableSize = Utils.convertDpToPx(24);
    }

    public void bind(int position,
                     User user,
                     boolean isAdmin,
                     boolean isInviter,
                     boolean showSelection,
                     boolean isSelected) {
        if (user == null) return;
        this.binding.getRoot().setOnClickListener(v -> {
            if (this.onClickListener == null) return;
            this.onClickListener.onClick(position, user, isSelected);
        });
        this.binding.getRoot().setOnLongClickListener(v -> {
            if (this.onLongClickListener == null) return false;
            return this.onLongClickListener.onLongClick(position, user);
        });
        this.setFullName(user);
        this.binding.username.setText(user.getUsername());
        this.binding.profilePic.setImageURI(user.getProfilePicUrl());
        this.setInfo(isAdmin, isInviter);
        this.setSelection(showSelection, isSelected);
    }

    private void setFullName(User user) {
        SpannableStringBuilder sb = new SpannableStringBuilder(user.getFullName());
        if (user.isVerified()) {
            if (this.verifiedSpan == null) {
                Drawable verifiedDrawable = AppCompatResources.getDrawable(this.itemView.getContext(), R.drawable.verified);
                if (verifiedDrawable != null) {
                    Drawable drawable = verifiedDrawable.mutate();
                    drawable.setBounds(0, 0, this.drawableSize, this.drawableSize);
                    this.verifiedSpan = new VerticalImageSpan(drawable);
                }
            }
            try {
                if (this.verifiedSpan != null) {
                    sb.append("  ");
                    sb.setSpan(this.verifiedSpan, sb.length() - 1, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } catch (final Exception e) {
                Log.e(DirectUserViewHolder.TAG, "bind: ", e);
            }
        }
        this.binding.fullName.setText(sb);
    }

    private void setInfo(boolean isAdmin, boolean isInviter) {
        if (!isAdmin && !isInviter) {
            this.binding.info.setVisibility(View.GONE);
            return;
        }
        if (isAdmin) {
            this.binding.info.setText(R.string.admin);
            return;
        }
        this.binding.info.setText(R.string.inviter);
    }

    private void setSelection(boolean showSelection, boolean isSelected) {
        this.binding.select.setVisibility(showSelection ? View.VISIBLE : View.GONE);
        this.binding.getRoot().setSelected(isSelected);
    }
}
