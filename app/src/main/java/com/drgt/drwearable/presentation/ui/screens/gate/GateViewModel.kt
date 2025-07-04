package com.drgt.drwearable.presentation.ui.screens.gate

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
import com.drgt.drwearable.presentation.data.WaggledanceRepository
import com.drgt.drwearable.presentation.data.model.GateAccessPayload
import com.drgt.drwearable.presentation.data.model.GateState
import com.drgt.drwearable.presentation.data.model.LastPlayer
import com.drgt.drwearable.presentation.data.model.Player
import com.drgt.drwearable.presentation.data.model.PlayerResponse
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
    object Green : BorderState()
    object Red : BorderState()
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

    private val _queue = MutableStateFlow<List<PlayerResponse>>(emptyList())
    val queue: StateFlow<List<PlayerResponse>> = _queue.asStateFlow()

    private val lastGateStates = mutableMapOf<String, String>()

    private val _lastHandledPlayer = MutableStateFlow<LastPlayer?>(null)
    val lastHandledPlayer: StateFlow<LastPlayer?> = _lastHandledPlayer.asStateFlow()

    private val _sessionId = MutableStateFlow("")
    val sessionId: StateFlow<String> = _sessionId.asStateFlow()

    private var _borderState = MutableStateFlow<BorderState>(BorderState.Neutral)
    val borderState: StateFlow<BorderState> = _borderState.asStateFlow()

    val isSseConnected: StateFlow<Boolean> = repository.isSseConnected

    private var retryingSession = false

    private var monitorJob: Job? = null

    init {
        initSession()
    }

    /**
     * Adds a new player to the queue by appending the given player to the current list of players
     */
    fun enQueue(player: PlayerResponse) {
        _queue.value = _queue.value + player
    }

    /**
     * Removes a player from the queue based on their position by filtering out the player with the specified position
     */
    fun removeByPosition(position: String) {
        _queue.value = _queue.value.filterNot { it.position == position }.toList()
    }

    /**
     * Clears the entire queue by setting it to an empty list
     */
    fun clearQueue() {
        _queue.value = emptyList()
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
                startSseStream()
            }
            result.onFailure { error ->
                Log.e("SESSION_ID", "Error fetching session ID: ${error.localizedMessage}")
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
    fun setStatusAccepted() {
        viewModelScope.launch {
            try {
                val currentPlayer = _queue.value[0]
                val fullName = "${currentPlayer.player.firstName} ${currentPlayer.player.lastName}"
                val response = repository.sendAccessEvent(
                    sessionId = sessionId.value,
                    payload =  GateAccessPayload(position = currentPlayer.position, isAccessGranted = true)
                )

                if (response.isSuccessful) {
                    _lastHandledPlayer.value = LastPlayer(
                        fullName,
                        isBlacklisted = currentPlayer.player.isBlacklisted,
                        isAccepted = true
                    )
                    _borderState.value = BorderState.Green
                    delay(3000)
                    _lastHandledPlayer.value = null
                    _borderState.value = BorderState.Neutral
                } else {
                    Log.e("GateViewModel", "API error: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
//                _swipeText.value = "Error sending acceptance"
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
                val currentPlayer = _queue.value[0]
                val fullName = "${currentPlayer.player.firstName} ${currentPlayer.player.lastName}"
                val response = repository.sendAccessEvent(
                    sessionId = sessionId.value,
                    payload = GateAccessPayload(
                        position = currentPlayer.position,
                        isAccessGranted = false
                    )
                )

                if (response.isSuccessful) {
                    _lastHandledPlayer.value = LastPlayer(
                        fullName,
                        isBlacklisted = currentPlayer.player.isBlacklisted,
                        isAccepted = false
                    )
                    _borderState.value = BorderState.Red
                    delay(5000)
                    _lastHandledPlayer.value = null
                    _borderState.value = BorderState.Neutral
                } else {
                    Log.e("GateViewModel", "API error: ${response.errorBody()?.string()}")
                }

            } catch (e: Exception) {
                Log.e("GateViewModel", "Network error: ${e.localizedMessage}")
            }
        }
    }

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

    /**
     * Monitors the SSE (Waggledance) connection status and retries session initialization if the connection is lost
     * - Periodically checks if the SSE (Waggledance) connection is active every 10 seconds
     * - If disconnected and not already retrying, schedule a session retry
     */
    private fun startConnectionMonitor() {
        if (monitorJob?.isActive == true) return

        monitorJob = viewModelScope.launch {
            while (isActive) {
                if (!repository.isSseConnected.value && !retryingSession) {
                    Log.w("SSE", "Connection lost, scheduling retry...")
                    scheduleRetrySession()
                }
                delay(5_000)
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
                    val photoObject = if (passphotosArray.size() > 1) {
                        passphotosArray[1].asJsonObject
                    } else {
                        passphotosArray[0].asJsonObject
                    }
                    val base64ImageData = photoObject.get("data").asString
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
                    if (isBlacklisted) {
                        _borderState.value = BorderState.Red
                    }

                    enQueue(response)
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

            if (lastGateStates[gatePosition] == gateState) {
                return@forEach
            }

            lastGateStates[gatePosition] = gateState

            when (gateState) {
                GateState.ACCESS_GRANTED.value, GateState.ACCESS_DENIED.value -> {
                    val isGatePositionInQueue = _queue.value.any { it.position == gatePosition }
                    if (isGatePositionInQueue) {
                        removeByPosition(gatePosition)
                    }
                }
                GateState.READY_FOR_USE.value -> {
                    val isGatePositionInQueue = _queue.value.any { it.position == gatePosition }
                    if (isGatePositionInQueue) {
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
        } else {
            clearQueue()
        }
    }
}