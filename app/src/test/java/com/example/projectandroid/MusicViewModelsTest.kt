package com.example.projectandroid

import androidx.lifecycle.SavedStateHandle
import com.example.projectandroid.data.MockTrackRepository
import com.example.projectandroid.ui.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class MusicViewModelsTest {
    @Test fun catalogItemsResolveToTheSameDetails() {
        val catalog = CatalogViewModel(MockTrackRepository).uiState.value.tracks
        assertTrue(catalog.size > 20)
        assertEquals(catalog.size, catalog.map { it.trackId }.toSet().size)
        catalog.forEach { track ->
            val model = DetailsViewModel(MockTrackRepository, SavedStateHandle(mapOf("trackId" to track.trackId)))
            assertEquals(DetailsUiState.Content(track), model.uiState.value)
        }
    }

    @Test fun missingAndUnknownIdsHaveAnExplicitState() {
        assertEquals(DetailsUiState.NotFound,
            DetailsViewModel(MockTrackRepository, SavedStateHandle()).uiState.value)
        assertEquals(DetailsUiState.NotFound,
            DetailsViewModel(MockTrackRepository, SavedStateHandle(mapOf("trackId" to -1L))).uiState.value)
    }

    @Test fun optionalMetadataIsSafeToDisplay() {
        assertEquals("—", formatDuration(null))
        assertEquals("—", formatDuration(-1))
        assertEquals("0:00", formatDuration(0))
        assertEquals("10:00", formatDuration(600_000))
        assertEquals("—", formatPrice(null, "USD"))
        assertEquals("—", formatPrice(-1.0, "USD"))
        assertEquals("—", formatPrice(1.29, "invalid"))
        assertEquals("$1.29", formatPrice(1.29, "USD", Locale.US))
        assertEquals("—", formatReleaseDate(null))
        assertEquals("—", formatReleaseDate("invalid"))
        assertEquals("17 May 2013", formatReleaseDate("2013-05-17T07:00:00Z", Locale.US))
    }
}

