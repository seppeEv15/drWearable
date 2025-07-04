package com.drgt.drwearable.presentation.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.W800
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import com.drgt.drwearable.presentation.theme.RedGradient

@Composable
fun GradientText(
    text: String,
    brush: Brush,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign = TextAlign.Start
) {
    Text(
        text = buildAnnotatedString {
            withStyle(
                style = SpanStyle(
                    brush = brush,
                    fontSize = fontSize,
                    fontWeight = fontWeight
                )
            ) {
                append(text)
            }
        },
        modifier = modifier,
        style = TextStyle(textAlign = textAlign)
    )
}

@Preview
@Composable
fun GradientTextPreview() {
    GradientText(
        text = "TEST",
        modifier = Modifier
            .fillMaxWidth(),
        brush = RedGradient,
        fontSize = 16.sp,
        fontWeight = W800,
        textAlign = TextAlign.Center,
    )
}