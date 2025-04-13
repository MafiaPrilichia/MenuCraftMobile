package com.example.menucraft.data

import com.example.menucraft.util.LocalDateTimeAdapter
import com.google.gson.annotations.JsonAdapter
import java.time.LocalDateTime

data class EventCRUD(
    val name: String,
    val theme: String,

    @JsonAdapter(LocalDateTimeAdapter::class)
    //@JsonFormat(pattern = "dd.MM.yyyy HH:mm")
    val eventDate: LocalDateTime,

    val location: String,
    val description: String,
    val guests: Int,
)
