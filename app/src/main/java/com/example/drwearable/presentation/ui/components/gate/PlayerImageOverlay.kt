package com.example.drwearable.presentation.ui.components.gate

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.drwearable.presentation.theme.BlackCustomGradient
import com.example.drwearable.presentation.theme.BlacklistGradient

@Composable
fun PlayerImageOverlay(imageBitmap: ImageBitmap?, isBlacklisted: Boolean) {
    Box {
        imageBitmap?.let {
            Image(
                bitmap = it,
                contentDescription = "Background Player Image",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .matchParentSize()
                    .background(if (isBlacklisted) BlacklistGradient else BlackCustomGradient)
            )
        }
    }
}