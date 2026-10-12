package com.example.projectandroid.data

import com.example.projectandroid.domain.*
import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.io.EOFException

class NetworkTrackRepository(
    private val api: ItunesApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TrackRepository {
    override suspend fun searchTracks(term: String, limit: Int): List<Track> = request {
        api.search(term, limit).tracks().distinctBy { it.trackId }
    }
    override suspend fun getTrack(id: Long): Track? = request {
        api.lookup(id).tracks().firstOrNull { it.trackId == id }
    }
    private fun SearchResponseDto.tracks(): List<Track> =
        (results ?: throw JsonParseException("Missing results")).mapNotNull { it.toDomain() }

    private suspend fun <T> request(block: suspend () -> T): T = withContext(ioDispatcher) {
        try { block() }
        catch (error: HttpException) {
            throw TrackLoadingException(if (error.code() == 429) FailureReason.RateLimit else FailureReason.Server, error)
        } catch (error: JsonParseException) {
            throw TrackLoadingException(FailureReason.InvalidResponse, error)
        } catch (error: MalformedJsonException) {
            throw TrackLoadingException(FailureReason.InvalidResponse, error)
        } catch (error: EOFException) {
            throw TrackLoadingException(FailureReason.InvalidResponse, error)
        } catch (error: IOException) {
            throw TrackLoadingException(FailureReason.Connection, error)
        }
    }
}
