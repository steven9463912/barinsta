package awais.instagrabber.adapters;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import java.util.Collection;
import java.util.List;

import awais.instagrabber.adapters.viewholder.FilterViewHolder;
import awais.instagrabber.databinding.ItemFilterBinding;
import awais.instagrabber.fragments.imageedit.filters.filters.Filter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;

public class FiltersAdapter extends ListAdapter<Filter<?>, FilterViewHolder> {

    private static final DiffUtil.ItemCallback<Filter<?>> DIFF_CALLBACK = new DiffUtil.ItemCallback<Filter<?>>() {
        @Override
        public boolean areItemsTheSame(@NonNull Filter<?> oldItem, @NonNull Filter<?> newItem) {
            return oldItem.getType().equals(newItem.getType());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Filter<?> oldItem, @NonNull Filter<?> newItem) {
            return oldItem.getType().equals(newItem.getType());
        }
    };

    private final Bitmap bitmap;
    private final OnFilterClickListener onFilterClickListener;
    private final Collection<GPUImageFilter> filters;
    private final String originalKey;
    private int selectedPosition;

    public FiltersAdapter(Collection<GPUImageFilter> filters,
                          String originalKey,
                          Bitmap bitmap,
                          OnFilterClickListener onFilterClickListener) {
        super(FiltersAdapter.DIFF_CALLBACK);
        this.filters = filters;
        this.originalKey = originalKey;
        this.bitmap = bitmap;
        this.onFilterClickListener = onFilterClickListener;
        this.setHasStableIds(true);
    }

    @NonNull
    @Override
    public FilterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ItemFilterBinding binding = ItemFilterBinding.inflate(layoutInflater, parent, false);
        return new FilterViewHolder(binding, this.filters, this.onFilterClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull FilterViewHolder holder, int position) {
        holder.bind(position, this.originalKey, this.bitmap, this.getItem(position), this.selectedPosition == position);
    }

    @Override
    public long getItemId(int position) {
        return this.getItem(position).getLabel();
    }

    public void setSelected(int position) {
        int prev = selectedPosition;
        selectedPosition = position;
        this.notifyItemChanged(position);
        this.notifyItemChanged(prev);
    }

    public void setSelectedFilter(GPUImageFilter instance) {
        List<Filter<?>> currentList = this.getCurrentList();
        int index = -1;
        for (int i = 0; i < currentList.size(); i++) {
            Filter<?> filter = currentList.get(i);
            GPUImageFilter filterInstance = filter.getInstance();
            if (filterInstance.getClass() == instance.getClass()) {
                index = i;
                break;
            }
        }
        if (index < 0) return;
        this.setSelected(index);
    }

    public interface OnFilterClickListener {
        void onClick(int position, Filter<?> filter);
    }
}
