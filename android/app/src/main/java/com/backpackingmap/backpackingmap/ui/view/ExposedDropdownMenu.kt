package com.backpackingmap.backpackingmap.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.backpackingmap.backpackingmap.R
import kotlinx.coroutines.flow.collect

data class ExposedDropdownMenuItem<T>(
    val id: T,
    val text: AnnotatedString,
) {
    constructor(id: T, text: String): this(id, AnnotatedString(text))
}

@Composable
fun <T> ExposedDropdownMenu(
    label: String,
    values: List<ExposedDropdownMenuItem<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    // Implementation of https://material.io/components/menus#exposed-dropdown-menu

    val interactionSource = remember { MutableInteractionSource() }
    val focusInteractions = remember { mutableStateListOf<FocusInteraction.Focus>() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Release -> {
                    if (focusInteractions.isEmpty()) {
                        interactionSource.tryEmit(FocusInteraction.Focus())
                    }
                }
                is FocusInteraction.Focus -> focusInteractions.add(interaction)
                is FocusInteraction.Unfocus -> focusInteractions.remove(interaction.focus)
            }
        }
    }

    val collapse = remember(interactionSource, focusInteractions) {
        {
            val lastFocus = focusInteractions.lastOrNull()
            if (lastFocus != null) {
                interactionSource.tryEmit(FocusInteraction.Unfocus(lastFocus))
            }
        }
    }

    val isExpanded = derivedStateOf { !focusInteractions.isEmpty() }

    Box(modifier) {
        SubcomposeLayout { constraints ->
            val fieldPlaceables = subcompose(ExposedDropdownMenuSlot.Field) {
                ExposedDropdownMenuField(
                    label = label,
                    selected = values.find { it.id == selected }!!.text,
                    isExpanded = isExpanded.value,
                    interactionSource = interactionSource
                )
            }.map {
                it.measure(constraints)
            }

            val fieldSize = fieldPlaceables.fold(IntSize.Zero) { currentMax, placeable ->
                IntSize(
                    width = maxOf(currentMax.width, placeable.width),
                    height = maxOf(currentMax.height, placeable.height)
                )

            }

            layout(fieldSize.width, fieldSize.height) {
                fieldPlaceables.forEach { it.placeRelative(0, 0) }

                subcompose(ExposedDropdownMenuSlot.Menu) {
                    ExposedDropdownMenuDropdown(
                        values = values,
                        selected = selected,
                        onSelect = onSelect,
                        isExpanded = isExpanded.value,
                        onCollapse = collapse,
                        fieldSize = DpOffset(fieldSize.width.toDp(), fieldSize.height.toDp())
                    )
                }.forEach {
                    it.measure(constraints).place(0, 0)
                }
            }
        }
    }
}

@Preview
@Composable
fun ExposedDropdownMenuPreview() {
    val items = listOf(
        ExposedDropdownMenuItem(1, AnnotatedString("Item 1")),
        ExposedDropdownMenuItem(2, AnnotatedString("Item 2")),
        ExposedDropdownMenuItem(3, AnnotatedString("Item 3"))
    )
    val selected = remember { mutableStateOf(2) }
    ExposedDropdownMenu(
        label = "Label",
        values = items,
        selected = selected.value,
        onSelect = { selected.value = it },
        modifier = Modifier.padding(20.dp)
    )
}

enum class ExposedDropdownMenuSlot { Field, Menu }

@Composable
private fun ExposedDropdownMenuField(
    label: String,
    selected: AnnotatedString,
    isExpanded: Boolean,
    interactionSource: MutableInteractionSource
) {
    OutlinedTextField(
        TextFieldValue(selected),
        onValueChange = {},
        readOnly = true,
        textStyle = MaterialTheme.typography.body1,
        modifier = Modifier.width(IntrinsicSize.Min),
        label = {
            Text(
                label,
                color = MaterialTheme.colors.primary,
                style = MaterialTheme.typography.caption
            )
        },
        trailingIcon = {
            if (isExpanded) {
                Icon(
                    painterResource(R.drawable.ic_expand_more),
                    stringResource(R.string.expand)
                )
            } else {
                Icon(
                    painterResource(R.drawable.ic_expand_less),
                    stringResource(R.string.collapse)
                )
            }
        },
        interactionSource = interactionSource,
    )
}

@Composable
private fun <T> ExposedDropdownMenuDropdown(
    values: List<ExposedDropdownMenuItem<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    isExpanded: Boolean,
    onCollapse: () -> Unit,
    fieldSize: DpOffset
) {
    DropdownMenu(
        expanded = isExpanded,
        onDismissRequest = onCollapse,
        offset = DpOffset(0.dp, fieldSize.y)
    ) {
        for (value in values) {
            DropdownMenuItem(
                onClick = {
                    onSelect(value.id)
                    onCollapse()
                }, contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    Modifier
                        .width(fieldSize.x)
                        .background(Color.Black.copy(alpha = if (value == selected) 0.12f else 0f))
                ) {
                    Text(
                        value.text,
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
