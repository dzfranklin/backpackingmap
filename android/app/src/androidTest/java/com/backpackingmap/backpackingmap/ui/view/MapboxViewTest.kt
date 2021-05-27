package com.backpackingmap.backpackingmap.ui.view

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.backpackingmap.backpackingmap.MainActivity
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.Style
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapboxViewTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val initialPosition = CameraPosition.Builder()
        .target(LatLng(43.7383, 7.4094))
        .zoom(5.0)
        .build()!!

    private lateinit var state: MapboxState

    @Before
    fun setUp() {
        composeTestRule.setContent {
            state = rememberMapboxState(
                initialStyle = Style.LIGHT,
                initialPosition = initialPosition
            )
            MapboxView(state)
        }
    }

    @Test
    fun registersMapWithState() {
        val map = runBlocking { state.awaitMap() }
        composeTestRule.runOnUiThread {
            assert(map.maxZoomLevel > 0)
        }
    }
}