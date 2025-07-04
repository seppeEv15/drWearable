package com.drgt.drwearable.presentation.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Text
import com.drgt.drwearable.R

@Composable
fun errorScreen(
) {
    Text(text = stringResource(R.string.loading_failed), modifier = Modifier.padding(16.dp))
}