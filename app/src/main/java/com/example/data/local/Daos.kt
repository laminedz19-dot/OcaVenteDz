package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ListingDao {
    @Query("SELECT * FROM listings WHERE status = 'PUBLISHED' ORDER BY isUrgent DESC, isFeatured DESC, createdAt DESC")
    fun getAllPublishedListings(): Flow<List<ListingEntity>>

    @Query("SELECT * FROM listings ORDER BY createdAt DESC")
    fun getAllListingsForAdmin(): Flow<List<ListingEntity>>

    @Query("SELECT * FROM listings WHERE userId = :userId ORDER BY createdAt DESC")
    fun getUserListings(userId: String): Flow<List<ListingEntity>>

    @Query("SELECT * FROM listings WHERE id = :id")
    fun getListingById(id: String): Flow<ListingEntity?>

    @Query("SELECT * FROM listings WHERE id = :id LIMIT 1")
    suspend fun getListingByIdDirect(id: String): ListingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListing(listing: ListingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListings(listings: List<ListingEntity>)

    @Update
    suspend fun updateListing(listing: ListingEntity)

    @Query("UPDATE listings SET status = :status, rejectionReason = :rejectionReason WHERE id = :id")
    suspend fun updateListingStatus(id: String, status: String, rejectionReason: String = "")

    @Query("UPDATE listings SET isPaid = 1, status = :status WHERE id = :id")
    suspend fun markListingPaid(id: String, status: String)

    @Query("DELETE FROM listings WHERE id = :id")
    suspend fun deleteListing(id: String)

    @Query("DELETE FROM listings WHERE userId = :userId")
    suspend fun deleteListingsForUser(userId: String)

    @Query("UPDATE listings SET status = :status WHERE id = :id AND userId = :userId")
    suspend fun updateStatusForOwner(id: String, userId: String, status: String): Int

    @Query("SELECT COUNT(*) FROM listings")
    suspend fun getCount(): Int
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserById(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserByIdDirect(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE phone = :input OR email = :input LIMIT 1")
    suspend fun getUserByPhoneOrEmail(input: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    suspend fun getAllUsersDirect(): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET isBanned = :isBanned WHERE id = :userId")
    suspend fun updateBanStatus(userId: String, isBanned: Boolean)

    @Query("UPDATE users SET isVerified = :isVerified, verificationRequested = 0 WHERE id = :userId")
    suspend fun updateVerificationStatus(userId: String, isVerified: Boolean)

    @Query("UPDATE users SET verificationRequested = 1 WHERE id = :userId")
    suspend fun requestVerification(userId: String)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: String)
}

@Dao
interface PaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(order: PaymentOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<PaymentOrderEntity>)

    @Query("SELECT * FROM payment_orders ORDER BY createdAt DESC")
    fun getAllPayments(): Flow<List<PaymentOrderEntity>>

    @Query("SELECT * FROM payment_orders WHERE userId = :userId ORDER BY createdAt DESC")
    fun getPaymentsForUser(userId: String): Flow<List<PaymentOrderEntity>>
}

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets WHERE userId = :userId LIMIT 1")
    fun getWallet(userId: String): Flow<WalletEntity?>

    @Query("SELECT * FROM wallets WHERE userId = :userId LIMIT 1")
    suspend fun getWalletDirect(userId: String): WalletEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWallet(wallet: WalletEntity)

    @Query("UPDATE wallets SET balanceDzd = balanceDzd - :amount, updatedAt = :updatedAt WHERE userId = :userId AND balanceDzd >= :amount")
    suspend fun debitIfSufficient(userId: String, amount: Int, updatedAt: Long): Int

    @Query("UPDATE wallets SET balanceDzd = balanceDzd + :amount, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun creditWallet(userId: String, amount: Int, updatedAt: Long): Int

    @Query("DELETE FROM wallets WHERE userId = :userId")
    suspend fun deleteWallet(userId: String)

    @Query("SELECT * FROM wallet_transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getTransactions(userId: String): Flow<List<WalletTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: WalletTransactionEntity)

    @Query("DELETE FROM wallet_transactions WHERE userId = :userId")
    suspend fun deleteTransactions(userId: String)
}

@Dao
interface TopUpRequestDao {
    @Query("SELECT * FROM top_up_requests ORDER BY createdAt DESC")
    fun getAllRequests(): Flow<List<TopUpRequestEntity>>

    @Query("SELECT * FROM top_up_requests ORDER BY createdAt DESC")
    suspend fun getAllRequestsDirect(): List<TopUpRequestEntity>

    @Query("SELECT * FROM top_up_requests WHERE userId = :userId ORDER BY createdAt DESC")
    fun getUserRequests(userId: String): Flow<List<TopUpRequestEntity>>

    @Query("SELECT * FROM top_up_requests WHERE id = :id LIMIT 1")
    suspend fun getRequestById(id: String): TopUpRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: TopUpRequestEntity)

    @Update
    suspend fun updateRequest(request: TopUpRequestEntity)

    @Query("UPDATE top_up_requests SET status = :status, adminNote = :note, reviewedAt = :reviewedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, note: String, reviewedAt: Long)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE listingId = :listingId AND ((senderId = :user1 AND receiverId = :user2) OR (senderId = :user2 AND receiverId = :user1)) ORDER BY timestamp ASC")
    fun getMessages(listingId: String, user1: String, user2: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE senderId = :userId OR receiverId = :userId ORDER BY timestamp DESC")
    fun getUserConversations(userId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("UPDATE chat_messages SET offerStatus = :status WHERE id = :messageId")
    suspend fun updateOfferStatus(messageId: String, status: String)
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE sellerId = :sellerId ORDER BY timestamp DESC")
    fun getReviewsForSeller(sellerId: String): Flow<List<ReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity)

    @Query("SELECT COUNT(*) FROM reviews WHERE sellerId = :sellerId AND buyerId = :buyerId AND listingId = :listingId")
    suspend fun hasReviewed(sellerId: String, buyerId: String, listingId: String): Int
}

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Query("UPDATE reports SET status = :status WHERE id = :id")
    suspend fun updateReportStatus(id: String, status: String)
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites WHERE userId = :userId ORDER BY timestamp DESC")
    fun getFavoritesForUser(userId: String): Flow<List<FavoriteEntity>>

    @Query("SELECT COUNT(*) FROM favorites WHERE userId = :userId AND listingId = :listingId")
    fun isFavorite(userId: String, listingId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(fav: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE userId = :userId AND listingId = :listingId")
    suspend fun deleteFavorite(userId: String, listingId: String)

    @Query("DELETE FROM favorites WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}

@Dao
interface PlatformSettingsDao {
    @Query("SELECT * FROM platform_settings WHERE id = 'global' LIMIT 1")
    fun getSettings(): Flow<PlatformSettingsEntity?>

    @Query("SELECT * FROM platform_settings WHERE id = 'global' LIMIT 1")
    suspend fun getSettingsDirect(): PlatformSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: PlatformSettingsEntity)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE buyerId = :buyerId ORDER BY createdAt DESC")
    fun getOrdersByBuyer(buyerId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE sellerId = :sellerId ORDER BY createdAt DESC")
    fun getOrdersBySeller(sellerId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    fun getOrderById(orderId: String): Flow<OrderEntity?>

    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderByIdDirect(orderId: String): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderEntity>)

    @Query("UPDATE orders SET status = :status, statusNote = :statusNote, updatedAt = :updatedAt WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: String, status: String, statusNote: String, updatedAt: Long)

    @Query("DELETE FROM orders WHERE id = :orderId")
    suspend fun deleteOrder(orderId: String)
}
