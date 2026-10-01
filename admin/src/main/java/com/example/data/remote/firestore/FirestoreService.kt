package com.example.data.remote.firestore

import android.util.Log
import com.example.data.local.ListingEntity
import com.example.data.local.PlatformSettingsEntity
import com.example.data.local.UserEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class FirestoreService(
    customFirestore: FirebaseFirestore? = null
) {
    companion object {
        private const val TAG = "FirestoreService"
        const val COLLECTION_USERS = "users"
        const val COLLECTION_LISTINGS = "listings"
        const val COLLECTION_PAYMENTS = "payments"
        const val COLLECTION_SETTINGS = "settings"
        const val COLLECTION_TOP_UP_REQUESTS = "topUpRequests"
        const val COLLECTION_WALLETS = "wallets"
        const val COLLECTION_ORDERS = "orders"
    }

    private val firestore: FirebaseFirestore? = customFirestore ?: run {
        try {
            FirebaseApp.getInstance()
            FirebaseFirestore.getInstance()
        } catch (t: Throwable) {
            Log.w(TAG, "Firebase is not configured or initialized (${t.message}). OcaVente DZ is operating seamlessly in local offline/Room database mode.")
            null
        }
    }

    // --- USERS COLLECTION ---

    suspend fun saveUser(user: UserEntity): Result<Unit> {
        val db = firestore ?: return Result.success(Unit)
        return try {
            val firestoreUser = FirestoreUser.fromUserEntity(user)
            db.collection(COLLECTION_USERS)
                .document(user.id)
                .set(firestoreUser)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user to Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getUser(userId: String): Result<FirestoreUser?> {
        val db = firestore ?: return Result.success(null)
        return try {
            val snapshot = db.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .await()
            val user = try { snapshot.toObject(FirestoreUser::class.java) } catch (_: Exception) { null }
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user from Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getAllUsers(): Result<List<FirestoreUser>> {
        val db = firestore ?: return Result.success(emptyList())
        return try {
            val snapshot = db.collection(COLLECTION_USERS).get().await()
            val users = snapshot.documents.mapNotNull { doc ->
                try { doc.toObject(FirestoreUser::class.java) } catch (_: Exception) { null }
            }
            Result.success(users)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all users from Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateUserVerification(userId: String, isVerified: Boolean): Result<Unit> {
        val db = firestore ?: return Result.success(Unit)
        return try {
            db.collection(COLLECTION_USERS).document(userId)
                .update("isVerified", isVerified)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user verification in Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateUserBanStatus(userId: String, isBanned: Boolean): Result<Unit> {
        val db = firestore ?: return Result.success(Unit)
        return try {
            db.collection(COLLECTION_USERS).document(userId)
                .update("isBanned", isBanned)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user ban status in Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    // --- LISTINGS COLLECTION ---

    suspend fun saveListing(listing: ListingEntity): Result<Unit> {
        val db = firestore ?: return Result.success(Unit)
        return try {
            val firestoreListing = FirestoreListing.fromListingEntity(listing)
            db.collection(COLLECTION_LISTINGS)
                .document(listing.id)
                .set(firestoreListing)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving listing to Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateListingStatus(listingId: String, status: String, rejectionReason: String = ""): Result<Unit> {
        val db = firestore ?: return Result.success(Unit)
        return try {
            val updates = mapOf(
                "status" to status,
                "rejectionReason" to rejectionReason
            )
            db.collection(COLLECTION_LISTINGS)
                .document(listingId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating listing status in Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getPublishedListings(): Result<List<FirestoreListing>> {
        val db = firestore ?: return Result.success(emptyList())
        return try {
            val snapshot = db.collection(COLLECTION_LISTINGS)
                .whereEqualTo("status", "PUBLISHED")
                .get()
                .await()
            val listings = snapshot.documents.mapNotNull { doc ->
                try { doc.toObject(FirestoreListing::class.java) } catch (_: Exception) { null }
            }
            Result.success(listings)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching published listings: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun getPublishedListingsFlow(): Flow<List<FirestoreListing>> {
        val db = firestore ?: return emptyFlow()
        return callbackFlow {
            val listenerRegistration = db.collection(COLLECTION_LISTINGS)
                .whereEqualTo("status", "PUBLISHED")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Listen failed: ${error.message}", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val listings = snapshot.documents.mapNotNull { doc ->
                            try { doc.toObject(FirestoreListing::class.java) } catch (_: Exception) { null }
                        }
                        trySend(listings)
                    }
                }
            awaitClose { listenerRegistration.remove() }
        }
    }

    suspend fun getAllListingsForAdmin(): Result<List<FirestoreListing>> {
        val db = firestore ?: return Result.success(emptyList())
        return try {
            val snapshot = db.collection(COLLECTION_LISTINGS).get().await()
            val listings = snapshot.documents.mapNotNull { doc ->
                try { doc.toObject(FirestoreListing::class.java) } catch (_: Exception) { null }
            }
            Result.success(listings)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all admin listings: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun getAllListingsForAdminFlow(): Flow<List<FirestoreListing>> {
        val db = firestore ?: return emptyFlow()
        return callbackFlow {
            val listenerRegistration = db.collection(COLLECTION_LISTINGS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Admin listings listen failed: ${error.message}", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val listings = snapshot.documents.mapNotNull { doc ->
                            try { doc.toObject(FirestoreListing::class.java) } catch (_: Exception) { null }
                        }
                        trySend(listings)
                    }
                }
            awaitClose { listenerRegistration.remove() }
        }
    }

    suspend fun deleteListing(listingId: String): Result<Unit> {
        val db = firestore ?: return Result.success(Unit)
        return try {
            db.collection(COLLECTION_LISTINGS)
                .document(listingId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting listing from Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    // --- PAYMENTS COLLECTION ---

    suspend fun recordPayment(payment: FirestorePayment): Result<Unit> {
        val db = firestore ?: return Result.success(Unit)
        return try {
            db.collection(COLLECTION_PAYMENTS)
                .document(payment.paymentId)
                .set(payment)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error recording payment in Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getAllPayments(): Result<List<FirestorePayment>> {
        val db = firestore ?: return Result.success(emptyList())
        return try {
            val snapshot = db.collection(COLLECTION_PAYMENTS).get().await()
            val payments = snapshot.documents.mapNotNull { doc ->
                try { doc.toObject(FirestorePayment::class.java) } catch (_: Exception) { null }
            }
            Result.success(payments)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all payments from Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    // --- TOP UP REQUESTS COLLECTION ---

    suspend fun submitTopUpRequest(request: FirestoreTopUpRequest): Result<Unit> {
        val db = firestore ?: return Result.failure(IllegalStateException("خدمة Firestore غير متصلة أو غير مهيأة"))
        return try {
            withTimeout(15000L) {
                val data = hashMapOf(
                    "id" to request.id,
                    "userId" to request.userId,
                    "userName" to request.userName,
                    "userPhone" to request.userPhone,
                    "amountDzd" to request.amountDzd,
                    "provider" to request.provider,
                    "reference" to request.reference,
                    "receiptImageUri" to request.receiptImageUri,
                    "status" to request.status,
                    "adminNote" to request.adminNote,
                    "createdAt" to (request.createdAt ?: FieldValue.serverTimestamp()),
                    "reviewedAt" to request.reviewedAt
                )
                db.collection(COLLECTION_TOP_UP_REQUESTS)
                    .document(request.id)
                    .set(data)
                    .await()
            }
            Log.i(TAG, "Successfully submitted top-up request: ${request.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting top-up request to Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun getUserTopUpRequestsFlow(userId: String): Flow<Result<List<FirestoreTopUpRequest>>> {
        val db = firestore ?: return kotlinx.coroutines.flow.flowOf(Result.failure(IllegalStateException("خدمة Firestore غير مهيأة")))
        return callbackFlow {
            val listener = db.collection(COLLECTION_TOP_UP_REQUESTS)
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Snapshot listener error for user $userId top-up requests: ${error.message}", error)
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try { doc.toObject(FirestoreTopUpRequest::class.java) } catch (_: Exception) { null }
                        }.sortedByDescending { it.createdAt?.time ?: 0L }
                        trySend(Result.success(list))
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    fun getAllTopUpRequestsFlow(): Flow<Result<List<FirestoreTopUpRequest>>> {
        val db = firestore ?: return kotlinx.coroutines.flow.flowOf(Result.failure(IllegalStateException("خدمة Firestore غير مهيأة")))
        return callbackFlow {
            val listener = db.collection(COLLECTION_TOP_UP_REQUESTS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Snapshot listener error for all top-up requests: ${error.message}", error)
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try { doc.toObject(FirestoreTopUpRequest::class.java) } catch (_: Exception) { null }
                        }.sortedByDescending { it.createdAt?.time ?: 0L }
                        trySend(Result.success(list))
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    suspend fun updateTopUpStatus(requestId: String, newStatus: String, adminNote: String): Result<Unit> {
        val db = firestore ?: return Result.failure(IllegalStateException("خدمة Firestore غير متصلة أو غير مهيأة"))
        return try {
            db.runTransaction { transaction ->
                val docRef = db.collection(COLLECTION_TOP_UP_REQUESTS).document(requestId)
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) {
                    throw IllegalStateException("طلب شحن الرصيد غير موجود في قاعدة البيانات")
                }
                val currentStatus = snapshot.getString("status")
                if (currentStatus != "PENDING") {
                    throw IllegalStateException("لا يمكن تعديل الطلب لأنه تمت معالجته مسبقاً (الحالة الحالية: $currentStatus)")
                }
                val updates = mapOf(
                    "status" to newStatus,
                    "adminNote" to adminNote,
                    "reviewedAt" to FieldValue.serverTimestamp()
                )
                transaction.update(docRef, updates)

                // If APPROVED, also credit the user's wallet in Firestore atomically
                if (newStatus == "APPROVED") {
                    val userId = snapshot.getString("userId").orEmpty()
                    val amountDzd = snapshot.getLong("amountDzd")?.toInt() ?: 0
                    if (userId.isNotBlank() && amountDzd > 0) {
                        val walletRef = db.collection(COLLECTION_WALLETS).document(userId)
                        val walletSnapshot = transaction.get(walletRef)
                        val currentBalance = if (walletSnapshot.exists()) walletSnapshot.getLong("balanceDzd")?.toInt() ?: 0 else 0
                        val newBalance = currentBalance + amountDzd
                        val walletData = mapOf(
                            "userId" to userId,
                            "balanceDzd" to newBalance,
                            "pendingBalanceDzd" to 0,
                            "currency" to "DZD",
                            "isActive" to true,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        transaction.set(walletRef, walletData, com.google.firebase.firestore.SetOptions.merge())
                    }
                }
            }.await()
            Log.i(TAG, "Successfully updated top-up request $requestId to $newStatus")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating top-up request $requestId status: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Reads the specified user's balance in DZD directly from Firestore.
     */
    suspend fun getCurrentUserBalance(userId: String): Result<Int> {
        val db = firestore ?: return Result.failure(IllegalStateException("خدمة Firestore غير متصلة"))
        if (userId.isBlank()) return Result.success(0)
        return try {
            val doc = db.collection(COLLECTION_WALLETS).document(userId).get().await()
            if (doc.exists()) {
                val balance = doc.getLong("balanceDzd")?.toInt() ?: 0
                Result.success(balance)
            } else {
                Result.success(0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading user balance for $userId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Retrieves the user's wallet document from Firestore.
     */
    suspend fun getUserWallet(userId: String): Result<FirestoreWallet?> {
        val db = firestore ?: return Result.failure(IllegalStateException("خدمة Firestore غير متصلة"))
        if (userId.isBlank()) return Result.success(null)
        return try {
            val doc = db.collection(COLLECTION_WALLETS).document(userId).get().await()
            if (doc.exists()) {
                val wallet = doc.toObject(FirestoreWallet::class.java)
                Result.success(wallet)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving wallet for $userId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Realtime flow for observing the user's wallet balance from Firestore.
     */
    fun getUserWalletFlow(userId: String): Flow<Result<FirestoreWallet?>> {
        val db = firestore ?: return kotlinx.coroutines.flow.flowOf(Result.failure(IllegalStateException("خدمة Firestore غير مهيأة")))
        return callbackFlow {
            val listener = db.collection(COLLECTION_WALLETS).document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val wallet = snapshot.toObject(FirestoreWallet::class.java)
                        trySend(Result.success(wallet))
                    } else {
                        trySend(Result.success(null))
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    /**
     * Saves or updates a user's wallet in Firestore.
     */
    suspend fun saveUserWallet(wallet: FirestoreWallet): Result<Unit> {
        val db = firestore ?: return Result.failure(IllegalStateException("خدمة Firestore غير متصلة"))
        return try {
            db.collection(COLLECTION_WALLETS).document(wallet.userId).set(wallet).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user wallet: ${e.message}", e)
            Result.failure(e)
        }
    }

    // --- SETTINGS COLLECTION ---

    suspend fun savePlatformSettings(settings: PlatformSettingsEntity): Result<Unit> {
        val db = firestore ?: return Result.success(Unit)
        return try {
            val firestoreSettings = FirestoreSettings.fromPlatformSettingsEntity(settings)
            db.collection(COLLECTION_SETTINGS)
                .document(firestoreSettings.id)
                .set(firestoreSettings)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving platform settings to Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getPlatformSettings(): Result<PlatformSettingsEntity?> {
        val db = firestore ?: return Result.success(null)
        return try {
            val doc = db.collection(COLLECTION_SETTINGS).document("global").get().await()
            val settings = doc.toObject(FirestoreSettings::class.java)?.toPlatformSettingsEntity()
            Result.success(settings)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching platform settings from Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun getPlatformSettingsFlow(): Flow<Result<PlatformSettingsEntity?>> {
        val db = firestore ?: return emptyFlow()
        return callbackFlow {
            val listener = db.collection(COLLECTION_SETTINGS).document("global")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val settings = try {
                            snapshot.toObject(FirestoreSettings::class.java)?.toPlatformSettingsEntity()
                        } catch (_: Exception) { null }
                        trySend(Result.success(settings))
                    } else {
                        trySend(Result.success(null))
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    // --- ORDERS COLLECTION REFERENCES & METHODS ---

    /**
     * Direct Firestore CollectionReference for the "orders" collection.
     */
    fun getOrdersCollection(): CollectionReference? = firestore?.collection(COLLECTION_ORDERS)

    /**
     * Direct Firestore DocumentReference for a specific order.
     */
    fun getOrderDocument(orderId: String): DocumentReference? = getOrdersCollection()?.document(orderId)

    /**
     * Creates or places a new order in Firestore.
     */
    suspend fun createOrder(order: FirestoreOrder): Result<String> {
        val db = firestore ?: return Result.failure(IllegalStateException("خدمة Firestore غير متصلة"))
        return try {
            val orderId = if (order.id.isNotBlank()) order.id else db.collection(COLLECTION_ORDERS).document().id
            val orderWithId = order.copy(id = orderId)
            db.collection(COLLECTION_ORDERS)
                .document(orderId)
                .set(orderWithId)
                .await()
            Log.i(TAG, "Order created successfully with ID: $orderId")
            Result.success(orderId)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating order in Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Saves or overwrites an order in Firestore.
     */
    suspend fun saveOrder(order: FirestoreOrder): Result<Unit> {
        val db = firestore ?: return Result.failure(IllegalStateException("خدمة Firestore غير متصلة"))
        return try {
            db.collection(COLLECTION_ORDERS)
                .document(order.id)
                .set(order)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving order in Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Retrieves an order by its ID from Firestore.
     */
    suspend fun getOrder(orderId: String): Result<FirestoreOrder?> {
        val db = firestore ?: return Result.success(null)
        if (orderId.isBlank()) return Result.success(null)
        return try {
            val doc = db.collection(COLLECTION_ORDERS).document(orderId).get().await()
            if (doc.exists()) {
                val order = try { doc.toObject(FirestoreOrder::class.java) } catch (_: Exception) { null }
                Result.success(order)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving order $orderId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Real-time Flow observing a specific order's status and details.
     */
    fun getOrderFlow(orderId: String): Flow<Result<FirestoreOrder?>> {
        val db = firestore ?: return emptyFlow()
        if (orderId.isBlank()) return emptyFlow()
        return callbackFlow {
            val listener = db.collection(COLLECTION_ORDERS).document(orderId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to order $orderId: ${error.message}", error)
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val order = try { snapshot.toObject(FirestoreOrder::class.java) } catch (_: Exception) { null }
                        trySend(Result.success(order))
                    } else {
                        trySend(Result.success(null))
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    /**
     * Real-time Flow retrieving all orders for a buyer (user's purchases), sorted by date.
     */
    fun getUserOrdersFlow(buyerId: String): Flow<Result<List<FirestoreOrder>>> {
        val db = firestore ?: return kotlinx.coroutines.flow.flowOf(Result.failure(IllegalStateException("خدمة Firestore غير مهيأة")))
        if (buyerId.isBlank()) return kotlinx.coroutines.flow.flowOf(Result.success(emptyList()))
        return callbackFlow {
            val listener = db.collection(COLLECTION_ORDERS)
                .whereEqualTo("buyerId", buyerId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to user $buyerId orders: ${error.message}", error)
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val orders = snapshot.documents.mapNotNull { doc ->
                            try { doc.toObject(FirestoreOrder::class.java) } catch (_: Exception) { null }
                        }.sortedByDescending { it.createdAt?.time ?: 0L }
                        trySend(Result.success(orders))
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    /**
     * Real-time Flow retrieving all orders received by a seller, sorted by date.
     */
    fun getSellerOrdersFlow(sellerId: String): Flow<Result<List<FirestoreOrder>>> {
        val db = firestore ?: return kotlinx.coroutines.flow.flowOf(Result.failure(IllegalStateException("خدمة Firestore غير مهيأة")))
        if (sellerId.isBlank()) return kotlinx.coroutines.flow.flowOf(Result.success(emptyList()))
        return callbackFlow {
            val listener = db.collection(COLLECTION_ORDERS)
                .whereEqualTo("sellerId", sellerId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to seller $sellerId orders: ${error.message}", error)
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val orders = snapshot.documents.mapNotNull { doc ->
                            try { doc.toObject(FirestoreOrder::class.java) } catch (_: Exception) { null }
                        }.sortedByDescending { it.createdAt?.time ?: 0L }
                        trySend(Result.success(orders))
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    /**
     * Real-time Flow retrieving all orders across the platform for admin monitoring.
     */
    fun getAllOrdersAdminFlow(): Flow<Result<List<FirestoreOrder>>> {
        val db = firestore ?: return kotlinx.coroutines.flow.flowOf(Result.failure(IllegalStateException("خدمة Firestore غير مهيأة")))
        return callbackFlow {
            val listener = db.collection(COLLECTION_ORDERS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to all orders: ${error.message}", error)
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val orders = snapshot.documents.mapNotNull { doc ->
                            try { doc.toObject(FirestoreOrder::class.java) } catch (_: Exception) { null }
                        }.sortedByDescending { it.createdAt?.time ?: 0L }
                        trySend(Result.success(orders))
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    /**
     * Updates an order's status and tracking information in Firestore.
     */
    suspend fun updateOrderStatus(
        orderId: String,
        newStatus: String,
        statusNote: String = "",
        trackingNumber: String? = null
    ): Result<Unit> {
        val db = firestore ?: return Result.failure(IllegalStateException("خدمة Firestore غير متصلة"))
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to newStatus,
                "statusNote" to statusNote,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!trackingNumber.isNullOrBlank()) {
                updates["trackingNumber"] = trackingNumber
            }
            if (newStatus == OrderStatus.DELIVERED) {
                updates["deliveredAt"] = System.currentTimeMillis()
                updates["isPaid"] = true
            } else if (newStatus == OrderStatus.CANCELLED || newStatus == OrderStatus.REJECTED) {
                updates["cancelledAt"] = System.currentTimeMillis()
            }
            db.collection(COLLECTION_ORDERS)
                .document(orderId)
                .update(updates)
                .await()
            Log.i(TAG, "Updated order $orderId status to: $newStatus")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating order status in Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }
}
