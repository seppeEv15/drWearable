package com.example.drwearable.presentation.data.model

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
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
)

class PlayerQueueManager {
    @SuppressLint("MutableCollectionMutableState")
    private val _queue = mutableStateOf(ArrayDeque<PlayerResponse>())
    val queue: ArrayDeque<PlayerResponse> get() = _queue.value

    private val _currentPlayer = MutableStateFlow<PlayerResponse?>(null)
    val currentPlayer: StateFlow<PlayerResponse?> = _currentPlayer.asStateFlow()

    fun enQueue(player: PlayerResponse) {
        _queue.value.add(player)
        updateCurrentPlayer()
    }

    fun acceptNext() {
        updateCurrentPlayer()
    }

    fun denyNext() {
        updateCurrentPlayer()
    }

    fun removeByPosition(position: String) {
        val updatedQueue = _queue.value.filterNot { it.position == position }
        _queue.value = ArrayDeque(updatedQueue)
        updateCurrentPlayer()
    }

    // Make sure to not double delete from queue when self accepting them
    fun clearQueue() {
        _queue.value.clear()
        updateCurrentPlayer()
    }

    private fun updateCurrentPlayer() {
        _queue.value = ArrayDeque(_queue.value) // Trigger recomposition
        _currentPlayer.value = _queue.value.firstOrNull()
    }
}