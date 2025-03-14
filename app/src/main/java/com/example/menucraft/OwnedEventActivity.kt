package com.example.menucraft

import EventActivity
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.menucraft.data.EventShort
import com.example.menucraft.util.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.format.DateTimeFormatter

class OwnedEventActivity : ComponentActivity() {
    private val TAG = "EventActivity" // Для логирования

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val authToken = intent.getStringExtra("jwt_token") ?: ""
        Log.d(TAG, "onCreate: Получен токен: $authToken")

        setContent {
            OwnedEventScreen(authToken = authToken)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun OwnedEventScreen(authToken: String, vm: OwnedEventViewModel = viewModel()) {
    val events by vm.events.collectAsState(initial = emptyList())
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    // Логирование при загрузке данных
    LaunchedEffect(Unit) {
        Log.d("EventScreen", "Загружаем мероприятия для токена: $authToken")
        try {
            vm.getOwnedEvents(authToken)
            Log.d("EventScreen", "Мероприятия успешно загружены")
            isLoading = false
        } catch (e: Exception) {
            errorMessage = "Ошибка при загрузке мероприятий"
            Log.e("EventScreen", "Ошибка при загрузке мероприятий: ${e.message}", e)
            isLoading = false
        }
    }

    // Отображение UI
    if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.fillMaxSize())
    } else if (errorMessage.isNotEmpty()) {
        Text(text = errorMessage, color = Color.Red)
    } else {
        LazyColumn {
            items(events) { event ->
                EventItem(event = event, onClick = {
                    // Переход на экран с полной информацией о мероприятии
                    val context = LocalContext.current
                    val intent = Intent(context, EventActivity::class.java).apply {
                        putExtra("event_id", event.id)
                        putExtra("jwt_token", authToken)
                    }
                    context.startActivity(intent)
                })
            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EventItem(event: EventShort, onClick: @Composable () -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    val formattedDate = event.eventDate.format(formatter)

    // Карточка для мероприятия
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF1F1F1),
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Мероприятие: ${event.name}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Тема: ${event.theme}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Дата: $formattedDate",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Локация: ${event.location}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Количество гостей: ${event.guests}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
        }
    }
}


class OwnedEventViewModel : ViewModel() {
    private val apiService = RetrofitInstance.apiService
    private val _events = MutableStateFlow<List<EventShort>>(emptyList())
    val events: StateFlow<List<EventShort>> = _events

    private val TAG = "EventViewModel" // Для логирования

    suspend fun getOwnedEvents(authToken: String) {
        Log.d(TAG, "getOwnedEvents: Загружаем мероприятия для токена $authToken")
        try {
            val fetchedEvents = apiService.getOwnedEvents("Bearer $authToken")
            _events.value = fetchedEvents
            Log.d(TAG, "getOwnedEvents: Мероприятия успешно получены")
        } catch (e: Exception) {
            Log.e(TAG, "getOwnedEvents: Ошибка при загрузке мероприятий: ${e.message}", e)
            throw e // Пробрасываем ошибку дальше
        }
    }
}