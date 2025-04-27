package com.example.menucraft.data

data class RecipeCRUD(
    val userId: Long?,
    val categoryId: Long,
    val name: String,
    val description: String,
    val servings: Int,
    val cookingTime: Int
)
