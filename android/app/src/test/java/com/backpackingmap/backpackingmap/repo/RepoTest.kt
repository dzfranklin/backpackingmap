package com.backpackingmap.backpackingmap.repo

import app.cash.turbine.test
import com.backpackingmap.backpackingmap.model.TrackConfig
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.time.Duration

class RepoTest : CoroutineScope {
    override val coroutineContext = Job()

    private val subject = Repo(
        coroutineContext = Job(),
        db = mockk(),
        bindTrackService = { /* noop */ }
    )

    @Test
    fun coordinateMode() = runBlocking {
        subject.coordinateMode().test {
            assertEquals(CoordinateMode.LatLng, expectItem())
            subject.setCoordinateMode(CoordinateMode.UTM)
            assertEquals(CoordinateMode.UTM, expectItem())
        }
    }

    @Test
    fun trackSettings() = runBlocking {
        subject.trackSettings().test {
            val initialTrackSettings = expectItem()
            assertEquals(
                TrackConfig(
                    interval = Duration.seconds(30)
                ), initialTrackSettings
            )
            subject.setTrackSettings(initialTrackSettings.copy(interval = Duration.milliseconds(42)))
            assertEquals(42, expectItem().interval.inWholeMilliseconds)
        }
    }
}