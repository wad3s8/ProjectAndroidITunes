package com.example.projectandroiditunes.data

import com.example.projectandroiditunes.data.remote.ITunesApi
import com.example.projectandroiditunes.data.remote.SearchResponseDto
import com.example.projectandroiditunes.data.remote.toDomainOrNull
import com.example.projectandroiditunes.domain.FailureReason
import com.example.projectandroiditunes.domain.MusicException
import com.example.projectandroiditunes.domain.Track
import com.example.projectandroiditunes.domain.TrackRepository
import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import java.io.EOFException
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class ITunesTrackRepository(
    private val api: ITunesApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TrackRepository {
    override suspend fun search(term: String, limit: Int): List<Track> = request {
        api.search(term, limit).tracks()
    }

    override suspend fun getTrack(id: Long): Track = request {
        api.lookup(id).tracks().firstOrNull { it.id == id }
            ?: throw MusicException(FailureReason.NOT_FOUND)
    }

    private fun SearchResponseDto.tracks(): List<Track> {
        val items = results ?: throw MusicException(FailureReason.INVALID_RESPONSE)
        return items.mapNotNull { it.toDomainOrNull() }.distinctBy { it.id }
    }

    // Retrofit's suspend API executes HTTP asynchronously; mapping also stays off the UI thread.
    private suspend fun <T> request(block: suspend () -> T): T = withContext(ioDispatcher) {
        try {
            block()
        } catch (error: CancellationException) {
            throw error // Never turn leaving a screen or replacing a search into a UI error.
        } catch (error: MusicException) {
            throw error
        } catch (error: HttpException) {
            throw MusicException(
                if (error.code() == 429) FailureReason.RATE_LIMIT else FailureReason.SERVER,
                error,
            )
        } catch (error: SocketTimeoutException) {
            throw MusicException(FailureReason.TIMEOUT, error)
        } catch (error: JsonParseException) {
            throw MusicException(FailureReason.INVALID_RESPONSE, error)
        } catch (error: MalformedJsonException) {
            throw MusicException(FailureReason.INVALID_RESPONSE, error)
        } catch (error: EOFException) {
            throw MusicException(FailureReason.INVALID_RESPONSE, error)
        } catch (error: IOException) {
            throw MusicException(FailureReason.NETWORK, error)
        }
    }
}