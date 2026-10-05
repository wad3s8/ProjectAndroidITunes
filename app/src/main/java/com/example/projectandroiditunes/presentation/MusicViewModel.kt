package com.example.projectandroiditunes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectandroiditunes.domain.FailureReason
import com.example.projectandroiditunes.domain.GetTrackUseCase
import com.example.projectandroiditunes.domain.MusicException
import com.example.projectandroiditunes.domain.SearchTracksUseCase
import com.example.projectandroiditunes.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface LoadState<out T> {
    data object Idle : LoadState<Nothing>
    data object Loading : LoadState<Nothing>
    data class Content<T>(val value: T) : LoadState<T>
    data class Error(val reason: FailureReason) : LoadState<Nothing>
}

data class MusicUiState(
    val query: String = "Daft Punk",
    val submittedQuery: String = "",
    val tracks: LoadState<List<Track>> = LoadState.Idle,
    val selectedId: Long? = null,
    val details: LoadState<Track> = LoadState.Idle,
)

class MusicViewModel(
    private val searchTracks: SearchTracksUseCase,
    private val getTrack: GetTrackUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(MusicUiState())
    val state = mutableState.asStateFlow()
    private var searchJob: Job? = null
    private var detailJob: Job? = null

    fun updateQuery(query: String) {
        mutableState.update { it.copy(query = query) }
    }

    fun search() = searchFor(state.value.query.trim())
    fun retrySearch() = searchFor(state.value.submittedQuery)

    private fun searchFor(query: String) {
        if (query.isBlank()) return
        searchJob?.cancel()
        mutableState.update { it.copy(submittedQuery = query, tracks = LoadState.Loading) }
        searchJob = viewModelScope.launch {
            val result = load { searchTracks(query) }
            mutableState.update { it.copy(tracks = result) }
        }
    }

    fun openTrack(id: Long) {
        detailJob?.cancel()
        mutableState.update { it.copy(selectedId = id, details = LoadState.Loading) }
        detailJob = viewModelScope.launch {
            val result = load { getTrack(id) }
            mutableState.update { it.copy(details = result) }
        }
    }

    fun retryDetails() { state.value.selectedId?.let(::openTrack) }

    fun closeDetails() {
        detailJob?.cancel()
        mutableState.update { it.copy(selectedId = null, details = LoadState.Idle) }
    }

    private suspend fun <T> load(block: suspend () -> T): LoadState<T> = try {
        LoadState.Content(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: MusicException) {
        LoadState.Error(error.reason)
    } catch (_: Exception) {
        LoadState.Error(FailureReason.UNKNOWN)
    }
}