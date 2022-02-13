package awais.instagrabber.utils;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.repositories.responses.Caption;
import awais.instagrabber.repositories.responses.FriendshipStatus;
import awais.instagrabber.repositories.responses.ImageVersions2;
import awais.instagrabber.repositories.responses.Location;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.MediaCandidate;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.stories.StoryMedia;

public final class ResponseBodyUtils {
    private static final String TAG = "ResponseBodyUtils";

    // isI: true if the content was requested from i.instagram.com instead of graphql
    @Nullable
    public static String getHighQualityPost(JSONArray resources, boolean isVideo, boolean isI, boolean low) {
        try {
            int resourcesLen = resources.length();

            String[] sources = new String[resourcesLen];
            int lastResMain = low ? 1000000 : 0, lastIndexMain = -1;
            int lastResBase = low ? 1000000 : 0, lastIndexBase = -1;
            for (int i = 0; i < resourcesLen; ++i) {
                JSONObject item = resources.getJSONObject(i);
                if (item != null && (!isVideo || item.has(Constants.EXTRAS_PROFILE) || isI)) {
                    sources[i] = item.getString(isI ? "url" : "src");
                    int currRes = item.getInt(isI ? "width" : "config_width") * item.getInt(isI ? "height" : "config_height");

                    String profile = isVideo ? item.optString(Constants.EXTRAS_PROFILE) : null;

                    if (!isVideo || "MAIN".equals(profile)) {
                        if (currRes > lastResMain && !low) {
                            lastResMain = currRes;
                            lastIndexMain = i;
                        } else if (currRes < lastResMain && low) {
                            lastResMain = currRes;
                            lastIndexMain = i;
                        }
                    } else {
                        if (currRes > lastResBase && !low) {
                            lastResBase = currRes;
                            lastIndexBase = i;
                        } else if (currRes < lastResBase && low) {
                            lastResBase = currRes;
                            lastIndexBase = i;
                        }
                    }
                }
            }

            if (lastIndexMain >= 0) return sources[lastIndexMain];
            else if (lastIndexBase >= 0) return sources[lastIndexBase];
        } catch (Exception e) {
            Log.e(ResponseBodyUtils.TAG, "", e);
        }
        return null;
    }

    public static String getHighQualityImage(JSONObject resources) {
        String src = null;
        try {
            if (resources.has("display_resources"))
                src = ResponseBodyUtils.getHighQualityPost(resources.getJSONArray("display_resources"), false, false, false);
            else if (resources.has("image_versions2"))
                src = ResponseBodyUtils.getHighQualityPost(resources.getJSONObject("image_versions2").getJSONArray("candidates"), false, true, false);
            if (src == null) return resources.getString("display_url");
        } catch (Exception e) {
            Log.e(ResponseBodyUtils.TAG, "", e);
        }
        return src;
    }

    // the "user" argument can be null, it's used because instagram redacts user details from responses
    public static Media parseGraphQLItem(JSONObject itemJson, User backup) throws JSONException {
        if (itemJson == null) {
            return null;
        }
        JSONObject feedItem = itemJson.has("node") ? itemJson.getJSONObject("node") : itemJson;
        String mediaType = feedItem.optString("__typename");
        if ("GraphSuggestedUserFeedUnit".equals(mediaType)) return null;

        boolean isVideo = feedItem.optBoolean("is_video");
        long videoViews = feedItem.optLong("video_view_count", 0);

        String displayUrl = feedItem.optString("display_url");
        if (TextUtils.isEmpty(displayUrl)) return null;
        String resourceUrl;
        if (isVideo && feedItem.has("video_url")) {
            resourceUrl = feedItem.getString("video_url");
        } else {
            resourceUrl = feedItem.has("display_resources") ? getHighQualityImage(feedItem) : displayUrl;
        }
        JSONObject tempJsonObject = feedItem.optJSONObject("edge_media_preview_comment");
        long commentsCount = tempJsonObject != null ? tempJsonObject.optLong("count") : 0;
        tempJsonObject = feedItem.optJSONObject("edge_media_preview_like");
        long likesCount = tempJsonObject != null ? tempJsonObject.optLong("count") : 0;
        tempJsonObject = feedItem.optJSONObject("edge_media_to_caption");
        JSONArray captions = tempJsonObject != null ? tempJsonObject.getJSONArray("edges") : null;
        String captionText = null;
        if (captions != null && captions.length() > 0) {
            if ((tempJsonObject = captions.optJSONObject(0)) != null &&
                    (tempJsonObject = tempJsonObject.optJSONObject("node")) != null) {
                captionText = tempJsonObject.getString("text");
            }
        }
        JSONObject locationJson = feedItem.optJSONObject("location");
        // Log.d(TAG, "location: " + (location == null ? null : location.toString()));
        long locationId = 0;
        String locationName = null;
        if (locationJson != null) {
            locationName = locationJson.optString("name");
            if (locationJson.has("id")) {
                locationId = locationJson.optLong("id");
            } else if (locationJson.has("pk")) {
                locationId = locationJson.optLong("pk");
            }
            // Log.d(TAG, "locationId: " + locationId);
        }
        int height = 0;
        int width = 0;
        JSONObject dimensions = feedItem.optJSONObject("dimensions");
        if (dimensions != null) {
            height = dimensions.optInt("height");
            width = dimensions.optInt("width");
        }
        final String thumbnailUrl = null;
        List<MediaCandidate> candidates = new ArrayList<MediaCandidate>();
        if (feedItem.has("display_resources") || feedItem.has("thumbnail_resources")) {
            JSONArray displayResources = feedItem.has("display_resources")
                                               ? feedItem.getJSONArray("display_resources")
                                               : feedItem.getJSONArray("thumbnail_resources");
            for (int i = 0; i < displayResources.length(); i++) {
                JSONObject displayResource = displayResources.getJSONObject(i);
                candidates.add(new MediaCandidate(
                        displayResource.getInt("config_width"),
                        displayResource.getInt("config_height"),
                        displayResource.getString("src")
                ));
            }
        }
        ImageVersions2 imageVersions2 = new ImageVersions2(candidates);

        User user = backup;
        long userId = -1;
        if (feedItem.has("owner") && user == null) {
            JSONObject owner = feedItem.getJSONObject("owner");
            FriendshipStatus friendshipStatus = new FriendshipStatus(
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false
            );
            userId = owner.optLong(Constants.EXTRAS_ID, -1);
            user = new User(
                    userId,
                    owner.optString(Constants.EXTRAS_USERNAME),
                    owner.optString("full_name"),
                    false,
                    owner.optString("profile_pic_url"),
                    owner.optBoolean("is_verified"));
        }
        String id = feedItem.getString(Constants.EXTRAS_ID);
        MediaCandidate videoVersion = null;
        if (isVideo) {
            videoVersion = new MediaCandidate(
                    width,
                    height,
                    resourceUrl
            );
        }
        Caption caption = new Caption(
                userId,
                captionText != null ? captionText : ""
        );

        boolean isSlider = "GraphSidecar".equals(mediaType) && feedItem.has("edge_sidecar_to_children");
        List<Media> childItems = null;
        if (isSlider) {
            childItems = new ArrayList<>();
            // feedModelBuilder.setItemType(MediaItemType.MEDIA_TYPE_SLIDER);
            JSONObject sidecar = feedItem.optJSONObject("edge_sidecar_to_children");
            if (sidecar != null) {
                JSONArray children = sidecar.optJSONArray("edges");
                if (children != null) {
                    // final List<PostChild> sliderItems = getSliderItems(children);
                    // feedModelBuilder.setSliderItems(sliderItems)
                    //                 .setImageHeight(sliderItems.get(0).getHeight())
                    //                 .setImageWidth(sliderItems.get(0).getWidth());
                    for (int i = 0; i < children.length(); i++) {
                        JSONObject child = children.optJSONObject(i);
                        if (child == null) continue;
                        Media media = ResponseBodyUtils.parseGraphQLItem(child, null);
                        media.setSidecarChild(true);
                        childItems.add(media);
                    }
                }
            }
        }
        MediaItemType mediaItemType = MediaItemType.MEDIA_TYPE_IMAGE;
        if (isSlider) {
            mediaItemType = MediaItemType.MEDIA_TYPE_SLIDER;
        } else if (isVideo) {
            mediaItemType = MediaItemType.MEDIA_TYPE_VIDEO;
        }
        Location location = new Location(
                locationId,
                locationName,
                locationName,
                null,
                null,
                -1,
                -1
        );
        return new Media(
                id,
                id,
                feedItem.optString(Constants.EXTRAS_SHORTCODE),
                feedItem.optLong("taken_at_timestamp", -1),
                user,
                false,
                imageVersions2,
                width,
                height,
                mediaItemType.getId(),
                false,
                feedItem.optBoolean("comments_disabled"),
                -1,
                commentsCount,
                likesCount,
                false,
                false,
                isVideo ? Collections.singletonList(videoVersion) : null,
                feedItem.optBoolean("has_audio"),
                feedItem.optDouble("video_duration"),
                videoViews,
                caption,
                false,
                null,
                null,
                childItems,
                location,
                null,
                false,
                false,
                null,
                null,
                null
        );
    }

    public static String getThumbUrl(Object media) {
        return ResponseBodyUtils.getImageCandidate(media, CandidateType.THUMBNAIL);
    }

    public static String getImageUrl(Object media) {
        return ResponseBodyUtils.getImageCandidate(media, CandidateType.DOWNLOAD);
    }

    private static String getImageCandidate(Object rawMedia, CandidateType type) {
        if (rawMedia == null) return null;
        ImageVersions2 imageVersions2;
        int originalWidth, originalHeight;
        if (rawMedia instanceof StoryMedia) {
            imageVersions2 = ((StoryMedia) rawMedia).getImageVersions2();
            originalWidth = ((StoryMedia) rawMedia).getOriginalWidth();
            originalHeight = ((StoryMedia) rawMedia).getOriginalHeight();
        }
        else if (rawMedia instanceof Media) {
            imageVersions2 = ((Media) rawMedia).getImageVersions2();
            originalWidth = ((Media) rawMedia).getOriginalWidth();
            originalHeight = ((Media) rawMedia).getOriginalHeight();
        }
        else return null;
        if (imageVersions2 == null) return null;
        List<MediaCandidate> candidates = imageVersions2.getCandidates();
        if (candidates == null || candidates.isEmpty()) return null;
        boolean isSquare = Integer.compare(originalWidth, originalHeight) == 0;
        List<MediaCandidate> sortedCandidates = candidates.stream()
                                                                .sorted((c1, c2) -> Integer.compare(c2.getWidth(), c1.getWidth()))
                                                                .collect(Collectors.toList());
        List<MediaCandidate> filteredCandidates = sortedCandidates.stream()
                                                                        .filter(c ->
                                                                                        c.getWidth() <= originalWidth
                                                                                                && c.getWidth() <= type.getValue()
                                                                                                && (isSquare || Integer
                                                                                                .compare(c.getWidth(), c.getHeight()) != 0)
                                                                        )
                                                                        .collect(Collectors.toList());
        if (filteredCandidates.size() == 0) return sortedCandidates.get(0).getUrl();
        MediaCandidate candidate = filteredCandidates.get(0);
        if (candidate == null) return null;
        return candidate.getUrl();
    }

    public static String getThumbVideoUrl(Media media) {
        return ResponseBodyUtils.getVideoCandidate(media, CandidateType.VIDEO_THUMBNAIL);
    }

    public static String getVideoUrl(Object media) {
        return ResponseBodyUtils.getVideoCandidate(media, CandidateType.DOWNLOAD);
    }

    // TODO: merge with getImageCandidate when Kotlin
    private static String getVideoCandidate(Object rawMedia, CandidateType type) {
        if (rawMedia == null) return null;
        List<MediaCandidate> candidates;
        int originalWidth, originalHeight;
        if (rawMedia instanceof StoryMedia) {
            candidates = ((StoryMedia) rawMedia).getVideoVersions();
            originalWidth = ((StoryMedia) rawMedia).getOriginalWidth();
            originalHeight = ((StoryMedia) rawMedia).getOriginalHeight();
        }
        else if (rawMedia instanceof Media) {
            candidates = ((Media) rawMedia).getVideoVersions();
            originalWidth = ((Media) rawMedia).getOriginalWidth();
            originalHeight = ((Media) rawMedia).getOriginalHeight();
        }
        else return null;
        if (candidates == null || candidates.isEmpty()) return null;
        boolean isSquare = Integer.compare(originalWidth, originalHeight) == 0;
        List<MediaCandidate> sortedCandidates = candidates.stream()
                                                                .sorted((c1, c2) -> Integer.compare(c2.getWidth(), c1.getWidth()))
                                                                .collect(Collectors.toList());
        List<MediaCandidate> filteredCandidates = sortedCandidates.stream()
                                                                        .filter(c ->
                                                                                        c.getWidth() <= originalWidth
                                                                                                && c.getWidth() <= type.getValue()
                                                                                                && (isSquare || Integer
                                                                                                .compare(c.getWidth(), c.getHeight()) != 0)
                                                                        )
                                                                        .collect(Collectors.toList());
        if (filteredCandidates.size() == 0) return sortedCandidates.get(0).getUrl();
        MediaCandidate candidate = filteredCandidates.get(0);
        if (candidate == null) return null;
        return candidate.getUrl();
    }

    private enum CandidateType {
        VIDEO_THUMBNAIL(700),
        THUMBNAIL(1000),
        DOWNLOAD(10000);

        private final int value;

        CandidateType(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }
}
