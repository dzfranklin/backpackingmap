package com.backpackingmap.backpackingmap.ui.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.backpackingmap.backpackingmap.R
import com.backpackingmap.backpackingmap.model.Coord
import com.backpackingmap.backpackingmap.repo.CoordinateMode
import com.backpackingmap.backpackingmap.ui.theme.BackpackingMapTheme

@Composable
fun CoordDialog(
    isOpen: Boolean,
    initialCoord: Coord,
    closeWith: (Coord) -> Unit,
    mode: CoordinateMode,
    setMode: (CoordinateMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isOpen) {
        return
    }

    val value = remember(initialCoord) { mutableStateOf(initialCoord) }
    val isInvalid = remember(initialCoord) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { closeWith(initialCoord) },
        title = { Text(stringResource(R.string.edit_coordinate)) },
        text = {
            Column {
                EditCoord(value.value, { value.value = it }, { isInvalid.value = it }, mode)
                PickCoordMode(mode, setMode, enabled = !isInvalid.value)
            }
        },
        dismissButton = {
            OutlinedButton({ closeWith(initialCoord) }, enabled = !isInvalid.value) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            Button({ closeWith(value.value) }, enabled = !isInvalid.value) {
                Text(stringResource(R.string.ok))
            }
        },

        modifier = modifier,
    )
}

@Preview
@Composable
fun EditCoordDialogPreview() {
    val isOpen = remember { mutableStateOf(false) }
    val coord = remember { mutableStateOf(Coord(0.0, 0.0)) }
    val mode = remember { mutableStateOf(CoordinateMode.LatLng) }

    BackpackingMapTheme {
        Button({ isOpen.value = true }, modifier = Modifier.width(60.dp)) {}
        CoordDialog(
            isOpen = isOpen.value,
            initialCoord = coord.value,
            closeWith = {
                coord.value = it
                isOpen.value = false
            },
            mode = mode.value,
            setMode = { mode.value = it },
        )
    }
}