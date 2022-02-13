package awais.instagrabber.adapters.viewholder;

import android.text.TextUtils;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import awais.instagrabber.R;
import awais.instagrabber.adapters.NotificationsAdapter;
import awais.instagrabber.databinding.ItemNotificationBinding;
import awais.instagrabber.models.enums.NotificationType;
import awais.instagrabber.repositories.responses.notification.Notification;
import awais.instagrabber.repositories.responses.notification.NotificationArgs;

public final class NotificationViewHolder extends RecyclerView.ViewHolder {
    private final ItemNotificationBinding binding;

    public NotificationViewHolder(ItemNotificationBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bind(Notification model,
                     NotificationsAdapter.OnNotificationClickListener notificationClickListener) {
        if (model == null) return;
        int text = -1;
        CharSequence subtext = null;
        NotificationArgs args = model.getArgs();
        switch (model.getType()) {
            case LIKE:
                text = R.string.liked_notif;
                break;
            case COMMENT: // untested
                text = R.string.comment_notif;
                subtext = args.getText();
                break;
            case TAGGED:
                text = R.string.tagged_notif;
                break;
            case FOLLOW:
                text = R.string.follow_notif;
                break;
            case REQUEST:
                text = R.string.request_notif;
                break;
            case COMMENT_MENTION:
            case COMMENT_LIKE:
            case TAGGED_COMMENT:
            case RESPONDED_STORY:
                subtext = args.getText();
                break;
            case AYML:
                subtext = args.getFullName();
                break;
        }
        this.binding.tvSubComment.setText(model.getType() == NotificationType.AYML ? args.getText() : subtext);
        if (text == -1 && subtext != null) {
            this.binding.tvComment.setText(args.getText());
            this.binding.tvComment.setVisibility(TextUtils.isEmpty(args.getText()) || args.getText().equals(args.getFullName())
                    ? View.GONE : View.VISIBLE);
            this.binding.tvSubComment.setText(subtext);
            this.binding.tvSubComment.setVisibility(model.getType() == NotificationType.AYML ? View.VISIBLE : View.GONE);
        } else if (text != -1) {
            this.binding.tvComment.setText(text);
            this.binding.tvSubComment.setVisibility(subtext == null ? View.GONE : View.VISIBLE);
        }

        this.binding.tvDate.setVisibility(model.getType() == NotificationType.AYML ? View.GONE : View.VISIBLE);
        if (model.getType() != NotificationType.AYML) {
            this.binding.tvDate.setText(args.getDateTime());
        }

        this.binding.isVerified.setVisibility(args.isVerified() ? View.VISIBLE : View.GONE);

        this.binding.tvUsername.setText(args.getUsername());
        this.binding.ivProfilePic.setImageURI(args.getProfilePic());
        this.binding.ivProfilePic.setOnClickListener(v -> {
            if (notificationClickListener == null) return;
            notificationClickListener.onProfileClick(args.getUsername());
        });

        if (model.getType() == NotificationType.AYML) {
            this.binding.ivPreviewPic.setVisibility(View.GONE);
        } else if (args.getMedia() == null) {
            this.binding.ivPreviewPic.setVisibility(View.INVISIBLE);
        } else {
            this.binding.ivPreviewPic.setVisibility(View.VISIBLE);
            this.binding.ivPreviewPic.setImageURI(args.getMedia().get(0).getImage());
            this.binding.ivPreviewPic.setOnClickListener(v -> {
                if (notificationClickListener == null) return;
                notificationClickListener.onPreviewClick(model);
            });
        }

        this.itemView.setOnClickListener(v -> {
            if (notificationClickListener == null) return;
            notificationClickListener.onNotificationClick(model);
        });
    }
}