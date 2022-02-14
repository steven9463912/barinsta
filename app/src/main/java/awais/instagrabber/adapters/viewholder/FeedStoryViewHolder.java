package awais.instagrabber.adapters.viewholder;

import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.adapters.FeedStoriesAdapter;
import awais.instagrabber.databinding.ItemHighlightBinding;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.stories.Story;

public final class FeedStoryViewHolder extends RecyclerView.ViewHolder {

    private final ItemHighlightBinding binding;

    public FeedStoryViewHolder(ItemHighlightBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bind(Story model,
                     int position,
                     FeedStoriesAdapter.OnFeedStoryClickListener listener) {
        if (model == null) return;
        this.binding.getRoot().setOnClickListener(v -> {
            if (listener == null) return;
            listener.onFeedStoryClick(model, position);
        });
        this.binding.getRoot().setOnLongClickListener(v -> {
            if (listener != null) listener.onFeedStoryLongClick(model, position);
            return true;
        });
        User profileModel = model.getUser();
        this.binding.title.setText(profileModel.getUsername());
        boolean isFullyRead =
                model.getSeen() != null &&
                model.getSeen().equals(model.getLatestReelMedia());
        this.binding.title.setAlpha(isFullyRead ? 0.5F : 1.0F);
        this.binding.icon.setImageURI(profileModel.getProfilePicUrl());
        this.binding.icon.setAlpha(isFullyRead ? 0.5F : 1.0F);

        if (model.getBroadcast() != null) this.binding.icon.setStoriesBorder(2);
        else if (model.getHasBestiesMedia() == true) this.binding.icon.setStoriesBorder(1);
        else this.binding.icon.setStoriesBorder(0);
    }
}