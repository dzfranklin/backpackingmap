package com.backpackingmap.backpackingmap.ui.view

import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.backpackingmap.backpackingmap.ui.Destination
import timber.log.Timber

@Composable
fun BottomBar(current: Destination, navigateTo: (Destination) -> Unit) {
    BottomNavigation {
        for (dest in Destination.values()) {
            BottomNavigationItem(
                selected = dest == current,
                onClick = {
                    navigateTo(dest)
                },
                icon = { Icon(painterResource(dest.icon), stringResource(dest.iconDescription)) },
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
            )
        }
    }
}

@Preview
@Composable
fun BottomBarPreview() {
    val current = Destination.Map
    Scaffold(
        bottomBar = {
            BottomBar(current) { Timber.i("Pretend navigating to %s", it) }
        }
    ) {
        Text("Content")
    }
}