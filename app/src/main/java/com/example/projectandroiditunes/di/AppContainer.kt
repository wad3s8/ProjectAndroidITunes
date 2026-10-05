package com.example.projectandroiditunes.di

import com.example.projectandroiditunes.data.ITunesTrackRepository
import com.example.projectandroiditunes.data.remote.ITunesApi
import com.example.projectandroiditunes.domain.GetTrackUseCase
import com.example.projectandroiditunes.domain.SearchTracksUseCase
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** One dependency graph per application; no Activity references or response cache. */
class AppContainer {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()
    private val api = Retrofit.Builder()
        .baseUrl("https://itunes.apple.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ITunesApi::class.java)
    private val repository = ITunesTrackRepository(api)
    val searchTracks = SearchTracksUseCase(repository)
    val getTrack = GetTrackUseCase(repository)
}