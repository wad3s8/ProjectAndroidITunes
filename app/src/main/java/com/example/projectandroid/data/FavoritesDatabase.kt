package com.example.projectandroid.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.projectandroid.domain.Track
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

class FavoritesDatabase(context: Context) : SQLiteOpenHelper(context, "favorites.db", null, 1) {
    private val gson = Gson()
    private val revision = MutableStateFlow(0L)
    val tracks = revision.map {
        readableDatabase.rawQuery("SELECT track FROM favorites ORDER BY added DESC, id DESC", null).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(gson.fromJson(cursor.getString(0), Track::class.java))
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE favorites (id INTEGER PRIMARY KEY, track TEXT NOT NULL, added INTEGER NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        error("Missing favorites migration from $oldVersion to $newVersion")
    }

    suspend fun setFavorite(track: Track, favorite: Boolean) = withContext(Dispatchers.IO) {
        if (favorite) {
            val values = ContentValues().apply {
                put("id", track.trackId)
                put("track", gson.toJson(track))
                put("added", System.currentTimeMillis())
            }
            writableDatabase.replaceOrThrow("favorites", null, values)
        } else {
            writableDatabase.delete("favorites", "id = ?", arrayOf(track.trackId.toString()))
        }
        revision.update { it + 1 }
    }
}
