package com.example.projectandroiditunes.data.remote

import com.example.projectandroiditunes.domain.Track
import com.google.gson.annotations.SerializedName

// Nullable API fields: Apple does not return every field for every media object.
data class SearchResponseDto(
    @SerializedName("results") val results: List<TrackDto>? = null,
)

data class TrackDto(
    @SerializedName("trackId") val trackId: Long? = null,
    @SerializedName("kind") val kind: String? = null,
    @SerializedName("trackName") val trackName: String? = null,
    @SerializedName("artistName") val artistName: String? = null,
    @SerializedName("collectionName") val collectionName: String? = null,
    @SerializedName("artworkUrl100") val artworkUrl100: String? = null,
    @SerializedName("primaryGenreName") val primaryGenreName: String? = null,
    @SerializedName("trackTimeMillis") val trackTimeMillis: Long? = null,
    @SerializedName("releaseDate") val releaseDate: String? = null,
    @SerializedName("trackPrice") val trackPrice: Double? = null,
    @SerializedName("currency") val currency: String? = null,
    @SerializedName("trackViewUrl") val trackViewUrl: String? = null,
)

internal fun TrackDto.toDomainOrNull(): Track? {
    val id = trackId?.takeIf { it > 0 } ?: return null
    val title = trackName?.takeIf { it.isNotBlank() } ?: return null
    if (kind != "song") return null
    return Track(
        id = id,
        title = title,
        artist = artistName?.takeIf { it.isNotBlank() } ?: "Неизвестный исполнитель",
        album = collectionName,
        artworkUrl = artworkUrl100,
        genre = primaryGenreName,
        durationMillis = trackTimeMillis?.takeIf { it >= 0 },
        releaseDate = releaseDate?.take(10),
        price = trackPrice?.takeIf { it >= 0 },
        currency = currency,
        storeUrl = trackViewUrl,
    )
}