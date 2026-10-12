package com.example.projectandroid.domain

interface TrackRepository {
    suspend fun searchTracks(term: String, limit: Int): List<Track>
    suspend fun getTrack(id: Long): Track?
}
class SearchTracksUseCase(private val repository: TrackRepository) {
    suspend operator fun invoke(term: String, limit: Int = 25): List<Track> {
        require(term.isNotBlank())
        require(limit in 1..200)
        return repository.searchTracks(term.trim(), limit)
    }
}
class GetTrackUseCase(private val repository: TrackRepository) {
    suspend operator fun invoke(id: Long): Track? = if (id > 0) repository.getTrack(id) else null
}
enum class FailureReason { Connection, Server, RateLimit, InvalidResponse }
class TrackLoadingException(val reason: FailureReason, cause: Throwable) : Exception(cause)
