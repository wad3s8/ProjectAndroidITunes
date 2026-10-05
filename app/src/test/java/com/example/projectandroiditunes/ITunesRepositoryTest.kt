package com.example.projectandroiditunes

import com.example.projectandroiditunes.data.ITunesTrackRepository
import com.example.projectandroiditunes.data.remote.ITunesApi
import com.example.projectandroiditunes.domain.FailureReason
import com.example.projectandroiditunes.domain.MusicException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ITunesRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var api: ITunesApi

    @Before fun setUp() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create()).build().create(ITunesApi::class.java)
    }

    @After fun tearDown() { server.shutdown() }

    @Test fun `search encodes parameters and maps optional fields`() = runTest {
        server.enqueue(MockResponse().setBody("""{"results":[{"kind":"song","trackId":7,"trackName":"One More Time","artistName":"Daft Punk"}]}"""))
        val repository = ITunesTrackRepository(api, StandardTestDispatcher(testScheduler))
        val tracks = repository.search("Daft Punk & музыка", 25)
        assertEquals(7L, tracks.single().id)
        assertEquals("One More Time", tracks.single().title)
        assertNull(tracks.single().album)
        val url = server.takeRequest().requestUrl!!
        assertEquals("/search", url.encodedPath)
        assertEquals("Daft Punk & музыка", url.queryParameter("term"))
        assertEquals("25", url.queryParameter("limit"))
        assertEquals("music", url.queryParameter("media"))
        assertEquals("song", url.queryParameter("entity"))
    }

    @Test fun `lookup uses ID and returns the matching song`() = runTest {
        server.enqueue(MockResponse().setBody("""{"results":[{"kind":"song","trackId":7,"trackName":"Title"}]}"""))
        val repository = ITunesTrackRepository(api, StandardTestDispatcher(testScheduler))
        assertEquals(7L, repository.getTrack(7).id)
        val url = server.takeRequest().requestUrl!!
        assertEquals("/lookup", url.encodedPath)
        assertEquals("7", url.queryParameter("id"))
    }

    @Test fun `empty search is success and missing lookup is not found`() = runTest {
        val repository = ITunesTrackRepository(api, StandardTestDispatcher(testScheduler))
        server.enqueue(MockResponse().setBody("""{"results":[]}"""))
        assertTrue(repository.search("missing", 25).isEmpty())
        server.enqueue(MockResponse().setBody("""{"results":[]}"""))
        try { repository.getTrack(7); fail("Expected missing track") }
        catch (error: MusicException) { assertEquals(FailureReason.NOT_FOUND, error.reason) }
    }

    @Test fun `HTTP errors and invalid responses are classified`() = runTest {
        val repository = ITunesTrackRepository(api, StandardTestDispatcher(testScheduler))
        for ((response, expected) in listOf(
            MockResponse().setResponseCode(429) to FailureReason.RATE_LIMIT,
            MockResponse().setResponseCode(503) to FailureReason.SERVER,
            MockResponse().setBody("{}") to FailureReason.INVALID_RESPONSE,
            MockResponse().setBody("not json") to FailureReason.INVALID_RESPONSE,
        )) {
            server.enqueue(response)
            try { repository.search("test", 25); fail("Expected $expected") }
            catch (error: MusicException) { assertEquals(expected, error.reason) }
        }
    }

    @Test fun `offline and timeout failures are classified and cancellation propagates`() = runTest {
        for ((exception, expected) in listOf(
            UnknownHostException() to FailureReason.NETWORK,
            SocketTimeoutException() to FailureReason.TIMEOUT,
        )) {
            val repository = ITunesTrackRepository(failingApi(exception), StandardTestDispatcher(testScheduler))
            try { repository.search("test", 25); fail("Expected $expected") }
            catch (error: MusicException) { assertEquals(expected, error.reason) }
        }
        val cancellation = CancellationException("screen closed")
        val repository = ITunesTrackRepository(failingApi(cancellation), StandardTestDispatcher(testScheduler))
        try { repository.search("test", 25); fail("Expected cancellation") }
        catch (error: CancellationException) { assertSame(cancellation, error) }
    }

    private fun failingApi(error: Exception) = object : ITunesApi {
        override suspend fun search(term: String, limit: Int, media: String, entity: String, country: String): com.example.projectandroiditunes.data.remote.SearchResponseDto = throw error
        override suspend fun lookup(id: Long, country: String): com.example.projectandroiditunes.data.remote.SearchResponseDto = throw error
    }
}