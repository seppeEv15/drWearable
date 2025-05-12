package com.example.drwearable.presentation.ui.components

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.example.drwearable.R

@Composable
fun SlideDecisionButton(
    modifier: Modifier = Modifier,
    onAccept: () -> Unit,
    onDeny: () -> Unit,
    iconSize: Dp = 30.dp,
    denyAcceptIconSize: Dp = 75.dp,
    maxSlideDistance: Dp = 100.dp
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val maxOffsetPx = with(LocalDensity.current) { maxSlideDistance.toPx() }

    Box(
        modifier = modifier
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF00C853), Color.Black, Color(0xFFD50000))
                ),
                shape = RoundedCornerShape(50)
            )
            .border(1.dp, Color.Cyan, RoundedCornerShape(50))
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
          modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // TODO: Icons
            Image(painter = painterResource(id = R.drawable.accept), contentDescription = "Accept", modifier = Modifier.size(denyAcceptIconSize))
            Spacer(modifier = Modifier.width(40.dp))
            Image(painter = painterResource(id = R.drawable.deny), contentDescription = "Deny", modifier = Modifier.size(denyAcceptIconSize))
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .size(60.dp)
                .clip(CircleShape)
                .background(Color.Black)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            offsetX.snapTo((offsetX.value + delta).coerceIn(-maxOffsetPx, maxOffsetPx))
                        }
                    },
                    onDragStopped = {
                        scope.launch {
                            when {
                                offsetX.value > maxOffsetPx * 0.7f -> {
                                    onAccept()
                                    offsetX.animateTo(0f)
                                }
                                offsetX.value < -maxOffsetPx * 0.7f -> {
                                    onDeny()
                                    offsetX.animateTo(0f)
                                }
                                else -> offsetX.animateTo(0f)
                            }
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.Center ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Green,
                    modifier = Modifier
                        .size(iconSize)
                        .rotate(180f))
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Red, modifier = Modifier.size(iconSize))
            }
        }
    }
}

@Preview
@Composable
fun SlideDecisionButtonPreview() {
    SlideDecisionButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        onAccept = { Log.d("SlideButton", "Accepted!") },
        onDeny = { Log.d("SlideButton", "Denied!") }
    )
}