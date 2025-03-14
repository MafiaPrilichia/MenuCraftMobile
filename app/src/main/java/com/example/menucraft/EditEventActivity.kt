package com.example.menucraft

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class EditEventActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val eventId = intent.getLongExtra("event_id", -1)
        val eventName = intent.getStringExtra("event_name") ?: ""
        val eventDate = intent.getStringExtra("event_date") ?: ""

        setContent {
            EditEventScreen(eventId = eventId, eventName = eventName, eventDate = eventDate)
        }
    }
}

@Composable
fun EditEventScreen(eventId: Long, eventName: String, eventDate: String) {
    var name by remember { mutableStateOf(eventName) }
    var date by remember { mutableStateOf(eventDate) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Редактирование мероприятия", fontSize = 24.sp)
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Название мероприятия") },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
        TextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Дата мероприятия") },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
        Button(
            onClick = {
                // Логика сохранения изменений мероприятия
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(text = "Сохранить изменения")
        }
    }
}