package com.backpackingmap.backpackingmap

import android.app.Activity
import android.content.Intent
import com.backpackingmap.backpackingmap.repo.BackpackingmapSharedPrefs
import com.backpackingmap.backpackingmap.setup_activity.SetupActivity
import timber.log.Timber

fun enforceLoggedIn(activity: Activity) {
    // NOTE: This doesn't "enforce" from a security perspective, it prevents a user from seeing
    // a broken interface because it requires data the application doesn't have.
    val prefs = BackpackingmapSharedPrefs(activity)

    if (prefs.isLoggedIn) {
        Timber.i("User is logged in")
    } else {
        Timber.i("Enforcing logged in by starting Setup activity")
        val intent = Intent(activity, SetupActivity::class.java)
        activity.startActivity(intent)
    }
}