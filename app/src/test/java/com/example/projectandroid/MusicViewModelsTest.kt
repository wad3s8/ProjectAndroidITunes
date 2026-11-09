package com.example.projectandroid

import androidx.lifecycle.SavedStateHandle
import com.example.projectandroid.domain.*
import com.example.projectandroid.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MusicViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    private class FakeRepository : TrackRepository {
        var failure = false
        val terms = mutableListOf<String>()
        override suspend fun searchTracks(term: String, limit: Int): List<Track> {
            terms += term
            if (term == "slow") delay(1000)
            if (failure) throw TrackLoadingException(FailureReason.Connection, java.io.IOException())
            return emptyList()
        }
        override suspend fun getTrack(id: Long): Track? {
            if (failure) throw TrackLoadingException(FailureReason.Server, java.io.IOException())
            return null
        }
    }

    @Test fun loadingErrorRetryAndEmptyResults() = runTest(dispatcher) {
        val repository = FakeRepository().apply { failure = true }
        val model = CatalogViewModel(SearchTracksUseCase(repository))
        assertTrue(model.uiState.value.isLoading)
        advanceUntilIdle()
        assertEquals(FailureReason.Connection, model.uiState.value.error)
        repository.failure = false
        model.retry()
        assertTrue(model.uiState.value.isLoading)
        advanceUntilIdle()
        assertNull(model.uiState.value.error)
        assertFalse(model.uiState.value.isLoading)
        assertTrue(model.uiState.value.tracks.isEmpty())
    }

    @Test fun searchTrimsQueryAndBlankDoesNotSendRequest() = runTest(dispatcher) {
        val repository = FakeRepository()
        val saved = SavedStateHandle()
        val model = CatalogViewModel(SearchTracksUseCase(repository), saved)
        advanceUntilIdle()
        model.changeQuery("  Muse  ")
        model.search()
        advanceUntilIdle()
        assertEquals("Muse", repository.terms.last())
        assertEquals("Muse", saved.get<String>("submittedQuery"))
        model.changeQuery("  ")
        model.search()
        advanceUntilIdle()
        assertEquals(2, repository.terms.size)
    }

    @Test fun cancelledSearchDoesNotReplaceLatestResultWithError() = runTest(dispatcher) {
        val repository = FakeRepository()
        val model = CatalogViewModel(SearchTracksUseCase(repository))
        advanceUntilIdle()
        model.changeQuery("slow")
        model.search()
        runCurrent()
        model.changeQuery("new")
        model.search()
        advanceUntilIdle()
        assertEquals("new", model.uiState.value.submittedQuery)
        assertNull(model.uiState.value.error)
        assertFalse(model.uiState.value.isLoading)
    }

    @Test fun detailsErrorRetryNotFoundAndInvalidId() = runTest(dispatcher) {
        val repository = FakeRepository().apply { failure = true }
        val useCase = GetTrackUseCase(repository)
        val model = DetailsViewModel(useCase, SavedStateHandle(mapOf("trackId" to 123L)))
        assertEquals(DetailsUiState.Loading, model.uiState.value)
        advanceUntilIdle()
        assertEquals(DetailsUiState.Error(FailureReason.Server), model.uiState.value)
        repository.failure = false
        model.retry()
        advanceUntilIdle()
        assertEquals(DetailsUiState.NotFound, model.uiState.value)
        assertEquals(DetailsUiState.NotFound, DetailsViewModel(useCase, SavedStateHandle()).uiState.value)
    }

    @Test fun optionalMetadataIsSafeToDisplay() {
        assertEquals("—", formatDuration(null))
        assertEquals("—", formatDuration(-1))
        assertEquals("10:00", formatDuration(600_000))
        assertEquals("—", formatPrice(null, "USD"))
        assertEquals("—", formatReleaseDate("invalid"))
    }
}
