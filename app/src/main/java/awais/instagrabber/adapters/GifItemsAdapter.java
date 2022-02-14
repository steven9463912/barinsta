package awais.instagrabber.adapters;

import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import java.util.Objects;

import awais.instagrabber.databinding.ItemMediaBinding;
import awais.instagrabber.repositories.responses.giphy.GiphyGif;
import awais.instagrabber.utils.Utils;

public class GifItemsAdapter extends ListAdapter<GiphyGif, GifItemsAdapter.GifViewHolder> {

    private static final DiffUtil.ItemCallback<GiphyGif> diffCallback = new DiffUtil.ItemCallback<GiphyGif>() {
        @Override
        public boolean areItemsTheSame(@NonNull GiphyGif oldItem, @NonNull GiphyGif newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull GiphyGif oldItem, @NonNull GiphyGif newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }
    };

    private final OnItemClickListener onItemClickListener;

    public GifItemsAdapter(OnItemClickListener onItemClickListener) {
        super(GifItemsAdapter.diffCallback);
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public GifViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ItemMediaBinding binding = ItemMediaBinding.inflate(layoutInflater, parent, false);
        return new GifViewHolder(binding, this.onItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull GifViewHolder holder, int position) {
        holder.bind(this.getItem(position));
    }

    public static class GifViewHolder extends RecyclerView.ViewHolder {
        private static final String TAG = GifViewHolder.class.getSimpleName();
        private static final int size = Utils.displayMetrics.widthPixels / 3;

        private final ItemMediaBinding binding;
        private final OnItemClickListener onItemClickListener;

        public GifViewHolder(@NonNull ItemMediaBinding binding,
                             OnItemClickListener onItemClickListener) {
            super(binding.getRoot());
            this.binding = binding;
            this.onItemClickListener = onItemClickListener;
            binding.duration.setVisibility(View.GONE);
            GenericDraweeHierarchyBuilder builder = new GenericDraweeHierarchyBuilder(this.itemView.getResources());
            builder.setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER);
            binding.item.setHierarchy(builder.build());
        }

        public void bind(GiphyGif item) {
            if (this.onItemClickListener != null) {
                this.itemView.setOnClickListener(v -> this.onItemClickListener.onItemClick(item));
            }
            BaseControllerListener<ImageInfo> controllerListener = new BaseControllerListener<ImageInfo>() {
                @Override
                public void onFailure(String id, Throwable throwable) {
                    Log.e(GifViewHolder.TAG, "onFailure: ", throwable);
                }
            };
            ImageRequest request = ImageRequestBuilder
                    .newBuilderWithSource(Uri.parse(item.getImages().getFixedHeight().getWebp()))
                    .setResizeOptions(ResizeOptions.forDimensions(GifViewHolder.size, GifViewHolder.size))
                    .build();
            PipelineDraweeControllerBuilder builder = Fresco.newDraweeControllerBuilder()
                                                                  .setImageRequest(request)
                                                                  .setAutoPlayAnimations(true)
                                                                  .setControllerListener(controllerListener);
            this.binding.item.setController(builder.build());
        }
    }

    public interface OnItemClickListener {
        void onItemClick(GiphyGif giphyGif);
    }
}
