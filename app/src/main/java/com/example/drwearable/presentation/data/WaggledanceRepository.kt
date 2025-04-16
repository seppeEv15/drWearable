package com.example.drwearable.presentation.data

import android.util.Log
import com.example.drwearable.presentation.network.SseClient
import com.example.drwearable.presentation.network.WaggleDanceService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

//TODO: Save the BASE_URL in a config file
private const val BASE_URL = "http://10.129.10.42:5050"

/**
 * Repository responsible for handling data operations related to the WaggleDance API.
 */
class WaggledanceRepository(private val service: WaggleDanceService) {
    suspend fun getSessionId(): Result<String> {
        return try {
            val sessionBody = """{"cmd": "newSession"}"""
                .toRequestBody(
                    contentType = "application/json".toMediaTypeOrNull(),
                )

            val response = service.getSessionId(sessionBody)

            if (response.isSuccessful) {
                Result.success(response.body()!!.sessionId)
            } else {
                Result.failure(Exception("HTTP ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun startSseStream(sessionId: String): Flow<String> {
        return callbackFlow {
            val sseClient = SseClient(
                sessionId = sessionId,
                apiUrl = BASE_URL,
                onMessage = { message ->
                    try {
                        trySend(message).isSuccess
                    } catch (e: Exception) {
                        Log.e("SSE", "Exception while offering message: ${e.localizedMessage}")
                    }
                },
                onOpen = {
                    Log.d("SSE", "Connection opened")
                },
                onError = { error ->
                    Log.e("SSE", "Connection error: ${error.localizedMessage}")
                }
            )

            sseClient.start()
            awaitClose { sseClient.stop() }
        }
    }
}