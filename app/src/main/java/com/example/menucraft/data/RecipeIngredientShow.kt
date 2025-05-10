package com.example.menucraft.data

import java.math.BigDecimal

data class RecipeIngredientShow(
    val recipeId: Long,
    val ingredient: Ingredient,
    val unit: Unit,
    val amount: BigDecimal
)