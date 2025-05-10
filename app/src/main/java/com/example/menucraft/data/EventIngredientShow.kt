package com.example.menucraft.data

import java.math.BigDecimal

class EventIngredientShow(
    val eventId: Long,
    val ingredient: Ingredient,
    val unit: Unit,
    val amount: BigDecimal
)