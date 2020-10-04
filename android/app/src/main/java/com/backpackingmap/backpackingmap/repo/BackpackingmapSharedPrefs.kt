package com.backpackingmap.backpackingmap.repo

import android.app.Activity
import android.app.Application
import android.content.SharedPreferences

class BackpackingmapSharedPrefs(private val prefs: SharedPreferences) {
    constructor(activity: Activity) :
            this(activity.getSharedPreferences(BACKPACKINGMAP_PREFS, 0))

    constructor(application: Application) :
            this(application.getSharedPreferences(BACKPACKINGMAP_PREFS, 0))

    fun edit(): SharedPreferences.Editor = prefs.edit()

    val isLoggedIn
        get() = prefs.getBoolean(IS_LOGGED_IN, false)

    fun setIsLoggedIn(prefEditor: SharedPreferences.Editor, value: Boolean) {
        prefEditor.putBoolean(IS_LOGGED_IN, value)
    }

    companion object {
        const val IS_LOGGED_IN = "is_logged_in"
        const val BACKPACKINGMAP_PREFS = "backpackingmap_prefs"

        // Mode 0 is application-private. Accessing the constant requires a Context
        // <https://developer.android.com/reference/android/content/Context#MODE_PRIVATE>
        const val PREFS_MODE = 0
    }
}