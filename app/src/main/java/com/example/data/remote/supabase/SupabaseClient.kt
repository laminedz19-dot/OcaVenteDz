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
import java.util.concurrent.TimeUnit

class SupabaseClient {
    companion object {
        const val BASE_URL = BuildConfig.SUPABASE_URL
        private const val ANON_KEY = BuildConfig.SUPABASE_ANON_KEY
        private const val PREFS = "supabase_session"
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
    private val http = OkHttpClient.Builder().connectTimeout(25, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS).writeTimeout(25, TimeUnit.SECONDS).build()
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun accessToken(): String? = SupabaseSessionStore.accessToken()
    private var tokenContext: Context? = null
    fun attachContext(context: Context) { tokenContext = context.applicationContext }
    fun setAccessToken(context: Context, token: String?, refresh: String? = null) { attachContext(context); if (token == null) SupabaseSessionStore.clear() else SupabaseSessionStore.save(token, refresh) }
    fun clearSession(context: Context) { prefs(context).edit().clear().apply() }

    private fun request(path: String, method: String = "GET", body: JSONObject? = null, extra: Map<String, String> = emptyMap()): Request {
        val b = Request.Builder().url("$BASE_URL$path").addHeader("apikey", ANON_KEY).addHeader("Authorization", "Bearer ${accessToken() ?: ANON_KEY}")
        extra.forEach { (k, v) -> b.addHeader(k, v) }
        if (body != null) b.method(method, body.toString().toRequestBody(JSON)) else b.method(method, null)
        return b.build()
    }
    private suspend fun call(path: String, method: String = "GET", body: JSONObject? = null, extra: Map<String, String> = emptyMap()): Result<String> = withContext(Dispatchers.IO) {
        try { http.newCall(request(path, method, body, extra)).execute().let { r -> val text = r.body?.string().orEmpty(); if (r.isSuccessful) Result.success(text) else Result.failure(Exception("Supabase ${r.code}: $text")) } } catch (e: Exception) { Result.failure(e) }
    }
    private suspend fun upsert(table: String, body: JSONObject, conflict: String): Result<String> = call("rest/v1/$table?on_conflict=$conflict", "POST", body, mapOf("Prefer" to "resolution=merge-duplicates,return=representation"))
    private fun rows(text: String): JSONArray = if (text.trim().startsWith("[")) JSONArray(text) else JSONArray()
    private fun s(o: JSONObject, k: String, d: String = "") = if (o.isNull(k)) d else o.optString(k, d)
    private fun l(o: JSONObject, k: String, d: Long = 0L) = if (o.isNull(k)) d else o.optLong(k, d)

    suspend fun saveListing(x: ListingEntity) = upsert("listings", JSONObject().apply { put("id", x.id); put("user_id", x.userId); put("title", x.title); put("description", x.description); put("category_id", x.categoryId); put("price_dzd", x.priceDzd); put("condition", x.condition); put("wilaya", x.wilayaName); put("commune", x.commune); put("images", JSONArray(x.imagesJson.split(",").filter { it.isNotBlank() })); put("status", if (x.status == "PENDING") "UNDER_REVIEW" else x.status); put("is_paid", x.isPaid); put("is_featured", x.isFeatured); put("is_urgent", x.isUrgent); put("views_count", x.viewsCount); put("expires_at", if (x.expiresAt > 0) java.time.Instant.ofEpochMilli(x.expiresAt).toString() else JSONObject.NULL) }, "id")
    suspend fun getListings(): Result<List<ListingEntity>> = call("rest/v1/listings?select=*&order=created_at.desc&limit=500").map { text -> rows(text).let { a -> (0 until a.length()).map { i -> val o=a.getJSONObject(i); ListingEntity(s(o,"id"),s(o,"user_id"),s(o,"user_name"),s(o,"user_phone"),o.optBoolean("is_phone_visible",true),s(o,"title"),s(o,"description"),s(o,"category_id"),s(o,"category_name_ar"),s(o,"subcategory"),l(o,"price_dzd"),o.optBoolean("is_negotiable",false),s(o,"condition","NEW"),o.optInt("wilaya_code",16),s(o,"wilaya"),s(o,"commune"),s(o,"images").ifBlank { o.optJSONArray("images")?.let { j -> (0 until j.length()).joinToString(",") { n -> j.optString(n) } } ?: "" },s(o,"video_url"),s(o,"status","DRAFT"),s(o,"rejection_reason"),s(o,"package_type","STANDARD"),o.optInt("publishing_fee_dzd",0),o.optBoolean("is_paid"),o.optBoolean("is_featured"),o.optBoolean("is_urgent"),o.optInt("views_count"),System.currentTimeMillis(),s(o,"expires_at").toInstantOrZero()) } }}
    suspend fun updateListingStatus(id: String, status: String, reason: String = "") = call("rest/v1/listings?id=eq.$id", "PATCH", JSONObject().apply { put("status", if (status=="PENDING") "UNDER_REVIEW" else status); put("rejection_reason", reason) }).map { }

    suspend fun saveUser(x: UserEntity) = upsert("profiles", JSONObject().apply { put("id", x.id); put("email", x.email); put("phone", x.phone); put("name", x.name); put("avatar_url", x.avatarUrl); put("wilaya", x.wilaya); put("commune", x.commune); put("bio", x.bio); put("seller_rating", x.sellerRating); put("reviews_count", x.reviewsCount); put("ads_count", x.adsCount); put("is_verified", x.isVerified); put("is_banned", x.isBanned) }, "id")
    suspend fun getAllUsers(): Result<List<UserEntity>> = call("rest/v1/profiles?select=*&limit=500").map { text -> rows(text).let { a -> (0 until a.length()).map { i -> val o=a.getJSONObject(i); UserEntity(s(o,"id"),s(o,"phone"),s(o,"email"),s(o,"name"),s(o,"avatar_url"),s(o,"wilaya"),s(o,"commune"),s(o,"bio"),o.optDouble("seller_rating",0.0),o.optInt("reviews_count"),o.optInt("ads_count"),System.currentTimeMillis(),o.optBoolean("is_verified"),false,o.optBoolean("is_banned"),"USER") } }}
    suspend fun updateUserBan(id: String, banned: Boolean) = call("rest/v1/profiles?id=eq.$id", "PATCH", JSONObject().put("is_banned", banned)).map { }
    suspend fun updateUserVerification(id: String, verified: Boolean) = call("rest/v1/profiles?id=eq.$id", "PATCH", JSONObject().put("is_verified", verified)).map { }

    suspend fun saveWallet(x: WalletEntity) = call("rest/v1/wallets?on_conflict=user_id", "POST", JSONObject().apply { put("user_id",x.userId); put("balance_dzd",x.balanceDzd); put("updated_at",java.time.Instant.ofEpochMilli(x.updatedAt).toString()) }, mapOf("Prefer" to "resolution=merge-duplicates,return=representation")).map { x.userId }
    suspend fun getWallet(id: String): Result<WalletEntity?> = call("rest/v1/wallets?user_id=eq.$id&select=*&limit=1").map { text -> rows(text).let { if (it.length()==0) null else { val o=it.getJSONObject(0); WalletEntity(id,o.optInt("balance_dzd"),System.currentTimeMillis()) } } }

    suspend fun submitTopUpRequest(x: TopUpRequestEntity) = call("rest/v1/top_up_requests", "POST", JSONObject().apply { put("id",x.id);put("user_id",x.userId);put("amount_dzd",x.amountDzd);put("provider",x.provider);put("reference",x.reference);put("receipt_path",x.receiptImageUri);put("status",x.status);put("admin_note",x.adminNote) }).map { x.id }
    suspend fun getTopUpRequests(): Result<List<TopUpRequestEntity>> = call("rest/v1/top_up_requests?select=*&order=created_at.desc&limit=500").map { text -> rows(text).let { a -> (0 until a.length()).map { i -> val o=a.getJSONObject(i); TopUpRequestEntity(s(o,"id"),s(o,"user_id"),"","",o.optInt("amount_dzd"),s(o,"provider"),s(o,"reference"),s(o,"receipt_path"),s(o,"status","PENDING"),s(o,"admin_note"),System.currentTimeMillis(),0L) } }}
    suspend fun updateTopUpStatus(id: String, status: String, note: String) = call("rest/v1/top_up_requests?id=eq.$id", "PATCH", JSONObject().apply { put("status",status);put("admin_note",note);put("reviewed_at",java.time.Instant.now().toString()) }).map { }

    suspend fun savePayment(x: PaymentOrderEntity) = upsert("payment_orders", JSONObject().apply { put("payment_id",x.paymentId);put("user_id",x.userId);put("listing_id",x.listingId);put("amount",x.amount);put("currency",x.currency);put("status",x.status);put("provider",x.provider);put("transaction_reference",x.transactionReference);put("created_at",java.time.Instant.ofEpochMilli(x.createdAt).toString());put("completed_at",if(x.completedAt>0) java.time.Instant.ofEpochMilli(x.completedAt).toString() else JSONObject.NULL) }, "payment_id")
    suspend fun getPayments(): Result<List<PaymentOrderEntity>> = call("rest/v1/payment_orders?select=*&order=created_at.desc&limit=500").map { text -> rows(text).let { a -> (0 until a.length()).map { i -> val o=a.getJSONObject(i); PaymentOrderEntity(s(o,"payment_id"),s(o,"user_id"),s(o,"listing_id"),o.optInt("amount"),s(o,"currency","DZD"),s(o,"status","PENDING"),s(o,"provider"),s(o,"transaction_reference"),System.currentTimeMillis(),0L) } }}

    suspend fun saveReview(x: ReviewEntity) = upsert("reviews", JSONObject().apply { put("id",x.id);put("seller_id",x.sellerId);put("buyer_id",x.buyerId);put("listing_id",x.listingId);put("buyer_name",x.buyerName);put("rating",x.rating);put("comment",x.comment);put("created_at",java.time.Instant.ofEpochMilli(x.timestamp).toString()) }, "id")
    suspend fun getReviews(): Result<List<ReviewEntity>> = call("rest/v1/reviews?select=*&order=created_at.desc&limit=500").map { text -> rows(text).let { a -> (0 until a.length()).map { i -> val o=a.getJSONObject(i); ReviewEntity(s(o,"id"),s(o,"seller_id"),s(o,"buyer_id"),s(o,"buyer_name"),s(o,"listing_id"),o.optInt("rating"),s(o,"comment"),System.currentTimeMillis()) } }}

    suspend fun saveChatMessage(x: ChatMessageEntity) = call("rest/v1/chat_messages", "POST", JSONObject().apply { put("id",x.id);put("listing_id",x.listingId);put("sender_id",x.senderId);put("receiver_id",x.receiverId);put("content",x.content);put("is_offer",x.isOffer);put("offer_amount_dzd",x.offerAmountDzd);put("offer_status",x.offerStatus) }).map { x.id }
    suspend fun getChatMessages(listingId: String, user1: String, user2: String): Result<List<ChatMessageEntity>> = call("rest/v1/chat_messages?listing_id=eq.$listingId&order=created_at.asc&limit=500").map { text -> rows(text).let { a -> (0 until a.length()).mapNotNull { i -> val o=a.getJSONObject(i); val sId=s(o,"sender_id"); val rId=s(o,"receiver_id"); if ((sId==user1&&rId==user2)||(sId==user2&&rId==user1)) ChatMessageEntity(s(o,"id"),listingId,sId,rId,s(o,"content"),System.currentTimeMillis(),o.optBoolean("is_offer"),l(o,"offer_amount_dzd"),s(o,"offer_status","NONE")) else null } }}
    suspend fun saveReport(x: ReportEntity) = call("rest/v1/reports", "POST", JSONObject().apply { put("id",x.id);put("reporter_id",x.reporterId);put("reported_listing_id",x.reportedListingId);put("reported_user_id",x.reportedUserId);put("reason",x.reason);put("comment",x.comment);put("status",x.status) }).map { x.id }
    suspend fun saveFavorite(x: FavoriteEntity) = upsert("favorites", JSONObject().apply { put("id", x.id); put("user_id", x.userId); put("listing_id", x.listingId); put("notify_price_drop", x.notifyPriceDrop); put("notify_similar", x.notifySimilar); put("created_at", java.time.Instant.ofEpochMilli(x.timestamp).toString()) }, "user_id,listing_id")
    suspend fun deleteFavorite(userId: String, listingId: String) = call("rest/v1/favorites?user_id=eq.$userId&listing_id=eq.$listingId", "DELETE").map { }
    suspend fun saveWalletTransaction(x: WalletTransactionEntity) = upsert("wallet_transactions", JSONObject().apply { put("id", x.id); put("user_id", x.userId); put("type", x.type); put("amount", x.amount); put("description", x.description); put("reference_id", x.referenceId); put("created_at", java.time.Instant.ofEpochMilli(x.timestamp).toString()) }, "id")
    suspend fun savePlatformSettings(x: PlatformSettingsEntity) = call("rest/v1/platform_settings?id=eq.global", "PATCH", JSONObject().apply { put("standard_ad_fee_dzd",x.standardAdFeeDzd);put("featured_ad_fee_dzd",x.featuredAdFeeDzd);put("urgent_ad_fee_dzd",x.urgentAdFeeDzd);put("ad_duration_days",x.adDurationDays);put("auto_publish_after_payment",x.autoPublishAfterPayment) }).map { "true" }
    suspend fun getPlatformSettings(): Result<PlatformSettingsEntity?> = call("rest/v1/platform_settings?select=*&limit=1").map { text -> rows(text).let { if(it.length()==0)null else { val o=it.getJSONObject(0); PlatformSettingsEntity(standardAdFeeDzd=o.optInt("standard_ad_fee_dzd",400),featuredAdFeeDzd=o.optInt("featured_ad_fee_dzd",600),urgentAdFeeDzd=o.optInt("urgent_ad_fee_dzd",1000),adDurationDays=o.optInt("ad_duration_days",30),autoPublishAfterPayment=o.optBoolean("auto_publish_after_payment",false)) } } }
    suspend fun saveOrder(x: OrderEntity) = call("rest/v1/orders", "POST", JSONObject().apply { put("id",x.id);put("order_number",x.orderNumber);put("listing_id",x.listingId);put("seller_id",x.sellerId);put("buyer_id",x.buyerId);put("total_amount_dzd",x.totalAmountDzd);put("status",x.status);put("is_paid",x.isPaid);put("tracking_number",x.trackingNumber) }).map { x.id }
    suspend fun updateOrderStatus(id: String,status:String,note:String,tracking:String?) = call("rest/v1/orders?id=eq.$id", "PATCH", JSONObject().apply { put("status",status);put("tracking_number",tracking ?: JSONObject.NULL) }).map { }
    suspend fun uploadListingImage(context: Context, ownerId: String, uri: Uri, name: String) =
        upload(context, "oca-vente-media", "$ownerId/listings/$name.jpg", uri) { path ->
            "$BASE_URL/storage/v1/object/public/oca-vente-media/$path"
        }

    suspend fun uploadReceiptImage(context: Context, ownerId: String, uri: Uri, name: String) =
        upload(context, "oca-vente-private", "$ownerId/receipts/$name.jpg", uri) { path ->
            "oca-vente-private/$path"
        }

    private suspend fun upload(
        context: Context,
        bucket: String,
        path: String,
        uri: Uri,
        result: (String) -> String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("تعذر فتح الصورة"))
            val bmp = BitmapFactory.decodeStream(input)
            input.close()
            if (bmp == null) return@withContext Result.failure(Exception("الصورة غير صالحة"))
            val out = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 75, out)
            val req = Request.Builder()
                .url("$BASE_URL/storage/v1/object/$bucket/$path")
                .post(out.toByteArray().toRequestBody("image/jpeg".toMediaType()))
                .addHeader("apikey", ANON_KEY)
                .addHeader("Authorization", "Bearer ${accessToken() ?: ANON_KEY}")
                .addHeader("x-upsert", "true")
                .build()
            http.newCall(req).execute().let { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.w("SupabaseStorage", "Upload failed (${response.code}) bucket=$bucket path=$path: $body")
                    val message = when {
                        response.code == 404 && (body.contains("Bucket not found", ignoreCase = true) || body.contains("NoSuchBucket", ignoreCase = true)) ->
                            "تعذر رفع الوصل لأن مساحة التخزين غير متاحة. حدّث التطبيق وحاول مرة أخرى."
                        response.code == 401 || response.code == 403 ->
                            "انتهت جلسة الدخول أو لا تملك صلاحية رفع الوصل. سجّل الدخول من جديد."
                        response.code in 500..599 ->
                            "خدمة التخزين غير متاحة مؤقتًا. حاول مرة أخرى لاحقًا."
                        else ->
                            "تعذر رفع صورة الوصل. تحقق من الاتصال بالإنترنت وحاول مرة أخرى."
                    }
                    Result.failure(Exception(message))
                } else Result.success(result(path))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteListingMedia(storagePathOrUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val path = storagePathOrUrl.trim()
                .removePrefix("$BASE_URL/storage/v1/object/public/oca-vente-media/")
                .removePrefix("oca-vente-media/")
            if (path.isBlank() || path.startsWith("http") || path.startsWith("content://")) {
                return@withContext Result.failure(Exception("مسار صورة الإعلان غير صالح"))
            }
            val response = http.newCall(
                Request.Builder().url("$BASE_URL/storage/v1/object/oca-vente-media/$path")
                    .delete().addHeader("apikey", ANON_KEY)
                    .addHeader("Authorization", "Bearer ${accessToken() ?: ANON_KEY}").build()
            ).execute()
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("فشل حذف صورة الإعلان: ${response.body?.string().orEmpty()}"))
        } catch (e: Exception) { Result.failure(e) }
    }
}
private fun String.toInstantOrZero(): Long = try { java.time.Instant.parse(this).toEpochMilli() } catch (_: Exception) { 0L }
