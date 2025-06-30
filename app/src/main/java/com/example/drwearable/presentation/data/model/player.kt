package com.example.drwearable.presentation.data.model

import android.annotation.SuppressLint
import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

class PlayerQueueManager {
    @SuppressLint("MutableCollectionMutableState")
    private val _queue = MutableStateFlow<List<PlayerResponse>>(emptyList())
    val queue: StateFlow<List<PlayerResponse>> = _queue.asStateFlow()

    private val _currentPlayer = MutableStateFlow<PlayerResponse?>(null)
    val currentPlayer: StateFlow<PlayerResponse?> = _currentPlayer.asStateFlow()

    fun enQueue(player: PlayerResponse) {
        _queue.value = _queue.value + player
    }

    fun acceptNext() {
        _queue.value = _queue.value.drop(1)
    }

    fun denyNext() {
        _queue.value = _queue.value.drop(1)
    }

    fun removeByPosition(position: String) {
        _queue.value = _queue.value.filterNot { it.position == position }
    }
}