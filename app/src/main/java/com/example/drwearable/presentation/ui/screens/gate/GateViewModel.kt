package com.example.drwearable.presentation.ui.screens.gate

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.drwearable.presentation.data.WaggledanceRepository
import com.example.drwearable.presentation.data.model.GateState
import com.example.drwearable.presentation.data.model.Player
import com.example.drwearable.presentation.data.model.PlayerResponse
import com.google.gson.JsonNull
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed class BorderState {
    object Neutral : BorderState()
    object Accepted : BorderState()
    object Denied : BorderState()
}

sealed class GateUiState {
    object NoConnection : GateUiState()
    object Idle : GateUiState()
    data class PlayerWaiting (val player: PlayerResponse) : GateUiState()
    data class PlayerAccepted (val playerName: String) : GateUiState()
    data class PlayerDenied (val playerName: String): GateUiState()
}

/**
 * ViewModel responsible for handling gate access logic:
 * - Establishing a session
 * - Managing SSE stream (Waggledance)
 * - Handling player access and gate responses
 * - Notifying UI of state changes
 */
class GateViewModel(
    application: Application,
    private val repository: WaggledanceRepository
) : AndroidViewModel(application) {
    private val appContext = getApplication<Application>().applicationContext

//    private val _uiState = MutableStateFlow<GateUiState>(GateUiState.NoConnection)
//    val uiState: StateFlow<GateUiState> = _uiState.asStateFlow()

//    val queueManager = PlayerQueueManager()

//    -----------------
    private val _queue = MutableStateFlow<List<PlayerResponse>>(emptyList())
    val queue: StateFlow<List<PlayerResponse>> = _queue.asStateFlow()

    private val _currentPlayer = MutableStateFlow<PlayerResponse?>(null)

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
        Log.d("PLAYER", "Before $position ${_queue.value}")
        _queue.value = _queue.value.filterNot { it.position == position }.toList()
        Log.d("PLAYER", "After $position ${_queue.value}")
    }

    fun clearQueue() {
        Log.d("CLEAR", "Queue has been cleared")
        _queue.value = emptyList()
    }
//    -----------------

    private val lastGateStates = mutableMapOf<String, String>()

    private val _swipeText = MutableStateFlow("")
    val swipeText: StateFlow<String> = _swipeText.asStateFlow()

    private val _sessionId = MutableStateFlow("")
    val sessionId: StateFlow<String> = _sessionId.asStateFlow()

    private val _borderState = MutableStateFlow<BorderState>(BorderState.Neutral)
    val borderState: StateFlow<BorderState> = _borderState.asStateFlow()

    val isSseConnected: StateFlow<Boolean> = repository.isSseConnected

    private var retryingSession = false

    private var monitorJob: Job? = null

    init {
        initSession()
    }

    /**
     * Starts a new session by requesting a session ID from the backend
     * If successful, start the SSE stream, Otherwise, retry after delay
     */
    private fun initSession() {
        viewModelScope.launch {
            retryingSession = false
            val result = repository.getSessionId()
            result.onSuccess { id ->
                Log.d("SESSION_ID", "Session ID: $id")
                _sessionId.value = id
//                _uiState.value = GateUiState.Idle
                startSseStream()
            }
            result.onFailure { error ->
                Log.e("SESSION_ID", "Error fetching session ID: ${error.localizedMessage}")
//                _uiState.value = GateUiState.NoConnection
                scheduleRetrySession()
            }
        }
    }

    /**
     * Retries to obtain a session ID every 10 seconds
     * if SSE (Waggledance) is disconnected and not already retrying
     */
    private fun scheduleRetrySession() {
        if (!retryingSession) {
            retryingSession = true
            viewModelScope.launch {
                while (retryingSession && !repository.isSseConnected.value) {
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
//    fun setStatusAccepted() {
//        viewModelScope.launch {
//            try {
//                val response = repository.sendAccessEvent(
//                    sessionId = sessionId.value.toString(),
//                    payload =  GateAccessPayload(position = currentPlayer.value?.position.toString(), isAccessGranted = true)
//                )
//
//                val playerName = currentPlayer.value?.player?.let {
//                    "${it.firstName} ${it.lastName}".trim()
//                } ?: "Player"
//
//                if (response.isSuccessful) {
//                    queueManager.acceptNext()
////                    _uiState.value = GateUiState.PlayerAccepted(playerName)
//                    _swipeText.value = "Accepted $playerName"
//                    _borderState.value = BorderState.Accepted
//                    delay(3000)
//                    _borderState.value = BorderState.Neutral
////                    _uiState.value = GateUiState.Idle
//                    _swipeText.value = ""
//                } else {
//                    _swipeText.value = "Failed to accept: ${response.code()}"
//                    Log.e("GateViewModel", "API error: ${response.errorBody()?.string()}")
////                    _uiState.value = GateUiState.Idle
//                }
//
//            } catch (e: Exception) {
//                _swipeText.value = "Error sending acceptance"
//                Log.e("GateViewModel", "Network error: ${e.localizedMessage}")
////                _uiState.value = GateUiState.Idle
//            }
//        }
//    }

    /**
     * Deny a player
     */
//    fun setStatusDenied() {
//        viewModelScope.launch {
//            try {
//                val response = repository.sendAccessEvent(
//                    sessionId = sessionId.value.toString(),
//                    payload = GateAccessPayload(position = currentPlayer.value?.position.toString(), isAccessGranted = false)
//                )
//
//                val playerName = currentPlayer.value?.player?.let {
//                    "${it.firstName} ${it.lastName}".trim()
//                } ?: "Player"
//
//                if (response.isSuccessful) {
//                    queueManager.denyNext()
//                    _swipeText.value = "Denied $playerName"
//                    _borderState.value = BorderState.Denied
////                    _uiState.value = GateUiState.PlayerDenied(playerName)
//                    delay(3000)
////                    _uiState.value = GateUiState.Idle
//                    _borderState.value = BorderState.Neutral
//                    _swipeText.value = ""
//                } else {
//                    _swipeText.value = "Failed to deny: ${response.code()}"
//                    Log.e("GateViewModel", "API error: ${response.errorBody()?.string()}")
////                    _uiState.value = GateUiState.Idle
//                }
//
//            } catch (e: Exception) {
//                _swipeText.value = "Error sending denial"
//                Log.e("GateViewModel", "Network error: ${e.localizedMessage}")
////                _uiState.value = GateUiState.Idle
//            }
//        }
//    }

    /**
     * Start listening to SSE (Waggledance) stream for messages
     * - Handles player and gate messages
     * - Sets connection state via repository
     */
    fun startSseStream() {
        monitorJob?.cancel()
        viewModelScope.launch {
            try {
                repository.startSseStream(sessionId.value)
                    .collect { message ->
                        when {
                            message.contains("drMemberCPPlayerData") -> {
                                if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                    handlePlayerData(message)
                                }
                            }
                            message.contains("drMemberCPGateArray") -> handleGateData(message)
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
                if (!repository.isSseConnected.value && !retryingSession) {
                    Log.w("SSE", "Connection lost, scheduling retry...")
                    scheduleRetrySession()
                }
                delay(10_000)
            }
        }
    }

    /**
     * Handles player-related SSE (Waggledance) messages
     * - Parses payload
     * - Convert image data from base64
     * - Notifies UI and enqueues player
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun handlePlayerData(message: String) {
        val payload = repository.getPayload(message)
        if (payload?.get("hasData")?.asBoolean == true) {
            if (payload.get("type")?.asString == "Gate") {
                val playerId = payload.get("playerId")?.takeIf { it !is JsonNull }?.asInt ?: -1
                val isDuplicate = queue.value.any { it.playerId == playerId}

                if (!isDuplicate) {
                    val playerObj = payload.getAsJsonObject("player")
                    val passphotosArray = payload.getAsJsonArray("passphotos")
                    val firstPhotoObject = passphotosArray[0].asJsonObject
                    val base64ImageData = firstPhotoObject.get("data").asString
                    val isBlacklisted = payload.get("isBlacklisted")?.asBoolean == true

                    val imageBytes = Base64.decode(base64ImageData, Base64.DEFAULT)

                    val response = PlayerResponse(
                        position = payload.get("position")?.takeIf { it !is JsonNull }?.asString ?: "",
                        playerId,
                        player = Player(
                            firstName = playerObj?.get("firstName")?.asString.orEmpty(),
                            secondName = playerObj?.get("secondName")?.asString.orEmpty(),
                            lastName = playerObj?.get("lastName")?.asString.orEmpty(),
                            lastName2 = playerObj?.get("lastName2")?.asString.orEmpty(),
                            image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size),
                            isBlacklisted = isBlacklisted
                        )
                    )

                    enQueue(response)
                    Log.d("QueueManager", "Queue: ${queue.value}")
//                    _uiState.value = GateUiState.PlayerWaiting(response)
//                    Log.d("GateViewModel", "UI State: ${_uiState.value}")
//                    NotificationHelper.notifyNewPlayer(appContext, response)
                }
            }
        }
    }

    /**
     * Handle gate related SSE (Waggledance) messages
     * - Parses payload
     * - Removes player from queue based on gate response
     */
    private fun handleGateData(message: String) {
        Log.d("GATE", message)
        val payload = repository.getPayload(message)
        val list = repository.getList(payload.toString())

        list?.mapNotNull { it.asJsonObject }?.forEach { gate ->
            val gatePosition = gate.get("position")?.asString.orEmpty()
            val gateState = gate.get("state")?.asString.orEmpty()

//            Check if state has changed
            if (lastGateStates[gatePosition] == gateState) {
                return@forEach
            }

            lastGateStates[gatePosition] = gateState

            when (gateState) {
                GateState.ACCESS_GRANTED.value, GateState.ACCESS_DENIED.value -> {
                    val isGatePositionInQueue = _queue.value.any { it.position == gatePosition }
                    if (isGatePositionInQueue) {
                        Log.d("GATE", "ACCEPT/DENY: Player at position $gatePosition accepted/denied: $payload")
                        removeByPosition(gatePosition)
                    }
                }
                GateState.READY_FOR_USE.value -> {
                    val isGatePositionInQueue = _queue.value.any { it.position == gatePosition }
                    if (isGatePositionInQueue) {
                        Log.d("GATE", "IDLE: Player at position $gatePosition removed from queue: $payload")
                        removeByPosition(gatePosition)
                    }
                }
            }
        }

        val allGatesReadyForUse = list?.all {
            it.asJsonObject.get("state")?.asString.orEmpty() == GateState.READY_FOR_USE.value
        } == true

        if (!allGatesReadyForUse) {
            for (gate in list?.mapNotNull { it.asJsonObject }!!) {
                val gateState = gate.get("state")?.asString.orEmpty()
                val gatePosition = gate.get("position")?.asString.orEmpty()

                when (gateState) {
                    GateState.ACCESS_GRANTED.value, GateState.ACCESS_DENIED.value -> {
                        val isGatePositionInQueue = _queue.value.any { it.position == gatePosition }
                        if (isGatePositionInQueue) {
                            Log.d("GATE", "ACCEPT/DENY: Player at position $gatePosition accepted/denied: $payload")
                            removeByPosition(gatePosition)
                        }
//                        viewModelScope.launch {
//                            delay(300) // delay in milliseconds
//                            _uiState.value = GateUiState.Idle
//                        }
                    }
                    GateState.READY_FOR_USE.value -> {
                        val isGatePositionInQueue = _queue.value.any { it.position == gatePosition }
                        if (isGatePositionInQueue) {
                            Log.d("GATE", "IDLE: Player at position $gatePosition removed from queue: $payload")
                            removeByPosition(gatePosition)
                        }
//                        viewModelScope.launch {
//                            delay(300) // delay in milliseconds
//                            _uiState.value = GateUiState.Idle
//                        }
                    }
                }
            }
        } else {
            Log.d("RFU", "Gates are reade for use:  ${_queue.value}")
            clearQueue()
        }
    }
}