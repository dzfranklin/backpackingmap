package com.backpackingmap.backpackingmap.repo

import androidx.annotation.StringRes
import com.backpackingmap.backpackingmap.R

enum class CoordinateMode(@StringRes val label: Int) {
    LatLng(R.string.latlng),
    UTM(R.string.utm);

    companion object {
        val DEFAULT = LatLng
    }
}