package com.backpackingmap.backpackingmap.ui.screen.map_screen

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.IconToggleButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.backpackingmap.backpackingmap.R
import com.backpackingmap.backpackingmap.ui.theme.LocalThemeExtras

@Composable
fun MapMain(isTracking: Boolean, setIsTracking: (Boolean) -> Unit, onSelectCreate: (CreateOption) -> Unit) {
    Box(Modifier.fillMaxSize().padding(10.dp), Alignment.BottomEnd) {
        Column(horizontalAlignment = Alignment.End) {
            CreateButton(onSelectCreate, Modifier.padding(bottom = 10.dp))
            TrackingButton(isTracking, setIsTracking)
        }
    }
}

@Composable
fun CreateButton(onSelect: (CreateOption) -> Unit, modifier: Modifier) {
    val isOpen = remember { mutableStateOf(false) }

    Row(modifier) {
        if (isOpen.value) Row(Modifier.padding(end = 15.dp)) {
            CreateOptionButton(
                { onSelect(CreateOption.Point) },
                R.drawable.marker,
                R.string.add_marker
            )
            CreateOptionButton(
                { onSelect(CreateOption.Route) },
                R.drawable.ic_route,
                R.string.add_route
            )
        }

        CornerButton(
            isEnabled = isOpen.value,
            setIsEnabled = { isOpen.value = it },
            icon = R.drawable.ic_add,
            iconDescription = R.string.add,
        )
    }
}

@Composable
fun CreateOptionButton(
    onSelect: () -> Unit,
    @DrawableRes icon: Int,
    @StringRes iconDescription: Int
) {
    IconButton(
        onSelect,
        Modifier
            .padding(horizontal = 5.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colors.surface)
    ) {
        Icon(
            painterResource(icon),
            stringResource(iconDescription),
            modifier = Modifier.size(30.dp)
        )
    }
}

enum class CreateOption {
    Route,
    Point,
}

@Composable
fun TrackingButton(
    isTracking: Boolean,
    setIsTracking: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    CornerButton(
        isEnabled = isTracking,
        setIsEnabled = setIsTracking,
        icon = R.drawable.ic_track_location,
        iconDescription = R.string.track_my_location,
        modifier = modifier
    )
}

@Composable
fun CornerButton(
    isEnabled: Boolean,
    setIsEnabled: (Boolean) -> Unit,
    @DrawableRes icon: Int,
    @StringRes iconDescription: Int,
    modifier: Modifier = Modifier
) {
    IconToggleButton(
        checked = isEnabled,
        onCheckedChange = { setIsEnabled(!isEnabled) },
        modifier = modifier
            .alpha(0.7f)
            .clip(CircleShape)
            .background(MaterialTheme.colors.surface)
    ) {
        val tint = if (isEnabled) {
            LocalThemeExtras.current.mapCornerButtonEnabled
        } else {
            MaterialTheme.colors.onSurface
        }

        Icon(
            painterResource(icon),
            stringResource(iconDescription),
            modifier = Modifier.size(30.dp),
            tint = tint,
        )
    }
}
