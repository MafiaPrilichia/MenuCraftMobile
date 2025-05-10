package com.example.menucraft

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.menucraft.data.Category
import com.example.menucraft.data.Recipe
import com.example.menucraft.data.RecipeCRUD

@RequiresApi(Build.VERSION_CODES.O)
class EditRecipeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val recipeId = intent.getLongExtra("recipe_id", -1)
        val authToken = intent.getStringExtra("jwt_token") ?: ""
        Log.d("EditRecipe", "Enter ${recipeId}")
        val viewModel: RecipeViewModel by viewModels()

        // Загружаем категории и рецепт
        viewModel.loadCategories(authToken)
        viewModel.loadRecipe(recipeId, authToken)

        setContent {
            val recipe by viewModel.recipe.collectAsState()
            val categories by viewModel.categories.collectAsState()

            recipe?.let { it ->
                var selectedCategory by remember { mutableStateOf<Category?>(it.category) }  // Инициализируем выбранную категорию
                var name by remember { mutableStateOf(it.name) }
                var description by remember { mutableStateOf(it.description) }
                var servings by remember { mutableStateOf(it.servings.toString()) }
                var cookingTime by remember { mutableStateOf(it.cookingTime.toString()) }
                var showDialog by remember { mutableStateOf(false) }

                val isFieldsChanged = remember {
                    derivedStateOf {
                        selectedCategory?.id != it.category.id ||
                                name != it.name ||
                                description != it.description ||
                                servings != it.servings.toString() ||
                                cookingTime != it.cookingTime.toString()
                    }
                }

                BackHandler {
                    if (isFieldsChanged.value) {
                        showDialog = true
                    } else {
                        finish()
                    }
                }

                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = { Text("Предупреждение") },
                        text = { Text("Изменения не будут сохранены. Выйти?") },
                        confirmButton = {
                            TextButton(onClick = {
                                showDialog = false
                                finish() // Завершаем активность, если пользователь подтвердил
                            }) {
                                Text("Да")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDialog = false }) {
                                Text("Нет")
                            }
                        }
                    )
                }

                EditRecipeScreen(
                    recipe = it,
                    categories = categories,
                    selectedCategory = selectedCategory,
                    name = name,
                    onNameChange = { name = it },
                    description = description,
                    onDescriptionChange = { description = it },
                    servings = servings,
                    onServingsChange = { servings = it },
                    cookingTime = cookingTime,
                    onCookingTimeChange = { cookingTime = it },
                    onCategorySelected = { selectedCategory = it },
                    onSave = { updatedRecipe ->
                        viewModel.updateRecipe(recipeId, authToken, updatedRecipe) { success ->
                            if (success) setResult(RESULT_OK)
                            finish()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun EditRecipeScreen(
    recipe: Recipe,
    categories: List<Category>,
    selectedCategory: Category?,
    name: String,
    onNameChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    servings: String,
    onServingsChange: (String) -> Unit,
    cookingTime: String,
    onCookingTimeChange: (String) -> Unit,
    onCategorySelected: (Category) -> Unit,
    onSave: (RecipeCRUD) -> Unit
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        CategorySelector(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected
        )

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Название рецепта") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Описание") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = servings,
            onValueChange = {
                if (it.isEmpty() || it.toIntOrNull() != null) onServingsChange(it)
            },
            label = { Text("Порции") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = cookingTime,
            onValueChange = {
                if (it.isEmpty() || it.toIntOrNull() != null) onCookingTimeChange(it)
            },
            label = { Text("Время готовки (мин)") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            val updatedRecipe = RecipeCRUD(
                userId = recipe.user.id,
                categoryId = selectedCategory?.id ?: recipe.category.id,
                name = name,
                description = description,
                servings = servings.toIntOrNull() ?: recipe.servings,
                cookingTime = cookingTime.toIntOrNull() ?: recipe.cookingTime
            )
            onSave(updatedRecipe)
        },
            modifier = Modifier.fillMaxWidth()) {
            Text("Сохранить изменения")
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelector(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .padding(vertical = 8.dp),
            readOnly = true,
            value = selectedCategory?.name ?: "Выберите категорию",
            onValueChange = {},
            label = { Text("Категория") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}