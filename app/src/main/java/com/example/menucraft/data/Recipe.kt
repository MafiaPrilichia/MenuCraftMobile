package com.example.menucraft.data

data class Recipe(
    val id: Long,
    val user: User,
    val category: Category,
    val name: String,
    val description: String,
    val servings: Int,
    val cookingTime: Int,
    val isPublic: Boolean
)
