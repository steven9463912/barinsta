package awais.instagrabber.repositories;

import awais.instagrabber.repositories.responses.giphy.GiphyGifResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface GifRepository {

    @GET("/api/v1/creatives/story_media_search_keyed_format/")
    Call<GiphyGifResponse> searchGiphyGifs(@Query("request_surface") String requestSurface,
                                           @Query("q") String query,
                                           @Query("media_types") String mediaTypes);
}
