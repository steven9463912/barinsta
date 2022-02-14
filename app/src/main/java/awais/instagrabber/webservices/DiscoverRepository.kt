package awais.instagrabber.webservices

import awais.instagrabber.repositories.FeedService

open class DiscoverRepository(private val repository: FeedService) {


    companion object {
        @Volatile
        private var INSTANCE: DiscoverRepository? = null

        fun getInstance(): DiscoverRepository {
            return INSTANCE ?: synchronized(this) {
                val service = RetrofitFactory.retrofit.create(FeedService::class.java)
                DiscoverRepository(service).also { INSTANCE = it }
            }
        }
    }
}