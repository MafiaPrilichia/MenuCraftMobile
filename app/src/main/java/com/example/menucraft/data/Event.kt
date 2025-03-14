package com.example.menucraft.data

import java.time.LocalDateTime

data class Event (
    val id: Long,
    val name: String,
    val theme: String,
    val eventDate: LocalDateTime,
    val location: String,
    val description: String,
    val guests: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)