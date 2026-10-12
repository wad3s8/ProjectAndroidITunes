package com.example.projectandroid.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectandroid.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CatalogUiState(
    val query: String = "Daft Punk",
    val submittedQuery: String = "Daft Punk",
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = true,
    val error: FailureReason? = null,
)
class CatalogViewModel(
    private val searchTracks: SearchTracksUseCase,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {
    private val state = MutableStateFlow(CatalogUiState(
        query = savedStateHandle["query"] ?: "Daft Punk",
        submittedQuery = savedStateHandle["submittedQuery"] ?: "Daft Punk",
    ))
    val uiState = state.asStateFlow()
    private var searchJob: Job? = null
    init { load(state.value.submittedQuery) }

    fun changeQuery(query: String) {
        savedStateHandle["query"] = query
        state.value = state.value.copy(query = query)
    }
    fun search() {
        val term = state.value.query.trim()
        if (term.isNotEmpty()) load(term)
    }
    fun retry() = load(state.value.submittedQuery)
    private fun load(term: String) {
        searchJob?.cancel()
        savedStateHandle["submittedQuery"] = term
        state.value = state.value.copy(submittedQuery = term, tracks = emptyList(), isLoading = true, error = null)
        searchJob = viewModelScope.launch {
            try {
                val tracks = searchTracks(term)
                state.value = state.value.copy(tracks = tracks, isLoading = false)
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                state.value = state.value.copy(isLoading = false,
                    error = (error as? TrackLoadingException)?.reason ?: FailureReason.InvalidResponse)
            }
        }
    }
}
sealed interface DetailsUiState {
    data object Loading : DetailsUiState
    data class Content(val track: Track) : DetailsUiState
    data class Error(val reason: FailureReason) : DetailsUiState
    data object NotFound : DetailsUiState
}
class DetailsViewModel(private val getTrack: GetTrackUseCase, savedStateHandle: SavedStateHandle) : ViewModel() {
    private val trackId = savedStateHandle.get<Long>("trackId")
    private val state = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val uiState = state.asStateFlow()
    private var job: Job? = null
    init { retry() }
    fun retry() {
        job?.cancel()
        val id = trackId
        if (id == null || id <= 0) { state.value = DetailsUiState.NotFound; return }
        state.value = DetailsUiState.Loading
        job = viewModelScope.launch {
            try {
                state.value = getTrack(id)?.let(DetailsUiState::Content) ?: DetailsUiState.NotFound
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                state.value = DetailsUiState.Error((error as? TrackLoadingException)?.reason ?: FailureReason.InvalidResponse)
            }
        }
    }
}

