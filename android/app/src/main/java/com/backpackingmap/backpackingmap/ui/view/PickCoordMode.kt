package com.backpackingmap.backpackingmap.ui.view

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.backpackingmap.backpackingmap.repo.CoordinateMode
import com.backpackingmap.backpackingmap.ui.theme.BackpackingMapTheme

@Composable
fun PickCoordMode(
    mode: CoordinateMode,
    setMode: (CoordinateMode) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    ) {
    Row(
        modifier
            .selectableGroup()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (opt in CoordinateMode.values()) {
            val isSelected = (opt == mode)
            Row(
                Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .selectable(
                        selected = isSelected,
                        onClick = { setMode(opt) },
                        role = Role.RadioButton,
                        enabled = enabled
                    )
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = isSelected, onClick = null, enabled = enabled)
                Text(
                    stringResource(opt.label),
                    style = MaterialTheme.typography.button,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Preview
@Composable
fun CoordModeSelectPreview() {
    val mode = remember { mutableStateOf(CoordinateMode.LatLng) }
    BackpackingMapTheme {
        PickCoordMode(mode.value, { mode.value = it })
    }
}