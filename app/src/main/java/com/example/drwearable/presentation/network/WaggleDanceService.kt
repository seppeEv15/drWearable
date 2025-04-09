package com.example.drwearable.presentation.network

import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class SessionIdResponse(val sessionId: String)

interface WaggleDanceService {
    @POST("client2server")
    suspend fun getSessionId(@Body body: RequestBody): Response<SessionIdResponse>

    @POST("client2server")
    suspend fun sendMessage(@Body body: RequestBody): Response<ResponseBody>

    @POST("client2server")
    suspend fun sendMessageWithSession(
        @Query("sessionId") sessionId: String,
        @Body body: RequestBody
    ): Response<ResponseBody>
}

object WaggleDanceApi {
    private const val BASE_URL = "http://10.129.100.80:5050"

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

