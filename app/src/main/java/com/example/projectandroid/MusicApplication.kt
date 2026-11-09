package com.example.projectandroid

import android.app.Application
import com.example.projectandroid.data.FavoritesDatabase
import com.example.projectandroid.data.FilterCache

class MusicApplication : Application() {
    val filters by lazy { FilterCache(getSharedPreferences("catalog_filters", MODE_PRIVATE)) }
    val favorites by lazy { FavoritesDatabase(this) }
}
