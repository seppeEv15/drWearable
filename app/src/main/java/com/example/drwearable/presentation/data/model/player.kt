package com.example.drwearable.presentation.data.model

data class PlayerResponse(
    val position: String,
    val playerId: Int,
    val player: Player
)

data class Player(
    val firstName: String,
    val secondName: String,
    val lastName: String,
    val lastName2: String
)