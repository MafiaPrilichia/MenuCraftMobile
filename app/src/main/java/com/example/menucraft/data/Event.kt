package com.example.menucraft.data

import com.example.menucraft.util.LocalDateTimeAdapter
import com.google.gson.annotations.JsonAdapter
import java.time.LocalDateTime

data class Event(
    val id: Long,
    val name: String,
    val theme: String,

    @JsonAdapter(LocalDateTimeAdapter::class)
    val eventDate: LocalDateTime,

    val location: String,
    val description: String,
    val guests: Int,

    @JsonAdapter(LocalDateTimeAdapter::class)
    val createdAt: LocalDateTime?,

    @JsonAdapter(LocalDateTimeAdapter::class)
    val updatedAt: LocalDateTime?
)