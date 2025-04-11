package com.example.drwearable.presentation.ui.screens.gate

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GateViewModel : ViewModel() {
    private val _statusText = MutableStateFlow("")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _pingColor = MutableStateFlow(Color.Gray)
    val pingColor: StateFlow<Color> = _pingColor.asStateFlow()

    fun setStatusAccepted() {
        _statusText.value = "Accepted"
    }

    fun setStatusDenied() {
        _statusText.value = "Denied"
    }

    // Optional: Update pingColor from elsewhere
    fun setPingColor(color: Color) {
        _pingColor.value = color
    }
}