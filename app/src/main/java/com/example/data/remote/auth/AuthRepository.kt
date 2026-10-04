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
        private const val TAG = "SupabaseAuthRepository"
    }

    private val http = OkHttpClient()

    private fun authRequest(path: String, body: JSONObject? = null, method: String = "POST"): Request {
        val b = Request.Builder().url("${BuildConfig.SUPABASE_URL}/auth/v1/$path")
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Content-Type", "application/json")
        if (body != null) {
            b.method(method, body.toString().toRequestBody("application/json".toMediaType()))
        } else {
            b.method(method, null)
        }
        return b.build()
    }

    val currentUserId: String?
        get() = SupabaseSessionStore.userId()

    val currentUser: AuthUser?
        get() = currentUserId?.let { AuthUser(it, null, null, null) }

    suspend fun registerWithEmail(email: String, password: String): Result<AuthUser?> =
        authenticate("signup", email, password)

    suspend fun loginWithEmail(email: String, password: String): Result<AuthUser?> =
        authenticate("token?grant_type=password", email, password)

    /**
     * إرسال رمز المصادقة (OTP) مباشرة إلى البريد الإلكتروني.
     */
    suspend fun sendEmailOtp(email: String, shouldCreateUser: Boolean = true): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) {
            return@withContext Result.failure(Exception("يرجى إدخال البريد الإلكتروني."))
        }
        try {
            val body = JSONObject().apply {
                put("email", cleanEmail)
                put("create_user", shouldCreateUser)
                val options = JSONObject().apply {
                    put("email_redirect_to", "ocaventedz://auth")
                }
                put("options", options)
            }
            val response = http.newCall(authRequest("otp", body)).execute()
            val text = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                Log.d(TAG, "OTP sent successfully to $cleanEmail")
                Result.success(Unit)
            } else {
                val json = runCatching { JSONObject(text) }.getOrNull() ?: JSONObject()
                Result.failure(Exception(toArabicMessage(json)))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending email OTP to $cleanEmail", e)
            Result.failure(Exception("تعذر إرسال رمز المصادقة إلى البريد. يرجى التحقق من اتصالك بالإنترنت."))
        }
    }

    /**
     * التحقق من رمز المصادقة (OTP) المرسل للبريد الإلكتروني وتثبيت جلسة الدخول.
     */
    suspend fun verifyEmailOtp(email: String, token: String, type: String = "email"): Result<AuthUser?> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val cleanToken = token.trim()
        if (cleanEmail.isBlank() || cleanToken.isBlank()) {
            return@withContext Result.failure(Exception("يرجى إدخال البريد الإلكتروني ورمز التحقق."))
        }
        try {
            var body = JSONObject().apply {
                put("type", type)
                put("email", cleanEmail)
                put("token", cleanToken)
            }
            var response = http.newCall(authRequest("verify", body)).execute()
            var text = response.body?.string().orEmpty()

            if (!response.isSuccessful && type == "email") {
                body = JSONObject().apply {
                    put("type", "signup")
                    put("email", cleanEmail)
                    put("token", cleanToken)
                }
                val retry = http.newCall(authRequest("verify", body)).execute()
                val retryText = retry.body?.string().orEmpty()
                if (retry.isSuccessful) {
                    response = retry
                    text = retryText
                }
            }

            if (!response.isSuccessful) {
                val json = runCatching { JSONObject(text) }.getOrNull() ?: JSONObject()
                return@withContext Result.failure(Exception(toArabicMessage(json)))
            }

            val json = JSONObject(text)
            val user = json.optJSONObject("user") ?: json
            val access = json.optString("access_token")
            val uid = user.optString("id")
            if (access.isNotBlank()) {
                SupabaseSessionStore.save(
                    access = access,
                    refresh = json.optString("refresh_token"),
                    userId = uid,
                    expiresInSeconds = json.optLong("expires_in", 3600L)
                )
            }
            Result.success(
                AuthUser(
                    uid = uid,
                    email = user.optString("email"),
                    displayName = user.optJSONObject("user_metadata")?.optString("name"),
                    phoneNumber = user.optString("phone")
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Verify OTP error", e)
            Result.failure(Exception("تعذر التحقق من رمز المصادقة", e))
        }
    }

    suspend fun updateUserPassword(newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        val token = SupabaseSessionStore.accessToken() ?: return@withContext Result.failure(Exception("جلسة الدخول غير صالحة"))
        try {
            val req = Request.Builder()
                .url("${BuildConfig.SUPABASE_URL}/auth/v1/user")
                .put(JSONObject().put("password", newPassword).toString().toRequestBody("application/json".toMediaType()))
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $token")
                .build()
            val res = http.newCall(req).execute()
            if (res.isSuccessful) Result.success(Unit)
            else {
                val json = runCatching { JSONObject(res.body?.string().orEmpty()) }.getOrNull() ?: JSONObject()
                Result.failure(Exception(toArabicMessage(json)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun authenticate(path: String, email: String, password: String): Result<AuthUser?> = withContext(Dispatchers.IO) {
        try {
            val response = http.newCall(
                authRequest(
                    path,
                    JSONObject().put("email", email.trim()).put("password", password)
                )
            ).execute()
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val json = runCatching { JSONObject(text) }.getOrNull() ?: JSONObject()
                return@withContext Result.failure(Exception(toArabicMessage(json)))
            }
            val json = JSONObject(text)
            val user = json.optJSONObject("user") ?: json
            val access = json.optString("access_token")
            val uid = user.optString("id")
            if (access.isNotBlank()) {
                SupabaseSessionStore.save(
                    access = access,
                    refresh = json.optString("refresh_token"),
                    userId = uid,
                    expiresInSeconds = json.optLong("expires_in", 3600L)
                )
            }
            Result.success(
                AuthUser(
                    uid = uid,
                    email = user.optString("email"),
                    displayName = user.optJSONObject("user_metadata")?.optString("name"),
                    phoneNumber = user.optString("phone")
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Auth error", e)
            Result.failure(Exception("تعذر الاتصال بخدمة المصادقة", e))
        }
    }

    suspend fun restoreSession(): Result<AuthUser?> = withContext(Dispatchers.IO) {
        try {
            val access = SupabaseSessionStore.accessToken()
            val refresh = SupabaseSessionStore.refreshToken()
            if (access.isNullOrBlank()) return@withContext Result.success(null)
            if (!SupabaseSessionStore.isExpired()) return@withContext fetchCurrentUser(access)
            if (refresh.isNullOrBlank()) {
                SupabaseSessionStore.clear()
                return@withContext Result.success(null)
            }
            val response = http.newCall(
                authRequest("token?grant_type=refresh_token", JSONObject().put("refresh_token", refresh))
            ).execute()
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                SupabaseSessionStore.clear()
                return@withContext Result.failure(Exception("انتهت جلسة الدخول، يرجى تسجيل الدخول من جديد"))
            }
            val json = JSONObject(text)
            val newAccess = json.optString("access_token")
            if (newAccess.isBlank()) {
                SupabaseSessionStore.clear()
                return@withContext Result.success(null)
            }
            val user = json.optJSONObject("user")
            val uid = user?.optString("id") ?: SupabaseSessionStore.userId()
            SupabaseSessionStore.save(
                access = newAccess,
                refresh = json.optString("refresh_token").ifBlank { refresh },
                userId = uid,
                expiresInSeconds = json.optLong("expires_in", 3600L)
            )
            fetchCurrentUser(newAccess)
        } catch (e: Exception) {
            Log.e(TAG, "Restore session error", e)
            SupabaseSessionStore.clear()
            Result.failure(Exception("تعذر استعادة جلسة الدخول", e))
        }
    }

    private fun fetchCurrentUser(access: String): Result<AuthUser?> {
        val response = http.newCall(
            Request.Builder()
                .url("${BuildConfig.SUPABASE_URL}/auth/v1/user")
                .get()
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $access")
                .build()
        ).execute()
        val text = response.body?.string().orEmpty()
        if (!response.isSuccessful) return Result.failure(Exception("جلسة الدخول غير صالحة"))
        val user = JSONObject(text)
        return Result.success(
            AuthUser(
                uid = user.optString("id"),
                email = user.optString("email").takeIf { it.isNotBlank() },
                displayName = user.optJSONObject("user_metadata")?.optString("name"),
                phoneNumber = user.optString("phone").takeIf { it.isNotBlank() }
            )
        )
    }

    suspend fun checkIsAdmin(): Boolean = withContext(Dispatchers.IO) {
        val token = SupabaseSessionStore.accessToken() ?: return@withContext false
        try {
            val rpcReq = Request.Builder()
                .url("${BuildConfig.SUPABASE_URL}/rest/v1/rpc/is_admin")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $token")
                .build()
            val rpcResponse = http.newCall(rpcReq).execute()
            val rpcBody = rpcResponse.body?.string().orEmpty().trim()
            if (rpcResponse.isSuccessful && rpcBody.equals("true", ignoreCase = true)) {
                return@withContext true
            }

            val uid = currentUserId ?: return@withContext false
            val tableReq = Request.Builder()
                .url("${BuildConfig.SUPABASE_URL}/rest/v1/user_roles?user_id=eq.$uid&role=eq.admin&select=user_id&limit=1")
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $token")
                .build()
            val tableResponse = http.newCall(tableReq).execute()
            val tableBody = tableResponse.body?.string().orEmpty().trim()
            tableResponse.isSuccessful && tableBody.startsWith("[") && tableBody.contains(uid)
        } catch (_: Exception) {
            false
        }
    }

    suspend fun checkIsCurrentAdmin(): Boolean = checkIsAdmin()

    suspend fun changeCurrentPassword(currentPass: String, newPass: String): Result<Unit> = withContext(Dispatchers.IO) {
        val email = currentUser?.email
        if (email.isNullOrBlank()) {
            return@withContext updateUserPassword(newPass)
        }
        val loginCheck = loginWithEmail(email, currentPass)
        if (loginCheck.isFailure) {
            return@withContext Result.failure(Exception("كلمة المرور الحالية غير صحيحة."))
        }
        updateUserPassword(newPass)
    }

    suspend fun loginAdminWithClaims(email: String, pass: String): Result<AuthUser> = withContext(Dispatchers.IO) {
        val res = loginWithEmail(email, pass)
        val user = res.getOrNull()
        if (res.isFailure || user == null) {
            return@withContext Result.failure(res.exceptionOrNull() ?: Exception("فشل تسجيل الدخول للمشرف"))
        }
        val isAdmin = checkIsAdmin()
        if (!isAdmin) {
            signOut()
            return@withContext Result.failure(Exception("هذا الحساب ليس لديه صلاحيات المشرف."))
        }
        Result.success(user)
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val r = http.newCall(authRequest("recover", JSONObject().put("email", email.trim()))).execute()
            if (r.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("تعذر إرسال رابط استعادة كلمة المرور"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        SupabaseSessionStore.clear()
    }

    private fun toArabicMessage(json: JSONObject): String {
        val code = json.optString("error_code").ifBlank { json.optString("code") }
        val desc = json.optString("error_description")
            .ifBlank { json.optString("message") }
            .ifBlank { json.optString("error") }
            .ifBlank { json.optString("msg") }

        return when {
            code == "invalid_credentials" || desc.contains("invalid login credentials", ignoreCase = true) ->
                "بيانات الدخول غير صحيحة. يرجى التأكد من البريد أو كلمة المرور."
            code == "otp_expired" || desc.contains("expired", ignoreCase = true) ->
                "رمز التحقق منتهي الصلاحية. يرجى طلب رمز جديد."
            code == "invalid_otp" || desc.contains("token has expired or is invalid", ignoreCase = true) || desc.contains("Token is invalid", ignoreCase = true) ->
                "رمز التحقق غير صحيح أو غير صالح. يرجى التأكد من الرمز وإعادة المحاولة."
            code == "email_exists" || code == "user_already_exists" || desc.contains("already registered", ignoreCase = true) ->
                "هذا الحساب مسجل مسبقاً. يرجى تسجيل الدخول بدلاً من إنشاء حساب جديد."
            code == "email_not_confirmed" || desc.contains("Email not confirmed", ignoreCase = true) ->
                "يرجى تأكيد الحساب عبر البريد الإلكتروني للمتابعة."
            code == "weak_password" || desc.contains("Password should be", ignoreCase = true) ->
                "كلمة المرور يجب أن تتكون من 6 أحرف أو أرقام على الأقل."
            code == "over_request_rate_limit" || code == "over_email_send_rate_limit" || desc.contains("rate limit", ignoreCase = true) || desc.contains("security purposes", ignoreCase = true) ->
                "تم تجاوز عدد المحاولات المسموح بها مؤقتاً، يرجى الانتظار دقيقة والمحاولة مجدداً."
            desc.isNotBlank() -> desc
            else -> "تعذر إتمام عملية المصادقة مع الخادم."
        }
    }
}
