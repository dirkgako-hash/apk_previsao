package com.dirosky.asianbets.data.api

import com.dirosky.asianbets.data.models.EventData
import com.dirosky.asianbets.data.models.LiveOddsResponse
import okhttp3.MultipartBody
import retrofit2.http.*

interface ScoreTrendApi {
    
    @Multipart
    @POST("get_asian_odds_full_data")
    suspend fun getOddsData(
        @Part("date") date: String
    ): List<EventData>
    
    @Multipart
    @POST("get_asian_odds_full_live")
    suspend fun getLiveOdds(
        @Part("game_id") gameId: String
    ): LiveOddsResponse?
    
    @GET("event/{eventId}")
    suspend fun getEventDetails(
        @Path("eventId") eventId: String
    ): Map<String, Any>
    
    @GET("graph/public/{eventId}")
    suspend fun getGraphData(
        @Path("eventId") eventId: String
    ): Map<String, Any>
}
