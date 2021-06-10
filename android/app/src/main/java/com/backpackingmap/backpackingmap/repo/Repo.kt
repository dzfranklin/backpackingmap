package com.backpackingmap.backpackingmap.repo

import android.content.Context
import com.backpackingmap.backpackingmap.model.TrackId
import com.backpackingmap.backpackingmap.model.TrackMeta
import com.backpackingmap.backpackingmap.model.TrackMoment
import com.backpackingmap.backpackingmap.model.TrackSettings
import com.backpackingmap.backpackingmap.track_service.TrackService
import com.mapbox.mapboxsdk.camera.CameraPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class Repo private constructor(override val coroutineContext: CoroutineContext, context: Context) :
    CoroutineScope {
    // NOTE: Constructor is private to enforce getting singleton

    val defaultZoom = 12.0

    var _pos: CameraPosition? = null

    suspend fun mapPosition(): CameraPosition? {
        // TODO
        return _pos
    }

    suspend fun setMapPosition(new: CameraPosition) {
        // TODO
        _pos = new
    }

    var _cm = MutableStateFlow(CoordinateMode.LatLng)

    fun coordinateMode(): Flow<CoordinateMode> {
        // TODO
        return _cm
    }

    suspend fun setCoordinateMode(new: CoordinateMode) {
        // TODO
        _cm.value = new
    }

    @OptIn(ExperimentalTime::class)
    val _trackSettings = MutableStateFlow(
        TrackSettings(
            interval = Duration.seconds(30)
        )
    )

    fun trackSettings(): Flow<TrackSettings> {
        return _trackSettings
    }

    suspend fun setTrackSettings(new: TrackSettings) {
        _trackSettings.value = new
    }

    val _activeTrack = MutableStateFlow<TrackId?>(null)

    suspend fun beginTrack(): TrackId {
        val id = TrackId.generate()
        // TODO: Record meta
        _activeTrack.value = id
        return id
    }

    fun activeTrack(): Flow<TrackId?> {
        return _activeTrack
    }

    suspend fun addTrackMoment(track: TrackId, moment: TrackMoment) {
        // TODO
    }

    suspend fun endTrack(track: TrackId) {
        _activeTrack.value = null
    }

    init {
        TrackService.bind(
            TrackService.Args(
                trackSettings = trackSettings(),
                activeTrack = activeTrack(),
                addTrackMoment = ::addTrackMoment
            ),
            // Use application since we want this to outlive the specific context passed in
            context.applicationContext
        )
    }

    init {
        // NOTE: We use Instant, not System.nanoTime(), because we need to track across boots.
//        val startTime = Instant.now()
//        launch {
//            val tracks = TODO("Tracks where meta.isActive and last moment is before startTime")
//            for (track in tracks) {
//                endTrack(track.id)
//            }
//        }
    }

    companion object {
        @Volatile
        private var instance: Repo? = null

        /** [context] is not retained. If this is the first time we were called we use it
         * to do some one-time setup.
         */
        fun get(context: Context): Repo {
            val cached = instance
            if (cached != null) {
                return cached
            }

            synchronized(this) {
                val cachedLocked = instance
                if (cachedLocked != null) {
                    return cachedLocked
                }

                val coroutineContext = Job()
                val new = Repo(coroutineContext, context)
                instance = new
                return new
            }
        }
    }
}