package com.backpackingmap.backpackingmap.repo

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.backpackingmap.backpackingmap.model.TrackId
import com.backpackingmap.backpackingmap.model.TrackMoment
import com.backpackingmap.backpackingmap.model.TrackConfig
import com.backpackingmap.backpackingmap.repo.db.Db
import com.backpackingmap.backpackingmap.track_service.TrackService
import com.mapbox.mapboxsdk.camera.CameraPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

/** Note: Expected to be a singleton. Using the constructor directly is undefined */
class Repo @VisibleForTesting internal constructor(
    override val coroutineContext: CoroutineContext,
    db: Db,
    bindTrackService: (TrackService.Args) -> Unit,
) : CoroutineScope {
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

    val _trackSettings = MutableStateFlow(
        TrackConfig(
            interval = Duration.seconds(30)
        )
    )

    fun trackSettings(): Flow<TrackConfig> {
        return _trackSettings
    }

    suspend fun setTrackSettings(new: TrackConfig) {
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
        bindTrackService(
            TrackService.Args(
                trackConfig = trackSettings(),
                activeTrack = activeTrack(),
                addTrackMoment = ::addTrackMoment
            )
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
                val new = Repo(
                    coroutineContext = coroutineContext,
                    db = Db(coroutineContext, context),
                    bindTrackService = { args ->
                        // Use application since we want this to outlive the specific context passed in
                        TrackService.bind(args, context.applicationContext)
                    }
                )

                instance = new
                return new
            }
        }
    }
}