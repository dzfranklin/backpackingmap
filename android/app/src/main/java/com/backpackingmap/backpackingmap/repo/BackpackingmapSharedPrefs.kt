package com.backpackingmap.backpackingmap.repo

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences

class BackpackingmapSharedPrefs(private val prefs: SharedPreferences) {
    fun edit(): SharedPreferences.Editor = prefs.edit()

    val isLoggedIn
        get() = prefs.getBoolean(IS_LOGGED_IN, false)

    fun setIsLoggedIn(prefEditor: SharedPreferences.Editor, value: Boolean) {
        prefEditor.putBoolean(IS_LOGGED_IN, value)
    }

    companion object {
        private const val IS_LOGGED_IN = "is_logged_in"
        private const val BACKPACKINGMAP_PREFS = "backpackingmap_prefs"

        // Mode 0 is application-private. Accessing the constant requires a Context
        // <https://developer.android.com/reference/android/content/Context#MODE_PRIVATE>
        private const val PREFS_MODE = 0

        @Volatile
        private var INSTANCE: BackpackingmapSharedPrefs? = null

        fun fromActivity(activity: Activity): BackpackingmapSharedPrefs {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = BackpackingmapSharedPrefs(activity.getSharedPreferences(
                    BACKPACKINGMAP_PREFS,
                    PREFS_MODE))
                INSTANCE = instance
                return instance
            }
        }

        fun fromContext(context: Context): BackpackingmapSharedPrefs {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = BackpackingmapSharedPrefs(context.getSharedPreferences(
                    BACKPACKINGMAP_PREFS,
                    PREFS_MODE))
                INSTANCE = instance
                return instance
            }
        }
    }
}