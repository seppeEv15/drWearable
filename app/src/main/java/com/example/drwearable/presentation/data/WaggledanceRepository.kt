package com.example.drwearable.presentation.data

import android.util.Log
import com.example.drwearable.BuildConfig
import com.example.drwearable.presentation.data.model.GateAccessPayload
import com.example.drwearable.presentation.network.SseClient
import com.example.drwearable.presentation.network.WaggleDanceService
import com.google.gson.Gson
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response

private const val BASE_URL = BuildConfig.BASE_URL

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
                Result.success(response.body()?.sessionId as String)
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

    suspend fun sendAccessEvent(payload: GateAccessPayload, sessionId: String): Response<Unit> {
        val requestBody = mapOf("drMemberCPAccess" to payload)
        val json = Gson().toJson(requestBody)
        Log.d("GATE", "requestBody: $json, sessionId: $sessionId")
        return service.sendMessageWithSession(sessionId, requestBody)
    }
}