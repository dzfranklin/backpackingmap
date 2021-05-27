package com.backpackingmap.backpackingmap

import android.app.Application
import android.os.StrictMode
import timber.log.Timber

@Suppress("unused") // used in AndroidManifest.xml
class BackpackingMapApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(LogDebugTree())

        if (BuildConfig.DEBUG) {
            val policy = StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
            StrictMode.setVmPolicy(policy)
        }
    }

    class LogDebugTree : Timber.DebugTree() {
        override fun createStackElementTag(element: StackTraceElement): String {
            return "(${element.fileName}:${element.lineNumber})#${element.methodName}"
        }
    }
}