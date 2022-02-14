package awais.instagrabber.adapters.viewholder;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.R;
import awais.instagrabber.adapters.FeedStoriesListAdapter;
import awais.instagrabber.adapters.HighlightStoriesListAdapter;
import awais.instagrabber.databinding.ItemNotificationBinding;
import awais.instagrabber.repositories.responses.stories.Story;
import awais.instagrabber.utils.ResponseBodyUtils;

public final class StoryListViewHolder extends RecyclerView.ViewHolder {
    private final ItemNotificationBinding binding;

    public StoryListViewHolder(ItemNotificationBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bind(Story model,
                     FeedStoriesListAdapter.OnFeedStoryClickListener notificationClickListener) {
        if (model == null) return;

        int storiesCount = model.getMediaCount();
        this.binding.tvComment.setVisibility(View.VISIBLE);
        this.binding.tvComment.setText(this.itemView.getResources().getQuantityString(R.plurals.stories_count, storiesCount, storiesCount));

        this.binding.tvSubComment.setVisibility(View.GONE);

        this.binding.tvDate.setText(model.getDateTime());

        this.binding.tvUsername.setText(model.getUser().getUsername());
        this.binding.ivProfilePic.setImageURI(model.getUser().getProfilePicUrl());
        this.binding.ivProfilePic.setOnClickListener(v -> {
            if (notificationClickListener == null) return;
            notificationClickListener.onProfileClick(model.getUser().getUsername());
        });

        if (model.getItems() != null && model.getItems().size() > 0) {
            this.binding.ivPreviewPic.setVisibility(View.VISIBLE);
            this.binding.ivPreviewPic.setImageURI(ResponseBodyUtils.getThumbUrl(model.getItems().get(0)));
        } else this.binding.ivPreviewPic.setVisibility(View.INVISIBLE);

        final float alpha = model.getSeen() != null && model.getSeen().equals(model.getLatestReelMedia())
                ? 0.5F : 1.0F;
        this.binding.ivProfilePic.setAlpha(alpha);
        this.binding.ivPreviewPic.setAlpha(alpha);
        this.binding.tvUsername.setAlpha(alpha);
        this.binding.tvComment.setAlpha(alpha);
        this.binding.tvDate.setAlpha(alpha);

        this.itemView.setOnClickListener(v -> {
            if (notificationClickListener == null) return;
            notificationClickListener.onFeedStoryClick(model);
        });
    }

    public void bind(Story model,
                     int position,
                     HighlightStoriesListAdapter.OnHighlightStoryClickListener notificationClickListener) {
        if (model == null) return;

        int storiesCount = model.getMediaCount();
        this.binding.tvComment.setVisibility(View.VISIBLE);
        this.binding.tvComment.setText(this.itemView.getResources().getQuantityString(R.plurals.stories_count, storiesCount, storiesCount));

        this.binding.tvSubComment.setVisibility(View.GONE);

        this.binding.tvUsername.setText(model.getDateTime());

        this.binding.ivProfilePic.setVisibility(View.GONE);

        this.binding.ivPreviewPic.setVisibility(View.VISIBLE);
        this.binding.ivPreviewPic.setImageURI(model.getCoverImageVersion().getUrl());

        this.itemView.setOnClickListener(v -> {
            if (notificationClickListener == null) return;
            notificationClickListener.onHighlightClick(model, position);
        });
    }
}