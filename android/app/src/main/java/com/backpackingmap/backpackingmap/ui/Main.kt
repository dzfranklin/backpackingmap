package com.backpackingmap.backpackingmap.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.backpackingmap.backpackingmap.repo.Repo
import com.backpackingmap.backpackingmap.ui.screen.map_screen.MapScreen
import com.backpackingmap.backpackingmap.ui.theme.BackpackingMapTheme

@Composable
fun Main(ensureFineLocation: suspend () -> Boolean) {
    val nav = rememberNavController()
    val navigateTo = { dest: Destination -> nav.navigate(dest.route) };

    val repo = remember { Repo() }

    BackpackingMapTheme {
        NavHost(nav, startDestination = Destination.Map.route) {
            composable(Destination.Map.route) {
                MapScreen(repo, navigateTo, ensureFineLocation)
            }
        }
    }
}