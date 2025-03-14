package com.example.menucraft

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.*
import androidx.lifecycle.ViewModel
import com.example.menucraft.util.AuthRequest
import com.example.menucraft.util.RetrofitInstance
import com.example.menucraft.util.saveToken


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val token = getToken(this)

        Log.d("AuthCheck", "Полученный токен: $token")

        if (token != null) {
            Log.d("AuthCheck", "Попытка обновления токена...")

            refreshToken(token) { isValid, newToken ->
                if (isValid) {
                    val finalToken = newToken ?: token
                    Log.d("AuthCheck", "Токен обновлён успешно: $finalToken")

                    saveToken(this, finalToken)
                    navigateToEventScreen(finalToken)
                } else {
                    Log.e("AuthCheck", "Ошибка обновления токена, переход на экран логина")

                    setContent { LoginScreen() }
                }
            }
        } else {
            Log.d("AuthCheck", "Токен отсутствует, показываем экран логина")

            setContent { LoginScreen() }
        }
    }

    private fun navigateToEventScreen(token: String) {
        val intent = Intent(this, OwnedEventActivity::class.java).apply {
            putExtra("jwt_token", token)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }
}


fun getToken(context: Context): String? {
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    return prefs.getString("jwt_token", null)
}


fun refreshToken(token: String, callback: (Boolean, String?) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            Log.d("RefreshToken", "Отправка запроса на обновление токена...")

            val response = RetrofitInstance.apiService.refreshToken(mapOf("token" to token))

            Log.d("RefreshToken", "Ответ от сервера: ${response.code()} - ${response.message()}")

            if (response.isSuccessful) {
                val newToken = response.body()?.get("token")
                Log.d("RefreshToken", "Новый токен: $newToken")

                withContext(Dispatchers.Main) {
                    callback(true, newToken)
                }
            } else {
                Log.e("RefreshToken", "Ошибка обновления токена: ${response.errorBody()?.string()}")

                withContext(Dispatchers.Main) {
                    callback(false, null)
                }
            }
        } catch (e: Exception) {
            Log.e("RefreshToken", "Исключение: ${e.message}", e)

            withContext(Dispatchers.Main) {
                callback(false, null)
            }
        }
    }
}


@Composable
@Preview
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

        TextButton(onClick = {
            context.startActivity(Intent(context, RegistrationActivity::class.java))
        }) {
            Text(text = "Зарегистрироваться", color = Color.Blue)
        }

        // Кнопка для входа
        Button(
            onClick = {
                isLoading = true
                viewModel.login(username, password) { success, token ->
                    isLoading = false
                    if (success) {
                        saveToken(context, token)
                        context.startActivity(
                            Intent(context, OwnedEventActivity::class.java).apply {
                                putExtra("jwt_token", token)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                        )
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


class LoginViewModel : ViewModel() {
    private val api = RetrofitInstance.apiService

    fun login(username: String, password: String, callback: (Boolean, String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("LoginViewModel", "Начинаем запрос авторизации...")

                val response = api.login(AuthRequest(username, password))

                Log.d("LoginViewModel", "Ответ от сервера: ${response}")

                withContext(Dispatchers.Main) {
                    callback(true, response)
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Ошибка при запросе авторизации: ${e.message}", e)

                withContext(Dispatchers.Main) {
                    callback(false, "Ошибка авторизации: ${e.message}")
                }
            }
        }
    }
}