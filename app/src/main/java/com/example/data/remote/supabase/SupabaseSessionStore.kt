package com.example.data.remote.supabase

import android.content.Context
import android.util.Base64
import org.json.JSONObject

object SupabaseSessionStore {
    private const val PREFS = "supabase_session"
    private const val ACCESS_TOKEN = "access_token"
    private const val REFRESH_TOKEN = "refresh_token"
    private const val EXPIRES_AT = "expires_at"
    private const val USER_ID = "user_id"
    private var appContext: Context? = null

    fun initialize(value: Context) {
        appContext = value.applicationContext
    }

    private fun prefs() = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun accessToken(): String? = prefs()?.getString(ACCESS_TOKEN, null)?.takeIf { it.isNotBlank() }
    fun refreshToken(): String? = prefs()?.getString(REFRESH_TOKEN, null)?.takeIf { it.isNotBlank() }
    fun userId(): String? = prefs()?.getString(USER_ID, null)?.takeIf { it.isNotBlank() }
    fun expiresAt(): Long = prefs()?.getLong(EXPIRES_AT, 0L) ?: 0L

    fun save(access: String, refresh: String?, expiresInSeconds: Long = 3600L, uid: String? = null) {
        val extractedUid = uid?.takeIf { it.isNotBlank() } ?: decodeUserId(access)
        val expTime = System.currentTimeMillis() + (expiresInSeconds.coerceAtLeast(60L) * 1000L)
        prefs()?.edit()
            ?.putString(ACCESS_TOKEN, access)
            ?.putString(REFRESH_TOKEN, refresh.orEmpty())
            ?.putString(USER_ID, extractedUid.orEmpty())
            ?.putLong(EXPIRES_AT, expTime)
            ?.apply()
    }

    fun hasSession(): Boolean = !accessToken().isNullOrBlank()

    fun isExpired(safetyWindowMillis: Long = 60_000L): Boolean {
        val token = accessToken() ?: return true
        val storedExpiry = expiresAt()
        if (storedExpiry > 0L) {
            return System.currentTimeMillis() + safetyWindowMillis >= storedExpiry
        }
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return true
            val padded = parts[1].padEnd(parts[1].length + ((4 - parts[1].length % 4) % 4), '=')
            val payload = String(Base64.decode(padded, Base64.URL_SAFE or Base64.NO_WRAP))
            val expirySeconds = JSONObject(payload).optLong("exp", 0L)
            expirySeconds <= 0L || System.currentTimeMillis() + safetyWindowMillis >= (expirySeconds * 1000L)
        } catch (_: Exception) {
            true
        }
    }

    fun clear() {
        prefs()?.edit()?.clear()?.apply()
    }

    fun decodeUserId(jwt: String): String? = try {
        val parts = jwt.split(".")
        if (parts.size < 2) null
        else {
            val padded = parts[1].padEnd(parts[1].length + ((4 - parts[1].length % 4) % 4), '=')
            val payload = String(Base64.decode(padded, Base64.URL_SAFE or Base64.NO_WRAP))
            JSONObject(payload).optString("sub").takeIf { it.isNotBlank() }
        }
    } catch (_: Exception) {
        null
    }
}
