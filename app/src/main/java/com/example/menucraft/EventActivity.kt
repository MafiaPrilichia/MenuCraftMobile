package com.example.menucraft

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.example.menucraft.data.Event
import com.example.menucraft.data.EventCRUD
import com.example.menucraft.data.EventRecipe
import com.example.menucraft.data.EventRecipeShow
import com.example.menucraft.util.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

class EventActivity : FragmentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val eventId = intent.getLongExtra("event_id", -1)
        val authToken = intent.getStringExtra("jwt_token") ?: ""

        Log.d("EventActivity", "Получен eventId: $eventId, authToken: ${authToken.take(10)}...")

        val viewModel: EventViewModel by viewModels()

        if (eventId != -1L) {
            Log.d("EventActivity", "Запрашиваем данные о мероприятии $eventId")
            lifecycleScope.launch {
                viewModel.getEventByID(eventId, authToken)
                viewModel.getEventRecipes(eventId, authToken)
            }
        }

        val recipeListLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                lifecycleScope.launch {
                    viewModel.getEventByID(eventId, authToken)
                    viewModel.getEventRecipes(eventId, authToken)
                }
            }
        }

        setContent {
            EventScreen(
                viewModel = viewModel,
                eventId = eventId,
                authToken = authToken,
                onEventDeleted = { finish() },
                refresh = { setResult(RESULT_OK) },
                onAddRecipeClick = {
                    val intent = Intent(this, RecipeListActivity::class.java).apply {
                        putExtra("event_id", eventId)
                        putExtra("jwt_token", authToken)
                        putExtra("added_recipe_ids", viewModel.recipes.value.map { it.recipe.id }.toLongArray())
                    }
                    recipeListLauncher.launch(intent)
                }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EventScreen(
    viewModel: EventViewModel,
    eventId: Long,
    authToken: String,
    onEventDeleted: () -> Unit,
    refresh: () -> Unit,
    onAddRecipeClick: () -> Unit
) {
    val event by viewModel.eventDetails.collectAsState(initial = null)
    val recipes by viewModel.recipes.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var shouldRefresh by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var expandedMenu by remember { mutableStateOf(false) }
    var recipeToEdit by remember { mutableStateOf<EventRecipeShow?>(null) }
    var recipeToDelete by remember { mutableStateOf<EventRecipeShow?>(null) }
    var newPortions by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            shouldRefresh = true
            refresh()
        }
    }

    LaunchedEffect(shouldRefresh) {
        if (shouldRefresh) {
            isLoading = true
            errorMessage = ""
            try {
                viewModel.getEventByID(eventId, authToken)
                viewModel.getEventRecipes(eventId, authToken)
            } catch (e: Exception) {
                errorMessage = "Ошибка при загрузке мероприятия"
            } finally {
                isLoading = false
                shouldRefresh = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = event?.name ?: "Мероприятие") },
                actions = {
                    IconButton(onClick = { expandedMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                    }

                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Редактировать") },
                            onClick = {
                                expandedMenu = false
                                val intent = Intent(context, EditEventActivity::class.java).apply {
                                    putExtra("event_id", eventId)
                                    putExtra("jwt_token", authToken)
                                }
                                launcher.launch(intent)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Удалить") },
                            onClick = {
                                expandedMenu = false
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (event != null) {
            Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
                Text(text = "Тема: ${event?.theme}", fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "Дата: ${event?.eventDate?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))

                Text(text = "Локация: ${event?.location}", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))

                Text(text = "Количество гостей: ${event?.guests}", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))

                Text(text = "Описание: ${event?.description}", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))

                Text(text = "Дата создания: ${event?.createdAt?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))

                Text(text = "Дата обновления: ${event?.updatedAt?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}", fontSize = 14.sp)

                Spacer(modifier = Modifier.height(20.dp))
                Text(text = "Блюда:", fontSize = 16.sp)

                if (recipes.isEmpty()) {
                    Text(text = "Блюда не добавлены", fontSize = 14.sp)
                } else {
                    Column {
                        recipes.forEach { item ->
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
                                            text = item.recipe.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Text(
                                            text = "Порций: ${item.portions}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )

                                        Text(
                                            text = "Категория: ${item.recipe.category.name}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            recipeToEdit = item
                                            newPortions = item.portions.toString()
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Изменить порции",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            recipeToDelete = item
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Удалить блюдо",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }


                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = {
                    val intent = Intent(context, RecipeListActivity::class.java).apply {
                        putExtra("event_id", eventId)
                        putExtra("jwt_token", authToken)
                        putExtra("added_recipe_ids", recipes.map { it.recipe.id }.toLongArray())
                    }
                    launcher.launch(intent) // <--- правильно
                }) {
                    Text("Добавить блюдо")
                }
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Подтверждение удаления") },
                    text = { Text("Вы уверены, что хотите удалить это мероприятие?") },
                    confirmButton = {
                        Button(onClick = {
                            showDeleteDialog = false
                            viewModel.deleteEvent(eventId, authToken) {
                                refresh()
                                onEventDeleted()
                            }
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

            recipeToDelete?.let { item ->
                AlertDialog(
                    onDismissRequest = { recipeToDelete = null },
                    title = { Text("Удаление блюда") },
                    text = { Text("Удалить '${item.recipe.name}' из мероприятия?") },
                    confirmButton = {
                        Button(onClick = {
                            recipeToDelete = null
                            viewModel.deleteRecipeFromEvent(item.eventId, item.recipe.id, authToken) {
                                shouldRefresh = true
                            }
                        }) {
                            Text("Удалить")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { recipeToDelete = null }) {
                            Text("Отмена")
                        }
                    }
                )
            }

            recipeToEdit?.let { item ->
                AlertDialog(
                    onDismissRequest = { recipeToEdit = null },
                    title = { Text("Изменить порции") },
                    text = {
                        Column {
                            Text("Новое количество порций для '${item.recipe.name}':")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newPortions,
                                onValueChange = { newValue ->
                                    newPortions = newValue.filter { it.isDigit() }
                                },
                                label = { Text("Порции") },
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    keyboardType = KeyboardType.Number
                                )
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val portions = newPortions.toIntOrNull() ?: item.portions
                                if (portions > 0) {
                                    viewModel.updateRecipePortions(item, portions, authToken) {
                                        shouldRefresh = true
                                        refresh()
                                    }
                                    recipeToEdit = null
                                }
                            },
                            enabled = newPortions.isNotEmpty() && newPortions.toIntOrNull()?.let { it > 0 } ?: false
                        ) {
                            Text("Обновить")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { recipeToEdit = null }) {
                            Text("Отмена")
                        }
                    }
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Мероприятие не найдено")
            }
        }
    }
}




@RequiresApi(Build.VERSION_CODES.O)
class EventViewModel : ViewModel() {
    private val apiService = RetrofitInstance.apiService
    private val _eventDetails = MutableStateFlow<Event?>(null)
    val eventDetails: StateFlow<Event?> = _eventDetails
    private val _recipes = MutableStateFlow<List<EventRecipeShow>>(emptyList())
    val recipes: StateFlow<List<EventRecipeShow>> = _recipes

    fun getEventRecipes(eventId: Long, authToken: String) {
        viewModelScope.launch {
            try {
                val recipes = apiService.getEventRecipes(eventId, "Bearer $authToken")
                _recipes.value = recipes
            } catch (e: Exception) {
                Log.e("EventViewModel", "Ошибка при загрузке блюд: ${e.message}", e)
            }
        }
    }

    fun getEventByID(eventId: Long, authToken: String) {
        viewModelScope.launch {
            try {
                val event = apiService.getEventByID(eventId, "Bearer $authToken")
                _eventDetails.value = event
            } catch (e: Exception) {
                Log.e("EventViewModel", "Ошибка при загрузке мероприятия: ${e.message}", e)
            }
        }
    }

    fun createEvent(authToken: String, event: EventCRUD, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = apiService.createEvent("Bearer $authToken", event)
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    Log.e("EventViewModel", "Ошибка создания: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("EventViewModel", "Ошибка при создании мероприятия: ${e.message}", e)
            }
        }
    }

    fun updateEvent(eventId: Long, authToken: String, updatedEvent: EventCRUD, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = apiService.updateEvent(eventId, "Bearer $authToken", updatedEvent)
                if (response.isSuccessful) {
                    _eventDetails.value = response.body()
                    onSuccess()
                } else {
                    Log.e("EventViewModel", "Ошибка обновления: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("EventViewModel", "Ошибка при обновлении мероприятия: ${e.message}", e)
            }
        }
    }

    fun deleteEvent(eventId: Long, authToken: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteEvent(eventId, "Bearer $authToken")
                if (response.isSuccessful) {
                    _eventDetails.value = null
                    onSuccess()
                } else {
                    Log.e("EventViewModel", "Ошибка удаления: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("EventViewModel", "Ошибка при удалении мероприятия: ${e.message}", e)
            }
        }
    }

    fun deleteRecipeFromEvent(eventId: Long, recipeId: Long, authToken: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteEventRecipe(
                    auth = "Bearer $authToken",
                    eventId = eventId.toInt(),
                    recipeId = recipeId.toInt()
                )
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    Log.e("EventViewModel", "Ошибка при удалении блюда: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("EventViewModel", "Ошибка при удалении блюда: ${e.message}", e)
            }
        }
    }

    fun updateRecipePortions(eventRecipe: EventRecipeShow, newPortions: Int, authToken: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val updatedEventRecipe = EventRecipe(
                    eventId = eventRecipe.eventId,
                    recipeId = eventRecipe.recipe.id,
                    portions = newPortions
                )
                val response = apiService.updateEventRecipe(
                    auth = "Bearer $authToken",
                    body = updatedEventRecipe
                )
                if (response.isSuccessful) {
                    // Обновляем локальное состояние
                    _recipes.value = _recipes.value.map { recipe ->
                        if (recipe.recipe.id == eventRecipe.recipe.id) {
                            recipe.copy(portions = newPortions)
                        } else {
                            recipe
                        }
                    }
                    // Загружаем свежие данные с сервера
                    getEventRecipes(eventRecipe.eventId, authToken)
                    onSuccess()
                } else {
                    Log.e("EventViewModel", "Ошибка при обновлении порций: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("EventViewModel", "Ошибка при обновлении порций: ${e.message}", e)
            }
        }
    }

}