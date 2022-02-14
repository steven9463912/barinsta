package awais.instagrabber.adapters.viewholder;

import android.annotation.SuppressLint;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.ui.StyledPlayerView;

import java.util.List;

import awais.instagrabber.adapters.SliderItemsAdapter;
import awais.instagrabber.customviews.VideoPlayerCallbackAdapter;
import awais.instagrabber.customviews.VideoPlayerViewHelper;
import awais.instagrabber.databinding.LayoutVideoPlayerWithThumbnailBinding;
import awais.instagrabber.fragments.settings.PreferenceKeys;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.MediaCandidate;
import awais.instagrabber.utils.NumberUtils;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.Utils;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class SliderVideoViewHolder extends SliderItemViewHolder {
    private static final String TAG = "SliderVideoViewHolder";

    private final LayoutVideoPlayerWithThumbnailBinding binding;
    private final boolean loadVideoOnItemClick;

    private VideoPlayerViewHelper videoPlayerViewHelper;

    @SuppressLint("ClickableViewAccessibility")
    public SliderVideoViewHolder(@NonNull LayoutVideoPlayerWithThumbnailBinding binding,
                                 boolean loadVideoOnItemClick) {
        super(binding.getRoot());
        this.binding = binding;
        this.loadVideoOnItemClick = loadVideoOnItemClick;
        GestureDetector.OnGestureListener videoPlayerViewGestureListener = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                binding.playerView.performClick();
                return true;
            }
        };
        GestureDetector gestureDetector = new GestureDetector(this.itemView.getContext(), videoPlayerViewGestureListener);
        binding.playerView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });
    }

    public void bind(@NonNull Media media,
                     int position,
                     SliderItemsAdapter.SliderCallback sliderCallback) {
        float vol = settingsHelper.getBoolean(PreferenceKeys.MUTED_VIDEOS) ? 0f : 1f;
        VideoPlayerViewHelper.VideoPlayerCallback videoPlayerCallback = new VideoPlayerCallbackAdapter() {

            @Override
            public void onThumbnailClick() {
                if (sliderCallback != null) {
                    sliderCallback.onItemClicked(position, media, SliderVideoViewHolder.this.binding.getRoot());
                }
            }

            @Override
            public void onThumbnailLoaded() {
                if (sliderCallback != null) {
                    sliderCallback.onThumbnailLoaded(position);
                }
            }

            @Override
            public void onPlayerViewLoaded() {
                // binding.itemFeedBottom.btnMute.setVisibility(View.VISIBLE);
                ViewGroup.LayoutParams layoutParams = SliderVideoViewHolder.this.binding.playerView.getLayoutParams();
                int requiredWidth = Utils.displayMetrics.widthPixels;
                int resultingHeight = NumberUtils.getResultingHeight(requiredWidth, media.getOriginalHeight(), media.getOriginalWidth());
                layoutParams.width = requiredWidth;
                layoutParams.height = resultingHeight;
                SliderVideoViewHolder.this.binding.playerView.requestLayout();
                // setMuteIcon(vol == 0f && Utils.sessionVolumeFull ? 1f : vol);
            }

            @Override
            public void onPlay() {
                if (sliderCallback != null) {
                    sliderCallback.onPlayerPlay(position);
                }
            }

            @Override
            public void onPause() {
                if (sliderCallback != null) {
                    sliderCallback.onPlayerPause(position);
                }
            }

            @Override
            public void onRelease() {
                if (sliderCallback != null) {
                    sliderCallback.onPlayerRelease(position);
                }
            }

            @Override
            public void onFullScreenModeChanged(boolean isFullScreen, StyledPlayerView playerView) {
                if (sliderCallback != null) {
                    sliderCallback.onFullScreenModeChanged(isFullScreen, playerView);
                }
            }

            @Override
            public boolean isInFullScreen() {
                if (sliderCallback != null) {
                    return sliderCallback.isInFullScreen();
                }
                return false;
            }
        };
        float aspectRatio = (float) media.getOriginalWidth() / media.getOriginalHeight();
        String videoUrl = null;
        List<MediaCandidate> videoVersions = media.getVideoVersions();
        if (videoVersions != null && !videoVersions.isEmpty()) {
            MediaCandidate videoVersion = videoVersions.get(0);
            if (videoVersion != null) {
                videoUrl = videoVersion.getUrl();
            }
        }
        if (videoUrl == null) return;
        this.videoPlayerViewHelper = new VideoPlayerViewHelper(this.binding.getRoot().getContext(),
                this.binding,
                                                          videoUrl,
                                                          vol,
                                                          aspectRatio,
                                                          ResponseBodyUtils.getThumbUrl(media),
                this.loadVideoOnItemClick,
                                                          videoPlayerCallback);
        this.binding.playerView.setOnClickListener(v -> {
            if (sliderCallback != null) {
                sliderCallback.onItemClicked(position, media, this.binding.getRoot());
            }
        });
    }

    public void pause() {
        if (this.videoPlayerViewHelper == null) return;
        this.videoPlayerViewHelper.pause();
    }

    public void releasePlayer() {
        if (this.videoPlayerViewHelper == null) return;
        this.videoPlayerViewHelper.releasePlayer();
    }
}
