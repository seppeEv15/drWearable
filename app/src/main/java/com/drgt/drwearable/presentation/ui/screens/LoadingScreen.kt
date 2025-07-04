package com.drgt.drwearable.presentation.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material.Text
import com.drgt.drwearable.R

@Composable
fun LoadingScreen(modifier: Modifier) {
    Text(text = stringResource(R.string.loading), modifier = modifier)
}