package awais.instagrabber.webservices

import awais.instagrabber.repositories.LocationService
import awais.instagrabber.repositories.responses.Location
import awais.instagrabber.repositories.responses.PostsFetchResponse
import awais.instagrabber.utils.TextUtils.isEmpty
import com.google.common.collect.ImmutableMap

open class LocationRepository(private val repository: LocationService) {
    suspend fun fetchPosts(locationId: Long, maxId: String): PostsFetchResponse? {
        val builder = ImmutableMap.builder<String, String>()
        if (!isEmpty(maxId)) {
            builder.put("max_id", maxId)
        }
        val body = repository.fetchPosts(locationId, builder.build()) ?: return null
        return PostsFetchResponse(
            body.items,
            body.moreAvailable,
            body.nextMaxId
        )
    }

    suspend fun fetch(locationId: Long): Location? {
        val place = repository.fetch(locationId) ?: return null
        return place.location
    }

    companion object {
        @Volatile
        private var INSTANCE: LocationRepository? = null

        fun getInstance(): LocationRepository {
            return INSTANCE ?: synchronized(this) {
                val service = RetrofitFactory.retrofit.create(LocationService::class.java)
                LocationRepository(service).also { INSTANCE = it }
            }
        }
    }
}