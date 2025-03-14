package com.example.menucraft.util

import android.content.Context

fun saveToken(context: Context, token: String) {
    val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    with(sharedPref.edit()) {
        putString("jwt_token", token)
        apply()
    }
}