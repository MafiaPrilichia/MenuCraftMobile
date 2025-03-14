package com.example.menucraft

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.menucraft.util.AuthRequest
import com.example.menucraft.util.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.menucraft.util.saveToken

class RegistrationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RegistrationScreen()
        }
    }

}

@Composable
@Preview
fun RegistrationScreen(viewModel: RegistrationViewModel = viewModel()) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Регистрация", fontSize = 24.sp, color = Color.Black)

        Spacer(modifier = Modifier.height(20.dp))

        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Логин") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        TextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Повторите пароль") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (password == confirmPassword) {
                    error = ""
                    isLoading = true
                    viewModel.register(username, password) { success, message, token ->
                        isLoading = false
                        if (success && token != null) {
                            saveToken(context, token)
                            context.startActivity(
                                Intent(context, OwnedEventActivity::class.java).apply {
                                    putExtra("jwt_token", token)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                            )
                        } else {
                            error = message
                        }
                    }
                } else {
                    error = "Пароли не совпадают!"
                }
            },
            enabled = !isLoading
        ) {
            Text(text = if (isLoading) "Загрузка..." else "Зарегистрироваться")
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (error.isNotEmpty()) {
            Text(text = error, color = Color.Red, modifier = Modifier.padding(8.dp))
        }
    }
}


class RegistrationViewModel : ViewModel() {
    private val api = RetrofitInstance.apiService

    fun register(username: String, password: String, callback: (Boolean, String, String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("RegisterViewModel", "Отправка данных на сервер...")

                api.register(AuthRequest(username, password))
                Log.d("RegisterViewModel", "Регистрация успешна")

                val token = api.login(AuthRequest(username, password))
                Log.d("RegisterViewModel", "Авторизация успешна, токен: $token")

                withContext(Dispatchers.Main) {
                    callback(true, "Регистрация прошла успешно!", token)
                }
            } catch (e: Exception) {
                Log.e("RegisterViewModel", "Ошибка: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(false, "Ошибка регистрации: ${e.message}", null)
                }
            }
        }
    }
}