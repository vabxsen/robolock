package com.robolock.app

import android.app.Application
import com.robolock.app.core.AppContainer

class RobolockApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
