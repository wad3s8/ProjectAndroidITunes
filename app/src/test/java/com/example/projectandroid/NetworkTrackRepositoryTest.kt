package com.example.projectandroid

import com.example.projectandroid.data.*
import com.example.projectandroid.domain.*
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NetworkTrackRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: NetworkTrackRepository
    @Before fun setup() {
        server = MockWebServer()
        server.start()
        repository = NetworkTrackRepository(Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create()).build().create(ItunesApi::class.java))
    }
    @After fun teardown() { server.shutdown() }

    @Test fun searchEncodesParametersMapsTracksAndFiltersOtherEntities() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"results":[
            {"kind":"song","trackId":42,"trackName":"Song","artistName":"Artist"},
            {"kind":"song","trackId":42,"trackName":"Duplicate"},
            {"kind":"music-video","trackId":7,"trackName":"Video"},
            {"kind":"song","trackName":"Missing ID"}
        ]}"""))
        val tracks = repository.searchTracks("Daft Punk & friends", 25)
        assertEquals(1, tracks.size)
        assertEquals(42L, tracks.single().trackId)
        assertNull(tracks.single().trackPrice)
        val url = server.takeRequest().requestUrl!!
        assertEquals("/search", url.encodedPath)
        assertEquals("Daft Punk & friends", url.queryParameter("term"))
        assertEquals("25", url.queryParameter("limit"))
        assertEquals("music", url.queryParameter("media"))
        assertEquals("song", url.queryParameter("entity"))
    }

    @Test fun lookupUsesIdAndHandlesMissingTrack() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"results":[{"kind":"song","trackId":42,"trackName":"Song"}]}"""))
        assertEquals("Song", repository.getTrack(42)?.trackName)
        val url = server.takeRequest().requestUrl!!
        assertEquals("/lookup", url.encodedPath)
        assertEquals("42", url.queryParameter("id"))
        server.enqueue(MockResponse().setBody("""{"results":[]}"""))
        assertNull(repository.getTrack(999))
    }

    @Test fun httpAndMalformedResponsesBecomeDomainErrors() = runBlocking {
        for ((response, reason) in listOf(
            MockResponse().setResponseCode(429) to FailureReason.RateLimit,
            MockResponse().setResponseCode(503) to FailureReason.Server,
            MockResponse().setBody("""{"wrong":[]}""") to FailureReason.InvalidResponse,
            MockResponse().setBody("not json") to FailureReason.InvalidResponse,
        )) {
            server.enqueue(response)
            try {
                repository.searchTracks("test", 25)
                fail("Expected domain error")
            } catch (error: TrackLoadingException) { assertEquals(reason, error.reason) }
        }
    }
}
