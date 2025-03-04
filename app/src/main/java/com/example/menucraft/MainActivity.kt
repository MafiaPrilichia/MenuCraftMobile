package com.example.menucraft

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import kotlinx.coroutines.*
import androidx.lifecycle.ViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoginScreen()
        }
    }
}

@Composable
fun LoginScreen(viewModel: LoginViewModel = viewModel()) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
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
        Text(text = "Вход в приложение", fontSize = 24.sp, color = Color.Black)

        Spacer(modifier = Modifier.height(20.dp))

        // Поле для ввода логина
        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Логин") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Поле для ввода пароля
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Кнопка для входа
        Button(
            onClick = {
                isLoading = true
                viewModel.login(username, password) { success, token ->
                    isLoading = false
                    if (success) {
                        saveToken(context, token)
                        context.startActivity(Intent(context, MainActivity::class.java))
                    } else {
                        error = token
                    }
                }
            },
            enabled = !isLoading
        ) {
            Text(text = if (isLoading) "Загрузка..." else "Войти")
        }

        // Отображение ошибки
        if (error.isNotEmpty()) {
            Text(text = error, color = Color.Red, modifier = Modifier.padding(top = 10.dp))
        }
    }
}


fun saveToken(context: Context, token: String) {
    val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    with(sharedPref.edit()) {
        putString("jwt_token", token)
        apply()
    }
}

interface ApiService {
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
}

data class LoginRequest(val username: String, val password: String)

data class LoginResponse(val token: String)

class LoginViewModel : ViewModel() {
    private val api = Retrofit.Builder()
        .baseUrl("http://192.168.0.100:8080")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

    fun login(username: String, password: String, callback: (Boolean, String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.login(LoginRequest(username, password))
                withContext(Dispatchers.Main) {
                    callback(true, response.token)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(false, "Ошибка авторизации")
                }
            }
        }
    }
}