package awais.instagrabber.adapters;

import android.view.View;

import awais.instagrabber.repositories.responses.Media;


public class FeedItemCallbackAdapter implements FeedAdapterV2.FeedItemCallback {
    @Override
    public void onPostClick(Media media) {}

    @Override
    public void onProfilePicClick(Media media) {}

    @Override
    public void onNameClick(Media media) {}

    @Override
    public void onLocationClick(Media media) {}

    @Override
    public void onMentionClick(String mention) {}

    @Override
    public void onHashtagClick(String hashtag) {}

    @Override
    public void onCommentsClick(Media media) {}

    @Override
    public void onDownloadClick(Media media, int childPosition, View popupLocation) {}

    @Override
    public void onEmailClick(String emailId) {}

    @Override
    public void onURLClick(String url) {}

    @Override
    public void onSliderClick(Media media, int position) {}
}
