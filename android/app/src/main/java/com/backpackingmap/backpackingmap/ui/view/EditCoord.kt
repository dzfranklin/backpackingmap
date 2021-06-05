package com.backpackingmap.backpackingmap.ui.view

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.backpackingmap.backpackingmap.R
import com.backpackingmap.backpackingmap.model.Coord
import com.backpackingmap.backpackingmap.model.Hemisphere
import com.backpackingmap.backpackingmap.repo.CoordinateMode
import com.backpackingmap.backpackingmap.ui.theme.BackpackingMapTheme
import kotlinx.coroutines.flow.collect

@Composable
fun EditCoord(
    value: Coord,
    setValue: (Coord) -> Unit,
    isInvalid: (Boolean) -> Unit,
    mode: CoordinateMode,
    modifier: Modifier = Modifier,
) {
    when (mode) {
        CoordinateMode.LatLng -> EditLatLng(value, setValue, isInvalid, modifier)
        CoordinateMode.UTM -> EditUtm(value, setValue, isInvalid, modifier)
    }
}

@Composable
fun EditUtm(
    value: Coord,
    setValue: (Coord) -> Unit,
    isInvalid: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val zone = remember(value) { mutableStateOf(value.zone.toString()) }
    val hemi = remember(value) { mutableStateOf(value.hemi) }
    val north = remember(value) { mutableStateOf(metersToDisplay(value.north)) }
    val east = remember(value) { mutableStateOf(metersToDisplay(value.east)) }
    val isErr = remember(value) { mutableStateOf(false) }

    LaunchedEffect(zone, hemi, north, east) {
        snapshotFlow {
            try {
                val zoneD = zone.value.toInt()
                val northD = north.value.toDouble()
                val eastD = east.value.toDouble()
                Coord(hemi.value, zoneD, eastD, northD)
            } catch (e: Exception) {
                null
            }
        }.collect {
            if (it == null) {
                isErr.value = true
                isInvalid(true)
            } else {
                setValue(it)
                isErr.value = false
                isInvalid(false)
            }
        }
    }

    val borderColor = if (isErr.value) {
        MaterialTheme.colors.error
    } else {
        Color.Transparent
    }

    Column(
        modifier
            .border(1.dp, borderColor, RoundedCornerShape(3.dp))
            .padding(10.dp)
    ) {
        Row(Modifier.padding(bottom = 14.dp)) {
            OutlinedTextField(
                zone.value,
                { zone.value = it },
                label = { Text(stringResource(R.string.zone)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .weight(1f, false),
            )

            ExposedDropdownMenu(
                stringResource(R.string.hemisphere),
                listOf(
                    ExposedDropdownMenuItem(
                        Hemisphere.North,
                        stringResource(Hemisphere.North.label)
                    ),
                    ExposedDropdownMenuItem(
                        Hemisphere.South,
                        stringResource(Hemisphere.South.label)
                    )
                ),
                hemi.value,
                { hemi.value = it },
                modifier = Modifier.weight(1.5f, false),
            )
        }

        OutlinedTextField(
            east.value,
            { east.value = it },
            label = { Text(stringResource(R.string.easting)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        OutlinedTextField(
            north.value,
            { north.value = it },
            label = { Text(stringResource(R.string.northing)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.padding(bottom = 14.dp),
        )
    }
}

@Preview
@Composable
fun EditUtmPreview() {
    val value = remember { mutableStateOf<Coord>(Coord(0.0, 0.0)) }
    BackpackingMapTheme {
        Surface {
            EditUtm(value.value, { value.value = it }, {})
        }
    }
}

fun metersToDisplay(value: Double) =
    String.format("%.1f", value)

@Composable
fun EditLatLng(
    value: Coord,
    setValue: (Coord) -> Unit,
    isInvalid: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val lat = remember(value) { mutableStateOf(decimalDegreesToDisplay(value.lat)) }
    val lng = remember(value) { mutableStateOf(decimalDegreesToDisplay(value.lng)) }
    val isErr = remember(lng) { mutableStateOf(false) }

    LaunchedEffect(lat, lng) {
        snapshotFlow {
            try {
                Coord(lat.value.toDouble(), lng.value.toDouble())
            } catch (e: Exception) {
                null
            }
        }.collect {
            if (it == null) {
                isErr.value = true
                isInvalid(true)
            } else {
                setValue(it)
                isErr.value = false
                isInvalid(false)
            }
        }
    }

    val borderColor = if (isErr.value) {
        MaterialTheme.colors.error
    } else {
        Color.Transparent
    }

    Column(
        modifier
            .border(1.dp, borderColor, RoundedCornerShape(3.dp))
            .padding(10.dp)
    ) {
        OutlinedTextField(
            lat.value,
            { lat.value = it },
            label = { Text(stringResource(R.string.latitude)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.padding(bottom = 14.dp),
        )

        OutlinedTextField(
            lng.value,
            { lng.value = it },
            label = { Text(stringResource(R.string.longitude)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
    }
}

@Preview
@Composable
fun EditLatLngPreview() {
    val value = remember { mutableStateOf(Coord(0.0, 0.0)) }
    BackpackingMapTheme {
        Surface {
            EditLatLng(value.value, { value.value = it }, {})
        }
    }
}

fun decimalDegreesToDisplay(value: Double) =
    String.format("%.4f", value)
