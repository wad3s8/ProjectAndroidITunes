package com.example.projectandroid.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.projectandroid.data.Track
import com.example.projectandroid.data.TrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CatalogUiState(val tracks: List<Track>)

class CatalogViewModel(repository: TrackRepository) : ViewModel() {
    val uiState = MutableStateFlow(CatalogUiState(repository.getTracks())).asStateFlow()
}

sealed interface DetailsUiState {
    data class Content(val track: Track) : DetailsUiState
    data object NotFound : DetailsUiState
}

class DetailsViewModel(repository: TrackRepository, savedStateHandle: SavedStateHandle) : ViewModel() {
    // Only the ID travels through navigation. SavedStateHandle restores it after recreation.
    val uiState = MutableStateFlow<DetailsUiState>(
        savedStateHandle.get<Long>("trackId")?.let(repository::getTrack)
            ?.let(DetailsUiState::Content) ?: DetailsUiState.NotFound,
    ).asStateFlow()
}

