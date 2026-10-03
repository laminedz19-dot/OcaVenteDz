package com.example.data.remote.supabase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.math.max

class SupabaseClient {
    companion object {
        const val BASE_URL = BuildConfig.SUPABASE_URL
        private const val ANON_KEY = BuildConfig.SUPABASE_ANON_KEY
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private const val TAG = "SupabaseAdminClient"
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    fun requireAccessToken(): Result<String> {
        val token = SupabaseSessionStore.accessToken()
        if (token.isNullOrBlank() || SupabaseSessionStore.isExpired()) {
            return Result.failure(
                IllegalStateException("جلسة تسجيل الدخول غير صالحة. يرجى تسجيل الدخول من جديد.")
            )
        }
        return Result.success(token)
    }

    private fun authorizedRequest(
        path: String,
        method: String = "GET",
        body: JSONObject? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): Result<Request> {
        val tokenRes = requireAccessToken()
        val token = tokenRes.getOrElse { return Result.failure(it) }

        val b = Request.Builder()
            .url("$BASE_URL$path")
            .addHeader("apikey", ANON_KEY)
            .addHeader("Authorization", "Bearer $token")

        extraHeaders.forEach { (k, v) -> b.addHeader(k, v) }
        if (body != null) {
            b.method(method, body.toString().toRequestBody(JSON))
        } else {
            b.method(method, null)
        }
        return Result.success(b.build())
    }

    private fun publicRequest(
        path: String,
        method: String = "GET",
        body: JSONObject? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): Request {
        val token = SupabaseSessionStore.accessToken()
        val b = Request.Builder()
            .url("$BASE_URL$path")
            .addHeader("apikey", ANON_KEY)
            .addHeader("Authorization", "Bearer ${if (!token.isNullOrBlank() && !SupabaseSessionStore.isExpired()) token else ANON_KEY}")

        extraHeaders.forEach { (k, v) -> b.addHeader(k, v) }
        if (body != null) {
            b.method(method, body.toString().toRequestBody(JSON))
        } else {
            b.method(method, null)
        }
        return b.build()
    }

    private suspend fun callAuthorized(
        path: String,
        method: String = "GET",
        body: JSONObject? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): Result<String> = withContext(Dispatchers.IO) {
        val req = authorizedRequest(path, method, body, extraHeaders).getOrElse { return@withContext Result.failure(it) }
        executeCall(req)
    }

    private suspend fun callPublic(
        path: String,
        method: String = "GET",
        body: JSONObject? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): Result<String> = withContext(Dispatchers.IO) {
        val req = publicRequest(path, method, body, extraHeaders)
        executeCall(req)
    }

    private fun executeCall(req: Request): Result<String> {
        return try {
            http.newCall(req).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success(text)
                } else {
                    Result.failure(Exception(mapHttpError(response.code, text)))
                }
            }
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }

    private suspend fun upsertAuthorized(table: String, body: JSONObject, conflict: String): Result<String> =
        callAuthorized(
            "rest/v1/$table?on_conflict=$conflict",
            "POST",
            body,
            mapOf("Prefer" to "resolution=merge-duplicates,return=representation")
        )

    private fun rows(text: String): JSONArray =
        if (text.trim().startsWith("[")) JSONArray(text) else JSONArray()

    private fun s(o: JSONObject, k: String, d: String = "") = if (o.isNull(k)) d else o.optString(k, d)
    private fun l(o: JSONObject, k: String, d: Long = 0L) = if (o.isNull(k)) d else o.optLong(k, d)

    // RPC
    suspend fun rpc(functionName: String, params: JSONObject = JSONObject()): Result<String> = withContext(Dispatchers.IO) {
        val tokenRes = requireAccessToken()
        val token = tokenRes.getOrElse { return@withContext Result.failure(it) }
        try {
            val req = Request.Builder()
                .url("$BASE_URL/rest/v1/rpc/$functionName")
                .post(params.toString().toRequestBody(JSON))
                .addHeader("apikey", ANON_KEY)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()
            http.newCall(req).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success(body)
                } else {
                    Result.failure(Exception(mapHttpError(response.code, body)))
                }
            }
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }

    // Admin Listings Management
    suspend fun getAllListingsAdmin(): Result<List<ListingEntity>> =
        callAuthorized("rest/v1/listings?select=*&order=created_at.desc&limit=500").map { text ->
            rows(text).let { a ->
                (0 until a.length()).map { i ->
                    val o = a.getJSONObject(i)
                    ListingEntity(
                        s(o, "id"), s(o, "user_id"), s(o, "user_name"), s(o, "user_phone"),
                        o.optBoolean("is_phone_visible", true), s(o, "title"), s(o, "description"),
                        s(o, "category_id"), s(o, "category_name_ar"), s(o, "subcategory"),
                        l(o, "price_dzd"), o.optBoolean("is_negotiable", false), s(o, "condition", "NEW"),
                        o.optInt("wilaya_code", 16), s(o, "wilaya"), s(o, "commune"),
                        s(o, "images").ifBlank {
                            o.optJSONArray("images")?.let { j ->
                                (0 until j.length()).joinToString(",") { n -> j.optString(n) }
                            } ?: ""
                        },
                        s(o, "video_url"), s(o, "status", "DRAFT"), s(o, "rejection_reason"),
                        s(o, "package_type", "STANDARD"), o.optInt("publishing_fee_dzd", 0),
                        o.optBoolean("is_paid"), o.optBoolean("is_featured"), o.optBoolean("is_urgent"),
                        o.optInt("views_count"), System.currentTimeMillis(), s(o, "expires_at").toInstantOrZero()
                    )
                }
            }
        }

    suspend fun updateListingStatus(id: String, status: String, reason: String = ""): Result<Unit> =
        callAuthorized(
            "rest/v1/listings?id=eq.$id",
            "PATCH",
            JSONObject().apply {
                put("status", if (status == "PENDING") "UNDER_REVIEW" else status)
                put("rejection_reason", reason)
            }
        ).map { }

    // Users Management
    suspend fun getAllUsers(): Result<List<UserEntity>> =
        callAuthorized("rest/v1/profiles?select=*&limit=500").map { text ->
            rows(text).let { a ->
                (0 until a.length()).map { i ->
                    val o = a.getJSONObject(i)
                    UserEntity(
                        s(o, "id"), s(o, "phone"), s(o, "email"), s(o, "name"), s(o, "avatar_url"),
                        s(o, "wilaya"), s(o, "commune"), s(o, "bio"), o.optDouble("seller_rating", 0.0),
                        o.optInt("reviews_count"), o.optInt("ads_count"), System.currentTimeMillis(),
                        o.optBoolean("is_verified"), false, o.optBoolean("is_banned"), "USER"
                    )
                }
            }
        }

    suspend fun updateUserBan(id: String, banned: Boolean): Result<Unit> =
        callAuthorized("rest/v1/profiles?id=eq.$id", "PATCH", JSONObject().put("is_banned", banned)).map { }

    suspend fun updateUserVerification(id: String, verified: Boolean): Result<Unit> =
        callAuthorized("rest/v1/profiles?id=eq.$id", "PATCH", JSONObject().put("is_verified", verified)).map { }

    // Top-Up Requests Management via Atomic RPC
    suspend fun getTopUpRequests(): Result<List<TopUpRequestEntity>> =
        callAuthorized("rest/v1/top_up_requests?select=*&order=created_at.desc&limit=500").map { text ->
            rows(text).let { a ->
                (0 until a.length()).map { i ->
                    val o = a.getJSONObject(i)
                    TopUpRequestEntity(
                        s(o, "id"), s(o, "user_id"), "", "",
                        o.optInt("amount_dzd"), s(o, "provider"), s(o, "reference"),
                        s(o, "receipt_path"), s(o, "status", "PENDING"), s(o, "admin_note"),
                        System.currentTimeMillis(), 0L
                    )
                }
            }
        }

    suspend fun approveTopUpRequest(requestId: String, adminNote: String): Result<String> {
        return rpc(
            "approve_top_up",
            JSONObject().apply {
                put("request_id", requestId)
                put("p_admin_note", adminNote)
            }
        )
    }

    suspend fun rejectTopUpRequest(requestId: String, reason: String): Result<String> {
        return rpc(
            "reject_top_up",
            JSONObject().apply {
                put("request_id", requestId)
                put("p_reason", reason)
            }
        )
    }

    // Platform Settings
    suspend fun savePlatformSettings(x: PlatformSettingsEntity): Result<Unit> =
        callAuthorized(
            "rest/v1/platform_settings?id=eq.global",
            "PATCH",
            JSONObject().apply {
                put("standard_ad_fee_dzd", x.standardAdFeeDzd)
                put("featured_ad_fee_dzd", x.featuredAdFeeDzd)
                put("urgent_ad_fee_dzd", x.urgentAdFeeDzd)
                put("ad_duration_days", x.adDurationDays)
                put("auto_publish_after_payment", x.autoPublishAfterPayment)
                put("official_rip", x.officialRip)
                put("official_key", x.officialKey)
                put("official_account_holder", x.officialAccountHolder)
                put("official_provider_name", x.officialProviderName)
                put("official_instructions", x.officialInstructions)
            }
        ).map { }

    suspend fun getPlatformSettings(): Result<PlatformSettingsEntity?> =
        callPublic("rest/v1/platform_settings?select=*&limit=1").map { text ->
            rows(text).let {
                if (it.length() == 0) null
                else {
                    val o = it.getJSONObject(0)
                    PlatformSettingsEntity(
                        standardAdFeeDzd = o.optInt("standard_ad_fee_dzd", 400),
                        featuredAdFeeDzd = o.optInt("featured_ad_fee_dzd", 600),
                        urgentAdFeeDzd = o.optInt("urgent_ad_fee_dzd", 1000),
                        adDurationDays = o.optInt("ad_duration_days", 30),
                        autoPublishAfterPayment = o.optBoolean("auto_publish_after_payment", true),
                        officialRip = s(o, "official_rip"),
                        officialKey = s(o, "official_key"),
                        officialAccountHolder = s(o, "official_account_holder"),
                        officialProviderName = s(o, "official_provider_name"),
                        officialInstructions = s(o, "official_instructions")
                    )
                }
            }
        }

    // Reports Management
    suspend fun getAllReports(): Result<List<ReportEntity>> =
        callAuthorized("rest/v1/reports?select=*&order=created_at.desc&limit=500").map { text ->
            rows(text).let { a ->
                (0 until a.length()).map { i ->
                    val o = a.getJSONObject(i)
                    ReportEntity(
                        id = s(o, "id"),
                        reporterId = s(o, "reporter_id"),
                        reportedListingId = s(o, "reported_listing_id"),
                        reportedUserId = s(o, "reported_user_id"),
                        reason = s(o, "reason"),
                        comment = s(o, "comment"),
                        timestamp = System.currentTimeMillis(),
                        status = s(o, "status", "PENDING")
                    )
                }
            }
        }

    suspend fun updateReportStatus(reportId: String, status: String): Result<Unit> =
        callAuthorized("rest/v1/reports?id=eq.$reportId", "PATCH", JSONObject().put("status", status)).map { }

    suspend fun getListings(): Result<List<ListingEntity>> = getAllListingsAdmin()

    suspend fun saveListing(x: ListingEntity): Result<String> =
        upsertAuthorized(
            "listings",
            JSONObject().apply {
                put("id", x.id)
                put("user_id", x.userId)
                put("user_name", x.userName)
                put("user_phone", x.userPhone)
                put("is_phone_visible", x.isPhoneVisible)
                put("title", x.title)
                put("description", x.description)
                put("category_id", x.categoryId)
                put("category_name_ar", x.categoryNameAr)
                put("subcategory", x.subcategory)
                put("price_dzd", x.priceDzd)
                put("is_negotiable", x.isNegotiable)
                put("condition", x.condition)
                put("wilaya_code", x.wilayaCode)
                put("wilaya_name", x.wilayaName)
                put("commune", x.commune)
                put("images_json", x.imagesJson)
                put("video_url", x.videoUrl)
                put("status", x.status)
                put("rejection_reason", x.rejectionReason)
                put("package_type", x.packageType)
                put("publishing_fee_dzd", x.publishingFeeDzd)
                put("is_paid", x.isPaid)
                put("is_featured", x.isFeatured)
                put("is_urgent", x.isUrgent)
                put("views_count", x.viewsCount)
            },
            "id"
        ).map { x.id }

    suspend fun saveUser(x: UserEntity): Result<String> =
        upsertAuthorized(
            "profiles",
            JSONObject().apply {
                put("id", x.id)
                put("phone", x.phone)
                put("email", x.email)
                put("name", x.name)
                put("avatar_url", x.avatarUrl)
                put("wilaya", x.wilaya)
                put("commune", x.commune)
                put("bio", x.bio)
                put("seller_rating", x.sellerRating)
                put("reviews_count", x.reviewsCount)
                put("ads_count", x.adsCount)
                put("is_verified", x.isVerified)
                put("verification_requested", x.verificationRequested)
                put("is_banned", x.isBanned)
                put("role", x.role)
            },
            "id"
        ).map { x.id }

    suspend fun saveWallet(x: WalletEntity): Result<String> =
        upsertAuthorized(
            "wallets",
            JSONObject().apply {
                put("user_id", x.userId)
                put("balance_dzd", x.balanceDzd)
            },
            "user_id"
        ).map { x.userId }

    suspend fun saveReview(x: ReviewEntity): Result<String> =
        upsertAuthorized(
            "reviews",
            JSONObject().apply {
                put("id", x.id)
                put("seller_id", x.sellerId)
                put("buyer_id", x.buyerId)
                put("buyer_name", x.buyerName)
                put("listing_id", x.listingId)
                put("rating", x.rating)
                put("comment", x.comment)
            },
            "id"
        ).map { x.id }

    suspend fun saveReport(x: ReportEntity): Result<String> =
        callAuthorized(
            "rest/v1/reports",
            "POST",
            JSONObject().apply {
                put("id", x.id)
                put("reporter_id", x.reporterId)
                put("reported_listing_id", x.reportedListingId)
                put("reported_user_id", x.reportedUserId)
                put("reason", x.reason)
                put("comment", x.comment)
                put("status", x.status)
            }
        ).map { x.id }

    suspend fun saveFavorite(x: FavoriteEntity): Result<String> =
        upsertAuthorized(
            "favorites",
            JSONObject().apply {
                put("id", x.id)
                put("user_id", x.userId)
                put("listing_id", x.listingId)
            },
            "id"
        ).map { x.id }

    suspend fun deleteFavorite(userId: String, listingId: String): Result<Unit> =
        callAuthorized("rest/v1/favorites?user_id=eq.$userId&listing_id=eq.$listingId", "DELETE").map { }

    suspend fun saveOrder(x: OrderEntity): Result<String> =
        callAuthorized(
            "rest/v1/orders",
            "POST",
            JSONObject().apply {
                put("id", x.id)
                put("order_number", x.orderNumber)
                put("listing_id", x.listingId)
                put("seller_id", x.sellerId)
                put("buyer_id", x.buyerId)
                put("total_amount_dzd", x.totalAmountDzd)
                put("status", x.status)
                put("is_paid", x.isPaid)
                put("tracking_number", x.trackingNumber)
            }
        ).map { x.id }

    suspend fun updateOrderStatus(id: String, status: String, note: String, tracking: String?): Result<Unit> =
        callAuthorized(
            "rest/v1/orders?id=eq.$id",
            "PATCH",
            JSONObject().apply {
                put("status", status)
                put("tracking_number", tracking ?: JSONObject.NULL)
            }
        ).map { }

    suspend fun uploadListingImage(context: Context, ownerId: String, uri: Uri, name: String): Result<String> = withContext(Dispatchers.IO) {
        val tokenRes = requireAccessToken()
        val token = tokenRes.getOrElse { return@withContext Result.failure(it) }
        try {
            val imageBytes = compressImage(context, uri)
            val path = "$ownerId/listings/$name.jpg"
            val req = Request.Builder()
                .url("$BASE_URL/storage/v1/object/oca-vente-media/$path")
                .post(imageBytes.toRequestBody("image/jpeg".toMediaType()))
                .addHeader("apikey", ANON_KEY)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("x-upsert", "true")
                .build()
            http.newCall(req).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Result.failure(Exception(mapHttpError(response.code, body)))
                } else {
                    Result.success("$BASE_URL/storage/v1/object/public/oca-vente-media/$path")
                }
            }
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }

    suspend fun deleteListingMedia(storagePathOrUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!storagePathOrUrl.startsWith("http")) return@withContext Result.success(Unit)
        val tokenRes = requireAccessToken()
        val token = tokenRes.getOrElse { return@withContext Result.failure(it) }
        try {
            val prefix = "$BASE_URL/storage/v1/object/public/oca-vente-media/"
            val rawPath = if (storagePathOrUrl.startsWith(prefix)) storagePathOrUrl.removePrefix(prefix) else storagePathOrUrl
            val req = Request.Builder()
                .url("$BASE_URL/storage/v1/object/oca-vente-media/$rawPath")
                .delete()
                .addHeader("apikey", ANON_KEY)
                .addHeader("Authorization", "Bearer $token")
                .build()
            http.newCall(req).execute().use { response ->
                if (response.isSuccessful || response.code == 404) Result.success(Unit)
                else Result.failure(Exception("فشل حذف الملف من التخزين السحابي (${response.code})"))
            }
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }

    private fun compressImage(context: Context, uri: Uri, maxDimension: Int = 1600, maxBytes: Int = 450 * 1024): ByteArray {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "تعذر فتح الصورة." }
            BitmapFactory.decodeStream(input, null, bounds)
        }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "صيغة الصورة غير صالحة." }
        var sampleSize = 1
        while (max(bounds.outWidth / sampleSize, bounds.outHeight / sampleSize) > maxDimension * 2) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "تعذر فتح الصورة." }
            BitmapFactory.decodeStream(input, null, options)
        } ?: error("تعذر معالجة الصورة.")

        val scale = minOf(1f, maxDimension.toFloat() / decoded.width, maxDimension.toFloat() / decoded.height)
        val bitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true
            ).also { decoded.recycle() }
        } else decoded

        var quality = 82
        var bytes: ByteArray
        do {
            bytes = ByteArrayOutputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)) { "تعذر ضغط الصورة" }
                output.toByteArray()
            }
            quality -= 5
        } while (bytes.size > maxBytes && quality >= 50)
        bitmap.recycle()
        return bytes
    }

    // Private Receipt Signed URL
    suspend fun createSignedReceiptUrl(
        storagePathOrUrl: String,
        expiresInSeconds: Int = 3600
    ): Result<String> = withContext(Dispatchers.IO) {
        val tokenRes = requireAccessToken()
        val token = tokenRes.getOrElse { return@withContext Result.failure(it) }

        try {
            val raw = storagePathOrUrl.trim()
                .removePrefix("$BASE_URL/storage/v1/object/public/oca-vente-private/")
                .removePrefix("$BASE_URL/storage/v1/object/sign/oca-vente-private/")
                .removePrefix("$BASE_URL/storage/v1/object/oca-vente-private/")
                .removePrefix("oca-vente-private/")

            if (raw.isBlank() || raw.startsWith("http://") || raw.startsWith("https://") || raw.startsWith("content://")) {
                return@withContext Result.failure(Exception("مسار الوصل غير صالح للمعاينة."))
            }

            val req = Request.Builder()
                .url("$BASE_URL/storage/v1/object/sign/oca-vente-private/$raw")
                .post(JSONObject().put("expiresIn", expiresInSeconds).toString().toRequestBody(JSON))
                .addHeader("apikey", ANON_KEY)
                .addHeader("Authorization", "Bearer $token")
                .build()

            http.newCall(req).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Result.failure(Exception(mapHttpError(response.code, body)))
                } else {
                    val json = JSONObject(body)
                    val signed = json.optString("signedURL").ifBlank { json.optString("signedUrl") }
                    if (signed.isBlank()) {
                        Result.failure(Exception("لم يُرجع الخادم رابط معاينة صالحاً للوصل."))
                    } else {
                        Result.success(if (signed.startsWith("http")) signed else "$BASE_URL$signed")
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }

    private fun mapHttpError(code: Int, body: String): String {
        val jsonMsg = runCatching {
            val j = JSONObject(body)
            j.optString("message").ifBlank { j.optString("msg") }.ifBlank { j.optString("error") }
        }.getOrNull()

        if (!jsonMsg.isNullOrBlank()) {
            return jsonMsg
        }

        return when (code) {
            401 -> "انتهت جلسة تسجيل الدخول. يرجى تسجيل الدخول من جديد."
            403 -> "ليس لديك الصلاحية لتنفيذ هذا الإجراء."
            404 -> "العنصر المطلوب غير موجود على الخادم."
            409 -> "يوجد تعارض في البيانات؛ ربما تم تنفيذ العملية مسبقاً."
            413 -> "حجم الملف أو الصورة كبير جداً."
            422 -> "البيانات المدخلة غير صحيحة."
            429 -> "تم إرسال طلبات كثيرة جداً في وقت قصير. يرجى الانتظار."
            500, 502, 503, 504 -> "خادم الخدمة السحابية غير متاح حالياً."
            else -> "فشلت العملية على الخادم (رمز الاستجابة: $code)."
        }
    }

    private fun mapException(e: Exception): Exception {
        val msg = when (e) {
            is SocketTimeoutException -> "انتهت مهلة الاتصال بالخادم."
            is UnknownHostException -> "تعذر الوصول إلى الخادم السحابي."
            is IOException -> "حدث خطأ في الاتصال بالشبكة."
            else -> e.message ?: "حدث خطأ غير متوقع."
        }
        return Exception(msg, e)
    }
}

private fun String.toInstantOrZero(): Long = try {
    java.time.Instant.parse(this).toEpochMilli()
} catch (_: Exception) {
    0L
}
