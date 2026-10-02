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
            if (access.isNotBlank()) SupabaseSessionStore.save(access, json.optString("refresh_token"))
            Result.success(AuthUser(user.optString("id"), user.optString("email"), user.optJSONObject("user_metadata")?.optString("name"), user.optString("phone")))
        } catch (e: Exception) { Log.e(TAG, "Auth error", e); Result.failure(Exception("تعذر الاتصال بخدمة المصادقة", e)) }
    }
    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try { val r=http.newCall(authRequest("recover", JSONObject().put("email",email.trim()))).execute(); if(r.isSuccessful) Result.success(Unit) else Result.failure(Exception("تعذر إرسال رابط استعادة كلمة المرور")) } catch(e:Exception){Result.failure(e)}
    }
    fun signOut() { SupabaseSessionStore.clear() }
    private fun decodeUserId(jwt: String): String? = try { val p=jwt.split("."); if(p.size<2)null else JSONObject(String(android.util.Base64.decode(p[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP))).optString("sub").takeIf{it.isNotBlank()} } catch(_:Exception){null}
    private fun toArabicMessage(json: JSONObject): String = when(json.optString("error_code")){"invalid_credentials"->"البريد أو كلمة المرور غير صحيحة";"email_exists"->"البريد الإلكتروني مستخدم من قبل";"email_not_confirmed"->"يرجى تأكيد البريد الإلكتروني أولاً";else->json.optString("msg", "تعذر تنفيذ عملية المصادقة")}
}
