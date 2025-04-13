package com.example.menucraft.util

import com.example.menucraft.data.Event
import com.example.menucraft.data.EventCRUD
import com.example.menucraft.data.EventShort
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @POST("/auth/login")
    suspend fun login(@Body request: AuthRequest): String

    @POST("/auth/refresh-token")
    suspend fun refreshToken(@Body tokenRequest: Map<String, String>): Response<Map<String, String>>

    @POST("/auth/register")
    suspend fun register(@Body request: AuthRequest): Response<Map<String, String>>

    @GET("/event/owned")
    suspend fun getOwnedEvents(@Header("Authorization") authToken: String): List<EventShort>

    @GET("/event/{id}")
    suspend fun getEventByID(
        @Path("id") eventId: Long,
        @Header("Authorization") authToken: String
    ): Event

    @POST("/event")
    suspend fun createEvent(
        @Header("Authorization") authToken: String,
        @Body event: EventCRUD
    ): Response<EventShort>

    @PUT("/event/{id}")
    suspend fun updateEvent(
        @Path("id") eventId: Long,
        @Header("Authorization") authToken: String,
        @Body event: EventCRUD
    ): Response<Event>

    @DELETE("/event/{id}")
    suspend fun deleteEvent(
        @Path("id") eventId: Long,
        @Header("Authorization") authToken: String
    ): Response<Unit>
}

data class AuthRequest(val username: String, val password: String)