package com.backpackingmap.backpackingmap

import android.app.Application
import timber.log.Timber

class BackpackingMapApplication :Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(LogDebugTree())
    }
}

class LogDebugTree : Timber.DebugTree() {
    override fun createStackElementTag(element: StackTraceElement): String? {
        return "(${element.fileName}:${element.lineNumber})#${element.methodName}"
    }
}
