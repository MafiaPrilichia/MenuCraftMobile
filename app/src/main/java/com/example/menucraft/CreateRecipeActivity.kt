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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menucraft.data.Category
import com.example.menucraft.data.Recipe
import com.example.menucraft.data.RecipeCRUD
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
    onCreated: (Boolean) -> Unit,
    onNavigateBack: () -> Unit
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

    fun createRecipe(authToken: String, recipe: RecipeCRUD, onSuccess: () -> Unit) {
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

    fun deleteRecipe(authToken: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val id = _recipe.value?.id ?: return
        viewModelScope.launch {
            try {
                val response = apiService.deleteRecipe(id, "Bearer $authToken")
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onError("Ошибка удаления")
                }
            } catch (e: Exception) {
                onError("Ошибка сети")
            }
        }
    }
}