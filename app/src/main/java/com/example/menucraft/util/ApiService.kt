package com.example.menucraft.util

import com.example.menucraft.data.Category
import com.example.menucraft.data.Event
import com.example.menucraft.data.EventCRUD
import com.example.menucraft.data.EventIngredientShow
import com.example.menucraft.data.EventRecipe
import com.example.menucraft.data.EventRecipeShow
import com.example.menucraft.data.EventShort
import com.example.menucraft.data.Ingredient
import com.example.menucraft.data.Recipe
import com.example.menucraft.data.RecipeCRUD
import com.example.menucraft.data.RecipeIngredientCRUD
import com.example.menucraft.data.RecipeIngredientShow
import com.example.menucraft.data.Unit
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

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

    @GET("/event/recipe/{id}")
    suspend fun getEventRecipes(
        @Path("id") eventId: Long,
        @Header("Authorization") authToken: String
    ): List<EventRecipeShow>

    @GET("/recipe/user")
    suspend fun getUserRecipes(@Header("Authorization") auth: String): List<Recipe>

    @GET("/recipe/user")
    suspend fun getUserRecipesFiltered(
        @Header("Authorization") auth: String,
        @Query("categoryIds") categoryIds: String
    ): List<Recipe>

    @POST("/event/recipe")
    suspend fun addRecipeToEvent(
        @Header("Authorization") auth: String,
        @Body body: EventRecipe
    ): Response<EventRecipe>

    @PUT("/event/recipe")
    suspend fun updateEventRecipe(
        @Header("Authorization") auth: String,
        @Body body: EventRecipe
    ): Response<EventRecipe>

    @DELETE("/event/recipe")
    suspend fun deleteEventRecipe(
        @Header("Authorization") auth: String,
        @Query("eventId") eventId: Int,
        @Query("recipeId") recipeId: Int
    ): Response<Void>

    @GET("/category")
    suspend fun getCategories(
        @Header("Authorization") auth: String
    ): List<Category>

    @POST("/recipe")
    suspend fun createRecipe(
        @Header("Authorization") authToken: String,
        @Body recipe: RecipeCRUD
    ): Response<RecipeCRUD>

    @GET("/recipe/{id}")
    suspend fun getRecipeById(
        @Path("id") recipeId: Long,
        @Header("Authorization") authToken: String
    ): Recipe

    @DELETE("/recipe/{id}")
    suspend fun deleteRecipe(
        @Path("id") recipeId: Long,
        @Header("Authorization") authToken: String
    ): Response<Void>

    @GET("/recipe/ingredient/{id}")
    suspend fun getIngredientsByRecipeId(
        @Path("id") recipeId: Long,
        @Header("Authorization") authToken: String
    ): List<RecipeIngredientShow>

    @GET("/event/ingredient/{id}")
    suspend fun getIngredientsByEventId(
        @Path("id") eventId: Long,
        @Header("Authorization") authToken: String
    ): List<EventIngredientShow>

    @GET("/ingredient")
    suspend fun getIngredients(
        @Header("Authorization") authToken: String
    ): List<Ingredient>

    @GET("/unit")
    suspend fun getUnits(
        @Header("Authorization") authToken: String
    ): List<Unit>

    @POST("/recipe/ingredient")
    suspend fun createRecipeIngredient(
        @Header("Authorization") authToken: String,
        @Body recipeIngredient: RecipeIngredientCRUD
    ): Response<RecipeIngredientCRUD>

    @DELETE("/recipe/ingredient")
    suspend fun deleteRecipeIngredient(
        @Header("Authorization") auth: String,
        @Query("recipeId") recipeId: Int,
        @Query("ingredientId") ingredientId: Int
    ): Response<Void>

    @PUT("/recipe/ingredient")
    suspend fun updateRecipeIngredient(
        @Header("Authorization") auth: String,
        @Body body: RecipeIngredientCRUD
    ): Response<RecipeIngredientCRUD>

    @PUT("/recipe/{id}")
    suspend fun updateRecipe(
        @Path("id") recipeId: Long,
        @Header("Authorization") authToken: String,
        @Body recipe: RecipeCRUD
    ): Response<Recipe>

    @POST("/recipe/another/{id}")
    suspend fun saveRecipeFromAnotherUser(
        @Header("Authorization") authToken: String,
        @Path("id") id: Long
    ): Response<Long>
}

data class AuthRequest(val username: String, val password: String)