package com.backpackingmap.backpackingmap

import android.app.Application
import timber.log.Timber

class BackpackingMapApplication :Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}
