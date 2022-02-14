package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import java.util.List;
import java.util.Objects;

import awais.instagrabber.adapters.viewholder.directmessages.DirectInboxItemViewHolder;
import awais.instagrabber.databinding.LayoutDmInboxItemBinding;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;

public final class DirectMessageInboxAdapter extends ListAdapter<DirectThread, DirectInboxItemViewHolder> {
    private final OnItemClickListener onClickListener;

    private static final DiffUtil.ItemCallback<DirectThread> diffCallback = new DiffUtil.ItemCallback<DirectThread>() {
        @Override
        public boolean areItemsTheSame(@NonNull DirectThread oldItem, @NonNull DirectThread newItem) {
            return oldItem.getThreadId().equals(newItem.getThreadId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull DirectThread oldThread,
                                          @NonNull DirectThread newThread) {
            boolean titleEqual = oldThread.getThreadTitle().equals(newThread.getThreadTitle());
            if (!titleEqual) return false;
            boolean lastSeenAtEqual = Objects.equals(oldThread.getLastSeenAt(), newThread.getLastSeenAt());
            if (!lastSeenAtEqual) return false;
            List<DirectItem> oldItems = oldThread.getItems();
            List<DirectItem> newItems = newThread.getItems();
            if (oldItems == null || newItems == null) return false;
            if (oldItems.size() != newItems.size()) return false;
            DirectItem oldItemFirst = oldThread.getFirstDirectItem();
            DirectItem newItemFirst = newThread.getFirstDirectItem();
            if (oldItemFirst == null || newItemFirst == null) return false;
            boolean idsEqual = oldItemFirst.getItemId().equals(newItemFirst.getItemId());
            if (!idsEqual) return false;
            return oldItemFirst.getTimestamp() == newItemFirst.getTimestamp();
        }
    };

    public DirectMessageInboxAdapter(OnItemClickListener onClickListener) {
        super(new AsyncDifferConfig.Builder<>(DirectMessageInboxAdapter.diffCallback).build());
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public DirectInboxItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        LayoutDmInboxItemBinding binding = LayoutDmInboxItemBinding.inflate(layoutInflater, parent, false);
        return new DirectInboxItemViewHolder(binding, this.onClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull DirectInboxItemViewHolder holder, int position) {
        DirectThread thread = this.getItem(position);
        holder.bind(thread);
    }

    @Override
    public long getItemId(int position) {
        return this.getItem(position).getThreadId().hashCode();
    }

    public interface OnItemClickListener {
        void onItemClick(DirectThread thread);
    }
}