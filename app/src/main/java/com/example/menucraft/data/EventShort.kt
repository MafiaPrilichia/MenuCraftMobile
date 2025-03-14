package com.example.menucraft.data

data class EventShort(
    val id: Long,
    val name: String,
    val theme: String,
    val eventDate: String,
    val location: String,
    val guests: Int
)