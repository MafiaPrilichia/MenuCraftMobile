package com.example.menucraft.util

import android.os.Build
import androidx.annotation.RequiresApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.time.LocalDateTime

@RequiresApi(Build.VERSION_CODES.O)
object RetrofitInstance {
    private const val BASE_URL = "http://192.168.1.140:8080" // main pc
    //private const val BASE_URL = "http://192.168.1.140:8080"  // laptop

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
        .setLenient()
        .create()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}
