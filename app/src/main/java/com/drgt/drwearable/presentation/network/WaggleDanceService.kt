package com.drgt.drwearable.presentation.network

import android.util.Log
import com.drgt.drwearable.BuildConfig
import com.drgt.drwearable.presentation.data.model.GateAccessPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class SessionIdResponse(val sessionId: String)

private const val BASE_URL = BuildConfig.BASE_URL

/**
 * WaggleDanceService is an interface that defines the API endpoints for the WaggleDance service.
 */
interface WaggleDanceService {
    @POST("client2server")
    suspend fun getSessionId(@Body body: RequestBody): Response<SessionIdResponse>

    // does not return a response, change this later
    @POST("client2server")
    suspend fun sendMessageWithSession(
        @Query("sessionId") sessionId: String,
        @Body body: Map<String, GateAccessPayload>
    ): Response<Unit>
}

object WaggleDanceApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()

    val service: WaggleDanceService by lazy {
        retrofit.create(WaggleDanceService::class.java)
    }
}

class SseClient(
    private val sessionId: String,
    private val apiUrl: String,
    private val onMessage: (String) -> Unit,
    private val onOpen: () -> Unit,
    private val onError: (Throwable) -> Unit,
) {
    private var call: Call? = null
    private val client = OkHttpClient()

    fun start() {
        val request = Request.Builder()
            .url("$apiUrl/server2client?sessionId=$sessionId")
            .build()

        call = client.newCall(request)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = call?.execute()  // Execute the request with OkHttp

                if (response?.isSuccessful != true) {
                    throw Exception("Unsuccessful response: ${response?.code}")
                }

                onOpen()

                val reader = BufferedReader(InputStreamReader(response.body?.byteStream()))
                val messageBuilder = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line
                    if (currentLine?.startsWith("data:") == true) {
                        messageBuilder.append(currentLine.substring(5).trim())
                    }
                    if (currentLine?.isEmpty() == true) {
                        val fullMessage = messageBuilder.toString()
                        messageBuilder.clear()

                        onMessage(fullMessage)
                    }
                }
            } catch (e: Exception) {
                Log.e("SSE", "Exception in SSE: ${e.localizedMessage}")
                onError(e)
            }
        }
    }

    fun stop() {
        call?.cancel()
    }
}


