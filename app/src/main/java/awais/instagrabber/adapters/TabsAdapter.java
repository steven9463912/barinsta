package awais.instagrabber.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import awais.instagrabber.R;
import awais.instagrabber.adapters.viewholder.TabViewHolder;
import awais.instagrabber.databinding.ItemFavSectionHeaderBinding;
import awais.instagrabber.databinding.ItemTabOrderPrefBinding;
import awais.instagrabber.models.Tab;
import awais.instagrabber.utils.Utils;

public class TabsAdapter extends ListAdapter<TabsAdapter.TabOrHeader, RecyclerView.ViewHolder> {
    private static final DiffUtil.ItemCallback<TabOrHeader> DIFF_CALLBACK = new DiffUtil.ItemCallback<TabOrHeader>() {
        @Override
        public boolean areItemsTheSame(@NonNull TabOrHeader oldItem, @NonNull TabOrHeader newItem) {
            if (oldItem.isHeader() && newItem.isHeader()) {
                return oldItem.header == newItem.header;
            }
            if (!oldItem.isHeader() && !newItem.isHeader()) {
                Tab oldTab = oldItem.tab;
                Tab newTab = newItem.tab;
                return oldTab.getIconResId() == newTab.getIconResId()
                        && Objects.equals(oldTab.getTitle(), newTab.getTitle());
            }
            return false;
        }

        @Override
        public boolean areContentsTheSame(@NonNull TabOrHeader oldItem, @NonNull TabOrHeader newItem) {
            if (oldItem.isHeader() && newItem.isHeader()) {
                return oldItem.header == newItem.header;
            }
            if (!oldItem.isHeader() && !newItem.isHeader()) {
                Tab oldTab = oldItem.tab;
                Tab newTab = newItem.tab;
                return oldTab.getIconResId() == newTab.getIconResId()
                        && Objects.equals(oldTab.getTitle(), newTab.getTitle());
            }
            return false;
        }
    };

    private final TabAdapterCallback tabAdapterCallback;

    private List<Tab> current = new ArrayList<>();
    private List<Tab> others = new ArrayList<>();

    public TabsAdapter(@NonNull TabAdapterCallback tabAdapterCallback) {
        super(TabsAdapter.DIFF_CALLBACK);
        this.tabAdapterCallback = tabAdapterCallback;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        if (viewType == 1) {
            ItemTabOrderPrefBinding binding = ItemTabOrderPrefBinding.inflate(layoutInflater, parent, false);
            return new TabViewHolder(binding, this.tabAdapterCallback);
        }
        ItemFavSectionHeaderBinding headerBinding = ItemFavSectionHeaderBinding.inflate(layoutInflater, parent, false);
        return new DirectUsersAdapter.HeaderViewHolder(headerBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof DirectUsersAdapter.HeaderViewHolder) {
            ((DirectUsersAdapter.HeaderViewHolder) holder).bind(R.string.other_tabs);
            return;
        }
        if (holder instanceof TabViewHolder) {
            Tab tab = this.getItem(position).tab;
            ((TabViewHolder) holder).bind(tab, this.others.contains(tab), this.current.size() == 5);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return this.getItem(position).isHeader() ? 0 : 1;
    }

    public void submitList(List<Tab> current, List<Tab> others, Runnable commitCallback) {
        ImmutableList.Builder<TabOrHeader> builder = ImmutableList.builder();
        if (current != null) {
            builder.addAll(current.stream()
                                  .map(TabOrHeader::new)
                                  .collect(Collectors.toList()));
        }
        builder.add(new TabOrHeader(R.string.other_tabs));
        if (others != null) {
            builder.addAll(others.stream()
                                 .map(TabOrHeader::new)
                                 .collect(Collectors.toList()));
        }
        // Mutable non-null copies
        this.current = current != null ? new ArrayList<>(current) : new ArrayList<>();
        this.others = others != null ? new ArrayList<>(others) : new ArrayList<>();
        this.submitList(builder.build(), commitCallback);
    }

    public void submitList(List<Tab> current, List<Tab> others) {
        this.submitList(current, others, null);
    }

    public void moveItem(int from, int to) {
        List<Tab> currentCopy = new ArrayList<>(this.current);
        Utils.moveItem(from, to, currentCopy);
        this.submitList(currentCopy, this.others);
        this.tabAdapterCallback.onOrderChange(currentCopy);
    }

    public int getCurrentCount() {
        return this.current.size();
    }

    public static class TabOrHeader {
        Tab tab;
        int header;

        public TabOrHeader(Tab tab) {
            this.tab = tab;
        }

        public TabOrHeader(@StringRes int header) {
            this.header = header;
        }

        boolean isHeader() {
            return this.header != 0;
        }
    }

    public interface TabAdapterCallback {
        void onStartDrag(TabViewHolder viewHolder);

        void onOrderChange(List<Tab> newOrderTabs);

        void onAdd(Tab tab);

        void onRemove(Tab tab);
    }
}
