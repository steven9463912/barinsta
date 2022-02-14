package awais.instagrabber.customviews;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioListener;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.StyledPlayerControlView;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import awais.instagrabber.R;
import awais.instagrabber.databinding.LayoutVideoPlayerWithThumbnailBinding;
import awais.instagrabber.utils.Utils;

public class VideoPlayerViewHelper implements Player.EventListener {
    private static final String TAG = VideoPlayerViewHelper.class.getSimpleName();

    private final Context context;
    private final LayoutVideoPlayerWithThumbnailBinding binding;
    private final float initialVolume;
    private final float thumbnailAspectRatio;
    private final String thumbnailUrl;
    private final boolean loadPlayerOnClick;
    private final VideoPlayerCallback videoPlayerCallback;
    private final String videoUrl;
    private final DefaultDataSourceFactory dataSourceFactory;
    private SimpleExoPlayer player;
    private AppCompatImageButton mute;

    private final AudioListener audioListener = new AudioListener() {
        @Override
        public void onVolumeChanged(float volume) {
            VideoPlayerViewHelper.this.updateMuteIcon(volume);
        }
    };
    private final View.OnClickListener muteOnClickListener = v -> this.toggleMute();
    private Object layoutManager;

    public VideoPlayerViewHelper(@NonNull Context context,
                                 @NonNull LayoutVideoPlayerWithThumbnailBinding binding,
                                 @NonNull String videoUrl,
                                 float initialVolume,
                                 float thumbnailAspectRatio,
                                 String thumbnailUrl,
                                 boolean loadPlayerOnClick,
                                 VideoPlayerCallback videoPlayerCallback) {
        this.context = context;
        this.binding = binding;
        this.initialVolume = initialVolume;
        this.thumbnailAspectRatio = thumbnailAspectRatio;
        this.thumbnailUrl = thumbnailUrl;
        this.loadPlayerOnClick = loadPlayerOnClick;
        this.videoPlayerCallback = videoPlayerCallback;
        this.videoUrl = videoUrl;
        dataSourceFactory = new DefaultDataSourceFactory(binding.getRoot().getContext(), "instagram");
        this.bind();
    }

    private void bind() {
        this.binding.thumbnailParent.setOnClickListener(v -> {
            if (this.videoPlayerCallback != null) {
                this.videoPlayerCallback.onThumbnailClick();
            }
            if (this.loadPlayerOnClick) {
                this.loadPlayer();
            }
        });
        this.setThumbnail();
    }

    private void setThumbnail() {
        this.binding.thumbnail.setAspectRatio(this.thumbnailAspectRatio);
        ImageRequest thumbnailRequest = null;
        if (this.thumbnailUrl != null) {
            thumbnailRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse(this.thumbnailUrl)).build();
        }
        PipelineDraweeControllerBuilder builder = Fresco
                .newDraweeControllerBuilder()
                .setControllerListener(new BaseControllerListener<ImageInfo>() {
                    @Override
                    public void onFailure(String id, Throwable throwable) {
                        if (VideoPlayerViewHelper.this.videoPlayerCallback != null) {
                            VideoPlayerViewHelper.this.videoPlayerCallback.onThumbnailLoaded();
                        }
                    }

                    @Override
                    public void onFinalImageSet(String id,
                                                ImageInfo imageInfo,
                                                Animatable animatable) {
                        if (VideoPlayerViewHelper.this.videoPlayerCallback != null) {
                            VideoPlayerViewHelper.this.videoPlayerCallback.onThumbnailLoaded();
                        }
                    }
                });
        if (thumbnailRequest != null) {
            builder.setImageRequest(thumbnailRequest);
        }
        this.binding.thumbnail.setController(builder.build());
    }

    private void loadPlayer() {
        if (this.videoUrl == null) return;
        if (this.binding.getRoot().getDisplayedChild() == 0) {
            this.binding.getRoot().showNext();
        }
        if (this.videoPlayerCallback != null) {
            this.videoPlayerCallback.onPlayerViewLoaded();
        }
        this.player = (SimpleExoPlayer) this.binding.playerView.getPlayer();
        if (this.player != null) {
            this.player.release();
        }
        ViewGroup.LayoutParams playerViewLayoutParams = this.binding.playerView.getLayoutParams();
        if (playerViewLayoutParams.height > Utils.displayMetrics.heightPixels * 0.8) {
            playerViewLayoutParams.height = (int) (Utils.displayMetrics.heightPixels * 0.8);
        }
        this.player = new SimpleExoPlayer.Builder(this.context)
                .setLooper(Looper.getMainLooper())
                .build();
        this.player.addListener(this);
        this.player.addAudioListener(this.audioListener);
        this.player.setVolume(this.initialVolume);
        this.player.setPlayWhenReady(true);
        this.player.setRepeatMode(Player.REPEAT_MODE_ALL);
        ProgressiveMediaSource.Factory sourceFactory = new ProgressiveMediaSource.Factory(this.dataSourceFactory);
        MediaItem mediaItem = MediaItem.fromUri(this.videoUrl);
        ProgressiveMediaSource mediaSource = sourceFactory.createMediaSource(mediaItem);
        this.player.setMediaSource(mediaSource);
        this.player.prepare();
        this.binding.playerView.setPlayer(this.player);
        this.binding.playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        this.binding.playerView.setShowNextButton(false);
        this.binding.playerView.setShowPreviousButton(false);
        this.binding.playerView.setControllerOnFullScreenModeChangedListener(isFullScreen -> {
            if (this.videoPlayerCallback == null) return;
            this.videoPlayerCallback.onFullScreenModeChanged(isFullScreen, this.binding.playerView);
        });
        this.setupControllerView();
    }

    private void setupControllerView() {
        try {
            StyledPlayerControlView controllerView = this.getStyledPlayerControlView();
            if (controllerView == null) return;
            this.layoutManager = this.setControlViewLayoutManager(controllerView);
            if (this.videoPlayerCallback != null && this.videoPlayerCallback.isInFullScreen()) {
                this.setControllerViewToFullScreenMode(controllerView);
            }
            ViewGroup exoBasicControls = controllerView.findViewById(R.id.exo_basic_controls);
            if (exoBasicControls == null) return;
            this.mute = new AppCompatImageButton(this.context);
            Resources resources = this.context.getResources();
            if (resources == null) return;
            int width = resources.getDimensionPixelSize(R.dimen.exo_small_icon_width);
            int height = resources.getDimensionPixelSize(R.dimen.exo_small_icon_height);
            int margin = resources.getDimensionPixelSize(R.dimen.exo_small_icon_horizontal_margin);
            int paddingHorizontal = resources.getDimensionPixelSize(R.dimen.exo_small_icon_padding_horizontal);
            int paddingVertical = resources.getDimensionPixelSize(R.dimen.exo_small_icon_padding_vertical);
            ViewGroup.MarginLayoutParams layoutParams = new ViewGroup.MarginLayoutParams(width, height);
            layoutParams.setMargins(margin, 0, margin, 0);
            this.mute.setLayoutParams(layoutParams);
            this.mute.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
            this.mute.setScaleType(ImageView.ScaleType.FIT_XY);
            this.mute.setBackgroundResource(Utils.getAttrResId(this.context, android.R.attr.selectableItemBackground));
            this.mute.setImageTintList(ColorStateList.valueOf(resources.getColor(R.color.white)));
            this.updateMuteIcon(this.player.getVolume());
            exoBasicControls.addView(this.mute, 0);
            this.mute.setOnClickListener(this.muteOnClickListener);
        } catch (final Exception e) {
            Log.e(VideoPlayerViewHelper.TAG, "loadPlayer: ", e);
        }
    }

    @Nullable
    private Object setControlViewLayoutManager(@NonNull StyledPlayerControlView controllerView)
            throws NoSuchFieldException, IllegalAccessException {
        Field controlViewLayoutManagerField = controllerView.getClass().getDeclaredField("controlViewLayoutManager");
        controlViewLayoutManagerField.setAccessible(true);
        return controlViewLayoutManagerField.get(controllerView);
    }

    private void setControllerViewToFullScreenMode(@NonNull StyledPlayerControlView controllerView)
            throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        // Exoplayer doesn't expose the fullscreen state, so using reflection
        Field fullScreenButtonField = controllerView.getClass().getDeclaredField("fullScreenButton");
        fullScreenButtonField.setAccessible(true);
        ImageView fullScreenButton = (ImageView) fullScreenButtonField.get(controllerView);
        Field isFullScreen = controllerView.getClass().getDeclaredField("isFullScreen");
        isFullScreen.setAccessible(true);
        isFullScreen.set(controllerView, true);
        Method updateFullScreenButtonForState = controllerView
                .getClass()
                .getDeclaredMethod("updateFullScreenButtonForState", ImageView.class, boolean.class);
        updateFullScreenButtonForState.setAccessible(true);
        updateFullScreenButtonForState.invoke(controllerView, fullScreenButton, true);

    }

    @Nullable
    private StyledPlayerControlView getStyledPlayerControlView() throws NoSuchFieldException, IllegalAccessException {
        Field controller = this.binding.playerView.getClass().getDeclaredField("controller");
        controller.setAccessible(true);
        return (StyledPlayerControlView) controller.get(this.binding.playerView);
    }

    @Override
    public void onTracksChanged(@NonNull final TrackGroupArray trackGroups, @NonNull final TrackSelectionArray trackSelections) {
        if (trackGroups.isEmpty()) {
            this.setHasAudio(false);
            return;
        }
        boolean hasAudio = false;
        for (int i = 0; i < trackGroups.length; i++) {
            for (int g = 0; g < trackGroups.get(i).length; g++) {
                String sampleMimeType = trackGroups.get(i).getFormat(g).sampleMimeType;
                if (sampleMimeType != null && sampleMimeType.contains("audio")) {
                    hasAudio = true;
                    break;
                }
            }
        }
        this.setHasAudio(hasAudio);
    }

    private void setHasAudio(boolean hasAudio) {
        if (this.mute == null) return;
        this.mute.setEnabled(hasAudio);
        this.mute.setAlpha(hasAudio ? 1f : 0.5f);
        this.updateMuteIcon(hasAudio ? 1f : 0f);
    }

    private void updateMuteIcon(float volume) {
        if (this.mute == null) return;
        if (volume == 0) {
            this.mute.setImageResource(R.drawable.ic_volume_off_24);
            return;
        }
        this.mute.setImageResource(R.drawable.ic_volume_up_24);
    }

    @Override
    public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
        if (this.videoPlayerCallback == null) return;
        if (playWhenReady) {
            this.videoPlayerCallback.onPlay();
            return;
        }
        this.videoPlayerCallback.onPause();
    }

    @Override
    public void onPlayerError(@NonNull ExoPlaybackException error) {
        Log.e(VideoPlayerViewHelper.TAG, "onPlayerError", error);
    }

    private void toggleMute() {
        if (this.player == null) return;
        if (this.layoutManager != null) {
            try {
                Method resetHideCallbacks = this.layoutManager.getClass().getDeclaredMethod("resetHideCallbacks");
                resetHideCallbacks.invoke(this.layoutManager);
            } catch (final Exception e) {
                Log.e(VideoPlayerViewHelper.TAG, "toggleMute: ", e);
            }
        }
        float vol = this.player.getVolume() == 0f ? 1f : 0f;
        this.player.setVolume(vol);
    }

    public void releasePlayer() {
        if (this.videoPlayerCallback != null) {
            this.videoPlayerCallback.onRelease();
        }
        if (this.player != null) {
            this.player.release();
            this.player = null;
        }
    }

    public void pause() {
        if (this.player != null) {
            this.player.pause();
        }
    }

    public interface VideoPlayerCallback {
        void onThumbnailLoaded();

        void onThumbnailClick();

        void onPlayerViewLoaded();

        void onPlay();

        void onPause();

        void onRelease();

        void onFullScreenModeChanged(boolean isFullScreen, StyledPlayerView playerView);

        boolean isInFullScreen();
    }
}
