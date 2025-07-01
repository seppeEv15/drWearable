package com.example.drwearable.presentation.data.model

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
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
    private val _queue = MutableStateFlow<List<PlayerResponse>>(emptyList())
    val queue: StateFlow<List<PlayerResponse>> = _queue.asStateFlow()

    private val _currentPlayer = MutableStateFlow<PlayerResponse?>(null)
    val currentPlayer: StateFlow<PlayerResponse?> = _currentPlayer.asStateFlow()

    fun enQueue(player: PlayerResponse) {
        _queue.value = _queue.value + player
        updateCurrentPlayer()
    }

    fun acceptNext() {
        _queue.value = _queue.value.drop(1)
        updateCurrentPlayer()
    }

    fun denyNext() {
        _queue.value = _queue.value.drop(1)
        updateCurrentPlayer()
    }

    fun removeByPosition(position: String) {
        Log.d("PLAYER", "Before $position ${_queue.value}")
        _queue.value = _queue.value.filterNot { it.position == position }.toList()
        Log.d("PLAYER", "After $position ${_queue.value}")
        updateCurrentPlayer()
    }

    private fun updateCurrentPlayer() {
        Log.d("CURRENT", "Before ${_currentPlayer.value}")
        _currentPlayer.value = _queue.value.firstOrNull()
        Log.d("CURRENT", "AFTER ${_currentPlayer.value}")
    }

    fun clearQueue() {
        _queue.value = emptyList()
        _currentPlayer.value = null
    }
}