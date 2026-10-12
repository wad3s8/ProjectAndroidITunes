package com.example.projectandroid.data

import com.example.projectandroid.domain.Track
import retrofit2.http.GET
import retrofit2.http.Query

interface ItunesApi {
    @GET("search")
    suspend fun search(
        @Query("term") term: String,
        @Query("limit") limit: Int,
        @Query("media") media: String = "music",
        @Query("entity") entity: String = "song",
        @Query("country") country: String = "US",
    ): SearchResponseDto

    @GET("lookup")
    suspend fun lookup(@Query("id") id: Long, @Query("country") country: String = "US"): SearchResponseDto
}
data class SearchResponseDto(val results: List<TrackDto>?)
data class TrackDto(
    val kind: String?, val trackId: Long?, val trackName: String?,
    val artistName: String?, val collectionName: String?,
    val trackNumber: Int?, val trackCount: Int?, val trackTimeMillis: Long?,
    val primaryGenreName: String?, val releaseDate: String?, val trackPrice: Double?,
    val currency: String?, val country: String?, val trackExplicitness: String?,
    val artworkUrl100: String?, val previewUrl: String?, val trackViewUrl: String?,
) {
    fun toDomain(): Track? {
        if (kind != "song" || trackId == null || trackId <= 0 || trackName.isNullOrBlank()) return null
        return Track(trackId, trackName, artistName.orEmpty(), collectionName.orEmpty(),
            trackNumber ?: 0, trackCount ?: 0, trackTimeMillis, primaryGenreName, releaseDate,
            trackPrice, currency, country, trackExplicitness, artworkUrl100, previewUrl, trackViewUrl)
    }
}
