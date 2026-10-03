package com.example.data.remote.supabase

import android.content.Context
import android.util.Base64
import org.json.JSONObject

object SupabaseSessionStore {
    private const val PREFS = "supabase_session"
    private const val ACCESS_TOKEN = "access_token"
    private const val REFRESH_TOKEN = "refresh_token"
    private const val USER_ID = "user_id"
    private const val EXPIRES_AT = "expires_at"
    private var context: Context? = null

    fun initialize(value: Context) { context = value.applicationContext }
    private fun prefs() = context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun accessToken(): String? = prefs()?.getString(ACCESS_TOKEN, null)
    fun refreshToken(): String? = prefs()?.getString(REFRESH_TOKEN, null)
    fun userId(): String? = prefs()?.getString(USER_ID, null)?.takeIf { it.isNotBlank() }
        ?: accessToken()?.let { decodeUserId(it) }
    fun expiresAt(): Long = prefs()?.getLong(EXPIRES_AT, 0L) ?: 0L

    fun save(access: String, refresh: String?, userId: String? = null, expiresInSeconds: Long = 3600L) {
        val resolvedUid = userId?.takeIf { it.isNotBlank() } ?: decodeUserId(access)
        prefs()?.edit()
            ?.putString(ACCESS_TOKEN, access)
            ?.putString(REFRESH_TOKEN, refresh.orEmpty())
            ?.putString(USER_ID, resolvedUid.orEmpty())
            ?.putLong(EXPIRES_AT, System.currentTimeMillis() + expiresInSeconds * 1000L)
            ?.apply()
    }

    fun save(access: String, refresh: String?, expiresInSeconds: Long) {
        save(access, refresh, null, expiresInSeconds)
    }

    fun hasSession(): Boolean = !accessToken().isNullOrBlank()

    fun isExpired(safetyWindowMillis: Long = 60_000L): Boolean {
        val token = accessToken() ?: return true
        val storedExpiry = expiresAt()
        if (storedExpiry > 0L) return System.currentTimeMillis() + safetyWindowMillis >= storedExpiry
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return true
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP))
            val expirySeconds = JSONObject(payload).optLong("exp", 0L)
            expirySeconds <= 0L || System.currentTimeMillis() + safetyWindowMillis >= expirySeconds * 1000L
        } catch (_: Exception) { true }
    }

    fun clear() { prefs()?.edit()?.clear()?.apply() }

    private fun decodeUserId(jwt: String): String? = try {
        val p = jwt.split(".")
        if (p.size < 2) null
        else JSONObject(String(Base64.decode(p[1], Base64.URL_SAFE or Base64.NO_WRAP))).optString("sub").takeIf { it.isNotBlank() }
    } catch (_: Exception) { null }
}
