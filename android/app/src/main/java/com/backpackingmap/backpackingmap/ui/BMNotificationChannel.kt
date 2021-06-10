package com.backpackingmap.backpackingmap.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.annotation.StringRes
import com.backpackingmap.backpackingmap.R

enum class BMNotificationChannel(val id: String, @StringRes val displayName: Int, val importance: Int) {
    TrackPersistent("track_persistent", R.string.recording_in_progress, NotificationManager.IMPORTANCE_LOW),
    Error("error", R.string.errors, NotificationManager.IMPORTANCE_HIGH);

    private fun ensureCreated(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            NotificationChannel(
                id,
                context.getText(displayName),
                importance
            )
        )
    }

    companion object {
        /** Called in Main */
        fun ensureAllCreated(context: Context) {
            for (chan in values()) {
                chan.ensureCreated(context)
            }
        }
    }
}