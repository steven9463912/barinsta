package awais.instagrabber.adapters.viewholder.directmessages;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;

import awais.instagrabber.adapters.DirectItemsAdapter;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmStoryShareBinding;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;

public class DirectItemPlaceholderViewHolder extends DirectItemViewHolder {

    private final LayoutDmStoryShareBinding binding;

    public DirectItemPlaceholderViewHolder(@NonNull LayoutDmBaseBinding baseBinding,
                                           LayoutDmStoryShareBinding binding,
                                           User currentUser,
                                           DirectThread thread,
                                           DirectItemsAdapter.DirectItemCallback callback) {
        super(baseBinding, currentUser, thread, callback);
        this.binding = binding;
        this.setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(DirectItem directItemModel, MessageDirection messageDirection) {
        this.binding.shareInfo.setText(directItemModel.getPlaceholder().getTitle());
        this.binding.text.setVisibility(View.VISIBLE);
        this.binding.text.setText(directItemModel.getPlaceholder().getMessage());
        this.binding.ivMediaPreview.setVisibility(View.GONE);
        this.binding.typeIcon.setVisibility(View.GONE);
    }

    @Override
    protected boolean allowLongClick() {
        return false;
    }

    @Override
    public int getSwipeDirection() {
        return ItemTouchHelper.ACTION_STATE_IDLE;
    }
}
