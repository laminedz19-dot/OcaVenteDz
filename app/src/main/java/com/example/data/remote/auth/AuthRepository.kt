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
import java.util.concurrent.TimeUnit

data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val phoneNumber: String?
)

class AuthRepository {
    companion object {
        private const val TAG = "SupabaseAuth"
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun authRequest(path: String, body: JSONObject? = null, method: String = "POST", token: String? = null): Request {
        val builder = Request.Builder()
            .url("${BuildConfig.SUPABASE_URL}/auth/v1/$path")
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Content-Type", "application/json")
        if (!token.isNullOrBlank()) {
            builder.addHeader("Authorization", "Bearer $token")
        }
        if (body != null) {
            builder.method(method, body.toString().toRequestBody(JSON))
        } else {
            builder.method(method, null)
        }
        return builder.build()
    }

    val currentUserId: String?
        get() = SupabaseSessionStore.userId() ?: SupabaseSessionStore.accessToken()?.let { SupabaseSessionStore.decodeUserId(it) }

    val currentUser: AuthUser?
        get() = currentUserId?.let { AuthUser(it, null, null, null) }

    suspend fun registerWithEmail(email: String, password: String): Result<AuthUser> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        try {
            val body = JSONObject().apply {
                put("email", cleanEmail)
                put("password", password)
            }
            val response = http.newCall(authRequest("signup", body)).execute()
            response.use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val msg = parseAuthErrorMessage(text, resp.code)
                    return@withContext Result.failure(Exception(msg))
                }
                val json = JSONObject(text)
                val userObj = json.optJSONObject("user") ?: json
                val uid = userObj.optString("id").takeIf { it.isNotBlank() }
                    ?: json.optString("id").takeIf { it.isNotBlank() }
                    ?: return@withContext Result.failure(Exception("لم يُرجع خادم المصادقة معرّف مستخدم صالح."))

                val accessToken = json.optString("access_token")
                val refreshToken = json.optString("refresh_token")
                val expiresIn = json.optLong("expires_in", 3600L)
                if (accessToken.isNotBlank()) {
                    SupabaseSessionStore.save(accessToken, refreshToken, expiresIn, uid)
                }

                val meta = userObj.optJSONObject("user_metadata")
                val user = AuthUser(
                    uid = uid,
                    email = userObj.optString("email").takeIf { it.isNotBlank() } ?: cleanEmail,
                    displayName = meta?.optString("name")?.takeIf { it.isNotBlank() },
                    phoneNumber = userObj.optString("phone").takeIf { it.isNotBlank() }
                )
                Result.success(user)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Registration connection failure: ${e.message}")
            Result.failure(mapNetworkException(e))
        }
    }

    suspend fun registerWithPhone(phone: String, password: String, displayName: String): Result<AuthUser> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("phone", phone)
                put("password", password)
                put("data", JSONObject().put("name", displayName.trim()))
            }
            http.newCall(authRequest("signup", body)).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext Result.failure(Exception(parseAuthErrorMessage(text, resp.code)))
                val json = JSONObject(text)
                val userObj = json.optJSONObject("user") ?: json
                val uid = userObj.optString("id").takeIf { it.isNotBlank() }
                    ?: return@withContext Result.failure(Exception("لم يُرجع خادم المصادقة معرّف مستخدم صالح."))
                val accessToken = json.optString("access_token")
                if (accessToken.isNotBlank()) SupabaseSessionStore.save(accessToken, json.optString("refresh_token"), json.optLong("expires_in", 3600L), uid)
                Result.success(AuthUser(uid, userObj.optString("email").takeIf { it.isNotBlank() }, displayName.trim(), userObj.optString("phone").takeIf { it.isNotBlank() } ?: phone))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Phone registration failure: ${e.message}")
            Result.failure(mapNetworkException(e))
        }
    }

    suspend fun loginWithEmail(email: String, password: String): Result<AuthUser> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        try {
            val body = JSONObject().apply {
                put("email", cleanEmail)
                put("password", password)
            }
            val response = http.newCall(authRequest("token?grant_type=password", body)).execute()
            response.use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val msg = parseAuthErrorMessage(text, resp.code)
                    return@withContext Result.failure(Exception(msg))
                }
                val json = JSONObject(text)
                val userObj = json.optJSONObject("user") ?: json
                val accessToken = json.optString("access_token")
                val refreshToken = json.optString("refresh_token")
                val expiresIn = json.optLong("expires_in", 3600L)
                val uid = userObj.optString("id").takeIf { it.isNotBlank() }
                    ?: SupabaseSessionStore.decodeUserId(accessToken)
                    ?: return@withContext Result.failure(Exception("تعذر استخراج معرّف الحساب من بيانات الدخول."))

                if (accessToken.isNotBlank()) {
                    SupabaseSessionStore.save(accessToken, refreshToken, expiresIn, uid)
                }

                val meta = userObj.optJSONObject("user_metadata")
                val user = AuthUser(
                    uid = uid,
                    email = userObj.optString("email").takeIf { it.isNotBlank() } ?: cleanEmail,
                    displayName = meta?.optString("name")?.takeIf { it.isNotBlank() },
                    phoneNumber = userObj.optString("phone").takeIf { it.isNotBlank() }
                )
                Result.success(user)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login connection failure: ${e.message}")
            Result.failure(mapNetworkException(e))
        }
    }

    suspend fun loginWithPhone(phone: String, password: String): Result<AuthUser> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().put("phone", phone).put("password", password)
            http.newCall(authRequest("token?grant_type=password", body)).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext Result.failure(Exception(parseAuthErrorMessage(text, resp.code)))
                val json = JSONObject(text)
                val userObj = json.optJSONObject("user") ?: json
                val accessToken = json.optString("access_token")
                val uid = userObj.optString("id").takeIf { it.isNotBlank() }
                    ?: SupabaseSessionStore.decodeUserId(accessToken)
                    ?: return@withContext Result.failure(Exception("تعذر استخراج معرّف الحساب من بيانات الدخول."))
                if (accessToken.isNotBlank()) SupabaseSessionStore.save(accessToken, json.optString("refresh_token"), json.optLong("expires_in", 3600L), uid)
                Result.success(AuthUser(uid, userObj.optString("email").takeIf { it.isNotBlank() }, userObj.optJSONObject("user_metadata")?.optString("name"), userObj.optString("phone").takeIf { it.isNotBlank() } ?: phone))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Phone login failure: ${e.message}")
            Result.failure(mapNetworkException(e))
        }
    }

    suspend fun restoreSession(): Result<AuthUser?> = withContext(Dispatchers.IO) {
        try {
            val access = SupabaseSessionStore.accessToken()
            val refresh = SupabaseSessionStore.refreshToken()
            if (access.isNullOrBlank()) return@withContext Result.success(null)

            if (!SupabaseSessionStore.isExpired()) {
                val current = fetchCurrentUser(access)
                if (current.isSuccess) return@withContext current
            }

            if (refresh.isNullOrBlank()) {
                SupabaseSessionStore.clear()
                return@withContext Result.success(null)
            }

            val body = JSONObject().put("refresh_token", refresh)
            val response = http.newCall(authRequest("token?grant_type=refresh_token", body)).execute()
            response.use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    if (resp.code in 400..403) {
                        SupabaseSessionStore.clear()
                        return@withContext Result.failure(Exception("انتهت جلسة تسجيل الدخول. يرجى تسجيل الدخول من جديد."))
                    }
                    return@withContext Result.failure(Exception("تعذر تحديث الجلسة (${resp.code})"))
                }
                val json = JSONObject(text)
                val newAccess = json.optString("access_token")
                val newRefresh = json.optString("refresh_token").ifBlank { refresh }
                val expiresIn = json.optLong("expires_in", 3600L)
                if (newAccess.isBlank()) {
                    SupabaseSessionStore.clear()
                    return@withContext Result.success(null)
                }
                val userObj = json.optJSONObject("user")
                val uid = userObj?.optString("id") ?: SupabaseSessionStore.decodeUserId(newAccess)
                SupabaseSessionStore.save(newAccess, newRefresh, expiresIn, uid)
                fetchCurrentUser(newAccess)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Restore session error: ${e.message}")
            Result.failure(mapNetworkException(e))
        }
    }

    private fun fetchCurrentUser(access: String): Result<AuthUser?> {
        return try {
            val req = Request.Builder()
                .url("${BuildConfig.SUPABASE_URL}/auth/v1/user")
                .get()
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $access")
                .build()
            http.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return Result.failure(Exception("جلسة تسجيل الدخول غير صالحة (${resp.code})"))
                }
                val userObj = JSONObject(text)
                val uid = userObj.optString("id")
                if (uid.isBlank()) return Result.failure(Exception("معرّف المستخدم مفقود في استجابة الجلسة."))
                val meta = userObj.optJSONObject("user_metadata")
                Result.success(
                    AuthUser(
                        uid = uid,
                        email = userObj.optString("email").takeIf { it.isNotBlank() },
                        displayName = meta?.optString("name")?.takeIf { it.isNotBlank() },
                        phoneNumber = userObj.optString("phone").takeIf { it.isNotBlank() }
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(mapNetworkException(e))
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        try {
            val body = JSONObject().put("email", cleanEmail)
            val response = http.newCall(authRequest("recover", body)).execute()
            response.use { resp ->
                val text = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val msg = parseAuthErrorMessage(text, resp.code)
                    Result.failure(Exception(msg))
                }
            }
        } catch (e: Exception) {
            Result.failure(mapNetworkException(e))
        }
    }

    suspend fun changeCurrentPassword(currentPassword: String, newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val access = SupabaseSessionStore.accessToken()
                ?: return@withContext Result.failure(IllegalStateException("جلسة تسجيل الدخول غير صالحة. يرجى تسجيل الدخول من جديد."))
            val email = fetchCurrentUser(access).getOrNull()?.email
                ?: return@withContext Result.failure(Exception("تعذر استرجاع البريد الإلكتروني للحساب الحالي."))

            // Verify current credentials first
            val verifyResp = http.newCall(authRequest("token?grant_type=password", JSONObject().put("email", email).put("password", currentPassword))).execute()
            verifyResp.use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception("كلمة المرور الحالية غير صحيحة."))
                }
            }

            val validToken = SupabaseSessionStore.accessToken() ?: access
            val body = JSONObject().put("password", newPassword)
            val updateReq = Request.Builder()
                .url("${BuildConfig.SUPABASE_URL}/auth/v1/user")
                .put(body.toString().toRequestBody(JSON))
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $validToken")
                .addHeader("Content-Type", "application/json")
                .build()
            http.newCall(updateReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val text = resp.body?.string().orEmpty()
                    Result.failure(Exception(parseAuthErrorMessage(text, resp.code)))
                }
            }
        } catch (e: Exception) {
            Result.failure(mapNetworkException(e))
        }
    }

    fun signOut() {
        try {
            val token = SupabaseSessionStore.accessToken()
            if (!token.isNullOrBlank()) {
                http.newCall(
                    Request.Builder()
                        .url("${BuildConfig.SUPABASE_URL}/auth/v1/logout")
                        .post("{}".toRequestBody(JSON))
                        .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                ).enqueue(object : okhttp3.Callback {
                    override fun onFailure(call: okhttp3.Call, e: IOException) {}
                    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) { response.close() }
                })
            }
        } catch (_: Exception) {}
        SupabaseSessionStore.clear()
    }

    private fun parseAuthErrorMessage(bodyText: String, statusCode: Int): String {
        val json = runCatching { JSONObject(bodyText) }.getOrNull()
        val code = json?.optString("error_code").orEmpty()
        val rawMsg = json?.optString("msg")?.ifBlank { json.optString("message") }?.ifBlank { json.optString("error_description") }.orEmpty()

        return when {
            code == "invalid_credentials" || rawMsg.contains("invalid login", ignoreCase = true) ->
                "البريد الإلكتروني أو كلمة المرور غير صحيحة."
            code == "email_exists" || rawMsg.contains("already registered", ignoreCase = true) ->
                "هذا البريد الإلكتروني مسجل مسبقاً. يرجى تسجيل الدخول أو استخدام بريد آخر."
            code == "email_not_confirmed" ->
                "يرجى تأكيد البريد الإلكتروني لتفعيل حسابك أولاً."
            code == "user_already_exists" ->
                "المستخدم مسجل مسبقاً في النظام."
            statusCode == 429 ->
                "تم تجاوز عدد المحاولات المسموح به. يرجى الانتظار بضع دقائق ثم المحاولة مجدداً."
            statusCode in 500..599 ->
                "خادم المصادقة غير متاح حالياً. يرجى المحاولة لاحقاً."
            rawMsg.isNotBlank() -> rawMsg
            else -> "فشلت عملية المصادقة (رمز الخطأ: $statusCode)."
        }
    }

    private fun mapNetworkException(e: Exception): Exception {
        val msg = when (e) {
            is SocketTimeoutException -> "انتهت مهلة الاتصال بخادم المصادقة. يرجى التحقق من اتصالك بالإنترنت."
            is UnknownHostException -> "تعذر الوصول إلى خادم المصادقة. يرجى التأكد من تشغيل الإنترنت."
            is IOException -> "حدث خطأ في الاتصال بالشبكة أثناء المصادقة."
            else -> e.message ?: "حدث خطأ غير متوقع أثناء المصادقة."
        }
        return Exception(msg, e)
    }
}
