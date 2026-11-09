package com.example.projectandroid.data

import android.content.SharedPreferences
import com.example.projectandroid.domain.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CatalogFilters(val genre: String = "", val hideExplicit: Boolean = false) {
    val active get() = genre.isNotEmpty() || hideExplicit

    fun matches(track: Track): Boolean =
        (genre.isEmpty() || track.primaryGenreName?.contains(genre, ignoreCase = true) == true) &&
            (!hideExplicit || track.trackExplicitness != "explicit")
}

class FilterCache(private val preferences: SharedPreferences) {
    private val state = MutableStateFlow(CatalogFilters(
        preferences.getString("genre", "").orEmpty(),
        preferences.getBoolean("hideExplicit", false),
    ))
    val applied = state.asStateFlow()

    fun apply(filters: CatalogFilters) {
        val normalized = filters.copy(genre = filters.genre.trim())
        preferences.edit().putString("genre", normalized.genre)
            .putBoolean("hideExplicit", normalized.hideExplicit).apply()
        state.value = normalized
    }
}
