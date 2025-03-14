import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.example.menucraft.data.Event
import com.example.menucraft.util.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.format.DateTimeFormatter

class EventActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Получаем ID события и JWT токен из Intent
        val eventId = intent.getLongExtra("event_id", -1)
        val authToken = intent.getStringExtra("jwt_token") ?: ""

        // Логика для запроса подробной информации о мероприятии
        val viewModel: EventViewModel = viewModel()

        if (eventId != -1L) {
            viewModel.getEventDetails(eventId, authToken)
        }

        setContent {
            EventScreen(viewModel)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EventScreen(viewModel: EventViewModel) {
    val eventDetails by viewModel.eventDetails.collectAsState(initial = null)

    if (eventDetails != null) {
        // Отображаем информацию о мероприятии
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Мероприятие: ${eventDetails?.name}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Тема: ${eventDetails?.theme}", fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))

            Text(text = "Дата: ${eventDetails?.eventDate?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}", fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))

            Text(text = "Локация: ${eventDetails?.location}", fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))

            Text(text = "Количество гостей: ${eventDetails?.guests}", fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))

            Text(text = "Описание: ${eventDetails?.description}", fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))

            Text(text = "Дата создания: ${eventDetails?.createdAt?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}", fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))

            Text(text = "Дата обновления: ${eventDetails?.updatedAt?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}", fontSize = 14.sp)
        }
    } else {
        CircularProgressIndicator()
    }
}


class EventViewModel : ViewModel() {
    private val apiService = RetrofitInstance.apiService
    private val _eventDetails = MutableStateFlow<Event?>(null)
    val eventDetails: StateFlow<Event?> = _eventDetails

    suspend fun getEventDetails(eventId: Long, authToken: String) {
        try {
            val event = apiService.getEventDetails(eventId, "Bearer $authToken")
            _eventDetails.value = event
        } catch (e: Exception) {
            // Обработка ошибок
        }
    }
}
