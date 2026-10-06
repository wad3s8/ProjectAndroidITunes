package com.example.projectandroid.data

/**
 * Fields correspond to iTunes Search/Lookup results with kind = song.
 * Optional store metadata can be absent in a future API response.
 * URLs are reserved for the later network practice; artwork is local for now.
 */
data class Track(
    val trackId: Long,
    val trackName: String,
    val artistName: String,
    val collectionName: String,
    val trackNumber: Int,
    val trackCount: Int,
    val trackTimeMillis: Long?,
    val primaryGenreName: String?,
    val releaseDate: String?,
    val trackPrice: Double?,
    val currency: String?,
    val country: String?,
    val trackExplicitness: String?,
    val artworkUrl100: String? = null,
    val previewUrl: String? = null,
    val trackViewUrl: String? = null,
)

