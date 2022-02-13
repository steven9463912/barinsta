package awais.instagrabber.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import awais.instagrabber.adapters.viewholder.FeedGridItemViewHolder;
import awais.instagrabber.adapters.viewholder.feed.FeedItemViewHolder;
import awais.instagrabber.adapters.viewholder.feed.FeedPhotoViewHolder;
import awais.instagrabber.adapters.viewholder.feed.FeedSliderViewHolder;
import awais.instagrabber.adapters.viewholder.feed.FeedVideoViewHolder;
import awais.instagrabber.databinding.ItemFeedGridBinding;
import awais.instagrabber.databinding.ItemFeedPhotoBinding;
import awais.instagrabber.databinding.ItemFeedSliderBinding;
import awais.instagrabber.databinding.ItemFeedVideoBinding;
import awais.instagrabber.models.PostsLayoutPreferences;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.repositories.responses.Caption;
import awais.instagrabber.repositories.responses.Media;

public final class FeedAdapterV2 extends ListAdapter<Media, RecyclerView.ViewHolder> {
    private static final String TAG = "FeedAdapterV2";

    private final FeedItemCallback feedItemCallback;
    private final SelectionModeCallback selectionModeCallback;
    private final Set<Integer> selectedPositions = new HashSet<>();
    private final Set<Media> selectedFeedModels = new HashSet<>();

    private PostsLayoutPreferences layoutPreferences;
    private boolean selectionModeActive;


    private static final DiffUtil.ItemCallback<Media> DIFF_CALLBACK = new DiffUtil.ItemCallback<Media>() {
        @Override
        public boolean areItemsTheSame(@NonNull Media oldItem, @NonNull Media newItem) {
            return Objects.equals(oldItem.getPk(), newItem.getPk());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Media oldItem, @NonNull Media newItem) {
            Caption oldItemCaption = oldItem.getCaption();
            Caption newItemCaption = newItem.getCaption();
            return Objects.equals(oldItem.getPk(), newItem.getPk())
                    && Objects.equals(this.getCaptionText(oldItemCaption), this.getCaptionText(newItemCaption));
        }

        private String getCaptionText(Caption caption) {
            if (caption == null) return null;
            return caption.getText();
        }
    };
    private final AdapterSelectionCallback adapterSelectionCallback = new AdapterSelectionCallback() {
        @Override
        public boolean onPostLongClick(int position, Media feedModel) {
            if (!FeedAdapterV2.this.selectionModeActive) {
                FeedAdapterV2.this.selectionModeActive = true;
                FeedAdapterV2.this.notifyDataSetChanged();
                if (FeedAdapterV2.this.selectionModeCallback != null) {
                    FeedAdapterV2.this.selectionModeCallback.onSelectionStart();
                }
            }
            FeedAdapterV2.this.selectedPositions.add(position);
            FeedAdapterV2.this.selectedFeedModels.add(feedModel);
            FeedAdapterV2.this.notifyItemChanged(position);
            if (FeedAdapterV2.this.selectionModeCallback != null) {
                FeedAdapterV2.this.selectionModeCallback.onSelectionChange(FeedAdapterV2.this.selectedFeedModels);
            }
            return true;
        }

        @Override
        public void onPostClick(int position, Media feedModel) {
            if (!FeedAdapterV2.this.selectionModeActive) return;
            if (FeedAdapterV2.this.selectedPositions.contains(position)) {
                FeedAdapterV2.this.selectedPositions.remove(position);
                FeedAdapterV2.this.selectedFeedModels.remove(feedModel);
            } else {
                FeedAdapterV2.this.selectedPositions.add(position);
                FeedAdapterV2.this.selectedFeedModels.add(feedModel);
            }
            FeedAdapterV2.this.notifyItemChanged(position);
            if (FeedAdapterV2.this.selectionModeCallback != null) {
                FeedAdapterV2.this.selectionModeCallback.onSelectionChange(FeedAdapterV2.this.selectedFeedModels);
            }
            if (FeedAdapterV2.this.selectedPositions.isEmpty()) {
                FeedAdapterV2.this.selectionModeActive = false;
                FeedAdapterV2.this.notifyDataSetChanged();
                if (FeedAdapterV2.this.selectionModeCallback != null) {
                    FeedAdapterV2.this.selectionModeCallback.onSelectionEnd();
                }
            }
        }
    };

    public FeedAdapterV2(@NonNull PostsLayoutPreferences layoutPreferences,
                         FeedItemCallback feedItemCallback,
                         SelectionModeCallback selectionModeCallback) {
        super(FeedAdapterV2.DIFF_CALLBACK);
        this.layoutPreferences = layoutPreferences;
        this.feedItemCallback = feedItemCallback;
        this.selectionModeCallback = selectionModeCallback;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        switch (this.layoutPreferences.getType()) {
            case LINEAR:
                return this.getLinearViewHolder(parent, layoutInflater, viewType);
            case GRID:
            case STAGGERED_GRID:
            default:
                ItemFeedGridBinding binding = ItemFeedGridBinding.inflate(layoutInflater, parent, false);
                return new FeedGridItemViewHolder(binding);
        }
    }

    @NonNull
    private RecyclerView.ViewHolder getLinearViewHolder(@NonNull ViewGroup parent,
                                                        LayoutInflater layoutInflater,
                                                        int viewType) {
        switch (MediaItemType.valueOf(viewType)) {
            case MEDIA_TYPE_VIDEO: {
                ItemFeedVideoBinding binding = ItemFeedVideoBinding.inflate(layoutInflater, parent, false);
                return new FeedVideoViewHolder(binding, this.feedItemCallback);
            }
            case MEDIA_TYPE_SLIDER: {
                ItemFeedSliderBinding binding = ItemFeedSliderBinding.inflate(layoutInflater, parent, false);
                return new FeedSliderViewHolder(binding, this.feedItemCallback);
            }
            case MEDIA_TYPE_IMAGE:
            default: {
                ItemFeedPhotoBinding binding = ItemFeedPhotoBinding.inflate(layoutInflater, parent, false);
                return new FeedPhotoViewHolder(binding, this.feedItemCallback);
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        Media feedModel = this.getItem(position);
        if (feedModel == null) return;
        switch (this.layoutPreferences.getType()) {
            case LINEAR:
                ((FeedItemViewHolder) viewHolder).bind(feedModel);
                break;
            case GRID:
            case STAGGERED_GRID:
            default:
                ((FeedGridItemViewHolder) viewHolder).bind(position,
                                                           feedModel,
                        this.layoutPreferences,
                        this.feedItemCallback,
                        this.adapterSelectionCallback,
                        this.selectionModeActive,
                        this.selectedPositions.contains(position));
        }
    }

    @Override
    public int getItemViewType(int position) {
        return this.getItem(position).getType().getId();
    }

    public void setLayoutPreferences(@NonNull PostsLayoutPreferences layoutPreferences) {
        this.layoutPreferences = layoutPreferences;
    }

    public void endSelection() {
        if (!this.selectionModeActive) return;
        this.selectionModeActive = false;
        this.selectedPositions.clear();
        this.selectedFeedModels.clear();
        this.notifyDataSetChanged();
        if (this.selectionModeCallback != null) {
            this.selectionModeCallback.onSelectionEnd();
        }
    }

    // @Override
    // public void onViewAttachedToWindow(@NonNull final FeedItemViewHolder holder) {
    //     super.onViewAttachedToWindow(holder);
    //     // Log.d(TAG, "attached holder: " + holder);
    //     if (!(holder instanceof FeedSliderViewHolder)) return;
    //     final FeedSliderViewHolder feedSliderViewHolder = (FeedSliderViewHolder) holder;
    //     feedSliderViewHolder.startPlayingVideo();
    // }
    //
    // @Override
    // public void onViewDetachedFromWindow(@NonNull final FeedItemViewHolder holder) {
    //     super.onViewDetachedFromWindow(holder);
    //     // Log.d(TAG, "detached holder: " + holder);
    //     if (!(holder instanceof FeedSliderViewHolder)) return;
    //     final FeedSliderViewHolder feedSliderViewHolder = (FeedSliderViewHolder) holder;
    //     feedSliderViewHolder.stopPlayingVideo();
    // }

    public interface FeedItemCallback {
        void onPostClick(Media feedModel);

        void onProfilePicClick(Media feedModel);

        void onNameClick(Media feedModel);

        void onLocationClick(Media feedModel);

        void onMentionClick(String mention);

        void onHashtagClick(String hashtag);

        void onCommentsClick(Media feedModel);

        void onDownloadClick(Media feedModel, int childPosition, View popupLocation);

        void onEmailClick(String emailId);

        void onURLClick(String url);

        void onSliderClick(Media feedModel, int position);
    }

    public interface AdapterSelectionCallback {
        boolean onPostLongClick(int position, Media feedModel);

        void onPostClick(int position, Media feedModel);
    }

    public interface SelectionModeCallback {
        void onSelectionStart();

        void onSelectionChange(Set<Media> selectedFeedModels);

        void onSelectionEnd();
    }
}