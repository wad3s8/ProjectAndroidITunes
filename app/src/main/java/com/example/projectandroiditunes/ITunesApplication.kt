package com.example.projectandroiditunes

import android.app.Application
import com.example.projectandroiditunes.di.AppContainer

class ITunesApplication : Application() {
    val container by lazy { AppContainer() }
}