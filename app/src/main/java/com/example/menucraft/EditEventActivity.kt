package com.example.menucraft

import android.app.Activity
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menucraft.data.EventCRUD
import com.example.menucraft.util.RetrofitInstance
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.datetime.time.timepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
class EditEventActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val eventId = intent.getLongExtra("event_id", -1)
        val authToken = intent.getStringExtra("jwt_token") ?: ""

        val viewModel: EventViewModel by viewModels()

        setContent {
            EditEventScreen(
                viewModel = viewModel,
                eventId = eventId,
                authToken = authToken,
                onUpdate = { updated ->
                    if (updated) {
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
fun EditEventScreen(
    viewModel: EventViewModel,
    eventId: Long,
    authToken: String,
    onUpdate: (Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    val event by viewModel.eventDetails.collectAsState(initial = null)
    val context = LocalContext.current
    val showDialog = remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        viewModel.getEventByID(eventId, authToken)
    }

    if (event != null) {
        var name by remember { mutableStateOf(event!!.name) }
        var theme by remember { mutableStateOf(event!!.theme) }
        var location by remember { mutableStateOf(event!!.location) }
        var description by remember { mutableStateOf(event!!.description) }
        var guestsText by remember { mutableStateOf(event!!.guests.toString()) }

        var selectedDate by remember { mutableStateOf(event!!.eventDate) }
        val dateDialogVisible = remember { mutableStateOf(false) }

        val dateText = selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))

        val isFieldsChanged = remember {
            derivedStateOf {
                name != event!!.name || theme != event!!.theme || location != event!!.location ||
                        description != event!!.description || guestsText != event!!.guests.toString() || selectedDate != event!!.eventDate
            }
        }

        val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

        // Обработчик нажатия кнопки "Назад"
        onBackPressedDispatcher?.addCallback(
            context as LifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (isFieldsChanged.value) {
                        showDialog.value = true // Показываем диалог
                    } else {
                        onNavigateBack() // Возвращаемся без предупреждения
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
                verticalAlignment = Alignment.CenterVertically, // Для центровки по вертикали
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = dateText,
                    onValueChange = {},
                    label = { Text("Дата и время") },
                    readOnly = true,
                    modifier = Modifier
                        .weight(1f) // TextField занимает всю доступную ширину
                )

                Spacer(modifier = Modifier.width(8.dp)) // Отступ между полем и кнопкой

                Button(
                    onClick = {
                        dateDialogVisible.value = true
                    },
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .height(56.dp) // Высота кнопки совпадает с высотой поля
                        .align(Alignment.CenterVertically) // Центрируем кнопку по вертикали
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
                val updatedEvent = EventCRUD(
                    name = name,
                    theme = theme,
                    eventDate = selectedDate,
                    location = location,
                    description = description,
                    guests = guests
                )

                viewModel.updateEvent(eventId, authToken, updatedEvent) {
                    Log.d("EditEventScreen", "Мероприятие обновлено")
                    onUpdate(true)
                }
            }) {
                Text("Сохранить изменения")
            }
        }

        if (showDialog.value) {
            AlertDialog(
                onDismissRequest = { showDialog.value = false },
                title = { Text("Предупреждение") },
                text = { Text("Если вы покинете экран, изменения будут утеряны.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDialog.value = false
                            onNavigateBack() // Возвращаемся без сохранения
                        }
                    ) {
                        Text("Да")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDialog.value = false
                        }
                    ) {
                        Text("Отмена")
                    }
                }
            )
        }

    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SelectDateTimeDialog(
    initialDate: LocalDateTime,
    onDismissRequest: () -> Unit,
    onDateTimeSelected: (LocalDateTime) -> Unit
) {
    var selectedDateTime by remember { mutableStateOf(initialDate) }

    val dateDialogState = rememberMaterialDialogState()
    val timeDialogState = rememberMaterialDialogState()

    // Показываем сначала выбор даты
    LaunchedEffect(Unit) {
        dateDialogState.show()
    }

    MaterialDialog(
        dialogState = dateDialogState,
        buttons = {
            positiveButton("Выбрать") {
                dateDialogState.hide()
                timeDialogState.show()
            }
            negativeButton("Отмена") {
                dateDialogState.hide()
                onDismissRequest()
            }
        }
    ) {
        datepicker(
            initialDate = selectedDateTime.toLocalDate(),
            onDateChange = { newDate ->
                selectedDateTime = LocalDateTime.of(newDate, selectedDateTime.toLocalTime())
            }
        )
    }

    MaterialDialog(
        dialogState = timeDialogState,
        buttons = {
            positiveButton("Выбрать") {
                onDateTimeSelected(selectedDateTime)
            }
            negativeButton("Отмена") {
                onDismissRequest()
            }
        }
    ) {
        timepicker(
            initialTime = selectedDateTime.toLocalTime(),
            is24HourClock = true,
            onTimeChange = { newTime ->
                selectedDateTime = LocalDateTime.of(selectedDateTime.toLocalDate(), newTime)
            }
        )
    }
}