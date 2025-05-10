package com.example.menucraft.util

import android.content.Context
import android.util.Base64
import org.json.JSONObject

fun saveToken(context: Context, token: String) {
    val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    with(sharedPref.edit()) {
        putString("jwt_token", token)
        apply()
    }
}

fun getUsernameFromJwt(token: String): String? {
    return try {
        val parts = token.split(".")
        if (parts.size != 3) return null

        val payload = parts[1]
        val decodedBytes = Base64.decode(payload, Base64.URL_SAFE)
        val json = JSONObject(String(decodedBytes))
        json.getString("sub")
    } catch (e: Exception) {
        null
    }
}