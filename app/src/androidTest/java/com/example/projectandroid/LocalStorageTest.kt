package com.example.projectandroid

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import com.example.projectandroid.data.CatalogFilters
import com.example.projectandroid.data.FavoritesDatabase
import com.example.projectandroid.data.FilterCache
import com.example.projectandroid.domain.Track
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LocalStorageTest {
    private val context = object : ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
        override fun getDatabasePath(name: String) = baseContext.getDatabasePath("test_$name")
    }
    private val track = Track(42, "Song", "Artist", "Album", 1, 10, 120000,
        "Electronic", null, null, null, "USA", "notExplicit")

    @Test fun filtersRestoreAndResetBadge() {
        val preferences = context.getSharedPreferences("test_filters", Context.MODE_PRIVATE)
        val cache = FilterCache(preferences)
        cache.apply(CatalogFilters(" elec ", true))
        val restored = FilterCache(preferences).applied.value
        assertTrue(restored.active)
        assertTrue(restored.matches(track))
        assertFalse(restored.matches(track.copy(trackExplicitness = "explicit")))
        assertFalse(restored.matches(track.copy(primaryGenreName = "Pop")))
        cache.apply(CatalogFilters())
        assertFalse(FilterCache(preferences).applied.value.active)
    }

    @Test fun favoriteIsUniqueAndCanBeReadAfterReopeningDatabase() = runBlocking {
        FavoritesDatabase(context).use { db ->
            db.setFavorite(track, true)
            db.setFavorite(track, true)
        }
        FavoritesDatabase(context).use { db ->
            assertEquals(listOf(track), db.tracks.first().filter { it.trackId == track.trackId })
            db.setFavorite(track, false)
            assertFalse(db.tracks.first().any { it.trackId == track.trackId })
        }
    }
}
