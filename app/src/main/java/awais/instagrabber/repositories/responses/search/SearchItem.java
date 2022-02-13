package awais.instagrabber.repositories.responses.search;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import awais.instagrabber.db.entities.Favorite;
import awais.instagrabber.db.entities.RecentSearch;
import awais.instagrabber.models.enums.FavoriteType;
import awais.instagrabber.repositories.responses.Hashtag;
import awais.instagrabber.repositories.responses.Location;
import awais.instagrabber.repositories.responses.Place;
import awais.instagrabber.repositories.responses.User;

public class SearchItem {
    private static final String TAG = SearchItem.class.getSimpleName();

    private final User user;
    private final Place place;
    private final Hashtag hashtag;
    private final int position;

    private boolean isRecent;
    private boolean isFavorite;

    public SearchItem(User user,
                      Place place,
                      Hashtag hashtag,
                      int position) {
        this.user = user;
        this.place = place;
        this.hashtag = hashtag;
        this.position = position;
    }

    public User getUser() {
        return this.user;
    }

    public Place getPlace() {
        return this.place;
    }

    public Hashtag getHashtag() {
        return this.hashtag;
    }

    public int getPosition() {
        return this.position;
    }

    public boolean isRecent() {
        return this.isRecent;
    }

    public void setRecent(boolean recent) {
        this.isRecent = recent;
    }

    public boolean isFavorite() {
        return this.isFavorite;
    }

    public void setFavorite(boolean favorite) {
        this.isFavorite = favorite;
    }

    @Nullable
    public FavoriteType getType() {
        if (this.user != null) {
            return FavoriteType.USER;
        }
        if (this.hashtag != null) {
            return FavoriteType.HASHTAG;
        }
        if (this.place != null) {
            return FavoriteType.LOCATION;
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        SearchItem that = (SearchItem) o;
        return Objects.equals(this.user, that.user) &&
                Objects.equals(this.place, that.place) &&
                Objects.equals(this.hashtag, that.hashtag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.user, this.place, this.hashtag);
    }

    @NonNull
    @Override
    public String toString() {
        return "SearchItem{" +
                "user=" + this.user +
                ", place=" + this.place +
                ", hashtag=" + this.hashtag +
                ", position=" + this.position +
                ", isRecent=" + this.isRecent +
                '}';
    }

    @NonNull
    public static List<SearchItem> fromRecentSearch(List<RecentSearch> recentSearches) {
        if (recentSearches == null) return Collections.emptyList();
        return recentSearches.stream()
                             .map(SearchItem::fromRecentSearch)
                             .filter(Objects::nonNull)
                             .collect(Collectors.toList());
    }

    @Nullable
    private static SearchItem fromRecentSearch(RecentSearch recentSearch) {
        if (recentSearch == null) return null;
        try {
            FavoriteType type = recentSearch.getType();
            SearchItem searchItem;
            switch (type) {
                case USER:
                    searchItem = new SearchItem(SearchItem.getUser(recentSearch), null, null, 0);
                    break;
                case HASHTAG:
                    searchItem = new SearchItem(null, null, SearchItem.getHashtag(recentSearch), 0);
                    break;
                case LOCATION:
                    searchItem = new SearchItem(null, SearchItem.getPlace(recentSearch), null, 0);
                    break;
                default:
                    return null;
            }
            searchItem.setRecent(true);
            return searchItem;
        } catch (final Exception e) {
            Log.e(SearchItem.TAG, "fromRecentSearch: ", e);
        }
        return null;
    }

    public static List<SearchItem> fromFavorite(List<Favorite> favorites) {
        if (favorites == null) {
            return Collections.emptyList();
        }
        return favorites.stream()
                        .map(SearchItem::fromFavorite)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
    }

    @Nullable
    private static SearchItem fromFavorite(Favorite favorite) {
        if (favorite == null) return null;
        FavoriteType type = favorite.getType();
        if (type == null) return null;
        SearchItem searchItem;
        switch (type) {
            case USER:
                searchItem = new SearchItem(SearchItem.getUser(favorite), null, null, 0);
                break;
            case HASHTAG:
                searchItem = new SearchItem(null, null, SearchItem.getHashtag(favorite), 0);
                break;
            case LOCATION:
                Place place = SearchItem.getPlace(favorite);
                if (place == null) return null;
                searchItem = new SearchItem(null, place, null, 0);
                break;
            default:
                return null;
        }
        searchItem.setFavorite(true);
        return searchItem;
    }

    @NonNull
    private static User getUser(@NonNull RecentSearch recentSearch) {
        return new User(
                Long.parseLong(recentSearch.getIgId()),
                recentSearch.getUsername(),
                recentSearch.getName(),
                false,
                recentSearch.getPicUrl(),
                false
        );
    }

    @NonNull
    private static User getUser(@NonNull Favorite favorite) {
        return new User(
                0,
                favorite.getQuery(),
                favorite.getDisplayName(),
                false,
                favorite.getPicUrl(),
                false
        );
    }

    @NonNull
    private static Hashtag getHashtag(@NonNull RecentSearch recentSearch) {
        return new Hashtag(
                recentSearch.getIgId(),
                recentSearch.getName(),
                0,
                null,
                null
        );
    }

    @NonNull
    private static Hashtag getHashtag(@NonNull Favorite favorite) {
        return new Hashtag(
                "0",
                favorite.getQuery(),
                0,
                null,
                null
        );
    }

    @NonNull
    private static Place getPlace(@NonNull RecentSearch recentSearch) {
        Location location = new Location(
                Long.parseLong(recentSearch.getIgId()),
                recentSearch.getName(),
                recentSearch.getName(),
                null, null, 0, 0
        );
        return new Place(
                location,
                recentSearch.getName(),
                null,
                null,
                null
        );
    }

    @Nullable
    private static Place getPlace(@NonNull Favorite favorite) {
        try {
            Location location = new Location(
                    Long.parseLong(favorite.getQuery()),
                    favorite.getDisplayName(),
                    favorite.getDisplayName(),
                    null, null, 0, 0
            );
            return new Place(
                    location,
                    favorite.getDisplayName(),
                    null,
                    null,
                    null
            );
        } catch (final Exception e) {
            Log.e(SearchItem.TAG, "getPlace: ", e);
            return null;
        }
    }
}
