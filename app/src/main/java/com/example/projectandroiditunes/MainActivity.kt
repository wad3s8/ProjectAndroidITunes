package com.example.projectandroiditunes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.projectandroiditunes.presentation.MusicScreen
import com.example.projectandroiditunes.presentation.MusicViewModel
import com.example.projectandroiditunes.ui.theme.ProjectAndroidITunesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as ITunesApplication).container
        val factory = viewModelFactory {
            initializer { MusicViewModel(container.searchTracks, container.getTrack) }
        }
        setContent {
            ProjectAndroidITunesTheme {
                MusicScreen(viewModel = viewModel(factory = factory))
            }
        }
    }
}