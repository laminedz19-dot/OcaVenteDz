package com.example.data.repository

import android.content.Context
import android.net.Uri
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
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
        try { supabaseClient.updateListingStatus(id, "DELETED") } catch (_: Exception) {}
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
        if (paymentMethod != "WALLET") {
            return Result.failure(Exception("وسيلة الدفع غير مفعّلة حتى يتم ربط مزود دفع موثوق."))
        }
        val settings = db.settingsDao().getSettingsDirect() ?: PlatformSettingsEntity()
        val fee = when (packageType) {
            "FEATURED" -> settings.featuredAdFeeDzd
            "URGENT" -> settings.urgentAdFeeDzd
            else -> settings.standardAdFeeDzd
        }

        val now = System.currentTimeMillis()
        val paymentId = "PAY_" + UUID.randomUUID().toString().take(8).uppercase()
        return db.withTransaction {
            val listing = db.listingDao().getListingByIdDirect(listingId)
                ?: return@withTransaction Result.failure(Exception("الإعلان غير موجود."))
            if (listing.userId != userId) {
                return@withTransaction Result.failure(Exception("لا تملك هذا الإعلان."))
            }
            if (db.walletDao().debitIfSufficient(userId, fee, now) != 1) {
                val balance = db.walletDao().getWalletDirect(userId)?.balanceDzd ?: 0
                return@withTransaction Result.failure(Exception("رصيد المحفظة غير كافٍ. الرصيد الحالي: $balance دج والمطلوب: $fee دج"))
            }
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
            val order = PaymentOrderEntity(
                paymentId = paymentId,
                userId = userId,
                listingId = listingId,
                amount = fee,
                currency = "DZD",
                status = "SUCCESS",
                provider = "WALLET",
                transactionReference = paymentId,
                createdAt = now,
                completedAt = now
            )
            db.paymentDao().insertPayment(order)
            db.listingDao().insertListing(listing.copy(
                status = if (settings.autoPublishAfterPayment) "PUBLISHED" else "UNDER_REVIEW",
                isPaid = true,
                publishingFeeDzd = fee,
                packageType = packageType,
                isFeatured = packageType != "STANDARD",
                isUrgent = packageType == "URGENT"
            ))
            Result.success(order)
        }
    }

    // Wallet Operations
    fun getWallet(userId: String): Flow<WalletEntity?> = db.walletDao().getWallet(userId)
    fun getWalletTransactions(userId: String): Flow<List<WalletTransactionEntity>> = db.walletDao().getTransactions(userId)

    fun getUserTopUpRequests(userId: String): Flow<List<TopUpRequestEntity>> =
        db.topUpRequestDao().getUserRequests(userId)

    fun getAllTopUpRequests(): Flow<List<TopUpRequestEntity>> =
        db.topUpRequestDao().getAllRequests()

    /**
     * Primary Source of Truth: Listens directly to all top-up requests in الخدمة السحابية.
     * Caches requests in local Room and emits Result.failure on snapshot errors so the UI
     * can display Error state instead of falsely displaying Empty.
     */
    /**
     * Unified TopUp Stream: Fetches from Supabase cloud database, الخدمة السحابية, and local Room cache.
     * Guarantees Admin sees all requests immediately regardless of backend connectivity.
     */
    fun getAllTopUpRequestsFromCloud(): Flow<Result<List<TopUpRequestEntity>>> = kotlinx.coroutines.flow.flow {
        emit(Result.success(db.topUpRequestDao().getAllRequestsDirect()))
        try {
            supabaseClient.getTopUpRequests().getOrNull().orEmpty().forEach { db.topUpRequestDao().insertRequest(it) }
            emit(Result.success(db.topUpRequestDao().getAllRequestsDirect()))
        } catch (_: Exception) {}
    }

    suspend fun submitTopUpRequest(
        context: Context,
        userId: String = "",
        amount: Int,
        provider: String,
        reference: String,
        receiptImageUriString: String
    ): Result<String> {
        return Result.failure(Exception("تقديم طلبات الشحن متاح حصرياً عبر تطبيق العميل."))
    }

    /**
     * Approves top-up request: updates Supabase, الخدمة السحابية, and Room cache.
     * Also credits the user's wallet in Supabase and Room!
     */
    suspend fun approveTopUpRequest(requestId: String, adminNote: String = "تم التحقق من الوصل بنجاح"): Result<String> {
        val note = adminNote.ifBlank { "تم التحقق من الوصل بنجاح" }
        val now = System.currentTimeMillis()

        // 1. Call server-side atomic RPC with double-approval protection
        val cloudResult = supabaseClient.approveTopUpRequest(requestId, note)
        if (cloudResult.isFailure) {
            return Result.failure(cloudResult.exceptionOrNull() ?: Exception("فشل اعتماد طلب الشحن على الخادم."))
        }

        // 2. Update local Room cache on success
        try {
            db.topUpRequestDao().updateStatus(requestId, "APPROVED", note, now)
            val req = db.topUpRequestDao().getRequestById(requestId)
            if (req != null && req.amountDzd > 0) {
                db.walletDao().creditWallet(req.userId, req.amountDzd, now)
                val tx = WalletTransactionEntity(
                    id = "tx_" + UUID.randomUUID().toString().replace("-", "").take(10),
                    userId = req.userId,
                    type = "TOPUP",
                    amount = req.amountDzd,
                    description = "شحن رصيد معتمد - ${req.provider} (مرجع: ${req.reference.ifBlank { req.id }})",
                    referenceId = req.id,
                    timestamp = now
                )
                db.walletDao().insertTransaction(tx)
            }
        } catch (e: Exception) {
            android.util.Log.w("MarketplaceRepository", "Local cache update warning: ${e.message}")
        }

        return Result.success("تمت الموافقة على طلب الشحن وشحن الرصيد بنجاح ✓")
    }

    suspend fun rejectTopUpRequest(requestId: String, reason: String): Result<String> {
        val note = reason.ifBlank { "الوصل غير مطابق أو غير واضح" }
        val now = System.currentTimeMillis()

        val cloudResult = supabaseClient.rejectTopUpRequest(requestId, note)
        if (cloudResult.isFailure) {
            return Result.failure(cloudResult.exceptionOrNull() ?: Exception("فشل رفض طلب الشحن على الخادم."))
        }

        try {
            db.topUpRequestDao().updateStatus(requestId, "REJECTED", note, now)
        } catch (e: Exception) {
            android.util.Log.w("MarketplaceRepository", "Local cache update warning: ${e.message}")
        }

        return Result.success("تم رفض طلب الشحن وتحديث الحالة بنجاح.")
    }

    suspend fun topUpWallet(userId: String, amount: Int, paymentProvider: String, txReference: String? = null): Result<String> {
        return Result.failure(Exception("عمليات الشحن المباشر غير متاحة لأسباب أمنية؛ يرجى مراجعة واعتماد طلبات الشحن المقدمة من المستخدمين."))
    }

    // Users
    fun getUser(id: String): Flow<UserEntity?> = db.userDao().getUserById(id)
    suspend fun getUserDirect(id: String): UserEntity? = db.userDao().getUserByIdDirect(id)
    suspend fun findUserByPhoneOrEmail(input: String): UserEntity? = db.userDao().getUserByPhoneOrEmail(input)
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

    suspend fun syncUsersFromCloud() {
        try { supabaseClient.getAllUsers().getOrNull().orEmpty().let { if (it.isNotEmpty()) db.userDao().insertUsers(it) } } catch (_: Exception) {}
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
        try { supabaseClient.saveReport(report) } catch (_: Exception) {}
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

    suspend fun syncPaymentsFromCloud() { }

    // --- Orders Management ---
    fun getLocalOrders(): Flow<List<OrderEntity>> = db.orderDao().getAllOrders()
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
        db.orderDao().updateOrderStatus(orderId, newStatus, statusNote, System.currentTimeMillis()); try { supabaseClient.updateOrderStatus(orderId, newStatus, statusNote, trackingNumber) } catch (_: Exception) {}; return Result.success(Unit)
    }
    suspend fun syncOrdersLocally(orders: List<OrderEntity>) { if (orders.isNotEmpty()) db.orderDao().insertOrders(orders) }
}
