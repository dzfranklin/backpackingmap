package com.backpackingmap.backpackingmap.ui.view

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun ErrorMsg(@StringRes text: Int) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.h6.copy(color = MaterialTheme.colors.error),
        modifier = Modifier.padding(20.dp)
    )
}