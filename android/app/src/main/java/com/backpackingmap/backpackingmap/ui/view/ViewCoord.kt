package com.backpackingmap.backpackingmap.ui.view

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import com.backpackingmap.backpackingmap.R
import com.backpackingmap.backpackingmap.model.Coord
import com.backpackingmap.backpackingmap.repo.CoordinateMode
import com.mapbox.mapboxsdk.geometry.LatLng

@Composable
fun CoordView(point: Coord, mode: CoordinateMode, modifier: Modifier = Modifier) {
    val value = when (mode) {
        CoordinateMode.LatLng -> latLngVal(point)
        CoordinateMode.UTM -> utmVal(point)
    }
    Text(value, modifier.semantics { testTag = "CoordView" })
}

@Composable
fun latLngVal(point: Coord) =
    stringResource(R.string.lat_lng_val, point.lat, point.lng)


@Composable
fun utmVal(point: Coord): String {
    return stringResource(
        R.string.utm_val,
        point.zone,
        point.gridZone,
        point.east,
        point.north,
    )
}
