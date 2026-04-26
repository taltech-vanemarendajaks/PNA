package com.example.callgrabber

import android.content.Context
import androidx.core.content.edit

object AuthStorage {
    private const val PREFS_NAME = "AppAuth"
    private const val KEY_GOOGLE_ID_TOKEN = "google_id_token"
    private const val KEY_USER_NAME = "user_name"

    fun saveLogin(context: Context, token: String, displayName: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_GOOGLE_ID_TOKEN, token)
                    .putString(KEY_USER_NAME, displayName)
            }
    }

    fun getToken(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_GOOGLE_ID_TOKEN, null)
    }

    fun getUserName(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_NAME, null)
    }
}