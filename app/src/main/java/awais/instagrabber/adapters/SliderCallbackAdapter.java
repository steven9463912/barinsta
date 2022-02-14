package awais.instagrabber.adapters;

import android.view.View;

import com.google.android.exoplayer2.ui.StyledPlayerView;

import awais.instagrabber.repositories.responses.Media;

public class SliderCallbackAdapter implements SliderItemsAdapter.SliderCallback {
    @Override
    public void onThumbnailLoaded(int position) {}

    @Override
    public void onItemClicked(int position, Media media, View view) {}

    @Override
    public void onPlayerPlay(int position) {}

    @Override
    public void onPlayerPause(int position) {}

    @Override
    public void onPlayerRelease(int position) {}

    @Override
    public void onFullScreenModeChanged(boolean isFullScreen, StyledPlayerView playerView) {}

    @Override
    public boolean isInFullScreen() {
        return false;
    }
}
