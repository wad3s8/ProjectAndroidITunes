package com.example.projectandroiditunes.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface ITunesApi {
    @GET("search")
    suspend fun search(
        @Query("term") term: String,
        @Query("limit") limit: Int,
        @Query("media") media: String = "music",
        @Query("entity") entity: String = "song",
        @Query("country") country: String = "US",
    ): SearchResponseDto

    @GET("lookup")
    suspend fun lookup(
        @Query("id") id: Long,
        @Query("country") country: String = "US",
    ): SearchResponseDto
}