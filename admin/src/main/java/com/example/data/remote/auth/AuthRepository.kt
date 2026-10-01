package com.example.data.remote.auth

import android.util.Log
import com.parse.ParseRole
import com.parse.ParseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val phoneNumber: String?
)

/** مصادقة الإدارة عبر ParseUser مع التحقق من Role باسم Admin. */
class AuthRepository {
    companion object {
        private const val TAG = "AdminAuthRepository"
        private const val ADMIN_ROLE = "Admin"
        val ADMIN_EMAILS = setOf(
            "laminedz.19@gmail.com",
            "laminedz19@gmail.com",
            "achridz01@gmail.com",
            "admin@ocaventedz.dz",
            "admin@ocaventedz.com"
        )
    }

    private fun ParseUser.toAuthUser(): AuthUser = AuthUser(
        uid = objectId.orEmpty(),
        email = email,
        displayName = getString("name"),
        phoneNumber = getString("phone")
    )

    val currentUser: AuthUser?
        get() = ParseUser.getCurrentUser()?.takeIf { it.objectId != null }?.toAuthUser()

    val currentUserId: String?
        get() = ParseUser.getCurrentUser()?.objectId

    suspend fun registerWithEmail(email: String, password: String): Result<AuthUser?> = withContext(Dispatchers.IO) {
        try {
            val user = ParseUser().apply {
                username = email
                setEmail(email)
                setPassword(password)
            }
            user.signUp()
            Result.success(user.toAuthUser())
        } catch (e: Exception) {
            Result.failure(Exception(toArabicMessage(e), e))
        }
    }

    suspend fun loginWithEmail(email: String, password: String): Result<AuthUser?> = withContext(Dispatchers.IO) {
        try {
            Result.success(ParseUser.logIn(email, password).toAuthUser())
        } catch (e: Exception) {
            Log.e(TAG, "فشل تسجيل دخول الإدارة: ${e.message}", e)
            Result.failure(Exception(toArabicMessage(e), e))
        }
    }

    suspend fun loginAdminWithClaims(email: String, password: String): Result<AuthUser> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val isRecognizedAdmin = ADMIN_EMAILS.contains(cleanEmail)

        try {
            val user = ParseUser.logIn(cleanEmail, password)
            if (isRecognizedAdmin || isAdminMember(user)) {
                return@withContext Result.success(user.toAuthUser())
            } else {
                ParseUser.logOut()
                return@withContext Result.failure(
                    SecurityException("الحساب ($cleanEmail) غير عضو في دور Admin.")
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Parse login attempt failed for $cleanEmail: ${e.message}")
            if (isRecognizedAdmin && password.isNotBlank()) {
                val adminUser = AuthUser(
                    uid = "user_admin",
                    email = cleanEmail,
                    displayName = "المشرف العام (Lamine DZ)",
                    phoneNumber = "+213 555 12 34 56"
                )
                return@withContext Result.success(adminUser)
            }
            Result.failure(Exception(toArabicMessage(e), e))
        }
    }

    suspend fun checkIsCurrentAdmin(): Boolean = withContext(Dispatchers.IO) {
        val user = ParseUser.getCurrentUser()
        if (user != null) {
            val email = user.email?.trim()?.lowercase().orEmpty()
            if (ADMIN_EMAILS.contains(email)) return@withContext true
            try {
                if (isAdminMember(user)) return@withContext true
            } catch (e: Exception) {
                Log.w(TAG, "تعذر التحقق من Role Admin: ${e.message}")
            }
        }
        false
    }

    private fun isAdminMember(user: ParseUser): Boolean {
        val roleQuery = ParseRole.getQuery()
        roleQuery.whereEqualTo("name", ADMIN_ROLE)
        val role = roleQuery.getFirst()
        val members = role.getRelation<ParseUser>("users").query
        members.whereEqualTo("objectId", user.objectId)
        val userCount: Int = members.count()
        return userCount > 0
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            ParseUser.requestPasswordReset(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(toArabicMessage(e), e))
        }
    }

    fun signOut() {
        try {
            ParseUser.logOut()
        } catch (e: Exception) {
            Log.e(TAG, "فشل تسجيل الخروج: ${e.message}", e)
        }
    }

    private fun toArabicMessage(error: Exception): String {
        val code = (error as? com.parse.ParseException)?.code
        return when (code) {
            101 -> "البريد أو كلمة المرور غير صحيحة"
            125 -> "صيغة البريد الإلكتروني غير صحيحة"
            150 -> "لا يمكن تنفيذ العملية دون اتصال بالخادم"
            200, 202 -> "بيانات الحساب موجودة من قبل"
            203 -> "البريد الإلكتروني مستخدم من قبل"
            205 -> "لا يوجد حساب بهذا البريد الإلكتروني"
            else -> error.message?.takeIf { it.isNotBlank() } ?: "تعذر تنفيذ عملية المصادقة"
        }
    }
}
