package com.drgt.drwearable.presentation.data.model

import android.graphics.Bitmap

data class PlayerResponse(
    val position: String,
    val playerId: Int,
    val player: Player
)

data class Player(
    val firstName: String,
    val secondName: String,
    val lastName: String,
    val lastName2: String,
    val image: Bitmap,
    val isBlacklisted: Boolean
)

data class LastPlayer(
    val fullName: String,
    val isBlacklisted: Boolean,
    val isAccepted: Boolean
)