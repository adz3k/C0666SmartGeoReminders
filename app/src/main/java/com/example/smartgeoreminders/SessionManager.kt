package com.example.smartgeoreminders

import android.content.Context

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("smartgeo_session", Context.MODE_PRIVATE)

    fun saveSession(userId: Long, email: String) {
        prefs.edit()
            .putBoolean("logged_in", true)
            .putLong("user_id", userId)
            .putString("email", email)
            .apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean("logged_in", false)

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
