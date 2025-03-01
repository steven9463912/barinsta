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
import awais.instagrabber.adapters.DirectPendingUsersAdapter;
import awais.instagrabber.customviews.VerticalImageSpan;
import awais.instagrabber.databinding.LayoutDmPendingUserItemBinding;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.utils.Utils;

public class DirectPendingUserViewHolder extends RecyclerView.ViewHolder {
    private static final String TAG = DirectPendingUserViewHolder.class.getSimpleName();

    private final LayoutDmPendingUserItemBinding binding;
    private final DirectPendingUsersAdapter.PendingUserCallback callback;
    private final int drawableSize;

    private VerticalImageSpan verifiedSpan;

    public DirectPendingUserViewHolder(@NonNull final LayoutDmPendingUserItemBinding binding,
                                       final DirectPendingUsersAdapter.PendingUserCallback callback) {
        super(binding.getRoot());
        this.binding = binding;
        this.callback = callback;
        this.drawableSize = Utils.convertDpToPx(24);
    }

    public void bind(int position, DirectPendingUsersAdapter.PendingUser pendingUser) {
        if (pendingUser == null) return;
        binding.getRoot().setOnClickListener(v -> {
            if (callback == null) return;
            callback.onClick(position, pendingUser);
        });
        setUsername(pendingUser);
        binding.requester.setText(itemView.getResources().getString(R.string.added_by, pendingUser.getRequester()));
        binding.profilePic.setImageURI(pendingUser.getUser().getProfilePicUrl());
        if (pendingUser.isInProgress()) {
            binding.approve.setVisibility(View.GONE);
            binding.deny.setVisibility(View.GONE);
            binding.progress.setVisibility(View.VISIBLE);
            return;
        }
        binding.approve.setVisibility(View.VISIBLE);
        binding.deny.setVisibility(View.VISIBLE);
        binding.progress.setVisibility(View.GONE);
        binding.approve.setOnClickListener(v -> {
            if (callback == null) return;
            callback.onApprove(position, pendingUser);
        });
        binding.deny.setOnClickListener(v -> {
            if (callback == null) return;
            callback.onDeny(position, pendingUser);
        });
    }

    private void setUsername(final DirectPendingUsersAdapter.PendingUser pendingUser) {
        User user = pendingUser.getUser();
        SpannableStringBuilder sb = new SpannableStringBuilder(user.getUsername());
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
                Log.e(DirectPendingUserViewHolder.TAG, "bind: ", e);
            }
        }
        this.binding.username.setText(sb);
    }
}
