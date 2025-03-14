package com.example.menucraft.util

import com.example.menucraft.data.EventShort
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST("/auth/login")
    suspend fun login(@Body request: AuthRequest): String

    @POST("/auth/refresh-token")
    suspend fun refreshToken(@Body tokenRequest: Map<String, String>): Response<Map<String, String>>

    @POST("/auth/register")
    suspend fun register(@Body request: AuthRequest): Response<Map<String, String>>

    @GET("/event/owned")
    suspend fun getOwnedEvents(@Header("Authorization") authToken: String): List<EventShort>
}

data class AuthRequest(val username: String, val password: String)