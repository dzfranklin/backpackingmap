package com.backpackingmap.backpackingmap.ui.view.bottom_bar

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.backpackingmap.backpackingmap.R
import com.backpackingmap.backpackingmap.ui.Route

enum class BottomBarDestination(
    val route: Route,
    @DrawableRes val icon: Int,
    @StringRes val iconDescription: Int
) {
    Map(Route.Map, R.drawable.ic_map_screen, R.string.map),
    Track(Route.Track, R.drawable.ic_track_location, R.string.track_location)
}