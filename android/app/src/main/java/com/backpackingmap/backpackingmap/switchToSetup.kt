package com.backpackingmap.backpackingmap

import android.content.Context
import android.content.Intent
import com.backpackingmap.backpackingmap.setup_activity.SetupActivity
import timber.log.Timber

fun switchToSetup(context: Context) {
    val intent = Intent(context, SetupActivity::class.java)
    context.startActivity(intent)
    Timber.i("Starting Setup activity")
}
