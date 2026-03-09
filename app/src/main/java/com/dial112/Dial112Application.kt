package com.dial112

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

/**
 * Dial112Application - Application class
 * Entry point for the Hilt dependency injection graph.
 */
@HiltAndroidApp
class Dial112Application : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Log.d("Dial112App", "Application initialized in DEBUG mode")
        }
    }
}
