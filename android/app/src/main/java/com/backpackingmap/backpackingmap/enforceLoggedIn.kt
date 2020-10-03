package com.backpackingmap.backpackingmap

import android.app.Activity
import android.content.Intent
import com.backpackingmap.backpackingmap.repo.Repo
import com.backpackingmap.backpackingmap.setup_activity.SetupActivity
import timber.log.Timber

fun enforceLoggedIn(activity: Activity) {
    val repo = Repo.fromApplication(activity.application)

    if (repo.isLoggedIn) {
        Timber.i("User is logged in")
    } else {
        Timber.i("Enforcing logged in by starting Setup activity")
        val intent = Intent(activity, SetupActivity::class.java)
        activity.startActivity(intent)
    }
}