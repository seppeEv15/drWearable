package com.example.drwearable.presentation.ui.screens.gate

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.drwearable.presentation.data.WaggledanceRepository
import com.example.drwearable.presentation.data.model.GateAccessPayload
import com.example.drwearable.presentation.data.model.GateResponse
import com.example.drwearable.presentation.data.model.GateState
import com.example.drwearable.presentation.data.model.Player
import com.example.drwearable.presentation.data.model.PlayerQueueManager
import com.example.drwearable.presentation.data.model.PlayerResponse
import com.example.drwearable.presentation.notifications.NotificationHelper
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

    private val queueManager = PlayerQueueManager()

    val currentPlayer = queueManager.currentPlayer
    private val _swipeText = MutableStateFlow("")
    val swipeText: StateFlow<String> = _swipeText.asStateFlow()

    private val _sessionId = MutableStateFlow("")
    val sessionId: StateFlow<String> = _sessionId.asStateFlow()

    private var lastAliveTimestamp = System.currentTimeMillis()

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
                val response = repository.sendAccessEvent(
                    sessionId = sessionId.value.toString(),
                    payload =  GateAccessPayload(position = currentPlayer.value?.position.toString(), isAccessGranted = true)
                )

                val playerName = currentPlayer.value?.player?.let {
                    "${it.firstName} ${it.lastName}".trim()
                } ?: "Player"

                if (response.isSuccessful) {
                    queueManager.acceptNext()
                    _swipeText.value = "Accepted $playerName"
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

                val playerName = currentPlayer.value?.player?.let {
                    "${it.firstName} ${it.lastName}".trim()
                } ?: "Player"

                if (response.isSuccessful) {
                    queueManager.denyNext()
                    _swipeText.value = "Denied $playerName"
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
        Log.d("SSE - payload", "$payload")
        if (payload?.get("hasData")?.asBoolean == true) {
            // Filter out "Query" type
            if (payload.get("type")?.asString == "Gate") {
                val playerObj = payload.getAsJsonObject("player")
                val passphotosArray = payload.getAsJsonArray("passphotos")
                val firstPhotoObject = passphotosArray[0].asJsonObject
                val base64ImageData = firstPhotoObject.get("data").asString
                val isBlacklisted = payload.get("isBlacklisted")?.asBoolean == true

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
                        image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size),
                        isBlacklisted = isBlacklisted
                    ),
                )


                onPlayerScanned(response, appContext)
                Log.d("SSE - blacklist", "isBlacklisted: ${payload.get("isBlacklisted")?.asBoolean}")
                Log.d("SSE - player", "drMemberCPPlayerData: $response")
                Log.d("SSE - playerObj", "$playerObj")
            }
        }
    }

    /**
     * Handle gate related SSE (Waggledance) messages
     * - Parses payload
     * - Removes player from queue based on gate response
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

        Log.d("SSE - gate", "drMemberCPGateArray: $response")
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun onPlayerScanned(player: PlayerResponse, context: Context) {
        queueManager.enQueue(player)
        Log.d("NOTIFICATION", "Triggering notification for ${player.player.firstName}")
        NotificationHelper.notifyNewPlayer(context, player)
    }

    // Check if needed/ how to do this
//    fun stopSseStream() {
//
//    }
}
