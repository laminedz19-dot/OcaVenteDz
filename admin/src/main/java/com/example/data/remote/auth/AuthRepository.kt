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
    companion object { private const val TAG = "SupabaseAdminAuthRepository"; val ADMIN_EMAILS = setOf("laminedz.19@gmail.com", "laminedz19@gmail.com", "achridz01@gmail.com", "admin@ocaventedz.dz", "admin@ocaventedz.com") }
    private val http = OkHttpClient()
    private fun request(path:String, body:JSONObject?=null):Request { val b=Request.Builder().url("${BuildConfig.SUPABASE_URL}/auth/v1/$path").addHeader("apikey",BuildConfig.SUPABASE_ANON_KEY).addHeader("Content-Type","application/json"); if(body!=null)b.post(body.toString().toRequestBody("application/json".toMediaType())); return b.build() }
    val currentUserId:String? get()=SupabaseSessionStore.accessToken()?.let{decodeUserId(it)}
    val currentUser:AuthUser? get()=currentUserId?.let{AuthUser(it,null,null,null)}
    suspend fun registerWithEmail(email:String,password:String):Result<AuthUser?>=authenticate("signup",email,password)
    suspend fun loginWithEmail(email:String,password:String):Result<AuthUser?>=authenticate("token?grant_type=password",email,password)
    suspend fun loginAdminWithClaims(email:String,password:String):Result<AuthUser> {
        val result = authenticate("token?grant_type=password", email, password)
        val user = result.getOrNull() ?: return Result.failure(result.exceptionOrNull() ?: Exception("تعذر تسجيل الدخول"))
        return if (ADMIN_EMAILS.contains(email.trim().lowercase()) || checkIsCurrentAdmin()) {
            Result.success(user)
        } else {
            signOut()
            Result.failure(SecurityException("هذا الحساب ليس مديرًا"))
        }
    }
    private suspend fun authenticate(path:String,email:String,password:String):Result<AuthUser?>=withContext(Dispatchers.IO){try{val r=http.newCall(request(path,JSONObject().put("email",email.trim()).put("password",password))).execute();val t=r.body?.string().orEmpty();if(!r.isSuccessful)return@withContext Result.failure(Exception(JSONObject(t).optString("msg","تعذر تنفيذ المصادقة")));val j=JSONObject(t);val u=j.optJSONObject("user")?:j;j.optString("access_token").takeIf{it.isNotBlank()}?.let{SupabaseSessionStore.save(it,j.optString("refresh_token"))};Result.success(AuthUser(u.optString("id"),u.optString("email"),u.optJSONObject("user_metadata")?.optString("name"),u.optString("phone")))}catch(e:Exception){Log.e(TAG,"Auth error",e);Result.failure(Exception("تعذر الاتصال بخدمة المصادقة",e))}}
    suspend fun checkIsCurrentAdmin():Boolean=withContext(Dispatchers.IO){val uid=currentUserId?:return@withContext false;try{val r=Request.Builder().url("${BuildConfig.SUPABASE_URL}/rest/v1/user_roles?user_id=eq.$uid&role=eq.admin&select=user_id&limit=1").addHeader("apikey",BuildConfig.SUPABASE_ANON_KEY).addHeader("Authorization","Bearer ${SupabaseSessionStore.accessToken()}").build();val t=http.newCall(r).execute().body?.string().orEmpty();t.trim().startsWith("[")&&t.contains(uid)}catch(_:Exception){false}}
    suspend fun sendPasswordReset(email:String):Result<Unit>=withContext(Dispatchers.IO){try{val r=http.newCall(request("recover",JSONObject().put("email",email.trim()))).execute();if(r.isSuccessful)Result.success(Unit)else Result.failure(Exception("تعذر إرسال رابط استعادة كلمة المرور"))}catch(e:Exception){Result.failure(e)}}
    fun signOut(){SupabaseSessionStore.clear()}
    private fun decodeUserId(jwt:String):String?=try{val p=jwt.split(".");if(p.size<2)null else JSONObject(String(android.util.Base64.decode(p[1],android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP))).optString("sub").takeIf{it.isNotBlank()}}catch(_:Exception){null}
}
