package com.backpackingmap.backpackingmap.track_service

import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.backpackingmap.backpackingmap.model.TrackId
import com.backpackingmap.backpackingmap.model.TrackMoment
import com.backpackingmap.backpackingmap.model.TrackSettings
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import kotlin.time.Duration

data class MomentData(
    val track: TrackId,
    val moment: TrackMoment
)

@RunWith(AndroidJUnit4::class)
class TrackServiceTest {
    // NOTE: This seems to be capped around 1sec. Going slower won't necessarily cause a visible
    // impact because you also have to consider fastestInterval
    private val trackSettings = MutableStateFlow(TrackSettings(interval = Duration.milliseconds(1)))

    private val activeTrack = MutableStateFlow<TrackId?>(null)

    private val moments = Channel<MomentData>(Channel.UNLIMITED)

    @Before
    fun setUp() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        TrackService.bind(
            TrackService.Args(
                trackSettings = trackSettings,
                activeTrack = activeTrack,
                addTrackMoment = { active, moment ->
                    moments.send(MomentData(active, moment))
                }
            ),
            targetContext
        )
    }

    @Test
    fun oneActive() {
        val expectedTrack = TrackId.generate()
        activeTrack.value = expectedTrack

        // Ensure we receive one
        runBlocking {
            moments.receive()
        }
    }

    @Test
    fun oneCancelled() {
        activeTrack.value = TrackId.generate()
        runBlocking {
            moments.receive()
        }

        activeTrack.value = null
        runBlocking {
            var timedOut = false
            try {
                withTimeout(Duration.seconds(5)) {
                    moments.receive()
                }
            } catch (e: TimeoutCancellationException) {
                timedOut = true
            }
            assertThat(timedOut, equalTo(true))
        }
    }

    @Test
    fun rebind() {
        val id1 = TrackId.generate()
        activeTrack.value = id1
        runBlocking {
            moments.receive()
        }

        val id2 = TrackId.generate()
        activeTrack.value = id2
        runBlocking {
            assertThat(moments.receive().track, equalTo(id2))
        }
    }
}