package com.example.projectandroiditunes.domain

/** App model: independent of Retrofit, JSON and Android. */
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String?,
    val artworkUrl: String?,
    val genre: String?,
    val durationMillis: Long?,
    val releaseDate: String?,
    val price: Double?,
    val currency: String?,
    val storeUrl: String?,
)

enum class FailureReason { NETWORK, TIMEOUT, RATE_LIMIT, SERVER, INVALID_RESPONSE, NOT_FOUND, UNKNOWN }

class MusicException(val reason: FailureReason, cause: Throwable? = null) : Exception(cause)