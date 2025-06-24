package com.example.drwearable.presentation.ui.components.gate

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.font.FontWeight.Companion.W800
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import com.example.drwearable.R
import com.example.drwearable.presentation.data.model.Player
import com.example.drwearable.presentation.theme.GreenGradient
import com.example.drwearable.presentation.theme.Grey
import com.example.drwearable.presentation.theme.RedGradient
import com.example.drwearable.presentation.theme.WhiteGradient

@Composable
fun PlayerInfoContent(
    player: Player?,
    fullName: String,
    swipeText: String,
    wasBlacklisted: Boolean,
    lastPlayerBlacklistedImage: Int?
) {
    val useRedImage = swipeText.lowercase().contains("denied")
    val useGreenImage = swipeText.lowercase().contains("accepted")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x00000000))
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when {
            player == null || fullName.isBlank() -> {
                Spacer(modifier = Modifier.height(25.dp))
                Image(
                    painter = painterResource(
                        id = when {
                            useGreenImage -> R.drawable.account_user_green
                            useRedImage && wasBlacklisted -> lastPlayerBlacklistedImage ?: R.drawable.account_user_red
                            useRedImage -> R.drawable.account_user_red
                            else -> R.drawable.account_user_default
                        }
                    ),
                    contentDescription = "Player silhouette",
                    modifier = Modifier.size(60.dp)
                )
                if (swipeText.isNotBlank()) {
                    BasicText(
                        text = swipeText,
                        style = TextStyle(
                            brush = when {
                                useGreenImage -> GreenGradient
                                useRedImage -> RedGradient
                                else -> WhiteGradient
                            },
                            fontSize = 16.sp,
                            fontWeight = W800,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                } else {
                    BasicText(
                        text = "No player waiting",
                        modifier = Modifier.padding(top = 4.dp),
                        style = TextStyle(
                            color = Grey,
                            fontSize = 16.sp,
                            fontWeight = W800,
                            textAlign = TextAlign.Center
                        )
                    )
                }
                Spacer(modifier = Modifier.height(25.dp))
                Image(
                    painter = painterResource(id = R.drawable.drgt),
                    contentDescription = "drgt logo",
                    modifier = Modifier.size(width = 50.dp, height = 20.dp)
                )
            }

            else -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    BasicText(
                        text = fullName,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 40.dp),
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.SansSerif
                        )
                    )
                    if (player.isBlacklisted) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.warning_svgrepo_com),
                                contentDescription = "Blacklist Icon",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = "Blacklisted",
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = Bold,
                                    textAlign = TextAlign.Center,
                                    fontFamily = FontFamily.SansSerif
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}