package com.backpackingmap.backpackingmap.ui.view

import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.backpackingmap.backpackingmap.ui.view.bottom_bar.BottomBarDestination
import timber.log.Timber

@Composable
fun BottomBar(current: BottomBarDestination, navigateTo: (BottomBarDestination) -> Unit) {
    BottomNavigation {
        for (dest in BottomBarDestination.values()) {
            BottomNavigationItem(
                selected = dest == current,
                onClick = {
                    if (dest != current) {
                        navigateTo(dest)
                    }
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
    val current = BottomBarDestination.Map
    Scaffold(
        bottomBar = {
            BottomBar(current) { Timber.i("Pretend navigating to %s", it) }
        }
    ) {
        Text("Content")
    }
}