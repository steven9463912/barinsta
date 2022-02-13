package awais.instagrabber.adapters.viewholder.directmessages;

import android.os.Handler;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Floats;

import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.adapters.DirectItemsAdapter;
import awais.instagrabber.customviews.DirectItemContextMenu;
import awais.instagrabber.databinding.LayoutDmBaseBinding;
import awais.instagrabber.databinding.LayoutDmVoiceMediaBinding;
import awais.instagrabber.repositories.responses.Audio;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectItemVoiceMedia;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.utils.TextUtils;

import static com.google.android.exoplayer2.C.TIME_UNSET;

public class DirectItemVoiceMediaViewHolder extends DirectItemViewHolder {
    private static final String TAG = "DirectItemVoiceMediaVH";

    private final LayoutDmVoiceMediaBinding binding;
    private final DefaultDataSourceFactory dataSourceFactory;
    private SimpleExoPlayer player;
    private Handler handler;
    private Runnable positionChecker;
    private Player.EventListener listener;

    public DirectItemVoiceMediaViewHolder(@NonNull LayoutDmBaseBinding baseBinding,
                                          @NonNull LayoutDmVoiceMediaBinding binding,
                                          User currentUser,
                                          DirectThread thread,
                                          DirectItemsAdapter.DirectItemCallback callback) {
        super(baseBinding, currentUser, thread, callback);
        this.binding = binding;
        dataSourceFactory = new DefaultDataSourceFactory(binding.getRoot().getContext(), "instagram");
        this.setItemView(binding.getRoot());
        binding.voiceMedia.getLayoutParams().width = this.mediaImageMaxWidth;
    }

    @Override
    public void bindItem(DirectItem directItemModel, MessageDirection messageDirection) {
        DirectItemVoiceMedia voiceMedia = directItemModel.getVoiceMedia();
        if (voiceMedia == null) return;
        Media media = voiceMedia.getMedia();
        if (media == null) return;
        Audio audio = media.getAudio();
        if (audio == null) return;
        List<Float> waveformData = audio.getWaveformData();
        this.binding.waveformSeekBar.setSample(Floats.toArray(waveformData));
        this.binding.waveformSeekBar.setEnabled(false);
        String text = String.format("%s/%s", TextUtils.millisToTimeString(0), TextUtils.millisToTimeString(audio.getDuration()));
        this.binding.duration.setText(text);
        AudioItemState audioItemState = new AudioItemState();
        this.player = new SimpleExoPlayer.Builder(this.itemView.getContext()).build();
        this.player.setVolume(1);
        this.player.setPlayWhenReady(true);
        this.player.setRepeatMode(Player.REPEAT_MODE_OFF);
        this.handler = new Handler();
        final long initialDelay = 0;
        final long recurringDelay = 60;
        this.positionChecker = new Runnable() {
            @Override
            public void run() {
                if (DirectItemVoiceMediaViewHolder.this.handler != null) {
                    DirectItemVoiceMediaViewHolder.this.handler.removeCallbacks(this);
                }
                if (DirectItemVoiceMediaViewHolder.this.player == null) return;
                long currentPosition = DirectItemVoiceMediaViewHolder.this.player.getCurrentPosition();
                long duration = DirectItemVoiceMediaViewHolder.this.player.getDuration();
                // Log.d(TAG, "currentPosition: " + currentPosition + ", duration: " + duration);
                if (duration == TIME_UNSET) return;
                // final float progress = ((float) currentPosition / duration /* * 100 */);
                int progress = (int) ((float) currentPosition / duration * 100);
                // Log.d(TAG, "progress: " + progress);
                String text = String.format("%s/%s", TextUtils.millisToTimeString(currentPosition), TextUtils.millisToTimeString(duration));
                DirectItemVoiceMediaViewHolder.this.binding.duration.setText(text);
                DirectItemVoiceMediaViewHolder.this.binding.waveformSeekBar.setProgress(progress);
                if (DirectItemVoiceMediaViewHolder.this.handler != null) {
                    DirectItemVoiceMediaViewHolder.this.handler.postDelayed(this, recurringDelay);
                }
            }
        };
        this.player.addListener(this.listener = new Player.EventListener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (!audioItemState.isPrepared() && state == Player.STATE_READY) {
                    DirectItemVoiceMediaViewHolder.this.binding.playPause.setIconResource(R.drawable.ic_round_pause_24);
                    audioItemState.setPrepared(true);
                    DirectItemVoiceMediaViewHolder.this.binding.playPause.setVisibility(View.VISIBLE);
                    DirectItemVoiceMediaViewHolder.this.binding.progressBar.setVisibility(View.GONE);
                    if (DirectItemVoiceMediaViewHolder.this.handler != null) {
                        DirectItemVoiceMediaViewHolder.this.handler.postDelayed(DirectItemVoiceMediaViewHolder.this.positionChecker, initialDelay);
                    }
                    return;
                }
                if (state == Player.STATE_ENDED) {
                    // binding.waveformSeekBar.setProgressInPercentage(0);
                    DirectItemVoiceMediaViewHolder.this.binding.waveformSeekBar.setProgress(0);
                    DirectItemVoiceMediaViewHolder.this.binding.playPause.setIconResource(R.drawable.ic_round_play_arrow_24);
                    if (DirectItemVoiceMediaViewHolder.this.handler != null) {
                        DirectItemVoiceMediaViewHolder.this.handler.removeCallbacks(DirectItemVoiceMediaViewHolder.this.positionChecker);
                    }
                }
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                Log.e(DirectItemVoiceMediaViewHolder.TAG, "onPlayerError: ", error);
            }
        });
        ProgressiveMediaSource.Factory sourceFactory = new ProgressiveMediaSource.Factory(this.dataSourceFactory);
        MediaItem mediaItem = MediaItem.fromUri(audio.getAudioSrc());
        ProgressiveMediaSource mediaSource = sourceFactory.createMediaSource(mediaItem);
        this.player.setMediaSource(mediaSource);
        this.binding.playPause.setOnClickListener(v -> {
            if (this.player == null) return;
            if (!audioItemState.isPrepared()) {
                this.player.prepare();
                this.binding.playPause.setVisibility(View.GONE);
                this.binding.progressBar.setVisibility(View.VISIBLE);
                return;
            }
            if (this.player.isPlaying()) {
                this.binding.playPause.setIconResource(R.drawable.ic_round_play_arrow_24);
                this.player.pause();
                return;
            }
            this.binding.playPause.setIconResource(R.drawable.ic_round_pause_24);
            if (this.player.getPlaybackState() == Player.STATE_ENDED) {
                this.player.seekTo(0);
                if (this.handler != null) {
                    this.handler.postDelayed(this.positionChecker, initialDelay);
                }
            }
            this.binding.waveformSeekBar.setEnabled(true);
            this.player.play();
        });
    }

    @Override
    public void cleanup() {
        super.cleanup();
        if (this.handler != null && this.positionChecker != null) {
            this.handler.removeCallbacks(this.positionChecker);
            this.handler = null;
            this.positionChecker = null;
        }
        if (this.player != null) {
            this.player.release();
            if (this.listener != null) {
                this.player.removeListener(this.listener);
            }
            this.player = null;
        }
    }

    @Override
    protected boolean canForward() {
        return false;
    }

    @Override
    protected List<DirectItemContextMenu.MenuItem> getLongClickOptions() {
        return ImmutableList.of(
                new DirectItemContextMenu.MenuItem(R.id.download, R.string.action_download)
        );
    }

    private static class AudioItemState {
        private boolean prepared;

        private AudioItemState() {}

        public boolean isPrepared() {
            return this.prepared;
        }

        public void setPrepared(boolean prepared) {
            this.prepared = prepared;
        }
    }
}
