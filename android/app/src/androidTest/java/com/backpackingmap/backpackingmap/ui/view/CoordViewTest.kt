package com.backpackingmap.backpackingmap.ui.view

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.backpackingmap.backpackingmap.MainActivity
import com.backpackingmap.backpackingmap.model.Coord
import com.backpackingmap.backpackingmap.repo.CoordinateMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoordViewTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val point = Coord(43.642567, -79.387139)

    @Test
    fun latLngMode() {
        composeTestRule.setContent {
            CoordView(point, CoordinateMode.LatLng)
        }
        composeTestRule
            .onNode(hasTestTag("CoordView"))
            .assertTextEquals("lat 43.6426, lng -79.3871")
    }

    @Test
    fun utmMode() {
        composeTestRule.setContent {
            CoordView(point, CoordinateMode.UTM)
        }
        composeTestRule
            .onNode(hasTestTag("CoordView"))
            .assertTextEquals("17T 630084 4833439")
    }
}