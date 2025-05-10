package com.example.menucraft.data

data class RecipeIngredientCRUD (
    val recipeId: Long,
    val ingredientId: Long,
    val unitId: Long,
    val amount: Double
)