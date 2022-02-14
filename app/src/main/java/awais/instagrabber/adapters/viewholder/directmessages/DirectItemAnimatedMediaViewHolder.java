package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.google.common.collect.ImmutableList;

import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.adapters.DirectItemsAdapter;
import awais.instagrabber.customviews.DirectItemContextMenu;
import awais.instagrabber.databinding.LayoutDmAnimatedMediaBinding;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.repositories.responses.AnimatedMediaFixedHeight;
import awais.instagrabber.repositories.responses.AnimatedMediaImages;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectItemAnimatedMedia;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.utils.NullSafePair;
import awais.instagrabber.utils.NumberUtils;
import awais.instagrabber.utils.Utils;

public class DirectItemAnimatedMediaViewHolder extends DirectItemViewHolder {

    private final LayoutDmAnimatedMediaBinding binding;

    public DirectItemAnimatedMediaViewHolder(@NonNull LayoutDmBaseBinding baseBinding,
                                             @NonNull LayoutDmAnimatedMediaBinding binding,
                                             User currentUser,
                                             DirectThread thread,
                                             DirectItemsAdapter.DirectItemCallback callback) {
        super(baseBinding, currentUser, thread, callback);
        this.binding = binding;
        this.setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(DirectItem item, MessageDirection messageDirection) {
        DirectItemAnimatedMedia animatedMediaModel = item.getAnimatedMedia();
        AnimatedMediaImages images = animatedMediaModel.getImages();
        if (images == null) return;
        AnimatedMediaFixedHeight fixedHeight = images.getFixedHeight();
        if (fixedHeight == null) return;
        String url = fixedHeight.getWebp();
        NullSafePair<Integer, Integer> widthHeight = NumberUtils.calculateWidthHeight(
                fixedHeight.getHeight(),
                fixedHeight.getWidth(),
                this.mediaImageMaxHeight,
                this.mediaImageMaxWidth
        );
        this.binding.ivAnimatedMessage.setVisibility(View.VISIBLE);
        ViewGroup.LayoutParams layoutParams = this.binding.ivAnimatedMessage.getLayoutParams();
        int width = widthHeight.first;
        int height = widthHeight.second;
        layoutParams.width = width;
        layoutParams.height = height;
        this.binding.ivAnimatedMessage.requestLayout();
        this.binding.ivAnimatedMessage.setController(Fresco.newDraweeControllerBuilder()
                                                      .setUri(url)
                                                      .setAutoPlayAnimations(true)
                                                      .build());
    }

    @Override
    public int getSwipeDirection() {
        return ItemTouchHelper.ACTION_STATE_IDLE;
    }

    @Override
    protected List<DirectItemContextMenu.MenuItem> getLongClickOptions() {
        return ImmutableList.of(
                new DirectItemContextMenu.MenuItem(R.id.detail, R.string.dms_inbox_giphy, item -> {
                    Utils.openURL(this.itemView.getContext(), "https://giphy.com/gifs/" + item.getAnimatedMedia().getId());
                    return null;
                })
        );
    }
}
