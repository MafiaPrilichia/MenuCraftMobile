package com.example.menucraft

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menucraft.data.Category
import com.example.menucraft.data.Ingredient
import com.example.menucraft.data.Recipe
import com.example.menucraft.data.RecipeCRUD
import com.example.menucraft.data.RecipeIngredientCRUD
import com.example.menucraft.data.RecipeIngredientShow
import com.example.menucraft.data.Unit
import com.example.menucraft.util.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class CreateRecipeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authToken = intent.getStringExtra("jwt_token") ?: ""
        val viewModel: RecipeViewModel by viewModels()

        setContent {
            CreateRecipeScreen(
                viewModel = viewModel,
                authToken = authToken,
                onCreated = { created ->
                    if (created) {
                        setResult(RESULT_OK)
                    }
                    finish()
                },
                onNavigateBack = {
                    finish()
                }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRecipeScreen(
    viewModel: RecipeViewModel,
    authToken: String,
    onCreated: (Boolean) -> kotlin.Unit,
    onNavigateBack: () -> kotlin.Unit
) {
    val context = LocalContext.current
    val showDialog = remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var servingsText by remember { mutableStateOf("") }
    var cookingTimeText by remember { mutableStateOf("") }

    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    val categories by viewModel.categories.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCategories(authToken)
    }

    val isAnyFieldNotEmpty = remember {
        derivedStateOf {
            name.isNotBlank() || description.isNotBlank() ||
                    servingsText.isNotBlank() || cookingTimeText.isNotBlank() || selectedCategory != null
        }
    }

    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    onBackPressedDispatcher?.addCallback(
        context as LifecycleOwner,
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isAnyFieldNotEmpty.value) {
                    showDialog.value = true
                } else {
                    onNavigateBack()
                }
            }
        }
    )

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Название рецепта") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Описание") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = servingsText,
            onValueChange = { servingsText = it },
            label = { Text("Количество порций") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = cookingTimeText,
            onValueChange = { cookingTimeText = it },
            label = { Text("Время приготовления (мин)") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                readOnly = true,
                value = selectedCategory?.name ?: "",
                onValueChange = {},
                label = { Text("Категория") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()  // <- обязательно добавить
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 400.dp)  // чтобы не вылезало за экран
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        onClick = {
                            selectedCategory = category
                            expanded = false
                        }
                    )
                }
            }
        }


        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val servings = servingsText.toIntOrNull() ?: 0
            val cookingTime = cookingTimeText.toIntOrNull() ?: 0

            val newRecipe = RecipeCRUD(
                userId = null,
                categoryId = selectedCategory?.id ?: 0L,
                name = name,
                description = description,
                servings = servings,
                cookingTime = cookingTime
            )

            viewModel.createRecipe(authToken, newRecipe) {
                Log.d("CreateRecipeScreen", "Рецепт создан")
                onCreated(true)
            }
        }, enabled = selectedCategory != null) {
            Text("Создать рецепт")
        }
    }

    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = { Text("Предупреждение") },
            text = { Text("Если вы покинете экран, введённые данные будут утеряны.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog.value = false
                        onNavigateBack()
                    }
                ) {
                    Text("Да")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog.value = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}


@RequiresApi(Build.VERSION_CODES.O)
class RecipeViewModel : ViewModel() {
    private val apiService = RetrofitInstance.apiService

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    fun loadCategories(authToken: String) {
        viewModelScope.launch {
            try {
                val categories = apiService.getCategories("Bearer $authToken")
                _categories.value = categories
            } catch (e: Exception) {
                Log.e("RecipeViewModel", "Ошибка при загрузке категорий: ${e.message}", e)
            }
        }
    }

    fun createRecipe(authToken: String, recipe: RecipeCRUD, onSuccess: () -> kotlin.Unit) {
        viewModelScope.launch {
            try {
                val response = apiService.createRecipe("Bearer $authToken", recipe)
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    Log.e("RecipeViewModel", "Ошибка создания рецепта: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("RecipeViewModel", "Ошибка при создании рецепта: ${e.message}", e)
            }
        }
    }

    private val _recipe = MutableStateFlow<Recipe?>(null)
    val recipe: StateFlow<Recipe?> = _recipe

    fun loadRecipe(recipeId: Long, authToken: String) {
        viewModelScope.launch {
            try {
                val loadedRecipe = apiService.getRecipeById(recipeId, "Bearer $authToken")
                _recipe.value = loadedRecipe
            } catch (e: Exception) {
                Log.e("RecipeViewModel", "Ошибка загрузки рецепта", e)
            }
        }
    }

    fun deleteRecipe(authToken: String, onSuccess: () -> kotlin.Unit, onError: (String) -> kotlin.Unit): kotlin.Unit {
        val id = _recipe.value?.id ?: return
        viewModelScope.launch {
            try {
                val response = apiService.deleteRecipe(id, "Bearer $authToken")
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onError(response.errorBody()?.string().toString())
                }
            } catch (e: Exception) {
                onError("Ошибка сети")
            }
        }
    }

    private val _ingredientsForRecipe = MutableStateFlow<List<RecipeIngredientShow>>(emptyList())
    val ingredientsForRecipe: StateFlow<List<RecipeIngredientShow>> = _ingredientsForRecipe

    fun loadIngredientsForRecipe(recipeId: Long, authToken: String) {
        viewModelScope.launch {
            try {
                val ingredientsResponse = apiService.getIngredientsByRecipeId(recipeId, "Bearer $authToken")
                _ingredientsForRecipe.value = ingredientsResponse
            } catch (e: Exception) {
                Log.e("RecipeViewModel", "Ошибка получения ингредиентов", e)
            }
        }
    }


    private val _ingredients = MutableStateFlow<List<Ingredient>>(emptyList())
    val ingredients: StateFlow<List<Ingredient>> = _ingredients

    fun loadIngredients(authToken: String) {
        viewModelScope.launch {
            try {
                val ingredientsResponse = apiService.getIngredients("Bearer $authToken")
                _ingredients.value = ingredientsResponse
            } catch (e: Exception) {
                Log.e("RecipeViewModel", "Ошибка получения ингредиентов", e)
            }
        }
    }


    private val _units = MutableStateFlow<List<Unit>>(emptyList())
    val units: StateFlow<List<Unit>> = _units

    fun loadUnits(authToken: String) {
        viewModelScope.launch {
            try {
                val unitsResponse = apiService.getUnits("Bearer $authToken")
                _units.value = unitsResponse
            } catch (e: Exception) {
                Log.e("RecipeViewModel", "Ошибка получения мер измерения", e)
            }
        }
    }

    fun addIngredientToRecipe(authToken: String, recipeIngredient: RecipeIngredientCRUD, onSuccess: () -> kotlin.Unit): kotlin.Unit {
        viewModelScope.launch {
            try {
                val response = apiService.createRecipeIngredient("Bearer $authToken", recipeIngredient)
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    Log.e("RecipeViewModel", "Ошибка добавления ингредиента: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("RecipeViewModel", "Ошибка при добавлении ингредиента: ${e.message}", e)
            }
        }
    }

    fun deleteIngredientFromRecipe(recipeId: Long, ingredientId: Long, authToken: String, onSuccess: () -> kotlin.Unit) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteRecipeIngredient(
                    auth = "Bearer $authToken",
                    recipeId = recipeId.toInt(),
                    ingredientId = ingredientId.toInt()
                )
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    Log.e("RecipeViewModel", "Ошибка при удалении ингредиента: ${response.code()} - ${response.errorBody()
                        ?.string()}")
                }
            } catch (e: Exception) {
                Log.e("RecipeViewModel", "Ошибка при удалении ингредиента: ${e.message}", e)
            }
        }
    }

    fun updateRecipeIngredient(
        updatedIngredient: RecipeIngredientCRUD,
        authToken: String,
        onSuccess: () -> kotlin.Unit
    ) {
        viewModelScope.launch {
            try {
                // Отправляем обновлённый ингредиент на сервер
                val response = apiService.updateRecipeIngredient(
                    auth = "Bearer $authToken",
                    body = updatedIngredient
                )
                if (response.isSuccessful) {
                    // Обновляем локальное состояние ингредиентов
                    _ingredientsForRecipe.value = _ingredientsForRecipe.value.map { ingredient ->
                        (if (ingredient.ingredient.id == updatedIngredient.ingredientId) {
                            _units.value.find{ it.id == updatedIngredient.unitId}?.let {
                                ingredient.copy(
                                    unit = it,
                                    amount = updatedIngredient.amount.toBigDecimal()
                                )
                            }
                        } else {
                            ingredient
                        })!!
                    }
                    // Загружаем свежие данные с сервера (например, список ингредиентов)
                    loadIngredientsForRecipe(updatedIngredient.recipeId, authToken)
                    onSuccess()
                } else {
                    Log.e("RecipeIngredientViewModel", "Ошибка при обновлении ингредиента: ${response.code()} - ${response.errorBody()
                        ?.string()}")
                }
            } catch (e: Exception) {
                Log.e("RecipeIngredientViewModel", "Ошибка при обновлении ингредиента: ${e.message}", e)
            }
        }
    }

    fun updateRecipe(recipeId: Long, authToken: String, updatedRecipe: RecipeCRUD, onResult: (Boolean) -> kotlin.Unit) {
        viewModelScope.launch {
            try {
                val response = apiService.updateRecipe(recipeId, "Bearer $authToken", updatedRecipe)
                if (response.isSuccessful) {
                    _recipe.value = response.body()
                    onResult(true)
                } else {
                    onResult(false)
                    Log.e("RecipeViewModel", "Ошибка обновления: ${response.code()} - ${response.errorBody()
                        ?.string()}")
                }
            } catch (e: Exception) {
                onResult(false)
                Log.e("RecipeViewModel", "Ошибка при обновлении блюда: ${e.message}", e)
            }
        }
    }

    fun saveRecipeFromAnotherUser(id: Long, authToken: String, onResult: (Long?) -> kotlin.Unit) {
        viewModelScope.launch {
            try {
                Log.d("RecipeViewModel", "$id")
                val response = apiService.saveRecipeFromAnotherUser("Bearer $authToken", id)
                if (response.isSuccessful) {
                    val newRecipeId = response.body()
                    onResult(newRecipeId)
                } else {
                    onResult(null)
                    Log.e("RecipeViewModel", "Ошибка сохранения рецепта: ${response.code()} - ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                onResult(null)
                Log.e("RecipeViewModel", "Ошибка при сохранении рецепта: ${e.message}", e)
            }
        }
    }
}