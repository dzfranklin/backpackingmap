package com.backpackingmap.backpackingmap.ui.screen.map_screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.backpackingmap.backpackingmap.repo.Repo
import com.backpackingmap.backpackingmap.ui.view.MapboxState
import com.backpackingmap.backpackingmap.ui.view.TouchArea


@Composable
fun MapCreateRoute(repo: Repo, mapbox: MapboxState) {
    DisposableEffect(mapbox) {
        val touch = TouchArea.Entire(
            id = TOUCH_AREA,
            onDrag = TouchArea.OnDrag(
                onStart = { event, latlng -> false },
                onDrag = { event, latlng -> false },
                onCancel = { },
                onEnd = { event, latlng -> false },
            )
        )
        mapbox.registerTouchArea(touch)

        onDispose {
            mapbox.deregisterTouchArea(TOUCH_AREA)
        }
    }
}

private const val TOUCH_AREA = "map_create_route_touch_area"