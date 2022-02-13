package awais.instagrabber.adapters.viewholder;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import java.util.concurrent.atomic.AtomicInteger;

import awais.instagrabber.R;
import awais.instagrabber.adapters.DiscoverTopicsAdapter;
import awais.instagrabber.adapters.SavedCollectionsAdapter;
import awais.instagrabber.databinding.ItemDiscoverTopicBinding;
import awais.instagrabber.repositories.responses.discover.TopicCluster;
import awais.instagrabber.repositories.responses.saved.SavedCollection;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.utils.ResponseBodyUtils;

public class TopicClusterViewHolder extends RecyclerView.ViewHolder {
    private final ItemDiscoverTopicBinding binding;
    private final DiscoverTopicsAdapter.OnTopicClickListener onTopicClickListener;
    private final SavedCollectionsAdapter.OnCollectionClickListener onCollectionClickListener;

    public TopicClusterViewHolder(@NonNull ItemDiscoverTopicBinding binding,
                                  DiscoverTopicsAdapter.OnTopicClickListener onTopicClickListener,
                                  SavedCollectionsAdapter.OnCollectionClickListener onCollectionClickListener) {
        super(binding.getRoot());
        this.binding = binding;
        this.onTopicClickListener = onTopicClickListener;
        this.onCollectionClickListener = onCollectionClickListener;
    }

    public void bind(TopicCluster topicCluster) {
        if (topicCluster == null) {
            return;
        }
        AtomicInteger titleColor = new AtomicInteger(-1);
        AtomicInteger backgroundColor = new AtomicInteger(-1);
        if (this.onTopicClickListener != null) {
            this.itemView.setOnClickListener(v -> this.onTopicClickListener.onTopicClick(
                    topicCluster,
                    this.binding.cover,
                    titleColor.get(),
                    backgroundColor.get()
            ));
            this.itemView.setOnLongClickListener(v -> {
                this.onTopicClickListener.onTopicLongClick(topicCluster.getCoverMedia());
                return true;
            });
        }
        // binding.title.setTransitionName("title-" + topicCluster.getId());
        this.binding.cover.setTransitionName("cover-" + topicCluster.getId());
        String thumbUrl = ResponseBodyUtils.getThumbUrl(topicCluster.getCoverMedia());
        if (thumbUrl == null) {
            this.binding.cover.setImageURI((String) null);
        } else {
            ImageRequest imageRequest = ImageRequestBuilder
                    .newBuilderWithSource(Uri.parse(thumbUrl))
                    .build();
            ImagePipeline imagePipeline = Fresco.getImagePipeline();
            DataSource<CloseableReference<CloseableImage>> dataSource = imagePipeline
                    .fetchDecodedImage(imageRequest, CallerThreadExecutor.getInstance());
            dataSource.subscribe(new BaseBitmapDataSubscriber() {
                @Override
                public void onNewResultImpl(@Nullable final Bitmap bitmap) {
                    if (dataSource.isFinished()) {
                        dataSource.close();
                    }
                    if (bitmap != null) {
                        Palette.from(bitmap).generate(p -> {
                            Resources resources = TopicClusterViewHolder.this.itemView.getResources();
                            int titleTextColor = resources.getColor(R.color.white);
                            if (p != null) {
                                Palette.Swatch swatch = p.getDominantSwatch();
                                if (swatch != null) {
                                    backgroundColor.set(swatch.getRgb());
                                    final GradientDrawable gd = new GradientDrawable(
                                            GradientDrawable.Orientation.TOP_BOTTOM,
                                            new int[]{Color.TRANSPARENT, backgroundColor.get()});
                                    titleTextColor = swatch.getTitleTextColor();
                                    TopicClusterViewHolder.this.binding.background.setBackground(gd);
                                }
                            }
                            titleColor.set(titleTextColor);
                            TopicClusterViewHolder.this.binding.title.setTextColor(titleTextColor);
                        });
                    }
                }

                @Override
                public void onFailureImpl(@NonNull final DataSource dataSource) {
                    dataSource.close();
                }
            }, CallerThreadExecutor.getInstance());
            this.binding.cover.setImageRequest(imageRequest);
        }
        this.binding.title.setText(topicCluster.getTitle());
    }

    public void bind(SavedCollection topicCluster) {
        if (topicCluster == null) {
            return;
        }
        AtomicInteger titleColor = new AtomicInteger(-1);
        AtomicInteger backgroundColor = new AtomicInteger(-1);
        if (this.onCollectionClickListener != null) {
            this.itemView.setOnClickListener(v -> this.onCollectionClickListener.onCollectionClick(
                    topicCluster,
                    this.binding.getRoot(),
                    this.binding.cover,
                    this.binding.title,
                    titleColor.get(),
                    backgroundColor.get()
            ));
        }
        // binding.title.setTransitionName("title-" + topicCluster.getCollectionId());
        this.binding.cover.setTransitionName("cover-" + topicCluster.getCollectionId());
        Media coverMedia = topicCluster.getCoverMediaList() == null
                ? topicCluster.getCoverMedia()
                : topicCluster.getCoverMediaList().get(0);
        String thumbUrl = ResponseBodyUtils.getThumbUrl(coverMedia);
        if (thumbUrl == null) {
            this.binding.cover.setImageURI((String) null);
        } else {
            ImageRequest imageRequest = ImageRequestBuilder
                    .newBuilderWithSource(Uri.parse(thumbUrl))
                    .build();
            ImagePipeline imagePipeline = Fresco.getImagePipeline();
            DataSource<CloseableReference<CloseableImage>> dataSource = imagePipeline
                    .fetchDecodedImage(imageRequest, CallerThreadExecutor.getInstance());
            dataSource.subscribe(new BaseBitmapDataSubscriber() {
                @Override
                public void onNewResultImpl(@Nullable final Bitmap bitmap) {
                    if (dataSource.isFinished()) {
                        dataSource.close();
                    }
                    if (bitmap != null) {
                        Palette.from(bitmap).generate(p -> {
                            Resources resources = TopicClusterViewHolder.this.itemView.getResources();
                            int titleTextColor = resources.getColor(R.color.white);
                            if (p != null) {
                                Palette.Swatch swatch = p.getDominantSwatch();
                                if (swatch != null) {
                                    backgroundColor.set(swatch.getRgb());
                                    final GradientDrawable gd = new GradientDrawable(
                                            GradientDrawable.Orientation.TOP_BOTTOM,
                                            new int[]{Color.TRANSPARENT, backgroundColor.get()}
                                    );
                                    titleTextColor = swatch.getTitleTextColor();
                                    TopicClusterViewHolder.this.binding.background.setBackground(gd);
                                }
                            }
                            titleColor.set(titleTextColor);
                            TopicClusterViewHolder.this.binding.title.setTextColor(titleTextColor);
                        });
                    }
                }

                @Override
                public void onFailureImpl(@NonNull final DataSource dataSource) {
                    dataSource.close();
                }
            }, CallerThreadExecutor.getInstance());
            this.binding.cover.setImageRequest(imageRequest);
        }
        this.binding.title.setText(topicCluster.getCollectionName());
    }
}
