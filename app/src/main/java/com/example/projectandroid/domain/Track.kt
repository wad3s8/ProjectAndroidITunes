package com.example.projectandroid.domain

/** A music track independent of transport and presentation. */
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
