package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import awais.instagrabber.adapters.viewholder.FollowsViewHolder;
import awais.instagrabber.databinding.ItemFollowBinding;
import awais.instagrabber.repositories.responses.User;

public final class LikesAdapter extends RecyclerView.Adapter<FollowsViewHolder> {
    private final List<User> profileModels;
    private final View.OnClickListener onClickListener;

    public LikesAdapter(List<User> profileModels,
                        View.OnClickListener onClickListener) {
        this.profileModels = profileModels;
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public FollowsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ItemFollowBinding binding = ItemFollowBinding.inflate(layoutInflater, parent, false);
        return new FollowsViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FollowsViewHolder holder, int position) {
        User model = this.profileModels.get(position);
        holder.bind(model, this.onClickListener);
    }

    @Override
    public int getItemCount() {
        return this.profileModels.size();
    }
}