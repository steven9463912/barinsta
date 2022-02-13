package awais.instagrabber.models;

import com.google.gson.Gson;

import java.util.Objects;

public final class PostsLayoutPreferences {
    private final PostsLayoutType type;
    private final int colCount;
    private final boolean isAvatarVisible;
    private final boolean isNameVisible;
    private final ProfilePicSize profilePicSize;
    private final boolean hasRoundedCorners;
    private final boolean hasGap;
    private final boolean animationDisabled;

    public static class Builder {
        private PostsLayoutType type = PostsLayoutType.GRID;
        private int colCount = 3;
        private boolean isAvatarVisible = true;
        private boolean isNameVisible;
        private ProfilePicSize profilePicSize = ProfilePicSize.SMALL;
        private boolean hasRoundedCorners = true;
        private boolean hasGap = true;
        private boolean animationDisabled;

        public Builder setType(PostsLayoutType type) {
            this.type = type;
            return this;
        }

        public Builder setColCount(int colCount) {
            this.colCount = (colCount <= 0 || colCount > 3) ? 1 : colCount;
            return this;
        }

        public Builder setAvatarVisible(boolean avatarVisible) {
            isAvatarVisible = avatarVisible;
            return this;
        }

        public Builder setNameVisible(boolean nameVisible) {
            isNameVisible = nameVisible;
            return this;
        }

        public Builder setProfilePicSize(ProfilePicSize profilePicSize) {
            this.profilePicSize = profilePicSize;
            return this;
        }

        public Builder setHasRoundedCorners(boolean hasRoundedCorners) {
            this.hasRoundedCorners = hasRoundedCorners;
            return this;
        }

        public Builder setHasGap(boolean hasGap) {
            this.hasGap = hasGap;
            return this;
        }

        public Builder setAnimationDisabled(boolean animationDisabled) {
            this.animationDisabled = animationDisabled;
            return this;
        }

        // Breaking builder pattern and adding getters to avoid too many object creations in PostsLayoutPreferencesDialogFragment
        public PostsLayoutType getType() {
            return this.type;
        }

        public int getColCount() {
            return this.colCount;
        }

        public boolean isAvatarVisible() {
            return this.isAvatarVisible;
        }

        public boolean isNameVisible() {
            return this.isNameVisible;
        }

        public ProfilePicSize getProfilePicSize() {
            return this.profilePicSize;
        }

        public boolean getHasRoundedCorners() {
            return this.hasRoundedCorners;
        }

        public boolean getHasGap() {
            return this.hasGap;
        }

        public boolean isAnimationDisabled() {
            return this.animationDisabled;
        }

        public Builder mergeFrom(PostsLayoutPreferences preferences) {
            if (preferences == null) {
                return this;
            }
            this.setColCount(preferences.getColCount());
            this.setAvatarVisible(preferences.isAvatarVisible());
            this.setNameVisible(preferences.isNameVisible());
            this.setType(preferences.getType());
            this.setProfilePicSize(preferences.getProfilePicSize());
            this.setHasRoundedCorners(preferences.getHasRoundedCorners());
            this.setHasGap(preferences.getHasGap());
            this.setAnimationDisabled(preferences.isAnimationDisabled());
            return this;
        }

        public PostsLayoutPreferences build() {
            return new PostsLayoutPreferences(this.type, this.colCount, this.isAvatarVisible, this.isNameVisible, this.profilePicSize, this.hasRoundedCorners, this.hasGap,
                    this.animationDisabled);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private PostsLayoutPreferences(PostsLayoutType type,
                                   int colCount,
                                   boolean isAvatarVisible,
                                   boolean isNameVisible,
                                   ProfilePicSize profilePicSize,
                                   boolean hasRoundedCorners,
                                   boolean hasGap,
                                   boolean animationDisabled) {

        this.type = type;
        this.colCount = colCount;
        this.isAvatarVisible = isAvatarVisible;
        this.isNameVisible = isNameVisible;
        this.profilePicSize = profilePicSize;
        this.hasRoundedCorners = hasRoundedCorners;
        this.hasGap = hasGap;
        this.animationDisabled = animationDisabled;
    }

    public PostsLayoutType getType() {
        return this.type;
    }

    public int getColCount() {
        return this.colCount;
    }

    public boolean isAvatarVisible() {
        return this.isAvatarVisible;
    }

    public boolean isNameVisible() {
        return this.isNameVisible;
    }

    public ProfilePicSize getProfilePicSize() {
        return this.profilePicSize;
    }

    public boolean getHasRoundedCorners() {
        return this.hasRoundedCorners;
    }

    public boolean getHasGap() {
        return this.hasGap;
    }

    public String getJson() {
        return new Gson().toJson(this);
    }

    public static PostsLayoutPreferences fromJson(String json) {
        if (json == null) return null;
        return new Gson().fromJson(json, PostsLayoutPreferences.class);
    }

    public boolean isAnimationDisabled() {
        return this.animationDisabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        PostsLayoutPreferences that = (PostsLayoutPreferences) o;
        return this.colCount == that.colCount &&
                this.isAvatarVisible == that.isAvatarVisible &&
                this.isNameVisible == that.isNameVisible &&
                this.type == that.type &&
                this.profilePicSize == that.profilePicSize &&
                this.animationDisabled == that.animationDisabled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.type, this.colCount, this.isAvatarVisible, this.isNameVisible, this.profilePicSize, this.animationDisabled);
    }

    @Override
    public String toString() {
        return "PostsLayoutPreferences{" +
                "type=" + this.type +
                ", colCount=" + this.colCount +
                ", isAvatarVisible=" + this.isAvatarVisible +
                ", isNameVisible=" + this.isNameVisible +
                ", profilePicSize=" + this.profilePicSize +
                ", hasRoundedCorners=" + this.hasRoundedCorners +
                ", hasGap=" + this.hasGap +
                ", animationDisabled=" + this.animationDisabled +
                '}';
    }

    public enum PostsLayoutType {
        GRID,
        STAGGERED_GRID,
        LINEAR
    }

    public enum ProfilePicSize {
        REGULAR,
        SMALL,
        TINY
    }
}
