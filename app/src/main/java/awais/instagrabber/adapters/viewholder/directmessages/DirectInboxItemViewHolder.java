package awais.instagrabber.adapters.viewholder.directmessages;

import android.content.res.Resources;
import android.graphics.Typeface;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.common.collect.ImmutableList;

import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.adapters.DirectMessageInboxAdapter;
import awais.instagrabber.databinding.LayoutDmInboxItemBinding;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.utils.DMUtils;
import awais.instagrabber.utils.TextUtils;

public final class DirectInboxItemViewHolder extends RecyclerView.ViewHolder {
    // private static final String TAG = "DMInboxItemVH";
    private final LayoutDmInboxItemBinding binding;
    private final DirectMessageInboxAdapter.OnItemClickListener onClickListener;
    private final List<SimpleDraweeView> multipleProfilePics;
    private final int childSmallSize;
    private final int childTinySize;

    public DirectInboxItemViewHolder(@NonNull final LayoutDmInboxItemBinding binding,
                                     final DirectMessageInboxAdapter.OnItemClickListener onClickListener) {
        super(binding.getRoot());
        this.binding = binding;
        this.onClickListener = onClickListener;
        this.multipleProfilePics = ImmutableList.of(
                binding.multiPic1,
                binding.multiPic2,
                binding.multiPic3
        );
        this.childSmallSize = this.itemView.getResources().getDimensionPixelSize(R.dimen.dm_inbox_avatar_size_small);
        this.childTinySize = this.itemView.getResources().getDimensionPixelSize(R.dimen.dm_inbox_avatar_size_tiny);
    }

    public void bind(DirectThread thread) {
        if (thread == null) return;
        if (this.onClickListener != null) {
            this.itemView.setOnClickListener((v) -> this.onClickListener.onItemClick(thread));
        }
        this.setProfilePics(thread);
        this.setTitle(thread);
        List<DirectItem> items = thread.getItems();
        if (items == null || items.isEmpty()) return;
        DirectItem item = thread.getFirstDirectItem();
        if (item == null) return;
        this.setDateTime(item);
        this.setSubtitle(thread);
        this.setReadState(thread);
    }

    private void setProfilePics(@NonNull DirectThread thread) {
        List<User> users = thread.getUsers();
        if (users.size() > 1) {
            this.binding.profilePic.setVisibility(View.GONE);
            this.binding.multiPicContainer.setVisibility(View.VISIBLE);
            for (int i = 0; i < Math.min(3, users.size()); ++i) {
                User user = users.get(i);
                SimpleDraweeView view = this.multipleProfilePics.get(i);
                view.setVisibility(user == null ? View.GONE : View.VISIBLE);
                if (user == null) return;
                String profilePicUrl = user.getProfilePicUrl();
                view.setImageURI(profilePicUrl);
                this.setChildSize(view, users.size());
                if (i == 1) {
                    this.updateConstraints(view, users.size());
                }
                view.requestLayout();
            }
            return;
        }
        this.binding.profilePic.setVisibility(View.VISIBLE);
        this.binding.multiPicContainer.setVisibility(View.GONE);
        String profilePicUrl = users.size() == 1 ? users.get(0).getProfilePicUrl() : null;
        if (profilePicUrl == null) {
            this.binding.profilePic.setController(null);
            return;
        }
        this.binding.profilePic.setImageURI(profilePicUrl);
    }

    private void updateConstraints(SimpleDraweeView view, int length) {
        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) view.getLayoutParams();
        if (length >= 2) {
            layoutParams.endToEnd = ConstraintSet.PARENT_ID;
            layoutParams.bottomToBottom = ConstraintSet.PARENT_ID;
        }
        if (length == 3) {
            layoutParams.startToStart = ConstraintSet.PARENT_ID;
            layoutParams.topToTop = ConstraintSet.PARENT_ID;
        }
    }

    private void setChildSize(SimpleDraweeView view, int length) {
        int size = length == 3 ? this.childTinySize : this.childSmallSize;
        ConstraintLayout.LayoutParams viewLayoutParams = new ConstraintLayout.LayoutParams(size, size);
        view.setLayoutParams(viewLayoutParams);
    }

    private void setTitle(@NonNull DirectThread thread) {
        String threadTitle = thread.getThreadTitle();
        this.binding.threadTitle.setText(threadTitle);
    }

    private void setSubtitle(@NonNull DirectThread thread) {
        Resources resources = this.itemView.getResources();
        long viewerId = thread.getViewerId();
//        final DirectThreadDirectStory directStory = thread.getDirectStory();
//        if (directStory != null && !directStory.getItems().isEmpty()) {
//            final DirectItem item = directStory.getItems().get(0);
//            final MediaItemType mediaType = item.getVisualMedia().getMedia().getMediaType();
//            final String username = DMUtils.getUsername(thread.getUsers(), item.getUserId(), viewerId, resources);
//            final String subtitle = DMUtils.getMediaSpecificSubtitle(username, resources, mediaType);
//            binding.subtitle.setText(subtitle);
//            return;
//        }
        DirectItem item = thread.getFirstDirectItem();
        if (item == null) return;
        String subtitle = DMUtils.getMessageString(thread, resources, viewerId, item);
        this.binding.subtitle.setText(subtitle != null ? subtitle : "");
    }

    private void setDateTime(@NonNull DirectItem item) {
        long timestamp = item.getTimestamp() / 1000;
        String dateTimeString = TextUtils.getRelativeDateTimeString(timestamp);
        this.binding.tvDate.setText(dateTimeString);
    }

    private void setReadState(@NonNull DirectThread thread) {
        boolean read = DMUtils.isRead(thread);
        this.binding.unread.setVisibility(read ? View.GONE : View.VISIBLE);
        this.binding.threadTitle.setTypeface(null, read ? Typeface.NORMAL : Typeface.BOLD);
        this.binding.subtitle.setTypeface(null, read ? Typeface.NORMAL : Typeface.BOLD);
    }
}