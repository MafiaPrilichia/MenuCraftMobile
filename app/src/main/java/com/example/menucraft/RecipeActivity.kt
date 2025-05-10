package com.example.menucraft

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.menucraft.data.RecipeIngredientCRUD
import com.example.menucraft.util.getUsernameFromJwt
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class RecipeActivity : ComponentActivity() {
    private val viewModel: RecipeViewModel by viewModels()
    lateinit var editRecipeLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val recipeId = intent.getLongExtra("recipe_id", -1)
        val authToken = intent.getStringExtra("jwt_token") ?: ""

        if (recipeId == -1L) {
            Toast.makeText(this, "Не передан ID рецепта", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // регистрируем launcher
        editRecipeLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                lifecycleScope.launch {
                    viewModel.loadRecipe(recipeId, authToken)
                    viewModel.loadIngredientsForRecipe(recipeId, authToken)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.loadRecipe(recipeId, authToken)
            viewModel.loadIngredientsForRecipe(recipeId, authToken)
            viewModel.loadIngredients(authToken)
            viewModel.loadUnits(authToken)
        }

        setContent {
            RecipeScreen(
                viewModel = viewModel,
                authToken = authToken,
                onBack = { finish() },
                onEdit = {
                    val intent = Intent(this, EditRecipeActivity::class.java).apply {
                        putExtra("recipe_id", recipeId)
                        putExtra("jwt_token", authToken)
                    }
                    editRecipeLauncher.launch(intent)
                }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RecipeScreen(
    viewModel: RecipeViewModel,
    authToken: String,
    onBack: () -> kotlin.Unit,
    onEdit: () -> kotlin.Unit
) {
    val recipe by viewModel.recipe.collectAsState()
    val ingredientsForRecipe by viewModel.ingredientsForRecipe.collectAsState()
    val ingredients by viewModel.ingredients.collectAsState()
    val units by viewModel.units.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val currentUsername = getUsernameFromJwt(authToken)
    val isOwner = remember(recipe, currentUsername) {
        recipe?.user?.username == currentUsername
    }
    var showAddDialog by remember { mutableStateOf(false) }

    var ingredientToDelete by remember { mutableStateOf<Pair<Long, Long>?>(null) }
    var ingredientToEdit by remember { mutableStateOf<RecipeIngredientCRUD?>(null) }

    Scaffold(
        topBar = {
            RecipeTopBar(
                isOwner = isOwner,
                onEdit = { onEdit() },
                onDelete = { showDeleteDialog = true },
                onSave = {
                    val recipeId = recipe?.id ?: return@RecipeTopBar
                    Log.d("RecipeViewModel", "Recipe ID: ${recipe?.id}")
                    viewModel.saveRecipeFromAnotherUser(recipeId, authToken) { newRecipeId ->
                        Log.d("RecipeViewModel", "Recipe ID: $newRecipeId")
                        if (newRecipeId != null) {
                            Log.d("RecipeViewModel", "Recipe ID: $newRecipeId")
                            val updatedIntent = Intent(context, RecipeActivity::class.java).apply {
                                putExtra("recipe_id", newRecipeId)
                                putExtra("jwt_token", authToken)
                            }
                            context.startActivity(updatedIntent)
                            (context as? ComponentActivity)?.setResult(RESULT_OK)
                            (context as? ComponentActivity)?.finish()  // Закрываем текущую активность
                        } else {
                            Toast.makeText(context, "Ошибка сохранения рецепта", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (recipe == null) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = recipe!!.name, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))

                Text(text = "Категория: ${recipe!!.category.name}")
                Spacer(Modifier.height(8.dp))

                Text(text = "Автор: ${recipe!!.user.username}")
                Spacer(Modifier.height(8.dp))

                Text(text = recipe!!.description)
                Spacer(Modifier.height(8.dp))

                Text(text = "Порции: ${recipe!!.servings}")
                Spacer(Modifier.height(8.dp))

                Text(text = "Время приготовления: ${recipe!!.cookingTime} минут")
                Spacer(Modifier.height(8.dp))

                Text(text = "Общедоступный: ${if (recipe!!.isPublic) "Да" else "Нет"}")
                Spacer(Modifier.height(12.dp))

                Text(text = "Ингредиенты:", style = MaterialTheme.typography.titleMedium)
                if (ingredientsForRecipe.isEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Нет ингредиентов")
                } else {
                    ingredientsForRecipe.forEach { item ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 16.dp)
                                ) {
                                    Text(
                                        text = item.ingredient.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Text(
                                        text = "Количество: ${item.amount} ${item.unit.name}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                if (isOwner) {
                                    IconButton(
                                        onClick = {
                                            ingredientToEdit = RecipeIngredientCRUD(item.recipeId, item.ingredient.id, item.unit.id, item.amount.toDouble())
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Редактировать ингредиент",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            ingredientToDelete = Pair(recipe!!.id, item.ingredient.id)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Удалить ингредиент",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (isOwner) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Добавить ингредиент")
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        Log.d("DEBUG", "Ingredients loaded: ${ingredients.size}")
        Log.d("DEBUG", "Units loaded: ${units.size}")

        val addedIngredientIds = ingredientsForRecipe.map { it.ingredient.id }.toSet()

        val availableIngredients = ingredients
            .filterNot { addedIngredientIds.contains(it.id) }
            .map { it.name }

        AddIngredientDialog(
            ingredientsList = availableIngredients,
            unitsList = units.map { it.name },
            onDismiss = { showAddDialog = false },
            onConfirm = { ingredientName, unitName, amount ->
                showAddDialog = false

                val ingredientId = viewModel.ingredients.value.firstOrNull { it.name == ingredientName }?.id
                val unitId = viewModel.units.value.firstOrNull { it.name == unitName }?.id

                if (ingredientId != null && unitId != null) {
                    val recipeId = recipe?.id ?: return@AddIngredientDialog

                    val recipeIngredient = RecipeIngredientCRUD(
                        recipeId = recipeId,
                        ingredientId = ingredientId,
                        unitId = unitId,
                        amount = amount
                    )

                    viewModel.addIngredientToRecipe(
                        authToken,
                        recipeIngredient,
                        onSuccess = {
                            Toast.makeText(context, "Ингредиент успешно добавлен", Toast.LENGTH_SHORT).show()
                            viewModel.loadIngredientsForRecipe(recipeId, authToken)
                            (context as? ComponentActivity)?.setResult(RESULT_OK)
                        }
                    )
                }
            }
        )
    }



    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить блюдо?") },
            text = { Text("Вы уверены, что хотите удалить это блюдо?") },
            confirmButton = {
                Button(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteRecipe(authToken,
                        onSuccess = {
                            Toast.makeText(context, "Рецепт удалён", Toast.LENGTH_SHORT).show()
                            (context as? ComponentActivity)?.setResult(RESULT_OK)
                            onBack()
                        },
                        onError = { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        })
                }) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (ingredientToDelete != null) {
        val (recipeIdToDelete, ingredientIdToDelete) = ingredientToDelete!!
        AlertDialog(
            onDismissRequest = { ingredientToDelete = null },
            title = { Text("Удалить ингредиент?") },
            text = { Text("Вы уверены, что хотите удалить ингредиент?") },
            confirmButton = {
                Button(onClick = {
                    ingredientToDelete = null
                    viewModel.deleteIngredientFromRecipe(
                        recipeId = recipeIdToDelete,
                        ingredientId = ingredientIdToDelete,
                        authToken = authToken,
                        onSuccess = {
                            Toast.makeText(context, "Ингредиент удалён", Toast.LENGTH_SHORT).show()
                            viewModel.loadIngredientsForRecipe(recipeIdToDelete, authToken)
                            (context as? ComponentActivity)?.setResult(RESULT_OK)
                        }
                    )
                }) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                Button(onClick = { ingredientToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (ingredientToEdit != null) {
        EditIngredientDialog(
            currentUnit = units[units.map {it.id}.indexOf(ingredientToEdit!!.unitId)].name,
            currentAmount = ingredientToEdit!!.amount,
            unitsList = units.map { it.name }, // Список единиц измерения
            onDismiss = { ingredientToEdit = null }, // Закрыть диалог
            onConfirm = { selectedUnit, amount ->
                val updatedIngredient = ingredientToEdit!!

                val updated = RecipeIngredientCRUD(
                    recipeId = updatedIngredient.recipeId,
                    ingredientId = updatedIngredient.ingredientId,
                    unitId = units.find { it.name == selectedUnit}!!.id,
                    amount = amount
                )

                viewModel.updateRecipeIngredient(
                    updatedIngredient = updated,
                    authToken = authToken,
                    onSuccess = {
                        Toast.makeText(context, "Ингредиент обновлён", Toast.LENGTH_SHORT).show()
                        viewModel.loadIngredientsForRecipe(updated.recipeId, authToken)
                        ingredientToEdit = null
                        (context as? ComponentActivity)?.setResult(RESULT_OK)
                    }
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeTopBar(
    isOwner: Boolean,
    onEdit: () -> kotlin.Unit,
    onDelete: () -> kotlin.Unit,
    onSave: () -> kotlin.Unit // Добавляем обработчик сохранения
) {
    var expanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("Блюдо") },
        actions = {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Меню")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (isOwner) {
                    DropdownMenuItem(
                        text = { Text("Редактировать") },
                        onClick = {
                            expanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Удалить") },
                        onClick = {
                            expanded = false
                            onDelete()
                        }
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text("Сохранить у себя") },
                        onClick = {
                            expanded = false
                            // Вызываем сохранение
                            onSave()
                        }
                    )
                }
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleSelectDropdown(
    label: String,
    items: List<String>,
    selectedItem: String?,
    onItemSelected: (String) -> kotlin.Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .menuAnchor(),
            readOnly = true,
            value = selectedItem ?: "Выберите $label",
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 400.dp)
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onItemSelected(item)  // Обновляем выбранный элемент
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun AddIngredientDialog(
    ingredientsList: List<String>,
    unitsList: List<String>,
    onDismiss: () -> kotlin.Unit,
    onConfirm: (String, String, Double) -> kotlin.Unit
) {
    var selectedIngredient by remember { mutableStateOf<String?>(null) }
    var selectedUnit by remember { mutableStateOf<String?>(null) }
    var amountText by remember { mutableStateOf("") }

    val isConfirmEnabled = selectedIngredient != null &&
            selectedUnit != null &&
            amountText.toDoubleOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить ингредиент") },
        text = {
            Column {
                // Выпадающий список для выбора ингредиента
                SingleSelectDropdown(
                    label = "Ингредиент",
                    items = ingredientsList,
                    selectedItem = selectedIngredient,
                    onItemSelected = { selectedIngredient = it },
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Выпадающий список для выбора единицы измерения
                SingleSelectDropdown(
                    label = "Единица измерения",
                    items = unitsList,
                    selectedItem = selectedUnit,
                    onItemSelected = { selectedUnit = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Поле для ввода количества
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Количество") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedIngredient!!, selectedUnit!!, amountText.toDouble())
                },
                enabled = isConfirmEnabled
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}


@Composable
fun EditIngredientDialog(
    currentUnit: String,
    currentAmount: Double,
    unitsList: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    var selectedUnit by remember { mutableStateOf(currentUnit) }
    var amountText by remember { mutableStateOf(currentAmount.toString()) }

    val isConfirmEnabled = selectedUnit.isNotBlank() && amountText.toDoubleOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать ингредиент") },
        text = {
            Column {
                // Выпадающий список для выбора единицы измерения
                SingleSelectDropdown(
                    label = "Единица измерения",
                    items = unitsList,
                    selectedItem = selectedUnit,
                    onItemSelected = { selectedUnit = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Поле для ввода количества
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Количество") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedUnit, amountText.toDouble())
                },
                enabled = isConfirmEnabled
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}