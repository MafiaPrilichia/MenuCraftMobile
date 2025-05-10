package com.example.menucraft

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menucraft.data.Category
import com.example.menucraft.data.EventRecipe
import com.example.menucraft.data.Recipe
import com.example.menucraft.util.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class RecipeListActivity : ComponentActivity() {

    private val viewModel: RecipeListViewModel by viewModels()

    private val recipeLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val authToken = intent.getStringExtra("jwt_token") ?: ""
            viewModel.loadRecipes(authToken)
        }
    }

    private val createRecipeLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val authToken = intent.getStringExtra("jwt_token") ?: ""
            viewModel.loadRecipes(authToken)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val eventId = intent.getLongExtra("event_id", -1L)
        val authToken = intent.getStringExtra("jwt_token") ?: ""
        val addedRecipeIds = intent.getLongArrayExtra("added_recipe_ids")?.toSet() ?: emptySet()

        viewModel.setAddedRecipeIds(addedRecipeIds)

        setContent {
            val context = LocalContext.current
            val recipes by viewModel.recipes.collectAsState()
            val categories by viewModel.categories.collectAsState()
            val selectedCategoryIds by viewModel.selectedCategoryIds.collectAsState()
            val isLoading by viewModel.isLoading.collectAsState()
            val errorMessage by viewModel.errorMessage.collectAsState()
            val addedRecipeIds by viewModel.addedRecipeIds.collectAsState()

            var selectedRecipeShort by remember { mutableStateOf<Recipe?>(null) }
            var showDialog by remember { mutableStateOf(false) }
            var dropdownExpanded by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(authToken) {
                viewModel.loadCategories(authToken)
                viewModel.loadRecipes(authToken)
                if (eventId != -1L) {
                    viewModel.loadAddedRecipes(eventId, authToken)
                }
            }

            Scaffold(
                topBar = {
                    TopAppBar(title = { Text("Выбор блюда") })
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            val intent = Intent(this, CreateRecipeActivity::class.java)
                            intent.putExtra("jwt_token", authToken)
                            createRecipeLauncher.launch(intent)
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить рецепт")
                    }
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    item {
                        ExposedDropdownMenuBox(
                            expanded = dropdownExpanded,
                            onExpandedChange = { dropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                readOnly = true,
                                value = if (selectedCategoryIds.isEmpty()) {
                                    "Все категории"
                                } else {
                                    categories.filter { selectedCategoryIds.contains(it.id) }
                                        .joinToString(", ") { it.name }
                                        .ifEmpty { "Все категории" }
                                },
                                onValueChange = {},
                                label = { Text("Фильтр по категориям") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                                },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )

                            ExposedDropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier.heightIn(max = 400.dp)
                            ) {
                                categories.forEach { category ->
                                    val isSelected = selectedCategoryIds.contains(category.id)
                                    DropdownMenuItem(
                                        text = { Text(category.name) },
                                        onClick = {
                                            val newSelected = selectedCategoryIds.toMutableSet()
                                            if (isSelected) newSelected.remove(category.id) else newSelected.add(category.id)
                                            viewModel.setSelectedCategories(newSelected)
                                            viewModel.loadRecipes(authToken, newSelected.toList())
                                        },
                                        leadingIcon = {
                                            if (isSelected) Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    when {
                        isLoading -> item {
                            Box(
                                modifier = Modifier
                                    .fillParentMaxSize()
                                    .padding(top = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        errorMessage.isNotEmpty() -> item {
                            Text(
                                text = errorMessage,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }

                        else -> {
                            items(recipes) { recipe ->
                                RecipeListItem(
                                    recipe = recipe,
                                    isAdded = addedRecipeIds.contains(recipe.id),
                                    onAddClick = {
                                        if (!addedRecipeIds.contains(recipe.id)) {
                                            selectedRecipeShort = recipe
                                            showDialog = true
                                        }
                                    },
                                    onRecipeClick = {
                                        val intent = Intent(context, RecipeActivity::class.java).apply {
                                            putExtra("recipe_id", recipe.id)
                                            putExtra("jwt_token", authToken)
                                        }
                                        recipeLauncher.launch(intent)
                                    }
                                )
                            }
                        }
                    }
                }

                if (showDialog && selectedRecipeShort != null) {
                    PortionsDialog(
                        onConfirm = { portions ->
                            showDialog = false
                            scope.launch {
                                viewModel.addRecipeToEvent(
                                    authToken,
                                    eventId,
                                    selectedRecipeShort!!.id,
                                    portions,
                                    onSuccess = {
                                        Toast.makeText(context, "Блюдо добавлено", Toast.LENGTH_SHORT).show()
                                        viewModel.loadRecipes(authToken)
                                        viewModel.loadAddedRecipes(eventId, authToken)
                                        setResult(RESULT_OK)
                                    },
                                    onError = { errorMsg ->
                                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        },
                        onDismiss = { showDialog = false }
                    )
                }
            }
        }
    }
}




@RequiresApi(Build.VERSION_CODES.O)
class RecipeListViewModel : ViewModel() {

    private val apiService = RetrofitInstance.apiService

    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> get() = _recipes

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> get() = _categories

    private val _selectedCategoryIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedCategoryIds: StateFlow<Set<Long>> get() = _selectedCategoryIds

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> get() = _errorMessage

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _addedRecipeIds = MutableStateFlow<Set<Long>>(emptySet())
    val addedRecipeIds: StateFlow<Set<Long>> get() = _addedRecipeIds

    fun setAddedRecipeIds(ids: Set<Long>) {
        _addedRecipeIds.value = ids
    }

    fun loadAddedRecipes(eventId: Long, authToken: String) {
        viewModelScope.launch {
            try {
                val response = apiService.getEventRecipes(eventId, "Bearer $authToken")
                _addedRecipeIds.value = response.map { it.recipe.id }.toSet()
            } catch (e: Exception) {
                Log.e("RecipeListVM", "Error loading added recipes", e)
            }
        }
    }

    fun loadRecipes(authToken: String, categoryIds: List<Long>? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = ""
            try {
                val categoryQuery = categoryIds?.joinToString(",")
                val response = if (categoryQuery.isNullOrEmpty()) {
                    apiService.getUserRecipes("Bearer $authToken")
                } else {
                    apiService.getUserRecipesFiltered("Bearer $authToken", categoryQuery)
                }
                _recipes.value = response
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка загрузки рецептов: ${e.message}"
                Log.e("RecipeListVM", "Error loading recipes", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadCategories(authToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = apiService.getCategories("Bearer $authToken")
                _categories.value = response
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка загрузки категорий: ${e.message}"
                Log.e("RecipeListVM", "Error loading categories", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setSelectedCategories(categories: Set<Long>) {
        _selectedCategoryIds.value = categories
    }

    fun addRecipeToEvent(
        authToken: String,
        eventId: Long,
        recipeId: Long,
        portions: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val body = EventRecipe(eventId, recipeId, portions)
                val response = apiService.addRecipeToEvent("Bearer $authToken", body)
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onError("Ошибка сервера: ${response.code()}")
                }
            } catch (e: Exception) {
                onError("Ошибка: ${e.message}")
                Log.e("RecipeListVM", "Error adding recipe to event", e)
            }
        }
    }
}

@Composable
fun RecipeListItem(
    recipe: Recipe,
    isAdded: Boolean,
    onAddClick: () -> Unit,
    onRecipeClick: () -> Unit   // Новый параметр для клика по карточке
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onRecipeClick() },  // Добавляем обработчик клика на всю карточку
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isAdded) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Категория: ${recipe.category.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "Пользователь: ${recipe.user.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = recipe.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 8.dp)
                )
                if (isAdded) {
                    Text(
                        text = "Уже добавлено",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            if (!isAdded) {
                IconButton(
                    onClick = onAddClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Добавить блюдо",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}


@Composable
fun PortionsDialog(onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var input by remember { mutableStateOf("") }
    val portions = input.toIntOrNull()
    val isValid = portions != null && portions > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Введите количество порций") },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.filter { c -> c.isDigit() } },
                label = { Text("Порции") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { if (isValid) onConfirm(portions!!) },
                enabled = isValid
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