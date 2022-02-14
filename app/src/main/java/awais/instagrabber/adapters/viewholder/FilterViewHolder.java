package awais.instagrabber.adapters.viewholder;

import android.graphics.Bitmap;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.collect.ImmutableList;

import java.util.Collection;

import awais.instagrabber.adapters.FiltersAdapter;
import awais.instagrabber.databinding.ItemFilterBinding;
import awais.instagrabber.fragments.imageedit.filters.filters.Filter;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.BitmapUtils;
import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;

public class FilterViewHolder extends RecyclerView.ViewHolder {
    private static final String TAG = FilterViewHolder.class.getSimpleName();

    private final ItemFilterBinding binding;
    private final Collection<GPUImageFilter> tuneFilters;
    private final FiltersAdapter.OnFilterClickListener onFilterClickListener;
    private final AppExecutors appExecutors;

    public FilterViewHolder(@NonNull ItemFilterBinding binding,
                            Collection<GPUImageFilter> tuneFilters,
                            FiltersAdapter.OnFilterClickListener onFilterClickListener) {
        super(binding.getRoot());
        this.binding = binding;
        this.tuneFilters = tuneFilters;
        this.onFilterClickListener = onFilterClickListener;
        this.appExecutors = AppExecutors.INSTANCE;
    }

    public void bind(int position, String originalKey, Bitmap originalBitmap, Filter<?> item, boolean isSelected) {
        if (originalBitmap == null || item == null) return;
        if (this.onFilterClickListener != null) {
            this.itemView.setOnClickListener(v -> this.onFilterClickListener.onClick(position, item));
        }
        if (item.getLabel() != -1) {
            this.binding.name.setVisibility(View.VISIBLE);
            this.binding.name.setText(item.getLabel());
            this.binding.name.setSelected(isSelected);
        } else {
            this.binding.name.setVisibility(View.GONE);
        }
        String filterKey = item.getLabel() + "_" + originalKey;
        // avoid resetting the bitmap
        if (this.binding.preview.getTag() != null && this.binding.preview.getTag().equals(filterKey)) return;
        this.binding.preview.setTag(filterKey);
        Bitmap bitmap = BitmapUtils.getBitmapFromMemCache(filterKey);
        if (bitmap == null) {
            GPUImageFilter filter = item.getInstance();
            this.appExecutors.getTasksThread().submit(() -> GPUImage.getBitmapForMultipleFilters(
                    originalBitmap,
                    ImmutableList.<GPUImageFilter>builder().add(filter).addAll(this.tuneFilters).build(),
                    filteredBitmap -> {
                        BitmapUtils.addBitmapToMemoryCache(filterKey, filteredBitmap, true);
                        this.appExecutors.getMainThread().execute(() -> this.binding.getRoot().post(() -> this.binding.preview.setImageBitmap(filteredBitmap)));
                    }
            ));
            return;
        }
        this.binding.getRoot().post(() -> this.binding.preview.setImageBitmap(bitmap));
    }
}
