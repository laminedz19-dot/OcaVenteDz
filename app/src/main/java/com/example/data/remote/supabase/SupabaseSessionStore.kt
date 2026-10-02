package com.example.data.remote.supabase

import android.content.Context

object SupabaseSessionStore {
    private const val PREFS = "supabase_session"
    private const val ACCESS_TOKEN = "access_token"
    private const val REFRESH_TOKEN = "refresh_token"
    private const val EXPIRES_AT = "expires_at"
    private var context: Context? = null
    fun initialize(value: Context) { context = value.applicationContext }
    private fun prefs() = context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun accessToken(): String? = prefs()?.getString(ACCESS_TOKEN, null)
    fun refreshToken(): String? = prefs()?.getString(REFRESH_TOKEN, null)
    fun expiresAt(): Long = prefs()?.getLong(EXPIRES_AT, 0L) ?: 0L
    fun save(access: String, refresh: String?, expiresInSeconds: Long = 3600L) {
        prefs()?.edit()?.putString(ACCESS_TOKEN, access)?.putString(REFRESH_TOKEN, refresh.orEmpty())
            ?.putLong(EXPIRES_AT, System.currentTimeMillis() + expiresInSeconds * 1000L)?.apply()
    }
    fun hasSession(): Boolean = !accessToken().isNullOrBlank()
    fun isExpired(safetyWindowMillis: Long = 60_000L): Boolean {
        val token = accessToken() ?: return true
        val storedExpiry = expiresAt()
        if (storedExpiry > 0L) return System.currentTimeMillis() + safetyWindowMillis >= storedExpiry
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return true
            val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP))
            val expirySeconds = org.json.JSONObject(payload).optLong("exp", 0L)
            expirySeconds <= 0L || System.currentTimeMillis() + safetyWindowMillis >= expirySeconds * 1000L
        } catch (_: Exception) { true }
    }
    fun clear() { prefs()?.edit()?.clear()?.apply() }
}
