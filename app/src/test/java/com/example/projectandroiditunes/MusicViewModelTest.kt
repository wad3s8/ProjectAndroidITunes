package com.example.projectandroiditunes

import com.example.projectandroiditunes.domain.*
import com.example.projectandroiditunes.presentation.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MusicViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeRepository
    private lateinit var model: MusicViewModel

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeRepository()
        model = MusicViewModel(SearchTracksUseCase(repository), GetTrackUseCase(repository))
    }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `search publishes loading and then content`() = runTest(dispatcher) {
        model.updateQuery("  Daft Punk  ")
        model.search()
        assertEquals(LoadState.Loading, model.state.value.tracks)
        advanceUntilIdle()
        assertEquals("Daft Punk", repository.lastQuery)
        assertEquals(LoadState.Content(listOf(track)), model.state.value.tracks)
    }

    @Test fun `blank input sends no request`() = runTest(dispatcher) {
        model.updateQuery("   ")
        model.search()
        advanceUntilIdle()
        assertNull(repository.lastQuery)
        assertEquals(LoadState.Idle, model.state.value.tracks)
    }

    @Test fun `retry uses failed query even if text field changed`() = runTest(dispatcher) {
        repository.failure = MusicException(FailureReason.NETWORK)
        model.search()
        advanceUntilIdle()
        assertEquals(LoadState.Error(FailureReason.NETWORK), model.state.value.tracks)
        model.updateQuery("Different draft")
        repository.failure = null
        model.retrySearch()
        advanceUntilIdle()
        assertEquals("Daft Punk", repository.lastQuery)
        assertTrue(model.state.value.tracks is LoadState.Content)
    }

    @Test fun `new search cancels previous result`() = runTest(dispatcher) {
        val pending = CompletableDeferred<List<Track>>()
        repository.pending = pending
        model.updateQuery("Old")
        model.search()
        runCurrent()
        repository.pending = null
        model.updateQuery("New")
        model.search()
        advanceUntilIdle()
        pending.complete(emptyList())
        advanceUntilIdle()
        assertEquals("New", model.state.value.submittedQuery)
        assertEquals(LoadState.Content(listOf(track)), model.state.value.tracks)
    }

    @Test fun `details load independently and back keeps search results`() = runTest(dispatcher) {
        model.search()
        advanceUntilIdle()
        val results = model.state.value.tracks
        model.openTrack(7)
        assertEquals(LoadState.Loading, model.state.value.details)
        advanceUntilIdle()
        assertEquals(LoadState.Content(track), model.state.value.details)
        model.closeDetails()
        assertNull(model.state.value.selectedId)
        assertEquals(results, model.state.value.tracks)
    }

    @Test fun `details error can be retried`() = runTest(dispatcher) {
        repository.failure = MusicException(FailureReason.TIMEOUT)
        model.openTrack(7)
        advanceUntilIdle()
        assertEquals(LoadState.Error(FailureReason.TIMEOUT), model.state.value.details)
        repository.failure = null
        model.retryDetails()
        advanceUntilIdle()
        assertEquals(LoadState.Content(track), model.state.value.details)
    }

    @Test fun `back cancels pending details without displaying error`() = runTest(dispatcher) {
        val pending = CompletableDeferred<Track>()
        repository.pendingDetails = pending
        model.openTrack(7)
        runCurrent()
        model.closeDetails()
        pending.complete(track)
        advanceUntilIdle()
        assertEquals(LoadState.Idle, model.state.value.details)
        assertNull(model.state.value.selectedId)
    }

    private class FakeRepository : TrackRepository {
        var lastQuery: String? = null
        var failure: MusicException? = null
        var pending: CompletableDeferred<List<Track>>? = null
        var pendingDetails: CompletableDeferred<Track>? = null
        override suspend fun search(term: String, limit: Int): List<Track> {
            lastQuery = term
            failure?.let { throw it }
            return pending?.await() ?: listOf(track)
        }
        override suspend fun getTrack(id: Long): Track {
            failure?.let { throw it }
            return pendingDetails?.await() ?: track
        }
    }

    companion object {
        private val track = Track(7, "Title", "Artist", null, null, null, null, null, null, null, null)
    }
}