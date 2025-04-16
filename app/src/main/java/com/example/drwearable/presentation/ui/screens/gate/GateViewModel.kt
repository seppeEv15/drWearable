package com.example.drwearable.presentation.ui.screens.gate

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.drwearable.presentation.data.WaggledanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SessionUiState {
    data class Success(val sessionId: String, val pingColor: Color) : SessionUiState
    data class Error(val message: String) : SessionUiState
    object Loading : SessionUiState
}

class GateViewModel(
    private val repository: WaggledanceRepository
) : ViewModel() {
    private val _swipeText = MutableStateFlow("")
    val swipeText: StateFlow<String> = _swipeText.asStateFlow()

    private val _pingColor = MutableStateFlow(Color.Gray)
    val pingColor: StateFlow<Color> = _pingColor.asStateFlow()

    private val _sessionId = MutableStateFlow("")
    val sessionId: StateFlow<String> = _sessionId.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val messages: StateFlow<List<String>> get()  =_messages

    private val _connectionsStatus = MutableStateFlow("Disconnected")
    val connectionsStatus: StateFlow<String> get() = _connectionsStatus

    init {
        Log.d("SESSION_ID", "Starting API call")
        viewModelScope.launch {
            val result = repository.getSessionId()
            result.onSuccess { id ->
                Log.d("SESSION_ID", "Session ID: $id")
                _sessionId.value = id

                startSseStream()
            }.onFailure { error ->
                Log.e("SESSION_ID", "Error fetching session ID: ${error.localizedMessage}")
                _errorMessage.value = "Error: ${error.localizedMessage}"
            }
        }
    }

    fun setStatusAccepted() {
        _swipeText.value = "Accepted"
    }

    fun setStatusDenied() {
        _swipeText.value = "Denied"
    }

    fun setPingColor(color: Color) {
        _pingColor.value = color
    }

    fun startSseStream() {
        viewModelScope.launch {
            repository.startSseStream(sessionId.value).collect { message ->
                Log.d("SSE", "Received message: $message")

                _messages.value = _messages.value + message
            }
        }
    }

    // Check if needed/ how to do this
    fun stopSseStream() {

    }
}
