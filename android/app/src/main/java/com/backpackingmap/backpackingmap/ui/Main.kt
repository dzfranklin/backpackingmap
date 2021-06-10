package com.backpackingmap.backpackingmap.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.backpackingmap.backpackingmap.repo.Repo
import com.backpackingmap.backpackingmap.ui.screen.map.MapScreen
import com.backpackingmap.backpackingmap.ui.screen.track.TrackScreen
import com.backpackingmap.backpackingmap.ui.theme.BackpackingMapTheme
import com.backpackingmap.backpackingmap.ui.view.BottomBar
import com.backpackingmap.backpackingmap.ui.view.bottom_bar.BottomBarDestination

@Composable
fun Main(startingAction: BMIntentAction?, ensureFineLocation: suspend () -> Boolean) {
    val context = LocalContext.current
    val nav = rememberNavController()
    BMNotificationChannel.ensureAllCreated(context)

    val repo = remember { Repo.get(context) }

    LaunchedEffect(startingAction) {
        if (startingAction != null) {
            when (startingAction) {
                BMIntentAction.ShowActiveTrack -> nav.navigate(Route.Track.name)
            }
        }
    }

    BackpackingMapTheme {
        NavHost(nav, startDestination = Route.Map.name) {
            composable(Route.Map.name) {
                MapScreen(repo, makeBottomBar(BottomBarDestination.Map, nav), ensureFineLocation)
            }
            composable(Route.Track.name) {
                TrackScreen(repo, makeBottomBar(BottomBarDestination.Track, nav))
            }
        }
    }
}

private fun makeBottomBar(current: BottomBarDestination, nav: NavController): @Composable () -> Unit {
    return {
        BottomBar(current) { nav.navigate(it.route.name) }
    }
}