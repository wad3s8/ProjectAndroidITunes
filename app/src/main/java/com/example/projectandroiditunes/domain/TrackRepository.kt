package com.example.projectandroiditunes.domain

interface TrackRepository {
    suspend fun search(term: String, limit: Int): List<Track>
    suspend fun getTrack(id: Long): Track
}

class SearchTracksUseCase(private val repository: TrackRepository) {
    suspend operator fun invoke(term: String, limit: Int = 25): List<Track> {
        val query = term.trim()
        require(query.isNotBlank()) { "Search query must not be empty" }
        require(limit in 1..200) { "Limit must be between 1 and 200" }
        return repository.search(query, limit)
    }
}

class GetTrackUseCase(private val repository: TrackRepository) {
    suspend operator fun invoke(id: Long): Track {
        require(id > 0) { "Track ID must be positive" }
        return repository.getTrack(id)
    }
}