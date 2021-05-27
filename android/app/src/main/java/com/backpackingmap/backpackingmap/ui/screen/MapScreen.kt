package com.backpackingmap.backpackingmap.ui.screen

import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.backpackingmap.backpackingmap.repo.Repo
import com.backpackingmap.backpackingmap.ui.Destination
import com.backpackingmap.backpackingmap.ui.view.BottomBar

@Composable
fun MapScreen(nav: NavController, repo: Repo) {
    Scaffold(
        bottomBar = { BottomBar(Destination.Map) { nav.navigate(it.route)} }
    ) {

    }
}