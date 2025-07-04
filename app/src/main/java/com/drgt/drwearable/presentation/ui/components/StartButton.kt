package com.drgt.drwearable.presentation.ui.components

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text

@Composable
fun StartButton() {
    Button(
        onClick = {  Log.d("ROUTE", "TO GATES") },
        modifier = Modifier
            .padding(25.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        enabled = true,
    ) {
        Text(text = "Start")
    }
}

@Preview
@Composable
fun StartButtonPreview() {
    StartButton()
}