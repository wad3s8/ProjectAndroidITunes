package com.example.projectandroid

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.projectandroid.domain.*
import com.example.projectandroid.ui.*
import com.example.projectandroid.ui.theme.ProjectAndroidTheme
import org.junit.Rule
import org.junit.Test

class MusicNavigationTest {
    @get:Rule val compose = createComposeRule()
    private val track = Track(42, "Test song", "Artist", "Album", 1, 10,
        120000, "Electronic", null, null, null, "USA", null)
    private val repository = object : TrackRepository {
        override suspend fun searchTracks(term: String, limit: Int) = listOf(track)
        override suspend fun getTrack(id: Long) = track.takeIf { it.trackId == id }
    }

    @Test fun listDetailsTabsAndBack() {
        compose.setContent {
            ProjectAndroidTheme { MusicApp(SearchTracksUseCase(repository), GetTrackUseCase(repository)) }
        }
        compose.waitUntil(5000) { compose.onAllNodesWithTag("track_42").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("track_list").performScrollToNode(hasTestTag("track_42"))
        compose.onNodeWithTag("track_42").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithTag("track_details").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Test song").assertIsDisplayed()
        compose.onNode(hasText("Избранное") and hasClickAction()).performClick()
        compose.onNode(hasText("Избранное") and hasClickAction()).assertIsSelected()
        compose.onNode(hasText("Настройки") and hasClickAction()).performClick()
        compose.onNode(hasText("Настройки") and hasClickAction()).assertIsSelected()
        compose.onNode(hasText("Музыка") and hasClickAction()).performClick()
        compose.onNodeWithTag("track_details").assertIsDisplayed()
        compose.onNodeWithContentDescription("Назад").performClick()
        compose.onNodeWithTag("track_42").assertIsDisplayed()
    }

    @Test fun errorOffersRetry() {
        var retries = 0
        compose.setContent {
            ProjectAndroidTheme {
                DetailsScreen(DetailsUiState.Error(FailureReason.Connection), {}, { retries++ })
            }
        }
        compose.onNodeWithTag("network_error").assertIsDisplayed()
        compose.onNodeWithText("Повторить").performClick()
        compose.runOnIdle { org.junit.Assert.assertEquals(1, retries) }
    }

    @Test fun loadingIsVisible() {
        compose.setContent {
            ProjectAndroidTheme { DetailsScreen(DetailsUiState.Loading, {}, {}) }
        }
        compose.onNodeWithTag("loading").assertIsDisplayed()
    }
}
