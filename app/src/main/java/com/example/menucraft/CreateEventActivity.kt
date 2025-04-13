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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import com.example.menucraft.data.EventCRUD
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
class CreateEventActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authToken = intent.getStringExtra("jwt_token") ?: ""
        val viewModel: EventViewModel by viewModels()

        setContent {
            CreateEventScreen(
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
@Composable
fun CreateEventScreen(
    viewModel: EventViewModel,
    authToken: String,
    onCreated: (Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val showDialog = remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var theme by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var guestsText by remember { mutableStateOf("") }

    var selectedDate by remember { mutableStateOf(LocalDateTime.now()) }
    val dateDialogVisible = remember { mutableStateOf(false) }

    val dateText = selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))

    val isAnyFieldNotEmpty = remember {
        derivedStateOf {
            name.isNotBlank() || theme.isNotBlank() || location.isNotBlank() ||
                    description.isNotBlank() || guestsText.isNotBlank()
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
            label = { Text("Название") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = theme,
            onValueChange = { theme = it },
            label = { Text("Тема") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Место") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Описание") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = guestsText,
            onValueChange = { guestsText = it },
            label = { Text("Количество гостей") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = dateText,
                onValueChange = {},
                label = { Text("Дата и время") },
                readOnly = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { dateDialogVisible.value = true },
                modifier = Modifier
                    .padding(top = 8.dp)
                    .height(56.dp)
                    .align(Alignment.CenterVertically)
            ) {
                Text("Выбрать")
            }
        }

        if (dateDialogVisible.value) {
            SelectDateTimeDialog(
                initialDate = selectedDate,
                onDismissRequest = { dateDialogVisible.value = false },
                onDateTimeSelected = {
                    selectedDate = it
                    dateDialogVisible.value = false
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val guests = guestsText.toIntOrNull() ?: 0
            val newEvent = EventCRUD(
                name = name,
                theme = theme,
                eventDate = selectedDate,
                location = location,
                description = description,
                guests = guests
            )

            viewModel.createEvent(authToken, newEvent) {
                Log.d("CreateEventScreen", "Мероприятие создано")
                onCreated(true)
            }
        }) {
            Text("Создать мероприятие")
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


