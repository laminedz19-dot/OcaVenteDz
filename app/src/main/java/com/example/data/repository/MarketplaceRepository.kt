package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.FavoriteEntity
import com.example.data.local.ListingEntity
import com.example.data.local.OrderEntity
import com.example.data.local.PaymentOrderEntity
import com.example.data.local.PlatformSettingsEntity
import com.example.data.local.ReportEntity
import com.example.data.local.ReviewEntity
import com.example.data.local.UserEntity
import com.example.data.local.WalletEntity
import com.example.data.local.WalletTransactionEntity
import com.example.data.local.TopUpRequestEntity
import com.example.data.remote.auth.AuthRepository
import com.example.data.remote.supabase.SupabaseClient
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

class MarketplaceRepository(
    private val db: AppDatabase,
    val authService: AuthRepository = AuthRepository(),
    val supabaseClient: SupabaseClient = SupabaseClient()
) {

    // Listings
    fun getPublishedListings(): Flow<List<ListingEntity>> = db.listingDao().getAllPublishedListings()
    fun getAllListingsAdmin(): Flow<List<ListingEntity>> = db.listingDao().getAllListingsForAdmin()
    fun getUserListings(userId: String): Flow<List<ListingEntity>> = db.listingDao().getUserListings(userId)
    fun getListingById(id: String): Flow<ListingEntity?> = db.listingDao().getListingById(id)
    suspend fun getListingDirect(id: String): ListingEntity? = db.listingDao().getListingByIdDirect(id)

    suspend fun syncListingsFromCloud() {
        try { supabaseClient.getListings().getOrNull().orEmpty().let { if (it.isNotEmpty()) db.listingDao().insertListings(it) } } catch (_: Exception) {}
    }

    suspend fun saveListing(listing: ListingEntity) {
        db.listingDao().insertListing(listing)
        try {
            supabaseClient.saveListing(listing)
        } catch (_: Exception) {}
    }

    suspend fun updateListingStatus(id: String, status: String, rejectionReason: String = "") {
        db.listingDao().updateListingStatus(id, status, rejectionReason)
        try {
            supabaseClient.updateListingStatus(id, status, rejectionReason)
        } catch (_: Exception) {}
    }

    suspend fun deleteListing(id: String) {
        db.listingDao().deleteListing(id)
        try {
            supabaseClient.updateListingStatus(id, "DELETED")
        } catch (_: Exception) {}
    }

    suspend fun markListingAsSold(listingId: String, userId: String): Boolean =
        db.listingDao().updateStatusForOwner(listingId, userId, "SOLD") == 1

    suspend fun deleteUserData(userId: String) {
        db.withTransaction {
            db.listingDao().deleteListingsForUser(userId)
            db.walletDao().deleteTransactions(userId)
            db.walletDao().deleteWallet(userId)
            db.favoriteDao().deleteForUser(userId)
            db.userDao().deleteUser(userId)
        }
    }

    // Payment & Publishing workflow
    suspend fun processAdPayment(
        listingId: String,
        userId: String,
        packageType: String, // "STANDARD", "FEATURED", "URGENT"
        paymentMethod: String // "WALLET", "EDAHABIA", "CIB", "BARIDIMOB"
    ): Result<PaymentOrderEntity> {
        val now = System.currentTimeMillis()
        val rpcRes = supabaseClient.rpcPayAndSubmitListing(listingId, packageType, paymentMethod)
        if (rpcRes.isFailure) {
            val err = rpcRes.exceptionOrNull()
            return Result.failure(Exception(err?.message ?: "فشلت عملية الدفع وتحديث الإعلان في الخادم السحابي.", err))
        }

        val json = rpcRes.getOrNull()
        val paymentId = json?.optString("payment_id")?.takeIf { it.isNotBlank() }
            ?: ("PAY_" + UUID.randomUUID().toString().take(8).uppercase())
        val newStatus = json?.optString("status")?.takeIf { it.isNotBlank() } ?: "PUBLISHED"
        val fee = json?.optInt("amount", 0)?.takeIf { it > 0 } ?: when (packageType) {
            "FEATURED" -> 600
            "URGENT" -> 1000
            else -> 400
        }

        val order = PaymentOrderEntity(
            paymentId = paymentId,
            userId = userId,
            listingId = listingId,
            amount = fee,
            currency = "DZD",
            status = "SUCCESS",
            provider = paymentMethod,
            transactionReference = paymentId,
            createdAt = now,
            completedAt = now
        )

        return db.withTransaction {
            val listing = db.listingDao().getListingByIdDirect(listingId)
                ?: return@withTransaction Result.failure(Exception("الإعلان غير موجود."))

            db.paymentDao().insertPayment(order)
            db.listingDao().insertListing(listing.copy(
                status = newStatus,
                isPaid = true,
                publishingFeeDzd = fee,
                packageType = packageType,
                isFeatured = packageType != "STANDARD",
                isUrgent = packageType == "URGENT"
            ))
            if (paymentMethod == "WALLET") {
                db.walletDao().debitIfSufficient(userId, fee, now)
                db.walletDao().insertTransaction(
                    WalletTransactionEntity(
                        id = "TX_" + UUID.randomUUID().toString().take(8),
                        userId = userId,
                        type = "AD_PAYMENT",
                        amount = -fee,
                        description = "دفع رسوم نشر إعلان ($packageType)",
                        referenceId = paymentId,
                        timestamp = now
                    )
                )
            }
            Result.success(order)
        }
    }

    // Wallet Operations
    fun getWallet(userId: String): Flow<WalletEntity?> = db.walletDao().getWallet(userId)
    fun getWalletTransactions(userId: String): Flow<List<WalletTransactionEntity>> = db.walletDao().getTransactions(userId)

    fun getUserTopUpRequests(userId: String): Flow<List<TopUpRequestEntity>> =
        db.topUpRequestDao().getUserRequests(userId)

    fun getUserTopUpRequestsSync(userId: String): Flow<Result<List<TopUpRequestEntity>>> =
        getUserTopUpRequests(userId).map { Result.success(it) }
    fun getAllTopUpRequests(): Flow<List<TopUpRequestEntity>> =
        db.topUpRequestDao().getAllRequests()

    /** Submits a top-up request using Supabase and caches it in Room. */
    suspend fun submitTopUpRequest(
        context: Context, userId: String = "", amount: Int, provider: String,
        reference: String, receiptImageUriString: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (amount < 200) return@withContext Result.failure(IllegalArgumentException("الحد الأدنى لشحن الرصيد هو 200 دج."))
        if (provider !in setOf("BARIDIMOB", "CCP", "EDAHABIA", "CIB")) return@withContext Result.failure(IllegalArgumentException("مزود الدفع المحدد غير مدعوم."))
        if (reference.isBlank() && receiptImageUriString.isBlank()) return@withContext Result.failure(IllegalArgumentException("يجب إرفاق صورة الوصل أو رقم المرجع."))
        val uid = authService.currentUserId ?: userId.takeIf { it.isNotBlank() } ?: return@withContext Result.failure(IllegalStateException("يجب تسجيل الدخول أولاً."))
        val localUser = db.userDao().getUserByIdDirect(uid)
        val storedReceipt = if (receiptImageUriString.isNotBlank()) {
            val uploadResult = supabaseClient.uploadReceiptImage(
                context,
                uid,
                Uri.parse(receiptImageUriString),
                "receipt_${System.currentTimeMillis()}"
            )
            uploadResult.getOrElse { error ->
                return@withContext Result.failure(
                    Exception(error.message ?: "تعذر رفع صورة الوصل إلى الخادم.", error)
                )
            }
        } else ""
        val request = TopUpRequestEntity(
            id = UUID.randomUUID().toString(), userId = uid,
            userName = localUser?.name ?: "مستخدم OcaVenteDz", userPhone = localUser?.phone ?: "",
            amountDzd = amount, provider = provider, reference = reference.trim(),
            receiptImageUri = storedReceipt.ifBlank { receiptImageUriString }, status = "PENDING",
            adminNote = "", createdAt = System.currentTimeMillis(), reviewedAt = 0L
        )
        val cloud = try {
            supabaseClient.submitTopUpRequest(request)
        } catch (error: Exception) {
            Result.failure(Exception("تعذر الاتصال بالخادم.", error))
        }
        if (cloud.isFailure) {
            return@withContext Result.failure(
                cloud.exceptionOrNull() ?: Exception("تعذر تسجيل طلب الشحن لدى المشرف.")
            )
        }
        db.topUpRequestDao().insertRequest(request)
        Result.success("تم إرسال طلب الشحن بنجاح! ستتم مراجعته واعتماد الرصيد قريباً.")
    }
    suspend fun approveTopUpRequest(requestId: String, adminNote: String = "تم التحقق من الوصل بنجاح"): Result<String> {
        val note = adminNote.ifBlank { "تم التحقق من الوصل بنجاح" }
        val rpcRes = supabaseClient.rpcApproveTopUp(requestId, note)
        if (rpcRes.isFailure) {
            val err = rpcRes.exceptionOrNull()
            return Result.failure(Exception(err?.message ?: "فشل اعتماد طلب الشحن عبر الخادم السحابي.", err))
        }

        val request = db.topUpRequestDao().getRequestById(requestId)
        val now = System.currentTimeMillis()
        if (request != null) {
            db.walletDao().creditWallet(request.userId, request.amountDzd, now)
            val tx = WalletTransactionEntity(
                id = "tx_" + UUID.randomUUID().toString().replace("-", "").take(10),
                userId = request.userId,
                type = "TOPUP",
                amount = request.amountDzd,
                description = "شحن رصيد - وصل تحويل ${request.provider} (مرجع: ${request.reference.ifBlank { request.id }})",
                referenceId = request.id,
                timestamp = now
            )
            db.walletDao().insertTransaction(tx)
            db.topUpRequestDao().updateStatus(request.id, "APPROVED", note, now)
        }

        return Result.success("تمت الموافقة بنجاح وشحن الرصيد إلى المحفظة عبر الخادم السحابي ✓")
    }

    suspend fun rejectTopUpRequest(requestId: String, reason: String): Result<String> {
        val note = reason.ifBlank { "الوصل غير مطابق أو غير واضح" }
        val rpcRes = supabaseClient.rpcRejectTopUp(requestId, note)
        if (rpcRes.isFailure) {
            val err = rpcRes.exceptionOrNull()
            return Result.failure(Exception(err?.message ?: "فشل رفض طلب الشحن في الخادم السحابي.", err))
        }

        val now = System.currentTimeMillis()
        db.topUpRequestDao().updateStatus(requestId, "REJECTED", note, now)
        return Result.success("تم رفض طلب الشحن بنجاح.")
    }

    suspend fun topUpWallet(userId: String, amount: Int, paymentProvider: String, txReference: String? = null): Result<String> {
        return Result.failure(Exception("يرجى إرسال وصل التحويل للمراجعة اليدوية عبر شحن المحفظة."))
    }

    /** Reads the balance from Supabase, then falls back to Room. */
    suspend fun getCurrentUserBalance(userId: String): Result<Int> {
        try {
            val remote = supabaseClient.getWallet(userId).getOrNull()
            if (remote != null) {
                db.walletDao().insertOrUpdateWallet(remote)
                return Result.success(remote.balanceDzd)
            }
        } catch (_: Exception) {}
        return Result.success(db.walletDao().getWalletDirect(userId)?.balanceDzd ?: 0)
    }

    // Users
    fun getUser(id: String): Flow<UserEntity?> = db.userDao().getUserById(id)
    suspend fun getUserDirect(id: String): UserEntity? = db.userDao().getUserByIdDirect(id)
    suspend fun findUserByPhoneOrEmail(input: String): UserEntity? {
        val clean = input.trim().lowercase()
        val direct = db.userDao().getUserByPhoneOrEmail(clean)
        if (direct != null) return direct

        val directRaw = db.userDao().getUserByPhoneOrEmail(input.trim())
        if (directRaw != null) return directRaw

        val digitsOnly = clean.filter { it.isDigit() }
        val normalizedInput = when {
            digitsOnly.startsWith("00213") -> digitsOnly.removePrefix("00213")
            digitsOnly.startsWith("213") -> digitsOnly.removePrefix("213")
            digitsOnly.startsWith("0") -> digitsOnly.removePrefix("0")
            else -> digitsOnly
        }

        val allUsers = db.userDao().getAllUsersDirect()
        return allUsers.firstOrNull { user ->
            val userEmail = user.email.trim().lowercase()
            if (userEmail.isNotBlank() && userEmail == clean) return@firstOrNull true

            if (normalizedInput.length >= 8) {
                val userDigits = user.phone.filter { it.isDigit() }
                val normalizedUserPhone = when {
                    userDigits.startsWith("00213") -> userDigits.removePrefix("00213")
                    userDigits.startsWith("213") -> userDigits.removePrefix("213")
                    userDigits.startsWith("0") -> userDigits.removePrefix("0")
                    else -> userDigits
                }
                normalizedUserPhone == normalizedInput
            } else {
                false
            }
        }
    }
    fun getAllUsers(): Flow<List<UserEntity>> = db.userDao().getAllUsers()
    suspend fun getAllUsersDirect(): List<UserEntity> = db.userDao().getAllUsersDirect()
    suspend fun saveUser(user: UserEntity) {
        db.userDao().insertUser(user)
        try {
            supabaseClient.saveUser(user)
        } catch (_: Exception) {}

    }

    suspend fun createEmptyWallet(userId: String) {
        val entity = WalletEntity(userId = userId, balanceDzd = 0, updatedAt = System.currentTimeMillis())
        db.walletDao().insertOrUpdateWallet(entity)
        try {
            supabaseClient.saveWallet(entity)
        } catch (_: Exception) {}
    }

    suspend fun updateUser(user: UserEntity) {
        db.userDao().updateUser(user)
        try {
            supabaseClient.saveUser(user)
        } catch (_: Exception) {}
    }
    suspend fun updateBanStatus(userId: String, banned: Boolean) {
        db.userDao().updateBanStatus(userId, banned)
        try {
            supabaseClient.updateUserBan(userId, banned)
        } catch (_: Exception) {}
    }
    suspend fun updateVerification(userId: String, verified: Boolean) {
        db.userDao().updateVerificationStatus(userId, verified)
        try {
            supabaseClient.updateUserVerification(userId, verified)
        } catch (_: Exception) {}
    }
    suspend fun requestVerification(userId: String) = db.userDao().requestVerification(userId)
    suspend fun deleteUser(userId: String) = db.userDao().deleteUser(userId)

    // Chat & Offers
    fun getChatMessages(listingId: String, user1: String, user2: String): Flow<List<ChatMessageEntity>> =
        db.chatDao().getMessages(listingId, user1, user2)

    fun getUserConversations(userId: String): Flow<List<ChatMessageEntity>> =
        db.chatDao().getUserConversations(userId)

    suspend fun sendMessage(
        listingId: String,
        senderId: String,
        receiverId: String,
        content: String,
        isOffer: Boolean = false,
        offerAmount: Long = 0
    ) {
        val msg = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            listingId = listingId,
            senderId = senderId,
            receiverId = receiverId,
            content = content,
            timestamp = System.currentTimeMillis(),
            isOffer = isOffer,
            offerAmountDzd = offerAmount,
            offerStatus = if (isOffer) "PENDING" else "NONE"
        )
        db.chatDao().insertMessage(msg)
        try {
            supabaseClient.saveChatMessage(msg)
        } catch (_: Exception) {}
    }

    suspend fun updateOfferStatus(messageId: String, status: String) {
        db.chatDao().updateOfferStatus(messageId, status)
    }

    // Reviews
    fun getReviews(sellerId: String): Flow<List<ReviewEntity>> = db.reviewDao().getReviewsForSeller(sellerId)

    suspend fun addReview(sellerId: String, buyerId: String, buyerName: String, listingId: String, rating: Int, comment: String): Result<String> {
        if (sellerId == buyerId) {
            return Result.failure(Exception("لا يمكنك تقييم نفسك."))
        }
        val count = db.reviewDao().hasReviewed(sellerId, buyerId, listingId)
        if (count > 0) {
            return Result.failure(Exception("لقد قمت بتقييم هذه الصفقة مسبقاً."))
        }
        val review = ReviewEntity(
            id = "REV_" + UUID.randomUUID().toString().take(8),
            sellerId = sellerId,
            buyerId = buyerId,
            buyerName = buyerName,
            listingId = listingId,
            rating = rating,
            comment = comment,
            timestamp = System.currentTimeMillis()
        )
        db.reviewDao().insertReview(review)
        try { supabaseClient.saveReview(review) } catch (_: Exception) {}
        return Result.success("تم إضافة تقييمك بنجاح")
    }

    // Reports
    fun getAllReports(): Flow<List<ReportEntity>> = db.reportDao().getAllReports()
    suspend fun submitReport(reporterId: String, listingId: String, userId: String, reason: String, comment: String) {
        val report = ReportEntity(
            id = UUID.randomUUID().toString(),
            reporterId = reporterId,
            reportedListingId = listingId,
            reportedUserId = userId,
            reason = reason,
            comment = comment,
            timestamp = System.currentTimeMillis(),
            status = "PENDING"
        )
        db.reportDao().insertReport(report)
        try {
            supabaseClient.saveReport(report)
        } catch (_: Exception) {}
    }
    suspend fun updateReportStatus(reportId: String, status: String) = db.reportDao().updateReportStatus(reportId, status)

    // Favorites
    fun getFavorites(userId: String): Flow<List<FavoriteEntity>> = db.favoriteDao().getFavoritesForUser(userId)
    fun isFavorite(userId: String, listingId: String): Flow<Int> = db.favoriteDao().isFavorite(userId, listingId)
    suspend fun toggleFavorite(userId: String, listingId: String, currentlyFav: Boolean) {
        if (currentlyFav) {
            db.favoriteDao().deleteFavorite(userId, listingId)
            try { supabaseClient.deleteFavorite(userId, listingId) } catch (_: Exception) {}
        } else {
            val favorite = FavoriteEntity(
                id = "FAV_${userId}_$listingId",
                userId = userId,
                listingId = listingId,
                notifyPriceDrop = true,
                notifySimilar = true,
                timestamp = System.currentTimeMillis()
            )
            db.favoriteDao().insertFavorite(favorite)
            try { supabaseClient.saveFavorite(favorite) } catch (_: Exception) {}
        }
    }

    // Platform Settings & Payments
    fun getPlatformSettings(): Flow<PlatformSettingsEntity?> = db.settingsDao().getSettings()

    suspend fun updatePlatformSettings(settings: PlatformSettingsEntity) {
        db.settingsDao().insertOrUpdateSettings(settings)
        try {
            supabaseClient.savePlatformSettings(settings)
        } catch (_: Exception) {}
    }

    suspend fun syncPlatformSettingsFromCloud() {
        try { supabaseClient.getPlatformSettings().getOrNull()?.let { db.settingsDao().insertOrUpdateSettings(it) } } catch (_: Exception) {}
    }



    fun getAllPayments(): Flow<List<PaymentOrderEntity>> = db.paymentDao().getAllPayments()

    // --- Orders Management ---
    fun getLocalOrdersByBuyer(buyerId: String): Flow<List<OrderEntity>> = db.orderDao().getOrdersByBuyer(buyerId)
    fun getLocalOrdersBySeller(sellerId: String): Flow<List<OrderEntity>> = db.orderDao().getOrdersBySeller(sellerId)
    fun getLocalOrderById(orderId: String): Flow<OrderEntity?> = db.orderDao().getOrderById(orderId)
    suspend fun createOrder(order: OrderEntity): Result<String> { db.orderDao().insertOrder(order); try { supabaseClient.saveOrder(order) } catch (_: Exception) {}; return Result.success(order.id) }
    suspend fun saveOrderLocally(order: OrderEntity) { db.orderDao().insertOrder(order); try { supabaseClient.saveOrder(order) } catch (_: Exception) {} }
    fun getOrderFlow(orderId: String): Flow<Result<OrderEntity?>> = db.orderDao().getOrderById(orderId).map { Result.success(it) }
    fun getUserOrdersFlow(buyerId: String): Flow<Result<List<OrderEntity>>> = db.orderDao().getOrdersByBuyer(buyerId).map { Result.success(it) }
    fun getSellerOrdersFlow(sellerId: String): Flow<Result<List<OrderEntity>>> = db.orderDao().getOrdersBySeller(sellerId).map { Result.success(it) }
    fun getAllOrdersAdminFlow(): Flow<Result<List<OrderEntity>>> = db.orderDao().getAllOrders().map { Result.success(it) }
    suspend fun updateOrderStatus(orderId: String, newStatus: String, statusNote: String = "", trackingNumber: String? = null): Result<Unit> {
        db.orderDao().updateOrderStatus(orderId, newStatus, statusNote, System.currentTimeMillis())
        try { supabaseClient.updateOrderStatus(orderId, newStatus, statusNote, trackingNumber) } catch (_: Exception) {}
        return Result.success(Unit)
    }
    suspend fun syncOrdersLocally(orders: List<OrderEntity>) { if (orders.isNotEmpty()) db.orderDao().insertOrders(orders) }
}
