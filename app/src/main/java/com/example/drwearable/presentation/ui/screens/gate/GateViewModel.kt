package com.example.drwearable.presentation.ui.screens.gate

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.drwearable.presentation.data.WaggledanceRepository
import com.example.drwearable.presentation.data.model.GateResponse
import com.example.drwearable.presentation.data.model.Player
import com.example.drwearable.presentation.data.model.PlayerResponse
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
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

    private val _connectionsStatus = MutableStateFlow("Disconnected")
    val connectionsStatus: StateFlow<String> get() = _connectionsStatus

    private val _playerResponse = MutableStateFlow<PlayerResponse?>(null)
    val playerResponse: StateFlow<PlayerResponse?> = _playerResponse.asStateFlow()

    private val _gateResponse = MutableStateFlow<GateResponse?>(null)
    val gateResponse: StateFlow<GateResponse?> = _gateResponse.asStateFlow()

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
                if (message.contains("drMemberCPPlayerData")) {
                    val payload = getPayload(message)
                    if (payload?.get("hasData")?.asBoolean == true) {
                        val playerObj = payload.getAsJsonObject("player")

                        val response = PlayerResponse(
                            position = payload.get("position").asString,
                            playerId = payload.get("playerId").asInt,
                            player = Player(
                                firstName = playerObj?.get("firstName")?.asString ?: "",
                                secondName = playerObj?.get("secondName")?.asString ?: "",
                                lastName = playerObj?.get("lastName")?.asString ?: "",
                                lastName2 = playerObj?.get("lastName2")?.asString ?: ""
                            )
                        )

                        _playerResponse.value = response
                        Log.d("SSE", "drMemberCPPlayerData: $response")
                    }
                }

                if (message.contains("drMemberCPGateArray")) {
                    val payload = getPayload(message)
                    val list = getList(payload.toString())

                    val gate = list?.firstOrNull()?.asJsonObject

                    val response = GateResponse(
                        position = gate?.get("position")?.asString ?: "",
                        state = gate?.get("state")?.asString ?: ""
                    )

                    _gateResponse.value = response
                    Log.d("SSE", "drMemberCPGateArray: $response")
                }
            }
        }
    }

    fun getPayload(message: String): JsonObject? {
        return try {
            val jsonParser = JsonParser()
            val jsonObject = jsonParser.parse(message).asJsonObject
            jsonObject.getAsJsonObject("payload")
        } catch (e: Exception) {
            Log.e("SSE", "Failed to parse payload", e)
            null
        }
    }

    fun getList(message: String): JsonArray? {
        return try {
            val jsonParser = JsonParser()
            val jsonElement = jsonParser.parse(message)
            val jsonObject = jsonElement.asJsonObject
            jsonObject.getAsJsonArray("list")
        } catch (e: Exception) {
            Log.e("SSE", "Failed to parse payload", e)
            null
        }
    }

    // Check if needed/ how to do this
    fun stopSseStream() {

    }
}
