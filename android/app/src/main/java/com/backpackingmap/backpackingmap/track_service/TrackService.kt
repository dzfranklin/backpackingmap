package com.backpackingmap.backpackingmap.track_service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.backpackingmap.backpackingmap.R
import com.backpackingmap.backpackingmap.model.TrackId
import com.backpackingmap.backpackingmap.model.TrackMoment
import com.backpackingmap.backpackingmap.model.TrackSettings
import com.backpackingmap.backpackingmap.ui.BMIntentAction
import com.backpackingmap.backpackingmap.ui.BMNotificationChannel
import com.google.android.gms.location.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber


class TrackService : LifecycleService() {
    data class Args(
        val trackSettings: Flow<TrackSettings>,
        val activeTrack: Flow<TrackId?>,
        val addTrackMoment: suspend (track: TrackId, moment: TrackMoment) -> Unit
    )

    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private var currentMainloop: Job? = null

    fun setup(args: Args) {
        val prev = currentMainloop
        if (prev != null) {
            Timber.i("Cancelling previous mainloop")
            prev.cancel()
        }

        currentMainloop = lifecycleScope.launch {
            mainloop(args)
        }
    }

    private suspend fun mainloop(args: Args) {
        Timber.i("Began mainloop with %s", args)

        val client = LocationServices.getFusedLocationProviderClient(this)
        var currentUpdates: LocationCallback? = null

        args.activeTrack.combine(args.trackSettings) { activeTrack, trackSettings -> activeTrack to trackSettings }
            .collect { (activeTrack, config) ->
                val cachedCurrentUpdates = currentUpdates
                if (cachedCurrentUpdates != null) {
                    Timber.i("Stopping location updates")
                    client.removeLocationUpdates(cachedCurrentUpdates)
                    stopForeground(true)
                }

                if (activeTrack == null) {
                    return@collect
                }

                Timber.i("Starting location updates for %s: %s", activeTrack, config)
                startInForegroundWithNotification()

                val locationRequest = LocationRequest.create().apply {
                    interval = config.interval.inWholeMilliseconds
                    fastestInterval = FASTEST_UPDATE_INTERVAL_MILLIS
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                }
                Timber.d("Requesting location with %s", locationRequest)
                val newCurrentUpdates = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        clearTransientError()
                        lifecycleScope.launch {
                            for (loc in result.locations) {
                                val moment = TrackMoment.fromPlatform(loc)
                                args.addTrackMoment(activeTrack, moment)
                            }
                        }
                    }

                    override fun onLocationAvailability(availability: LocationAvailability) {
                        if (availability.isLocationAvailable) {
                            Timber.i("Location now available")
                            clearTransientError()
                        } else {
                            Timber.i("Location now unavailable")
                            transientError(R.string.location_unavailable)
                        }
                    }
                }
                currentUpdates = newCurrentUpdates

                try {
                    client.requestLocationUpdates(locationRequest, newCurrentUpdates, this.mainLooper)
                } catch (e: SecurityException) {
                    fatalError(R.string.fine_location_denied)
                }
            }
    }

    private var isNotifyingError = false

    private fun transientError(@StringRes msg: Int) {
        isNotifyingError = true
        val msgText = getText(msg)
        Timber.w("Transient error: %s", msgText)
        val notification = errorNotification(msgText)
        notificationManager.notify(PERSISTENT_NOTIFICATION_ID, notification)
    }

    private fun fatalError(@StringRes msg: Int) {
        val msgText = getText(msg)
        Timber.w("Transient error: %s", msgText)
        val notification = errorNotification(msgText)
        notificationManager.notify(FATAL_ERROR_NOTIFICATION_ID, notification)
        stopForeground(true)
    }

    private fun clearTransientError() {
        if (!isNotifyingError) {
            return
        }
        isNotifyingError = false

        val notification = inProgressNotificationBuilder().build()
        notificationManager.notify(PERSISTENT_NOTIFICATION_ID, notification)
    }

    private fun startInForegroundWithNotification() {
        val contentIntent = Intent(BMIntentAction.ShowActiveTrack.actionName()).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        val notification = inProgressNotificationBuilder()
            .setContentIntent(contentIntent)
            .build()
        startForeground(PERSISTENT_NOTIFICATION_ID, notification)
    }

    private fun inProgressNotificationBuilder() =
        Notification.Builder(this, BMNotificationChannel.TrackPersistent.id)
            .setContentTitle(getText(R.string.tracking))
            .setContentText(getText(R.string.tracking_in_progress))
            .setSmallIcon(R.drawable.ic_tracking)

    private fun errorNotification(msg: CharSequence) =
        Notification.Builder(this, BMNotificationChannel.Error.id)
            .setContentTitle(getText(R.string.error_tracking))
            .setContentText(msg)
            .setSmallIcon(R.drawable.ic_tracking_unavailable)
            .build()

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    private val binder = InnerBinder(this)

    private class InnerBinder(val service: TrackService) : Binder()

    companion object {
        private const val PERSISTENT_NOTIFICATION_ID = 10
        private const val FATAL_ERROR_NOTIFICATION_ID = 11

        // Receive up to 1 update per millisecond if another app has made it available
        const val FASTEST_UPDATE_INTERVAL_MILLIS = 1L

        /** Only guaranteed to live as long as the provided context. If you re-bind you will cancel
         * the mainloop running the previous args. */
        fun bind(args: Args, context: Context) {
            val intent = Intent(context, TrackService::class.java)

            context.bindService(intent, object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, innerBinder: IBinder?) {
                    val service = (innerBinder as InnerBinder).service
                    service.setup(args)
                }

                override fun onServiceDisconnected(name: ComponentName?) {}
            }, Context.BIND_AUTO_CREATE)
        }
    }
}