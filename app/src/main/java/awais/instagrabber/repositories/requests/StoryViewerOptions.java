package awais.instagrabber.repositories.requests;

import java.io.Serializable;

public class StoryViewerOptions implements Serializable {
    private final long id;
    private final String name;
    private final Type type;
    private int currentFeedStoryIndex;

    private StoryViewerOptions(int position, Type type) {
        this.id = 0;
        this.name = null;
        currentFeedStoryIndex = position;
        this.type = type;
    }

    private StoryViewerOptions(String name, Type type) {
        this.name = name;
        id = 0;
        this.type = type;
    }

    private StoryViewerOptions(long id, Type type) {
        name = null;
        this.id = id;
        this.type = type;
    }

    private StoryViewerOptions(long id, String name, Type type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public static StoryViewerOptions forHashtag(String name) {
        return new StoryViewerOptions(name, Type.HASHTAG);
    }

    public static StoryViewerOptions forLocation(long id, String name) {
        return new StoryViewerOptions(id, name, Type.LOCATION);
    }

    public static StoryViewerOptions forUser(long id, String name) {
        return new StoryViewerOptions(id, name, Type.USER);
    }

    public static StoryViewerOptions forHighlight(long id, String highlight) {
        return new StoryViewerOptions(id, highlight, Type.HIGHLIGHT);
    }

    public static StoryViewerOptions forStory(long mediaId, String username) {
        return new StoryViewerOptions(mediaId, username, Type.STORY);
    }

    public static StoryViewerOptions forFeedStoryPosition(int position) {
        return new StoryViewerOptions(position, Type.FEED_STORY_POSITION);
    }

    public static StoryViewerOptions forStoryArchive(String id) {
        return new StoryViewerOptions(id, Type.STORY_ARCHIVE);
    }

    public static StoryViewerOptions forStoryArchive(int position) {
        return new StoryViewerOptions(position, Type.STORY_ARCHIVE);
    }

    public long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Type getType() {
        return this.type;
    }

    public int getCurrentFeedStoryIndex() {
        return this.currentFeedStoryIndex;
    }

    public void setCurrentFeedStoryIndex(int index) {
        currentFeedStoryIndex = index;
    }

    public enum Type {
        HASHTAG,
        LOCATION,
        USER,
        HIGHLIGHT,
        STORY,
        FEED_STORY_POSITION,
        STORY_ARCHIVE
    }
}
