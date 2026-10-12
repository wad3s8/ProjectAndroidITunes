package com.example.projectandroid

import com.example.projectandroid.data.ItunesApi
import com.example.projectandroid.data.NetworkTrackRepository
import com.example.projectandroid.domain.GetTrackUseCase
import com.example.projectandroid.domain.SearchTracksUseCase
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object AppContainer {
    private val repository by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
        NetworkTrackRepository(Retrofit.Builder()
            .baseUrl("https://itunes.apple.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(ItunesApi::class.java))
    }
    val searchTracks by lazy { SearchTracksUseCase(repository) }
    val getTrack by lazy { GetTrackUseCase(repository) }
}
