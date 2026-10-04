package com.example.data.remote.supabase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.ChatMessageEntity
import com.example.data.local.FavoriteEntity
import com.example.data.local.ListingEntity
import com.example.data.local.OrderEntity
import com.example.data.local.PaymentOrderEntity
import com.example.data.local.PlatformSettingsEntity
import com.example.data.local.ReportEntity
import com.example.data.local.ReviewEntity
import com.example.data.local.TopUpRequestEntity
import com.example.data.local.UserEntity
import com.example.data.local.WalletEntity
import com.example.data.local.WalletTransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class SupabaseClient {
    companion object {
        const val BASE_URL = BuildConfig.SUPABASE_URL
        private const val ANON_KEY = BuildConfig.SUPABASE_ANON_KEY
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private const val TAG = "SupabaseClient"
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private fun accessToken(): String? = SupabaseSessionStore.accessToken()

    fun attachContext(context: Context) {
        SupabaseSessionStore.initialize(context)
    }

    fun setAccessToken(context: Context, token: String?, refresh: String? = null) {
        attachContext(context)
        if (token == null) SupabaseSessionStore.clear()
        else SupabaseSessionStore.save(token, refresh)
    }

    fun clearSession(context: Context) {
        SupabaseSessionStore.clear()
    }

    /**
     * Builds request. For authenticated calls, requires valid Supabase user JWT token.
     * Never sends ANON_KEY as Bearer token for protected resources.
     */
    private fun request(
        path: String,
        method: String = "GET",
        body: JSONObject? = null,
        extra: Map<String, String> = emptyMap(),
        requiresAuth: Boolean = true
    ): Result<Request> {
        val b = Request.Builder()
            .url("$BASE_URL$path")
            .addHeader("apikey", ANON_KEY)

        if (requiresAuth) {
            val token = accessToken()
            if (token.isNullOrBlank()) {
                return Result.failure(Exception("جلسة تسجيل الدخول غير صالحة. يرجى تسجيل الدخول من جديد."))
            }
            b.addHeader("Authorization", "Bearer $token")
        } else {
            accessToken()?.let { b.addHeader("Authorization", "Bearer $it") }
        }

        extra.forEach { (k, v) -> b.addHeader(k, v) }
        if (body != null) {
            b.method(method, body.toString().toRequestBody(JSON))
        } else {
            b.method(method, null)
        }
        return Result.success(b.build())
    }

    private suspend fun call(
        path: String,
        method: String = "GET",
        body: JSONObject? = null,
        extra: Map<String, String> = emptyMap(),
        requiresAuth: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        val reqResult = request(path, method, body, extra, requiresAuth)
        if (reqResult.isFailure) return@withContext Result.failure(reqResult.exceptionOrNull()!!)
        try {
            http.newCall(reqResult.getOrThrow()).execute().use { r ->
                val text = r.body?.string().orEmpty()
                if (r.isSuccessful) {
                    Result.success(text)
                } else {
                    val errMsg = try {
                        val j = JSONObject(text)
                        j.optString("message", j.optString("error_description", j.optString("hint", text)))
                    } catch (_: Exception) { text }
                    Result.failure(Exception("خطأ في الخادم (${r.code}): $errMsg"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun upsert(
        table: String,
        body: JSONObject,
        conflict: String,
        requiresAuth: Boolean = true
    ): Result<String> = call(
        "rest/v1/$table?on_conflict=$conflict",
        "POST",
        body,
        mapOf("Prefer" to "resolution=merge-duplicates,return=representation"),
        requiresAuth
    )

    private fun rows(text: String): JSONArray =
        if (text.trim().startsWith("[")) JSONArray(text) else JSONArray()

    private fun s(o: JSONObject, k: String, d: String = "") = if (o.isNull(k)) d else o.optString(k, d)
    private fun l(o: JSONObject, k: String, d: Long = 0L) = if (o.isNull(k)) d else o.optLong(k, d)

    // --- RPCs ---
    suspend fun rpc(functionName: String, params: JSONObject = JSONObject()): Result<String> = withContext(Dispatchers.IO) {
        val token = accessToken()
        if (token.isNullOrBlank()) {
            return@withContext Result.failure(Exception("جلسة تسجيل الدخول غير صالحة. يرجى تسجيل الدخول من جديد."))
        }
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
                    val errMsg = try {
                        val j = JSONObject(body)
                        j.optString("message", j.optString("error_description", j.optString("hint", body)))
                    } catch (_: Exception) { body }
                    Result.failure(Exception(errMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rpcIsAdmin(): Result<Boolean> {
        val res = rpc("is_admin")
        return res.map { it.trim().equals("true", ignoreCase = true) }
    }

    suspend fun rpcApproveTopUp(requestId: String, adminNote: String): Result<Boolean> {
        val params = JSONObject().put("p_request_id", requestId).put("p_admin_note", adminNote)
        return rpc("approve_top_up", params).map { true }
    }

    suspend fun rpcRejectTopUp(requestId: String, reason: String): Result<Boolean> {
        val params = JSONObject().put("p_request_id", requestId).put("p_reason", reason)
        return rpc("reject_top_up", params).map { true }
    }

    suspend fun rpcDebitWallet(userId: String, amount: Int, description: String, referenceId: String): Result<Boolean> {
        val params = JSONObject()
            .put("p_user_id", userId)
            .put("p_amount", amount)
            .put("p_description", description)
            .put("p_reference_id", referenceId)
        return rpc("debit_wallet", params).map { true }
    }

    suspend fun rpcPayAndSubmitListing(listingId: String, packageType: String, paymentMethod: String): Result<JSONObject> {
        val params = JSONObject()
            .put("p_listing_id", listingId)
            .put("p_package_type", packageType)
            .put("p_payment_method", paymentMethod)
        return rpc("pay_and_submit_listing", params).map { JSONObject(it) }
    }

    suspend fun rpcIncrementListingViews(listingId: String): Result<Unit> {
        val params = JSONObject().put("p_listing_id", listingId)
        return rpc("increment_listing_views", params).map { }
    }

    // --- Listings ---
    suspend fun saveListing(x: ListingEntity) = upsert(
        "listings",
        JSONObject().apply {
            put("id", x.id)
            put("user_id", x.userId)
            put("title", x.title)
            put("description", x.description)
            put("category_id", x.categoryId)
            put("price_dzd", x.priceDzd)
            put("condition", x.condition)
            put("wilaya", x.wilayaName)
            put("commune", x.commune)
            put("images", JSONArray(x.imagesJson.split(",").filter { it.isNotBlank() }))
            put("status", if (x.status == "PENDING") "UNDER_REVIEW" else x.status)
            put("is_paid", x.isPaid)
            put("is_featured", x.isFeatured)
            put("is_urgent", x.isUrgent)
            put("views_count", x.viewsCount)
            put("expires_at", if (x.expiresAt > 0) java.time.Instant.ofEpochMilli(x.expiresAt).toString() else JSONObject.NULL)
        },
        "id"
    )

    suspend fun getListings(): Result<List<ListingEntity>> = call(
        "rest/v1/listings?select=*&order=created_at.desc&limit=500",
        requiresAuth = false
    ).map { text ->
        rows(text).let { a ->
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                ListingEntity(
                    s(o, "id"),
                    s(o, "user_id"),
                    s(o, "user_name"),
                    s(o, "user_phone"),
                    o.optBoolean("is_phone_visible", true),
                    s(o, "title"),
                    s(o, "description"),
                    s(o, "category_id"),
                    s(o, "category_name_ar"),
                    s(o, "subcategory"),
                    l(o, "price_dzd"),
                    o.optBoolean("is_negotiable", false),
                    s(o, "condition", "NEW"),
                    o.optInt("wilaya_code", 16),
                    s(o, "wilaya"),
                    s(o, "commune"),
                    s(o, "images").ifBlank {
                        o.optJSONArray("images")?.let { j -> (0 until j.length()).joinToString(",") { n -> j.optString(n) } } ?: ""
                    },
                    s(o, "video_url"),
                    s(o, "status", "DRAFT"),
                    s(o, "rejection_reason"),
                    s(o, "package_type", "STANDARD"),
                    o.optInt("publishing_fee_dzd", 0),
                    o.optBoolean("is_paid"),
                    o.optBoolean("is_featured"),
                    o.optBoolean("is_urgent"),
                    o.optInt("views_count"),
                    System.currentTimeMillis(),
                    s(o, "expires_at").toInstantOrZero()
                )
            }
        }
    }

    suspend fun updateListingStatus(id: String, status: String, reason: String = "") =
        call(
            "rest/v1/listings?id=eq.$id",
            "PATCH",
            JSONObject().apply {
                put("status", if (status == "PENDING") "UNDER_REVIEW" else status)
                put("rejection_reason", reason)
            },
            requiresAuth = true
        ).map { }

    // --- Profiles ---
    suspend fun saveUser(x: UserEntity) = upsert(
        "profiles",
        JSONObject().apply {
            put("id", x.id)
            put("email", x.email)
            put("phone", x.phone)
            put("name", x.name)
            put("avatar_url", x.avatarUrl)
            put("wilaya", x.wilaya)
            put("commune", x.commune)
            put("bio", x.bio)
            put("seller_rating", x.sellerRating)
            put("reviews_count", x.reviewsCount)
            put("ads_count", x.adsCount)
            put("is_verified", x.isVerified)
            put("is_banned", x.isBanned)
        },
        "id",
        requiresAuth = true
    )

    suspend fun getAllUsers(): Result<List<UserEntity>> = call(
        "rest/v1/profiles?select=*&limit=500",
        requiresAuth = true
    ).map { text ->
        rows(text).let { a ->
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                UserEntity(
                    s(o, "id"),
                    s(o, "phone"),
                    s(o, "email"),
                    s(o, "name"),
                    s(o, "avatar_url"),
                    s(o, "wilaya"),
                    s(o, "commune"),
                    s(o, "bio"),
                    o.optDouble("seller_rating", 0.0),
                    o.optInt("reviews_count"),
                    o.optInt("ads_count"),
                    System.currentTimeMillis(),
                    o.optBoolean("is_verified"),
                    false,
                    o.optBoolean("is_banned"),
                    s(o, "role", "USER")
                )
            }
        }
    }

    suspend fun getUser(id: String): Result<UserEntity?> = call(
        "rest/v1/profiles?id=eq.$id&select=*&limit=1",
        requiresAuth = true
    ).map { text ->
        rows(text).let { a ->
            if (a.length() == 0) null
            else {
                val o = a.getJSONObject(0)
                UserEntity(
                    s(o, "id"),
                    s(o, "phone"),
                    s(o, "email"),
                    s(o, "name"),
                    s(o, "avatar_url"),
                    s(o, "wilaya"),
                    s(o, "commune"),
                    s(o, "bio"),
                    o.optDouble("seller_rating", 0.0),
                    o.optInt("reviews_count"),
                    o.optInt("ads_count"),
                    System.currentTimeMillis(),
                    o.optBoolean("is_verified"),
                    false,
                    o.optBoolean("is_banned"),
                    s(o, "role", "USER")
                )
            }
        }
    }

    suspend fun updateUserBan(id: String, banned: Boolean) =
        call("rest/v1/profiles?id=eq.$id", "PATCH", JSONObject().put("is_banned", banned), requiresAuth = true).map { }

    suspend fun updateUserVerification(id: String, verified: Boolean) =
        call("rest/v1/profiles?id=eq.$id", "PATCH", JSONObject().put("is_verified", verified), requiresAuth = true).map { }

    // --- Wallets ---
    suspend fun saveWallet(x: WalletEntity): Result<Boolean> = upsert(
        "wallets",
        JSONObject().apply {
            put("user_id", x.userId)
            put("balance_dzd", x.balanceDzd)
        },
        "user_id"
    ).map { true }

    suspend fun getWallet(id: String): Result<WalletEntity?> =
        call("rest/v1/wallets?user_id=eq.$id&select=*&limit=1", requiresAuth = true).map { text ->
            rows(text).let {
                if (it.length() == 0) null
                else {
                    val o = it.getJSONObject(0)
                    WalletEntity(id, o.optInt("balance_dzd"), System.currentTimeMillis())
                }
            }
        }

    // --- Top Up Requests ---
    suspend fun submitTopUpRequest(x: TopUpRequestEntity) = call(
        "rest/v1/top_up_requests",
        "POST",
        JSONObject().apply {
            put("id", x.id)
            put("user_id", x.userId)
            put("amount_dzd", x.amountDzd)
            put("provider", x.provider)
            put("reference", x.reference)
            put("receipt_path", x.receiptImageUri)
            put("status", x.status)
            put("admin_note", x.adminNote)
        },
        requiresAuth = true
    ).map { x.id }

    suspend fun getTopUpRequests(): Result<List<TopUpRequestEntity>> = call(
        "rest/v1/top_up_requests?select=*&order=created_at.desc&limit=500",
        requiresAuth = true
    ).map { text ->
        rows(text).let { a ->
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                TopUpRequestEntity(
                    s(o, "id"),
                    s(o, "user_id"),
                    "",
                    "",
                    o.optInt("amount_dzd"),
                    s(o, "provider"),
                    s(o, "reference"),
                    s(o, "receipt_path"),
                    s(o, "status", "PENDING"),
                    s(o, "admin_note"),
                    System.currentTimeMillis(),
                    0L
                )
            }
        }
    }

    // --- Payment Orders ---
    suspend fun savePayment(x: PaymentOrderEntity) = upsert(
        "payment_orders",
        JSONObject().apply {
            put("payment_id", x.paymentId)
            put("user_id", x.userId)
            put("listing_id", x.listingId)
            put("amount", x.amount)
            put("currency", x.currency)
            put("status", x.status)
            put("provider", x.provider)
            put("transaction_reference", x.transactionReference)
            put("created_at", java.time.Instant.ofEpochMilli(x.createdAt).toString())
            put("completed_at", if (x.completedAt > 0) java.time.Instant.ofEpochMilli(x.completedAt).toString() else JSONObject.NULL)
        },
        "payment_id",
        requiresAuth = true
    )

    suspend fun getPayments(): Result<List<PaymentOrderEntity>> = call(
        "rest/v1/payment_orders?select=*&order=created_at.desc&limit=500",
        requiresAuth = true
    ).map { text ->
        rows(text).let { a ->
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                PaymentOrderEntity(
                    s(o, "payment_id"),
                    s(o, "user_id"),
                    s(o, "listing_id"),
                    o.optInt("amount"),
                    s(o, "currency", "DZD"),
                    s(o, "status", "PENDING"),
                    s(o, "provider"),
                    s(o, "transaction_reference"),
                    System.currentTimeMillis(),
                    0L
                )
            }
        }
    }

    // --- Reviews ---
    suspend fun saveReview(x: ReviewEntity) = upsert(
        "reviews",
        JSONObject().apply {
            put("id", x.id)
            put("seller_id", x.sellerId)
            put("buyer_id", x.buyerId)
            put("listing_id", x.listingId)
            put("buyer_name", x.buyerName)
            put("rating", x.rating)
            put("comment", x.comment)
            put("created_at", java.time.Instant.ofEpochMilli(x.timestamp).toString())
        },
        "id",
        requiresAuth = true
    )

    suspend fun getReviews(): Result<List<ReviewEntity>> = call(
        "rest/v1/reviews?select=*&order=created_at.desc&limit=500",
        requiresAuth = false
    ).map { text ->
        rows(text).let { a ->
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                ReviewEntity(
                    s(o, "id"),
                    s(o, "seller_id"),
                    s(o, "buyer_id"),
                    s(o, "buyer_name"),
                    s(o, "listing_id"),
                    o.optInt("rating"),
                    s(o, "comment"),
                    System.currentTimeMillis()
                )
            }
        }
    }

    // --- Chat Messages ---
    suspend fun saveChatMessage(x: ChatMessageEntity) = call(
        "rest/v1/chat_messages",
        "POST",
        JSONObject().apply {
            put("id", x.id)
            put("listing_id", x.listingId)
            put("sender_id", x.senderId)
            put("receiver_id", x.receiverId)
            put("content", x.content)
            put("is_offer", x.isOffer)
            put("offer_amount_dzd", x.offerAmountDzd)
            put("offer_status", x.offerStatus)
        },
        requiresAuth = true
    ).map { x.id }

    suspend fun getChatMessages(listingId: String, user1: String, user2: String): Result<List<ChatMessageEntity>> = call(
        "rest/v1/chat_messages?listing_id=eq.$listingId&order=created_at.asc&limit=500",
        requiresAuth = true
    ).map { text ->
        rows(text).let { a ->
            (0 until a.length()).mapNotNull { i ->
                val o = a.getJSONObject(i)
                val sId = s(o, "sender_id")
                val rId = s(o, "receiver_id")
                if ((sId == user1 && rId == user2) || (sId == user2 && rId == user1)) {
                    ChatMessageEntity(
                        s(o, "id"),
                        listingId,
                        sId,
                        rId,
                        s(o, "content"),
                        System.currentTimeMillis(),
                        o.optBoolean("is_offer"),
                        l(o, "offer_amount_dzd"),
                        s(o, "offer_status", "NONE")
                    )
                } else null
            }
        }
    }

    // --- Reports & Favorites ---
    suspend fun saveReport(x: ReportEntity) = call(
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
        },
        requiresAuth = true
    ).map { x.id }

    suspend fun saveFavorite(x: FavoriteEntity) = upsert(
        "favorites",
        JSONObject().apply {
            put("id", x.id)
            put("user_id", x.userId)
            put("listing_id", x.listingId)
            put("notify_price_drop", x.notifyPriceDrop)
            put("notify_similar", x.notifySimilar)
            put("created_at", java.time.Instant.ofEpochMilli(x.timestamp).toString())
        },
        "user_id,listing_id",
        requiresAuth = true
    )

    suspend fun deleteFavorite(userId: String, listingId: String) =
        call("rest/v1/favorites?user_id=eq.$userId&listing_id=eq.$listingId", "DELETE", requiresAuth = true).map { }

    // --- Platform Settings ---
    suspend fun savePlatformSettings(x: PlatformSettingsEntity) = call(
        "rest/v1/platform_settings?id=eq.global",
        "PATCH",
        JSONObject().apply {
            put("standard_ad_fee_dzd", x.standardAdFeeDzd)
            put("featured_ad_fee_dzd", x.featuredAdFeeDzd)
            put("urgent_ad_fee_dzd", x.urgentAdFeeDzd)
            put("ad_duration_days", x.adDurationDays)
            put("auto_publish_after_payment", x.autoPublishAfterPayment)
        },
        requiresAuth = true
    ).map { "true" }

    suspend fun getPlatformSettings(): Result<PlatformSettingsEntity?> = call(
        "rest/v1/platform_settings?select=*&limit=1",
        requiresAuth = false
    ).map { text ->
        rows(text).let {
            if (it.length() == 0) null
            else {
                val o = it.getJSONObject(0)
                PlatformSettingsEntity(
                    standardAdFeeDzd = o.optInt("standard_ad_fee_dzd", 400),
                    featuredAdFeeDzd = o.optInt("featured_ad_fee_dzd", 600),
                    urgentAdFeeDzd = o.optInt("urgent_ad_fee_dzd", 1000),
                    adDurationDays = o.optInt("ad_duration_days", 30),
                    autoPublishAfterPayment = o.optBoolean("auto_publish_after_payment", false)
                )
            }
        }
    }

    // --- Orders ---
    suspend fun saveOrder(x: OrderEntity) = call(
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
        },
        requiresAuth = true
    ).map { x.id }

    suspend fun updateOrderStatus(id: String, status: String, note: String, tracking: String?) = call(
        "rest/v1/orders?id=eq.$id",
        "PATCH",
        JSONObject().apply {
            put("status", status)
            put("tracking_number", tracking ?: JSONObject.NULL)
        },
        requiresAuth = true
    ).map { }

    // --- Storage ---
    suspend fun uploadListingImage(context: Context, ownerId: String, uri: Uri, name: String): Result<String> {
        val currentUid = SupabaseSessionStore.userId()
        if (currentUid.isNullOrBlank() || currentUid != ownerId) {
            return Result.failure(Exception("جلسة المستخدم غير صالحة لرفع صور الإعلان."))
        }
        return upload(context, "oca-vente-media", "$ownerId/listings/$name.jpg", uri) { path ->
            "$BASE_URL/storage/v1/object/public/oca-vente-media/$path"
        }
    }

    /**
     * Uploads top-up payment receipt to private storage bucket.
     * Enforces authentic user UID path and valid access token.
     */
    suspend fun uploadReceiptImage(context: Context, ownerId: String, uri: Uri, name: String): Result<String> {
        val currentUid = SupabaseSessionStore.userId()
        if (currentUid.isNullOrBlank() || currentUid != ownerId) {
            return Result.failure(Exception("جلسة تسجيل الدخول غير صالحة. يرجى تسجيل الدخول من جديد."))
        }
        return upload(context, "oca-vente-private", "$ownerId/receipts/$name.jpg", uri) { path ->
            "oca-vente-private/$path"
        }
    }

    /**
     * Resolves private receipt object path into a temporary secure signed URL.
     */
    suspend fun createSignedReceiptUrl(storagePath: String, expiresInSeconds: Int = 3600): Result<String> =
        getSignedReceiptUrl(storagePath, expiresInSeconds)

    suspend fun getSignedReceiptUrl(storagePath: String, expiresInSeconds: Int = 3600): Result<String> = withContext(Dispatchers.IO) {
        val token = accessToken()
        if (token.isNullOrBlank()) {
            return@withContext Result.failure(Exception("جلسة تسجيل الدخول غير صالحة."))
        }
        try {
            val cleanPath = storagePath.trim()
                .removePrefix("$BASE_URL/storage/v1/object/sign/oca-vente-private/")
                .removePrefix("$BASE_URL/storage/v1/object/public/oca-vente-private/")
                .removePrefix("oca-vente-private/")

            val req = Request.Builder()
                .url("$BASE_URL/storage/v1/object/sign/oca-vente-private/$cleanPath")
                .post(JSONObject().put("expiresIn", expiresInSeconds).toString().toRequestBody(JSON))
                .addHeader("apikey", ANON_KEY)
                .addHeader("Authorization", "Bearer $token")
                .build()

            http.newCall(req).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    val signedPath = json.optString("signedURL")
                    if (signedPath.isNotBlank()) {
                        Result.success("$BASE_URL/storage/v1$signedPath")
                    } else {
                        Result.failure(Exception("تعذر الحصول على رابط الوصل."))
                    }
                } else {
                    Result.failure(Exception("فشل إنشاء رابط المعاينة (${response.code})."))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun upload(
        context: Context,
        bucket: String,
        path: String,
        uri: Uri,
        result: (String) -> String
    ): Result<String> = withContext(Dispatchers.IO) {
        val token = accessToken()
        if (token.isNullOrBlank()) {
            return@withContext Result.failure(Exception("جلسة تسجيل الدخول غير صالحة. يرجى تسجيل الدخول من جديد."))
        }
        var bmp: Bitmap? = null
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("تعذر فتح الصورة من المعرض."))
            bmp = BitmapFactory.decodeStream(input)
            input.close()
            if (bmp == null) return@withContext Result.failure(Exception("الصورة المحددة غير صالحة."))

            val out = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 75, out)
            val bytes = out.toByteArray()
            out.close()

            val req = Request.Builder()
                .url("$BASE_URL/storage/v1/object/$bucket/$path")
                .post(bytes.toRequestBody("image/jpeg".toMediaType()))
                .addHeader("apikey", ANON_KEY)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("x-upsert", "true")
                .build()

            http.newCall(req).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.w(TAG, "Storage upload failed (${response.code}) bucket=$bucket path=$path: $body")
                    val message = when {
                        response.code == 401 || response.code == 403 ->
                            "انتهت جلسة تسجيل الدخول أو لا تملك صلاحية رفع الملف. يرجى تسجيل الدخول مجدداً."
                        response.code == 404 ->
                            "مساحة التخزين غير متوفرة حالياً على الخادم."
                        response.code in 500..599 ->
                            "خدمة التخزين السحابي غير متاحة مؤقتاً. يرجى المحاولة لاحقاً."
                        else ->
                            "تعذر رفع الصورة. تحقق من الاتصال بالإنترنت وحاول مرة أخرى."
                    }
                    Result.failure(Exception(message))
                } else {
                    Result.success(result(path))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload exception", e)
            Result.failure(Exception("حدث خطأ أثناء تجهيز الصورة ورفعها: ${e.message}", e))
        } finally {
            bmp?.recycle()
        }
    }

    suspend fun deleteListingMedia(storagePathOrUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        val token = accessToken()
        if (token.isNullOrBlank()) {
            return@withContext Result.failure(Exception("جلسة تسجيل الدخول غير صالحة."))
        }
        try {
            val path = storagePathOrUrl.trim()
                .removePrefix("$BASE_URL/storage/v1/object/public/oca-vente-media/")
                .removePrefix("oca-vente-media/")
            if (path.isBlank() || path.startsWith("http") || path.startsWith("content://")) {
                return@withContext Result.failure(Exception("مسار صورة الإعلان غير صالح"))
            }
            val response = http.newCall(
                Request.Builder()
                    .url("$BASE_URL/storage/v1/object/oca-vente-media/$path")
                    .delete()
                    .addHeader("apikey", ANON_KEY)
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            ).execute()
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("فشل حذف صورة الإعلان: ${response.body?.string().orEmpty()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun String.toInstantOrZero(): Long = try {
    java.time.Instant.parse(this).toEpochMilli()
} catch (_: Exception) {
    0L
}
