package awais.instagrabber.repositories

import awais.instagrabber.repositories.responses.LocationFeedResponse
import awais.instagrabber.repositories.responses.Place
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.QueryMap

interface LocationService {
    @GET("/api/v1/locations/{location}/info/")
    suspend fun fetch(@Path("location") locationId: Long): Place?

    @GET("/api/v1/feed/location/{location}/")
    suspend fun fetchPosts(
        @Path("location") locationId: Long,
        @QueryMap queryParams: Map<String?, String?>?
    ): LocationFeedResponse?
}