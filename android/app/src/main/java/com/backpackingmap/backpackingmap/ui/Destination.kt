package com.backpackingmap.backpackingmap.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.backpackingmap.backpackingmap.R

enum class Destination(
    val route: String,
    @DrawableRes val icon: Int,
    @StringRes val iconDescription: Int
) {
    Map("map", R.drawable.ic_map_screen, R.string.map)
}