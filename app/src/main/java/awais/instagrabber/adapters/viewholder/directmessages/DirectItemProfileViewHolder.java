package awais.instagrabber.adapters.viewholder.directmessages;

import android.content.res.Resources;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;

import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.view.SimpleDraweeView;
import com.google.common.collect.ImmutableList;

import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.adapters.DirectItemsAdapter;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmProfileBinding;
import awais.instagrabber.models.enums.DirectItemType;
import awais.instagrabber.repositories.responses.Location;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.TextUtils;

public class DirectItemProfileViewHolder extends DirectItemViewHolder {

    private final LayoutDmProfileBinding binding;
    private final ImmutableList<SimpleDraweeView> previewViews;

    public DirectItemProfileViewHolder(@NonNull LayoutDmBaseBinding baseBinding,
                                       @NonNull LayoutDmProfileBinding binding,
                                       User currentUser,
                                       DirectThread thread,
                                       DirectItemsAdapter.DirectItemCallback callback) {
        super(baseBinding, currentUser, thread, callback);
        this.binding = binding;
        this.setItemView(binding.getRoot());
        this.previewViews = ImmutableList.of(
                binding.preview1,
                binding.preview2,
                binding.preview3,
                binding.preview4,
                binding.preview5,
                binding.preview6
        );
    }

    @Override
    public void bindItem(@NonNull DirectItem item,
                         MessageDirection messageDirection) {
        this.binding.getRoot().setBackgroundResource(messageDirection == MessageDirection.INCOMING
                                                ? R.drawable.bg_speech_bubble_incoming
                                                : R.drawable.bg_speech_bubble_outgoing);
        if (item.getItemType() == DirectItemType.PROFILE) {
            this.setProfile(item);
        } else if (item.getItemType() == DirectItemType.LOCATION) {
            this.setLocation(item);
        } else {
            return;
        }
        for (SimpleDraweeView previewView : this.previewViews) {
            previewView.setImageURI((String) null);
        }
        List<Media> previewMedias = item.getPreviewMedias();
        if (previewMedias == null || previewMedias.size() <= 0) {
            this.binding.firstRow.setVisibility(View.GONE);
            this.binding.secondRow.setVisibility(View.GONE);
            return;
        }
        Resources resources = this.itemView.getResources();
        if (previewMedias.size() <= 3) {
            this.binding.firstRow.setVisibility(View.VISIBLE);
            this.binding.secondRow.setVisibility(View.GONE);
            this.binding.preview1.setHierarchy(new GenericDraweeHierarchyBuilder(resources)
                    .setRoundingParams(RoundingParams.fromCornersRadii(0, 0, 0, this.dmRadius))
                    .build());
            this.binding.preview3.setHierarchy(new GenericDraweeHierarchyBuilder(resources)
                    .setRoundingParams(RoundingParams.fromCornersRadii(0, 0, this.dmRadius, 0))
                    .build());
        }
        if (previewMedias.size() > 3) {
            this.binding.preview4.setHierarchy(new GenericDraweeHierarchyBuilder(resources)
                    .setRoundingParams(RoundingParams.fromCornersRadii(0, 0, 0, this.dmRadius))
                    .build());
            this.binding.preview6.setHierarchy(new GenericDraweeHierarchyBuilder(resources)
                    .setRoundingParams(RoundingParams.fromCornersRadii(0, 0, this.dmRadius, 0))
                    .build());
        }
        for (int i = 0; i < previewMedias.size(); i++) {
            Media previewMedia = previewMedias.get(i);
            if (previewMedia == null) continue;
            String url = ResponseBodyUtils.getThumbUrl(previewMedia);
            if (url == null) continue;
            this.previewViews.get(i).setImageURI(url);
        }
    }

    private void setProfile(@NonNull DirectItem item) {
        User profile = item.getProfile();
        if (profile == null) return;
        this.binding.profilePic.setImageURI(profile.getProfilePicUrl());
        this.binding.username.setText(profile.getUsername());
        String fullName = profile.getFullName();
        if (!TextUtils.isEmpty(fullName)) {
            this.binding.fullName.setVisibility(View.VISIBLE);
            this.binding.fullName.setText(fullName);
        } else {
            this.binding.fullName.setVisibility(View.GONE);
        }
        this.binding.isVerified.setVisibility(profile.isVerified() ? View.VISIBLE : View.GONE);
        this.itemView.setOnClickListener(v -> this.openProfile(profile.getUsername()));
    }

    private void setLocation(@NonNull DirectItem item) {
        Location location = item.getLocation();
        if (location == null) return;
        this.binding.profilePic.setVisibility(View.GONE);
        this.binding.username.setText(location.getName());
        String address = location.getAddress();
        if (!TextUtils.isEmpty(address)) {
            this.binding.fullName.setText(address);
            this.binding.fullName.setVisibility(View.VISIBLE);
        } else {
            this.binding.fullName.setVisibility(View.GONE);
        }
        this.binding.isVerified.setVisibility(View.GONE);
        this.itemView.setOnClickListener(v -> this.openLocation(location.getPk()));
    }

    @Override
    public int getSwipeDirection() {
        return ItemTouchHelper.ACTION_STATE_IDLE;
    }
}
