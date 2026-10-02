package com.example.data.remote.auth

import android.util.Log
import com.example.BuildConfig
import com.example.data.remote.supabase.SupabaseSessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val phoneNumber: String?
)

class AuthRepository {
    companion object {
        private const val TAG = "SupabaseAdminAuthRepository"
        val ADMIN_EMAILS = setOf(
            "laminedz.19@gmail.com",
            "laminedz19@gmail.com",
            "achridz01@gmail.com",
            "admin@ocaventedz.dz",
            "admin@ocaventedz.com"
        )
    }

    private val http = OkHttpClient()

    private fun request(path: String, body: JSONObject? = null): Request {
        val builder = Request.Builder()
            .url("${BuildConfig.SUPABASE_URL}/auth/v1/$path")
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Content-Type", "application/json")
        if (body != null) {
            builder.post(body.toString().toRequestBody("application/json".toMediaType()))
        }
        return builder.build()
    }

    val currentUserId: String?
        get() = SupabaseSessionStore.accessToken()?.let(::decodeUserId)

    val currentUser: AuthUser?
        get() = currentUserId?.let { AuthUser(it, null, null, null) }

    suspend fun registerWithEmail(email: String, password: String): Result<AuthUser?> =
        authenticate("signup", email, password)

    suspend fun loginWithEmail(email: String, password: String): Result<AuthUser?> =
        authenticate("token?grant_type=password", email, password)

    suspend fun loginAdminWithClaims(email: String, password: String): Result<AuthUser> {
        val result = authenticate("token?grant_type=password", email, password)
        val user = result.getOrNull()
            ?: return Result.failure(result.exceptionOrNull() ?: Exception("تعذر تسجيل الدخول"))
        return if (ADMIN_EMAILS.contains(email.trim().lowercase()) || checkIsCurrentAdmin()) {
            Result.success(user)
        } else {
            signOut()
            Result.failure(SecurityException("هذا الحساب ليس مديرًا"))
        }
    }

    private suspend fun authenticate(
        path: String,
        email: String,
        password: String
    ): Result<AuthUser?> = withContext(Dispatchers.IO) {
        try {
            val response = http.newCall(
                request(
                    path,
                    JSONObject()
                        .put("email", email.trim())
                        .put("password", password)
                )
            ).execute()
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception(JSONObject(text).optString("msg", "تعذر تنفيذ المصادقة"))
                )
            }
            val json = JSONObject(text)
            val user = json.optJSONObject("user") ?: json
            json.optString("access_token")
                .takeIf { it.isNotBlank() }
                ?.let { SupabaseSessionStore.save(it, json.optString("refresh_token")) }
            Result.success(
                AuthUser(
                    uid = user.optString("id"),
                    email = user.optString("email"),
                    displayName = user.optJSONObject("user_metadata")?.optString("name"),
                    phoneNumber = user.optString("phone")
                )
            )
        } catch (error: Exception) {
            Log.e(TAG, "Auth error", error)
            Result.failure(Exception("تعذر الاتصال بخدمة المصادقة", error))
        }
    }

    suspend fun checkIsCurrentAdmin(): Boolean = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext false
        try {
            val response = Request.Builder()
                .url("${BuildConfig.SUPABASE_URL}/rest/v1/user_roles?user_id=eq.$uid&role=eq.admin&select=user_id&limit=1")
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer ${SupabaseSessionStore.accessToken()}")
                .build()
            val text = http.newCall(response).execute().body?.string().orEmpty()
            text.trim().startsWith("[") && text.contains(uid)
        } catch (_: Exception) {
            false
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = http.newCall(
                request("recover", JSONObject().put("email", email.trim()))
            ).execute()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("تعذر إرسال رابط استعادة كلمة المرور"))
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    fun signOut() {
        SupabaseSessionStore.clear()
    }

    private fun decodeUserId(jwt: String): String? = try {
        val parts = jwt.split(".")
        if (parts.size < 2) {
            null
        } else {
            JSONObject(
                String(
                    android.util.Base64.decode(
                        parts[1],
                        android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
                    )
                )
            ).optString("sub").takeIf { it.isNotBlank() }
        }
    } catch (_: Exception) {
        null
    }
}
