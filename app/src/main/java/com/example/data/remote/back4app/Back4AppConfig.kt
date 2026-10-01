package com.example.data.remote.back4app

import android.content.Context
import android.content.SharedPreferences

object Back4AppConfig {
    private const val PREFS_NAME = "back4app_prefs"
    private const val KEY_APP_ID = "back4app_app_id"
    private const val KEY_REST_KEY = "back4app_rest_key"
    private const val KEY_SERVER_URL = "back4app_server_url"
    private const val KEY_IS_ENABLED = "back4app_enabled"

    // Default Server URL for Back4App
    const val DEFAULT_SERVER_URL = "https://parseapi.back4app.com"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getAppId(context: Context): String {
        return getPrefs(context).getString(KEY_APP_ID, "") ?: ""
    }

    fun getRestKey(context: Context): String {
        return getPrefs(context).getString(KEY_REST_KEY, "") ?: ""
    }

    fun getServerUrl(context: Context): String {
        return getPrefs(context).getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    }

    fun isConfigured(context: Context): Boolean {
        return getAppId(context).isNotBlank() && getRestKey(context).isNotBlank()
    }

    fun saveConfig(context: Context, appId: String, restKey: String, serverUrl: String = DEFAULT_SERVER_URL) {
        getPrefs(context).edit()
            .putString(KEY_APP_ID, appId.trim())
            .putString(KEY_REST_KEY, restKey.trim())
            .putString(KEY_SERVER_URL, if (serverUrl.isBlank()) DEFAULT_SERVER_URL else serverUrl.trim())
            .putBoolean(KEY_IS_ENABLED, true)
            .apply()
    }

    fun clearConfig(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
