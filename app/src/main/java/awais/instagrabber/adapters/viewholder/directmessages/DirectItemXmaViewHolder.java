package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;

import com.facebook.drawee.backends.pipeline.Fresco;

import awais.instagrabber.adapters.DirectItemsAdapter;
import awais.instagrabber.databinding.LayoutDmAnimatedMediaBinding;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectItemXma;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.repositories.responses.directmessages.XmaUrlInfo;
import awais.instagrabber.utils.NullSafePair;
import awais.instagrabber.utils.NumberUtils;

public class DirectItemXmaViewHolder extends DirectItemViewHolder {

    private final LayoutDmAnimatedMediaBinding binding;

    public DirectItemXmaViewHolder(@NonNull LayoutDmBaseBinding baseBinding,
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
        DirectItemXma xma = item.getXma();
        XmaUrlInfo playableUrlInfo = xma.getPlayableUrlInfo();
        XmaUrlInfo previewUrlInfo = xma.getPreviewUrlInfo();
        if (playableUrlInfo == null && previewUrlInfo == null) {
            this.binding.ivAnimatedMessage.setController(null);
            return;
        }
        XmaUrlInfo urlInfo = playableUrlInfo != null ? playableUrlInfo : previewUrlInfo;
        String url = urlInfo.getUrl();
        NullSafePair<Integer, Integer> widthHeight = NumberUtils.calculateWidthHeight(
                urlInfo.getHeight(),
                urlInfo.getWidth(),
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
}
