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
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class AuthUser(val uid: String, val email: String?, val displayName: String?, val phoneNumber: String?)

class AuthRepository {
    companion object { private const val TAG = "SupabaseAuthRepository" }
    private val http = OkHttpClient()
    private fun authRequest(path: String, body: JSONObject? = null, method: String = "POST"): Request {
        val b = Request.Builder().url("${BuildConfig.SUPABASE_URL}/auth/v1/$path")
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY).addHeader("Content-Type", "application/json")
        if (body != null) b.method(method, body.toString().toRequestBody("application/json".toMediaType())) else b.method(method, null)
        return b.build()
    }
    val currentUserId: String? get() = SupabaseSessionStore.accessToken()?.let { decodeUserId(it) }
    val currentUser: AuthUser? get() = currentUserId?.let { AuthUser(it, null, null, null) }
    suspend fun registerWithEmail(email: String, password: String): Result<AuthUser?> = authenticate("signup", email, password)
    suspend fun loginWithEmail(email: String, password: String): Result<AuthUser?> = authenticate("token?grant_type=password", email, password)
    private suspend fun authenticate(path: String, email: String, password: String): Result<AuthUser?> = withContext(Dispatchers.IO) {
        try {
            val response = http.newCall(authRequest(path, JSONObject().put("email", email.trim()).put("password", password))).execute()
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) return@withContext Result.failure(Exception(toArabicMessage(JSONObject(text))))
            val json = JSONObject(text); val user = json.optJSONObject("user") ?: json; val access = json.optString("access_token")
            if (access.isNotBlank()) SupabaseSessionStore.save(access, json.optString("refresh_token"), json.optLong("expires_in", 3600L))
            Result.success(AuthUser(user.optString("id"), user.optString("email"), user.optJSONObject("user_metadata")?.optString("name"), user.optString("phone")))
        } catch (e: Exception) { Log.e(TAG, "Auth error", e); Result.failure(Exception("تعذر الاتصال بخدمة المصادقة", e)) }
    }
    suspend fun restoreSession(): Result<AuthUser?> = withContext(Dispatchers.IO) {
        try {
            val access = SupabaseSessionStore.accessToken()
            val refresh = SupabaseSessionStore.refreshToken()
            if (access.isNullOrBlank()) return@withContext Result.success(null)
            if (!SupabaseSessionStore.isExpired()) return@withContext fetchCurrentUser(access)
            if (refresh.isNullOrBlank()) { SupabaseSessionStore.clear(); return@withContext Result.success(null) }
            val response = http.newCall(authRequest("token?grant_type=refresh_token", JSONObject().put("refresh_token", refresh))).execute()
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) { SupabaseSessionStore.clear(); return@withContext Result.failure(Exception("انتهت جلسة الدخول، يرجى تسجيل الدخول من جديد")) }
            val json = JSONObject(text)
            val newAccess = json.optString("access_token")
            if (newAccess.isBlank()) { SupabaseSessionStore.clear(); return@withContext Result.success(null) }
            SupabaseSessionStore.save(newAccess, json.optString("refresh_token").ifBlank { refresh }, json.optLong("expires_in", 3600L))
            fetchCurrentUser(newAccess)
        } catch (e: Exception) {
            Log.e(TAG, "Restore session error", e)
            // Keep the persisted session during a temporary network failure.
            // It is cleared only when Supabase explicitly rejects the refresh token
            // or when the user chooses Sign out.
            val message = when (e) {
                is SocketTimeoutException -> "انتهت مهلة الاتصال؛ ستبقى الجلسة محفوظة وسيُعاد المحاولة لاحقًا."
                is UnknownHostException, is IOException -> "تعذر الاتصال مؤقتًا؛ ستبقى الجلسة محفوظة حتى يعود الإنترنت."
                else -> "تعذر التحقق من الجلسة مؤقتًا؛ ستبقى الجلسة محفوظة."
            }
            Result.failure(Exception(message, e))
        }
    }
    private fun fetchCurrentUser(access: String): Result<AuthUser?> {
        val response = http.newCall(Request.Builder().url("${BuildConfig.SUPABASE_URL}/auth/v1/user").get()
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer $access").build()).execute()
        val text = response.body?.string().orEmpty()
        if (!response.isSuccessful) return Result.failure(Exception("جلسة الدخول غير صالحة"))
        val user = JSONObject(text)
        return Result.success(AuthUser(user.optString("id"), user.optString("email").takeIf { it.isNotBlank() }, user.optJSONObject("user_metadata")?.optString("name"), user.optString("phone").takeIf { it.isNotBlank() }))
    }
    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try { val r=http.newCall(authRequest("recover", JSONObject().put("email",email.trim()))).execute(); if(r.isSuccessful) Result.success(Unit) else Result.failure(Exception("تعذر إرسال رابط استعادة كلمة المرور")) } catch(e:Exception){Result.failure(e)}
    }
    fun signOut() { SupabaseSessionStore.clear() }
    private fun decodeUserId(jwt: String): String? = try { val p=jwt.split("."); if(p.size<2)null else JSONObject(String(android.util.Base64.decode(p[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP))).optString("sub").takeIf{it.isNotBlank()} } catch(_:Exception){null}
    private fun toArabicMessage(json: JSONObject): String = when(json.optString("error_code")){"invalid_credentials"->"البريد أو كلمة المرور غير صحيحة";"email_exists"->"البريد الإلكتروني مستخدم من قبل";"email_not_confirmed"->"يرجى تأكيد البريد الإلكتروني أولاً";else->json.optString("msg", "تعذر تنفيذ عملية المصادقة")}
}
