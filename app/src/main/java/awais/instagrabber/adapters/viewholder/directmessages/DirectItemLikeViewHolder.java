package awais.instagrabber.adapters.viewholder.directmessages;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;

import awais.instagrabber.adapters.DirectItemsAdapter;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmLikeBinding;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;

public class DirectItemLikeViewHolder extends DirectItemViewHolder {

    public DirectItemLikeViewHolder(@NonNull LayoutDmBaseBinding baseBinding,
                                    @NonNull LayoutDmLikeBinding binding,
                                    User currentUser,
                                    DirectThread thread,
                                    DirectItemsAdapter.DirectItemCallback callback) {
        super(baseBinding, currentUser, thread, callback);
        this.setItemView(binding.getRoot());
    }

    @Override
    public void bindItem(DirectItem directItemModel, MessageDirection messageDirection) {}

    @Override
    protected boolean canForward() {
        return false;
    }

    @Override
    public int getSwipeDirection() {
        return ItemTouchHelper.ACTION_STATE_IDLE;
    }
}
