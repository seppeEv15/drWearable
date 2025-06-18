package com.example.drwearable.presentation.ui.screens.gate

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.drwearable.presentation.data.WaggledanceRepository
import com.example.drwearable.presentation.data.model.GateAccessPayload
import com.example.drwearable.presentation.data.model.GateResponse
import com.example.drwearable.presentation.data.model.GateState
import com.example.drwearable.presentation.data.model.Player
import com.example.drwearable.presentation.data.model.PlayerQueueManager
import com.example.drwearable.presentation.data.model.PlayerResponse
import com.google.gson.JsonNull
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface SessionUiState {
    data class Success(val sessionId: String) : SessionUiState
    data class Error(val message: String) : SessionUiState
    object Loading : SessionUiState
}

sealed class BorderState {
    object Neutral : BorderState()
    object Accepted : BorderState()
    object Denied : BorderState()
}

class GateViewModel(
    private val repository: WaggledanceRepository
) : ViewModel() {
    private val queueManager = PlayerQueueManager()

    val currentPlayer = queueManager.currentPlayer

    private val _sessionState = MutableStateFlow<SessionUiState>(SessionUiState.Loading)
    val sessionState: StateFlow<SessionUiState> = _sessionState.asStateFlow()

    private val _swipeText = MutableStateFlow("")
    val swipeText: StateFlow<String> = _swipeText.asStateFlow()

    private val _sessionId = MutableStateFlow("")
    val sessionId: StateFlow<String> = _sessionId.asStateFlow()

    private val _gateResponse = MutableStateFlow<GateResponse?>(null)
    val gateResponse: StateFlow<GateResponse?> = _gateResponse.asStateFlow()

    private var lastAliveTimestamp = System.currentTimeMillis()
    private val _isConnectionAlive = MutableStateFlow(true)
    val isConnectionAlive: StateFlow<Boolean> = _isConnectionAlive.asStateFlow()

    private val _connectionsStatus = MutableStateFlow("Disconnected")
    val connectionsStatus: StateFlow<String> get() = _connectionsStatus

    private val _borderState = MutableStateFlow<BorderState>(BorderState.Neutral)
    val borderState: StateFlow<BorderState> = _borderState.asStateFlow()

    val isSseConnected: StateFlow<Boolean> = repository.isSseConnected

    private var retryingSession = false

    private var monitorJob: Job? = null

    init {
        initSession()
    }

    private fun initSession() {
        viewModelScope.launch {
            _sessionState.value = SessionUiState.Loading
            _connectionsStatus.value = "Initializing"
            retryingSession = false
            val result = repository.getSessionId()
            result.onSuccess { id ->
                Log.d("SESSION_ID", "Session ID: $id")
                _sessionId.value = id
                _sessionState.value = SessionUiState.Success(id)
                startSseStream()
            }
            result.onFailure { error ->
                Log.e("SESSION_ID", "Error fetching session ID: ${error.localizedMessage}")
                _sessionState.value = SessionUiState.Error(error.localizedMessage ?: "Unknown error")
                scheduleRetrySession()
            }
        }
    }

    private fun scheduleRetrySession() {
        if (!retryingSession) {
            retryingSession = true
            viewModelScope.launch {
                while (retryingSession && !isConnectionAlive.value) {
                    _connectionsStatus.value = "Retrying..."
                    val result = repository.getSessionId()
                    result.onSuccess { id ->
                        _sessionId.value = id
                        retryingSession = false
                        startSseStream()
                    }
                    result.onFailure {
                        Log.e("RETRY_SESSION", "Retry failed: ${it.localizedMessage}")
                    }

                    delay(10_000)
                }
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
                    _borderState.value = BorderState.Accepted

                    delay(3000)
                    _borderState.value = BorderState.Neutral
                    _swipeText.value = ""
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
                    _borderState.value = BorderState.Denied

                    delay(3000)
                    _borderState.value = BorderState.Neutral
                    _swipeText.value = ""
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
                            message.contains("test") -> {
                                lastAliveTimestamp = System.currentTimeMillis()
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e("SSE", "Error in SSE stream: ${e.localizedMessage}")
            }
        }
        startConnectionMonitor()
    }

    private fun startConnectionMonitor() {
        if (monitorJob?.isActive == true) return

        monitorJob = viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val isAlive = (now - lastAliveTimestamp) < 10_000
                _isConnectionAlive.value = isAlive

                if (!isAlive && !retryingSession) {
                    Log.w("SSE", "Connection lost, Scheduling retry...")
                    _connectionsStatus.value = "Disconnected"
                    scheduleRetrySession()
                }

                delay(10_000)
            }
        }
    }

    private fun handlePlayerData(message: String) {
        val payload = repository.getPayload(message)
        if (payload?.get("hasData")?.asBoolean == true) {
            // Filter out "Query" type
            if (payload.get("type")?.asString == "Gate") {
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
                Log.d("SSE - player", "drMemberCPPlayerData: $response")
            }
        }
    }

    /**
     * Handle gate messages:
     * - Remove accepted or denied players form the queue
     */
    private fun handleGateData(message: String) {
        val payload = repository.getPayload(message)
        val list = repository.getList(payload.toString())
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
        Log.d("SSE - gate", "drMemberCPGateArray: $response")
    }

    fun onPlayerScanned(player: PlayerResponse) {
        queueManager.enQueue(player)
    }

    // Check if needed/ how to do this
//    fun stopSseStream() {
//
//    }
}
