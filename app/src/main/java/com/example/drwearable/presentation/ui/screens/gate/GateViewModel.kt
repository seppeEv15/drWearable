package com.example.drwearable.presentation.ui.screens.gate

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.drwearable.presentation.data.WaggledanceRepository
import com.example.drwearable.presentation.data.model.GateAccessPayload
import com.example.drwearable.presentation.data.model.GateResponse
import com.example.drwearable.presentation.data.model.GateState
import com.example.drwearable.presentation.data.model.Player
import com.example.drwearable.presentation.data.model.PlayerQueueManager
import com.example.drwearable.presentation.data.model.PlayerResponse
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface SessionUiState {
    data class Success(val sessionId: String, val pingColor: Color) : SessionUiState
    data class Error(val message: String) : SessionUiState
    object Loading : SessionUiState
}

class GateViewModel(
    private val repository: WaggledanceRepository
) : ViewModel() {
    private val queueManager = PlayerQueueManager()

    val currentPlayer = queueManager.currentPlayer

    val queue = queueManager.queue

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

    private val _gateResponse = MutableStateFlow<GateResponse?>(null)
    val gateResponse: StateFlow<GateResponse?> = _gateResponse.asStateFlow()

    val isSseConnected: StateFlow<Boolean> = repository.isSseConnected

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

    /**
     * Accept a player
     */
    fun setStatusAccepted() {
        viewModelScope.launch {
            try {
                val response = repository.sendAccessEvent(
                    sessionId = sessionId.value.toString(),
                    payload =  GateAccessPayload(position = currentPlayer.value?.position.toString(), isAccessGranted = true)
                )

                if (response.isSuccessful) {
                    queueManager.acceptNext()
                    _swipeText.value = "Accepted"

                    viewModelScope.launch {
                        delay(5000)
                        _swipeText.value = ""
                    }
                } else {
                    _swipeText.value = "Failed to accept: ${response.code()}"
                    Log.e("GateViewModel", "API error: ${response.errorBody()?.string()}")
                }

            } catch (e: Exception) {
                _swipeText.value = "Error sending acceptance"
                Log.e("GateViewModel", "Network error: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Deny a player
     */
    fun setStatusDenied() {
        viewModelScope.launch {
            try {
                val response = repository.sendAccessEvent(
                    sessionId = sessionId.value.toString(),
                    payload = GateAccessPayload(position = currentPlayer.value?.position.toString(), isAccessGranted = false)
                )

                if (response.isSuccessful) {
                    queueManager.denyNext()
                    _swipeText.value = "Denied"

                    viewModelScope.launch {
                        delay(5000)
                        _swipeText.value = ""
                    }
                } else {
                    _swipeText.value = "Failed to deny: ${response.code()}"
                    Log.e("GateViewModel", "API error: ${response.errorBody()?.string()}")
                }

            } catch (e: Exception) {
                _swipeText.value = "Error sending denial"
                Log.e("GateViewModel", "Network error: ${e.localizedMessage}")
            }
        }
    }

    fun setPingColor(color: Color) {
        _pingColor.value = color
    }

    /**
     * - Start SSE stream
     * - listen to the messages and handle them (player & gate)
     */
    fun startSseStream() {
        viewModelScope.launch {
            try {
                repository.startSseStream(sessionId.value)
                    .collect { message ->
                        when {
                            message.contains("drMemberCPPlayerData") -> handlePlayerData(message)
                            message.contains("drMemberCPGateArray") -> handleGateData(message)
                        }
                    }
            } catch (e: Exception) {
                Log.e("SSE", "Error in SSE stream: ${e.localizedMessage}")
                _errorMessage.value = "SSE error: ${e.localizedMessage}"
            }
        }
        startConnectionMonitor()
    }

    // TODO: check if this is good, or if it needs to be changed to the original methode (checking how long it has been since the last message)
    private fun startConnectionMonitor() {
        viewModelScope.launch {
            while (isActive) {
                delay(10_000L) // 10 seconds
                if (!repository.isSseConnected.value) {
                    Log.w("SSE", "WaggleDance connection lost")
                }
            }
        }
    }

    private fun handlePlayerData(message: String) {
        val payload = getPayload(message)
        if (payload?.get("hasData")?.asBoolean == true) {
            val playerObj = payload.getAsJsonObject("player")
            val passphotosArray = payload.getAsJsonArray("passphotos")
            val firstPhotoObject = passphotosArray[0].asJsonObject
            val base64ImageData = firstPhotoObject.get("data").asString

            val imageBytes = Base64.decode(base64ImageData, Base64.DEFAULT)

            // TODO: verify these safe calls, cause it did not work
            val response = PlayerResponse(
                position = payload.get("position")?.takeIf { it !is JsonNull }?.asString ?: "",
                playerId = payload.get("playerId")?.takeIf { it !is JsonNull }?.asInt ?: -1,
                player = Player(
                    firstName = playerObj?.get("firstName")?.asString.orEmpty(),
                    secondName = playerObj?.get("secondName")?.asString.orEmpty(),
                    lastName = playerObj?.get("lastName")?.asString.orEmpty(),
                    lastName2 = playerObj?.get("lastName2")?.asString.orEmpty(),
                    image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                )
            )

            onPlayerScanned(response)
            Log.d("SSE", "drMemberCPPlayerData: $response")
//            Log.d("Base64Image", base64ImageData.take(100))
        }
    }

    /**
     * Handle gate messages:
     * - Remove accepted or denied players form the queue
     */
    private fun handleGateData(message: String) {
        val payload = getPayload(message)
        val list = getList(payload.toString())
        val gate = list?.firstOrNull()?.asJsonObject
        val gateState = gate?.get("state")?.asString.orEmpty()
        val gatePosition = gate?.get("position")?.asString.orEmpty()

        if (gateState == GateState.ACCESS_GRANTED.value || gateState == GateState.ACCESS_DENIED.value ) {
            queueManager.removeByPosition(gatePosition)
        }

        val response = GateResponse(
            position = gatePosition,
            state = gateState
        )

        _gateResponse.value = response
        Log.d("SSE", "drMemberCPGateArray: $response")
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

    fun onPlayerScanned(player: PlayerResponse) {
        queueManager.enQueue(player)
    }

    // Check if needed/ how to do this
    fun stopSseStream() {

    }
}
