package awais.instagrabber.adapters.viewholder;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.databinding.ItemFollowBinding;
import awais.instagrabber.repositories.responses.User;

public final class FollowsViewHolder extends RecyclerView.ViewHolder {

    private final ItemFollowBinding binding;

    public FollowsViewHolder(@NonNull ItemFollowBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bind(User model,
                     View.OnClickListener onClickListener) {
        if (model == null) return;
        this.itemView.setTag(model);
        this.itemView.setOnClickListener(onClickListener);
        this.binding.username.setUsername("@" + model.getUsername(), model.isVerified());
        this.binding.fullName.setText(model.getFullName());
        this.binding.profilePic.setImageURI(model.getProfilePicUrl());
    }
}