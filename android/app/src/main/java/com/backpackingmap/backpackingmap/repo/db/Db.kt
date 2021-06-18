package com.backpackingmap.backpackingmap.repo.db

import android.content.Context
import com.backpackingmap.backpackingmap.MainDatabase
import com.backpackingmap.backpackingmap.model.TrackMeta
import com.backpackingmap.backpackingmap.model.TripMeta
import com.backpackingmap.backpackingmap.repo.CoordinateMode
import com.backpackingmap.backpackingmap.repo.db.adapter.TableAdapters
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrDefault
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class Db(override val coroutineContext: CoroutineContext, context: Context) : CoroutineScope {
    private val driver = AndroidSqliteDriver(
        schema = MainDatabase.Schema,
        context = context,
        name = DB_NAME,
        factory = RequerySQLiteOpenHelperFactory()
    )

    private val db = MainDatabase(
        driver = driver,
        trackMetaTableAdapter = TableAdapters.trackMeta,
        tripMetaTableAdapter = TableAdapters.tripMeta,
        coordinateModeTableAdapter = TableAdapters.coordinateMode,
    )

    fun coordinateMode(): Flow<CoordinateMode> =
        db.coordinateModeQueries.get().asFlow().mapToOneOrDefault(CoordinateMode.DEFAULT)

    suspend fun setCoordinateMode(mode: CoordinateMode): Unit = withContext(Dispatchers.IO) {
        db.coordinateModeQueries.set(mode)
    }

    suspend fun createTrip(trip: TripMeta): Unit = withContext(Dispatchers.IO) {
        db.tripQueries.insert(trip.id, trip.name)
    }

    suspend fun createTrack(track: TrackMeta) = withContext(Dispatchers.IO) {
        db.trackQueries.insert(track.id, track.trip, track.name)
    }

    companion object {
        // Keep in sync with build.gradle.kts sqldelight block
        private const val DB_NAME = "backpackingmap_main.db"
    }
}