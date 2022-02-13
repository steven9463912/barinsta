package awais.instagrabber.adapters.viewholder.directmessages;

import android.content.res.Resources;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.adapters.UserSearchResultsAdapter;
import awais.instagrabber.databinding.LayoutDmUserItemBinding;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.repositories.responses.directmessages.RankedRecipient;

public class RecipientThreadViewHolder extends RecyclerView.ViewHolder {
    private static final String TAG = RecipientThreadViewHolder.class.getSimpleName();

    private final LayoutDmUserItemBinding binding;
    private final UserSearchResultsAdapter.OnRecipientClickListener onThreadClickListener;
    private final float translateAmount;

    public RecipientThreadViewHolder(@NonNull final LayoutDmUserItemBinding binding,
                                     final UserSearchResultsAdapter.OnRecipientClickListener onThreadClickListener) {
        super(binding.getRoot());
        this.binding = binding;
        this.onThreadClickListener = onThreadClickListener;
        binding.info.setVisibility(View.GONE);
        Resources resources = this.itemView.getResources();
        int avatarSize = resources.getDimensionPixelSize(R.dimen.dm_inbox_avatar_size);
        this.translateAmount = ((float) avatarSize) / 7;
    }

    public void bind(int position,
                     DirectThread thread,
                     boolean showSelection,
                     boolean isSelected) {
        if (thread == null || thread.getUsers().size() == 0) return;
        this.binding.getRoot().setOnClickListener(v -> {
            if (this.onThreadClickListener == null) return;
            this.onThreadClickListener.onClick(position, RankedRecipient.of(thread), isSelected);
        });
        this.binding.fullName.setText(thread.getThreadTitle());
        this.setUsername(thread);
        this.setProfilePic(thread);
        this.setSelection(showSelection, isSelected);
    }

    private void setProfilePic(DirectThread thread) {
        List<User> users = thread.getUsers();
        this.binding.profilePic.setImageURI(users.get(0).getProfilePicUrl());
        this.binding.profilePic.setScaleX(1);
        this.binding.profilePic.setScaleY(1);
        this.binding.profilePic.setTranslationX(0);
        this.binding.profilePic.setTranslationY(0);
        if (users.size() > 1) {
            this.binding.profilePic2.setVisibility(View.VISIBLE);
            this.binding.profilePic2.setImageURI(users.get(1).getProfilePicUrl());
            this.binding.profilePic2.setTranslationX(this.translateAmount);
            this.binding.profilePic2.setTranslationY(this.translateAmount);
            final float scaleAmount = 0.75f;
            this.binding.profilePic2.setScaleX(scaleAmount);
            this.binding.profilePic2.setScaleY(scaleAmount);
            this.binding.profilePic.setScaleX(scaleAmount);
            this.binding.profilePic.setScaleY(scaleAmount);
            this.binding.profilePic.setTranslationX(-this.translateAmount);
            this.binding.profilePic.setTranslationY(-this.translateAmount);
            return;
        }
        this.binding.profilePic2.setVisibility(View.GONE);
    }

    private void setUsername(DirectThread thread) {
        if (thread.isGroup()) {
            this.binding.username.setVisibility(View.GONE);
            return;
        }
        this.binding.username.setVisibility(View.VISIBLE);
        // for a non-group thread, the thread title is the username so set the full name in the username text view
        this.binding.username.setText(thread.getUsers().get(0).getFullName());
    }

    private void setSelection(boolean showSelection, boolean isSelected) {
        this.binding.select.setVisibility(showSelection ? View.VISIBLE : View.GONE);
        this.binding.getRoot().setSelected(isSelected);
    }
}
