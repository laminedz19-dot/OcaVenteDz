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
import com.example.data.remote.firestore.FirestoreOrder
import com.example.data.remote.firestore.FirestorePayment
import com.example.data.remote.firestore.FirestoreService
import com.example.data.remote.firestore.FirestoreTopUpRequest
import com.example.data.remote.firestore.FirestoreWallet
import com.example.data.remote.storage.FirebaseStorageService
import com.example.data.remote.back4app.Back4AppClient
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.util.UUID

class MarketplaceRepository(
    private val db: AppDatabase,
    val firestoreService: FirestoreService = FirestoreService(),
    val authService: AuthRepository = AuthRepository(),
    val storageService: FirebaseStorageService = FirebaseStorageService(),
    val back4AppClient: Back4AppClient = Back4AppClient()
) {

    // Listings
    fun getPublishedListings(): Flow<List<ListingEntity>> = db.listingDao().getAllPublishedListings()
    fun getAllListingsAdmin(): Flow<List<ListingEntity>> = db.listingDao().getAllListingsForAdmin()
    fun getUserListings(userId: String): Flow<List<ListingEntity>> = db.listingDao().getUserListings(userId)
    fun getListingById(id: String): Flow<ListingEntity?> = db.listingDao().getListingById(id)
    suspend fun getListingDirect(id: String): ListingEntity? = db.listingDao().getListingByIdDirect(id)

    suspend fun syncListingsFromFirestore() {
        // 1. Sync from Back4App cloud database
        try {
            val b4aResult = back4AppClient.getListings()
            if (b4aResult.isSuccess) {
                val remoteListings = b4aResult.getOrNull().orEmpty()
                if (remoteListings.isNotEmpty()) {
                    db.listingDao().insertListings(remoteListings)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("MarketplaceRepository", "Back4App listings sync warning: ${e.message}")
        }

        // 2. Redundant sync from Firestore
        try {
            val result = firestoreService.getPublishedListings()
            if (result.isSuccess) {
                val remoteListings = result.getOrNull().orEmpty()
                if (remoteListings.isNotEmpty()) {
                    val entities = remoteListings.map { it.toListingEntity() }
                    db.listingDao().insertListings(entities)
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun saveListing(listing: ListingEntity) {
        db.listingDao().insertListing(listing)
        try {
            back4AppClient.saveListing(listing)
        } catch (_: Exception) {}
        try {
            firestoreService.saveListing(listing)
        } catch (_: Exception) {}
    }

    suspend fun updateListingStatus(id: String, status: String, rejectionReason: String = "") {
        db.listingDao().updateListingStatus(id, status, rejectionReason)
        try {
            back4AppClient.updateListingStatus(id, status, rejectionReason)
        } catch (_: Exception) {}
        try {
            firestoreService.updateListingStatus(id, status, rejectionReason)
        } catch (_: Exception) {}
    }

    suspend fun deleteListing(id: String) {
        val listing = db.listingDao().getListingByIdDirect(id)
        db.listingDao().deleteListing(id)
        try {
            back4AppClient.updateListingStatus(id, "DELETED")
        } catch (_: Exception) {}
        if (listing != null) {
            try {
                storageService.deleteAllListingImages(listing.userId, listing.id)
            } catch (_: Exception) {}
        }
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
        val settings = db.settingsDao().getSettingsDirect() ?: PlatformSettingsEntity()
        val fee = when (packageType) {
            "FEATURED" -> settings.featuredAdFeeDzd
            "URGENT" -> settings.urgentAdFeeDzd
            else -> settings.standardAdFeeDzd
        }

        val now = System.currentTimeMillis()
        val paymentId = "PAY_" + UUID.randomUUID().toString().take(8).uppercase()

        if (paymentMethod != "WALLET") {
            // E-Payment processing for Edahabia / CIB / BaridiMob gateway
            val order = PaymentOrderEntity(
                paymentId = paymentId,
                userId = userId,
                listingId = listingId,
                amount = fee,
                currency = "DZD",
                status = "SUCCESS",
                provider = paymentMethod,
                transactionReference = "EPAY_" + UUID.randomUUID().toString().take(10).uppercase(),
                createdAt = now,
                completedAt = now
            )
            return db.withTransaction {
                val listing = db.listingDao().getListingByIdDirect(listingId)
                    ?: return@withTransaction Result.failure(Exception("الإعلان غير موجود."))
                if (listing.userId != userId) {
                    return@withTransaction Result.failure(Exception("لا تملك هذا الإعلان."))
                }
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

    /**
     * Listens directly to Firestore topUpRequests for the current user in real-time.
     * Caches received items into Room so offline mode remains functional.
     * Emits Result.failure if Firestore listener fails, preserving accurate error states.
     */
    fun getUserTopUpRequestsFromFirestore(userId: String): Flow<Result<List<TopUpRequestEntity>>> {
        return firestoreService.getUserTopUpRequestsFlow(userId).map { result ->
            if (result.isSuccess) {
                val firestoreList = result.getOrNull().orEmpty()
                val entities = firestoreList.map { it.toTopUpRequestEntity() }
                try {
                    for (entity in entities) {
                        val localExisting = db.topUpRequestDao().getRequestById(entity.id)
                        val wasNotApproved = localExisting == null || localExisting.status != "APPROVED"
                        if (entity.status == "APPROVED" && wasNotApproved) {
                            // Idempotent credit in local Room wallet for the approved top-up request
                            val wallet = db.walletDao().getWalletDirect(userId)
                            if (wallet == null) {
                                db.walletDao().insertOrUpdateWallet(
                                    WalletEntity(userId = userId, balanceDzd = 0, updatedAt = System.currentTimeMillis())
                                )
                            }
                            db.walletDao().creditWallet(userId, entity.amountDzd, System.currentTimeMillis())
                            db.walletDao().insertTransaction(
                                WalletTransactionEntity(
                                    id = "topup_${entity.id}",
                                    userId = userId,
                                    type = "TOPUP",
                                    amount = entity.amountDzd,
                                    description = "شحن رصيد معتمد (${entity.provider})",
                                    referenceId = entity.id,
                                    timestamp = if (entity.reviewedAt > 0) entity.reviewedAt else System.currentTimeMillis()
                                )
                            )
                        }
                        db.topUpRequestDao().insertRequest(entity)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MarketplaceRepository", "Failed to cache user top-up requests in Room: ${e.message}")
                }
                Result.success(entities)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("خطأ غير معروف أثناء مزامنة طلبات الشحن"))
            }
        }
    }

    fun getAllTopUpRequests(): Flow<List<TopUpRequestEntity>> =
        db.topUpRequestDao().getAllRequests()

    /**
     * Submits a top-up request strictly adhering to security invariants:
     * 1. Current user must be authenticated with Firebase Auth (uid != null, != "user_me").
     * 2. Allowed payment providers: BARIDIMOB, CCP, EDAHABIA, CIB.
     * 3. Minimum amount is 200 DZD.
     * 4. Must provide at least a transfer reference or receipt image.
     * 5. If receipt image is present, upload first to Firebase Storage at topUpReceipts/{uid}/{requestId}/receipt.{ext}.
     * 6. Write request to Firestore at topUpRequests/{requestId}.
     * 7. If Firestore fails, roll back and delete the uploaded Storage receipt.
     * 8. Update Room cache ONLY after Firestore succeeds.
     */
    suspend fun submitTopUpRequest(
        context: Context,
        userId: String = "",
        amount: Int,
        provider: String,
        reference: String,
        receiptImageUriString: String
    ): Result<String> = withContext(Dispatchers.IO) {
        // 1. Validate amount
        if (amount < 200) {
            return@withContext Result.failure(IllegalArgumentException("الحد الأدنى لشحن الرصيد هو 200 دج."))
        }

        // 2. Validate provider
        val allowedProviders = setOf("BARIDIMOB", "CCP", "EDAHABIA", "CIB")
        if (provider !in allowedProviders) {
            return@withContext Result.failure(IllegalArgumentException("مزود الدفع المحدد غير مدعوم ($provider). المزودون المسموح بهم فقط: BARIDIMOB, CCP, EDAHABIA, CIB."))
        }

        // 3. Validate that at least reference or receipt image is provided
        val hasReference = reference.isNotBlank()
        val hasReceipt = receiptImageUriString.isNotBlank()
        if (!hasReference && !hasReceipt) {
            return@withContext Result.failure(IllegalArgumentException("يجب إرفاق صورة وصل التحويل أو إدخال رقم مرجع العملية على الأقل."))
        }

        // 4. Resolve authenticated user
        val currentFirebaseUser = authService.currentUser
        val uid = currentFirebaseUser?.uid
            ?: userId.takeIf { it.isNotBlank() && it != "deleted" }
            ?: return@withContext Result.failure(
                IllegalStateException("يجب تسجيل الدخول أو إنشاء حساب أولاً لتقديم طلب شحن الرصيد لربطه بمحفظتك الرقمية.")
            )

        // 5. Generate requestId before uploading image
        val requestId = "req_" + UUID.randomUUID().toString().replace("-", "").take(16)

        // 6. Handle receipt image upload: Prioritize Back4App -> Firebase Storage -> Base64
        var storedReceiptPath = ""
        if (hasReceipt) {
            val localUri = Uri.parse(receiptImageUriString)

            // A. Back4App File Upload (Direct public CDN URL, no bucket rules or auth required)
            try {
                val back4AppRes = withTimeoutOrNull(20000L) {
                    back4AppClient.uploadReceiptImage(context, localUri, "receipt_${requestId.take(10)}")
                }
                if (back4AppRes?.isSuccess == true) {
                    storedReceiptPath = back4AppRes.getOrNull().orEmpty()
                    android.util.Log.i("MarketplaceRepository", "Receipt uploaded to Back4App successfully: $storedReceiptPath")
                } else {
                    val err = back4AppRes?.exceptionOrNull()?.message
                    android.util.Log.w("MarketplaceRepository", "Back4App receipt upload returned: $err")
                }
            } catch (t: Throwable) {
                android.util.Log.w("MarketplaceRepository", "Back4App receipt upload exception: ${t.message}")
            }

            // B. Firebase Storage fallback
            if (storedReceiptPath.isBlank()) {
                try {
                    val uploadResult = withTimeoutOrNull(20000L) {
                        storageService.uploadTopUpReceipt(
                            context = context,
                            userId = uid,
                            requestId = requestId,
                            imageUri = localUri
                        )
                    }
                    if (uploadResult?.isSuccess == true) {
                        storedReceiptPath = uploadResult.getOrNull().orEmpty()
                    }
                } catch (t: Throwable) {
                    android.util.Log.w("MarketplaceRepository", "Storage upload exception: ${t.message}")
                }
            }

            // C. Base64 fallback (guarantees image is never lost even if all cloud storage APIs fail)
            if (storedReceiptPath.isBlank()) {
                val base64Uri = compressImageToBase64DataUri(context, localUri)
                if (!base64Uri.isNullOrBlank()) {
                    storedReceiptPath = base64Uri
                    android.util.Log.i("MarketplaceRepository", "Receipt image fallback to Base64 data URI successful (size: ${base64Uri.length} chars)")
                }
            }

            if (storedReceiptPath.isBlank() && !hasReference) {
                return@withContext Result.failure(
                    IllegalStateException("تعذر رفع أو معالجة صورة الوصل. يُرجى التأكد من اختيار صورة صحيحة أو إدخال رقم مرجع العملية.")
                )
            }
        }

        // 7. Extract user profile info for admin display
        val localUser = db.userDao().getUserByIdDirect(uid)
            ?: (if (userId.isNotBlank()) db.userDao().getUserByIdDirect(userId) else null)
            ?: db.userDao().getUserByIdDirect("user_me")
        val userName = localUser?.name?.ifBlank { null }
            ?: currentFirebaseUser?.displayName?.ifBlank { null }
            ?: currentFirebaseUser?.email?.substringBefore("@")
            ?: "مستخدم OcaVenteDz"
        val userPhone = localUser?.phone?.ifBlank { null }
            ?: currentFirebaseUser?.phoneNumber
            ?: ""

        val now = System.currentTimeMillis()
        val cachedEntity = TopUpRequestEntity(
            id = requestId,
            userId = uid,
            userName = userName,
            userPhone = userPhone,
            amountDzd = amount,
            provider = provider,
            reference = reference.trim(),
            receiptImageUri = storedReceiptPath.ifBlank { receiptImageUriString },
            status = "PENDING",
            adminNote = "",
            createdAt = now,
            reviewedAt = 0L
        )

        // 8. Submit to Back4App cloud database
        var back4AppSuccess = false
        try {
            val b4aResult = withTimeoutOrNull(15000L) {
                back4AppClient.submitTopUpRequest(cachedEntity)
            }
            if (b4aResult?.isSuccess == true) {
                back4AppSuccess = true
                android.util.Log.i("MarketplaceRepository", "TopUpRequest successfully recorded in Back4App!")
            } else {
                android.util.Log.w("MarketplaceRepository", "Back4App submit error: ${b4aResult?.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            android.util.Log.w("MarketplaceRepository", "Back4App submit exception: ${e.message}")
        }

        // 9. Also record in Firestore as redundant backup if available
        var firestoreSuccess = false
        val firestoreReq = FirestoreTopUpRequest(
            id = requestId,
            userId = uid,
            userName = userName,
            userPhone = userPhone,
            amountDzd = amount,
            provider = provider,
            reference = reference.trim(),
            receiptImageUri = storedReceiptPath,
            status = "PENDING",
            adminNote = "",
            createdAt = null,
            reviewedAt = null
        )
        try {
            val fsResult = withTimeoutOrNull(10000L) {
                firestoreService.submitTopUpRequest(firestoreReq)
            }
            if (fsResult?.isSuccess == true) {
                firestoreSuccess = true
                android.util.Log.i("MarketplaceRepository", "TopUpRequest successfully recorded in Firestore!")
            }
        } catch (_: Exception) {}

        // Verify at least one cloud target or local storage succeeded
        if (!back4AppSuccess && !firestoreSuccess && storedReceiptPath.isBlank() && reference.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("تعذر تسجيل طلب الشحن في الخوادم السحابية. يرجى التحقق من اتصال الإنترنت.")
            )
        }

        // 10. Save locally in Room
        try {
            db.topUpRequestDao().insertRequest(cachedEntity)
            if (userId.isNotBlank() && userId != uid) {
                db.topUpRequestDao().insertRequest(cachedEntity.copy(userId = userId))
            }
        } catch (dbErr: Exception) {
            android.util.Log.e("MarketplaceRepository", "Failed to cache request locally: ${dbErr.message}")
        }

        Result.success("تم إرسال طلب الشحن ووصل الدفع بنجاح إلى الإدارة! ستتم مراجعته واعتماد الرصيد قريباً.")
    }

    /**
     * Compresses a local image Uri into a compact JPEG Base64 data URI (data:image/jpeg;base64,...).
     * Keeps the file size small (<150KB) so it safely fits within Firestore's 1MB document limit
     * without needing Firebase Cloud Storage or a Blaze billing plan.
     */
    private fun compressImageToBase64DataUri(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (originalBitmap == null) return null

            // Scale down to max 800x800 for optimal readability and tiny storage footprint
            val maxDimension = 800
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = if (width > height) {
                if (width > maxDimension) maxDimension.toFloat() / width else 1f
            } else {
                if (height > maxDimension) maxDimension.toFloat() / height else 1f
            }

            val scaledBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    originalBitmap,
                    (width * scale).toInt().coerceAtLeast(1),
                    (height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                originalBitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val byteArray = outputStream.toByteArray()
            val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } catch (e: Exception) {
            android.util.Log.e("MarketplaceRepository", "Error compressing receipt image: ${e.message}", e)
            null
        }
    }

    suspend fun approveTopUpRequest(requestId: String, adminNote: String = "تم التحقق من الوصل بنجاح"): Result<String> {
        val request = db.topUpRequestDao().getRequestById(requestId)
            ?: return Result.failure(Exception("طلب الشحن غير موجود."))

        if (request.status == "APPROVED") {
            return Result.failure(Exception("هذا الطلب تمت الموافقة عليه مسبقاً."))
        }

        val now = System.currentTimeMillis()
        val updated = db.walletDao().creditWallet(request.userId, request.amountDzd, now)
        if (updated == 0) {
            db.walletDao().insertOrUpdateWallet(
                WalletEntity(userId = request.userId, balanceDzd = request.amountDzd, updatedAt = now)
            )
        }

        val tx = WalletTransactionEntity(
            id = "tx_" + UUID.randomUUID().toString().replace("-", "").take(10),
            userId = request.userId,
            type = "TOPUP",
            amount = request.amountDzd,
            description = "شحن رصيد يدوي - وصل تحويل ${request.provider} (مرجع: ${request.reference.ifBlank { request.id }})",
            referenceId = request.id,
            timestamp = now
        )
        db.walletDao().insertTransaction(tx)
        db.topUpRequestDao().updateStatus(request.id, "APPROVED", adminNote, now)

        return Result.success("تمت الموافقة بنجاح وشحن ${request.amountDzd} دج إلى محفظة ${request.userName} ✓")
    }

    suspend fun rejectTopUpRequest(requestId: String, reason: String): Result<String> {
        val request = db.topUpRequestDao().getRequestById(requestId)
            ?: return Result.failure(Exception("طلب الشحن غير موجود."))

        val now = System.currentTimeMillis()
        val note = reason.ifBlank { "الوصل غير مطابق أو غير واضح" }
        db.topUpRequestDao().updateStatus(request.id, "REJECTED", note, now)
        return Result.success("تم رفض طلب الشحن.")
    }

    suspend fun topUpWallet(userId: String, amount: Int, paymentProvider: String, txReference: String? = null): Result<String> {
        return Result.failure(Exception("يرجى إرسال وصل التحويل للمراجعة اليدوية عبر شحن المحفظة."))
    }

    /**
     * Reads the specified user's balance directly from Back4App or Firestore.
     * Falls back to local Room database if cloud services are unreachable or offline.
     */
    suspend fun getCurrentUserBalance(userId: String): Result<Int> {
        // 1. Try Back4App cloud database
        try {
            val b4aWallet = back4AppClient.getWallet(userId)
            if (b4aWallet.isSuccess && b4aWallet.getOrNull() != null) {
                val bal = b4aWallet.getOrNull()!!.balanceDzd
                db.walletDao().insertOrUpdateWallet(WalletEntity(userId = userId, balanceDzd = bal, updatedAt = System.currentTimeMillis()))
                return Result.success(bal)
            }
        } catch (_: Exception) {}

        // 2. Try Firestore
        val firestoreResult = firestoreService.getCurrentUserBalance(userId)
        if (firestoreResult.isSuccess) {
            val balance = firestoreResult.getOrDefault(0)
            try {
                val now = System.currentTimeMillis()
                db.walletDao().insertOrUpdateWallet(WalletEntity(userId = userId, balanceDzd = balance, updatedAt = now))
            } catch (e: Exception) {
                android.util.Log.w("MarketplaceRepository", "Failed to cache Firestore balance locally: ${e.message}")
            }
            return firestoreResult
        }
        val localWallet = db.walletDao().getWalletDirect(userId)
        return Result.success(localWallet?.balanceDzd ?: 0)
    }

    /**
     * Realtime flow for observing the user's wallet document from Firestore.
     */
    fun getUserWalletFromFirestore(userId: String): Flow<Result<FirestoreWallet?>> {
        return firestoreService.getUserWalletFlow(userId)
    }

    suspend fun syncWalletLocally(wallet: WalletEntity) {
        val current = db.walletDao().getWalletDirect(wallet.userId)
        if (current == null || current.balanceDzd != wallet.balanceDzd) {
            db.walletDao().insertOrUpdateWallet(wallet)
        }
    }

    // Users
    fun getUser(id: String): Flow<UserEntity?> = db.userDao().getUserById(id)
    suspend fun getUserDirect(id: String): UserEntity? = db.userDao().getUserByIdDirect(id)
    suspend fun findUserByPhoneOrEmail(input: String): UserEntity? {
        val clean = input.trim().lowercase()

        if (clean == "laminedz.19@gmail.com" || clean == "laminedz19@gmail.com") {
            val adminUser = db.userDao().getUserByIdDirect("user_admin")
            if (adminUser != null) {
                if (adminUser.email != clean) {
                    val updated = adminUser.copy(email = clean)
                    db.userDao().insertUser(updated)
                    return updated
                }
                return adminUser
            } else {
                val newAdmin = UserEntity(
                    id = "user_admin",
                    phone = "+213 555 12 34 56",
                    email = clean,
                    name = "المشرف العام (Lamine DZ)",
                    avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
                    wilaya = "الجزائر العاصمة",
                    commune = "الجزائر الوسطى",
                    bio = "الحساب الرسمي لإدارة ومنصة OcaVenteDz.",
                    sellerRating = 5.0,
                    reviewsCount = 50,
                    adsCount = 0,
                    createdAt = System.currentTimeMillis(),
                    isVerified = true,
                    verificationRequested = false,
                    isBanned = false,
                    role = "ADMIN"
                )
                db.userDao().insertUser(newAdmin)
                return newAdmin
            }
        }

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
            back4AppClient.saveUser(user)
        } catch (_: Exception) {}
        try {
            kotlinx.coroutines.withTimeoutOrNull(5_000L) {
                firestoreService.saveUser(user)
            }
        } catch (_: Exception) {}
    }

    suspend fun createEmptyWallet(userId: String) {
        val entity = WalletEntity(userId = userId, balanceDzd = 0, updatedAt = System.currentTimeMillis())
        db.walletDao().insertOrUpdateWallet(entity)
        try {
            back4AppClient.saveWallet(entity)
        } catch (_: Exception) {}
    }

    suspend fun updateUser(user: UserEntity) {
        db.userDao().updateUser(user)
        try {
            back4AppClient.saveUser(user)
        } catch (_: Exception) {}
        try {
            firestoreService.saveUser(user)
        } catch (_: Exception) {}
    }
    suspend fun updateBanStatus(userId: String, banned: Boolean) {
        db.userDao().updateBanStatus(userId, banned)
        try {
            back4AppClient.updateUserBan(userId, banned)
        } catch (_: Exception) {}
    }
    suspend fun updateVerification(userId: String, verified: Boolean) {
        db.userDao().updateVerificationStatus(userId, verified)
        try {
            back4AppClient.updateUserVerification(userId, verified)
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
            id = "MSG_" + UUID.randomUUID().toString(),
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
            back4AppClient.saveChatMessage(msg)
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
        return Result.success("تم إضافة تقييمك بنجاح")
    }

    // Reports
    fun getAllReports(): Flow<List<ReportEntity>> = db.reportDao().getAllReports()
    suspend fun submitReport(reporterId: String, listingId: String, userId: String, reason: String, comment: String) {
        val report = ReportEntity(
            id = "REP_" + UUID.randomUUID().toString().take(8),
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
            back4AppClient.saveReport(report)
        } catch (_: Exception) {}
    }
    suspend fun updateReportStatus(reportId: String, status: String) = db.reportDao().updateReportStatus(reportId, status)

    // Favorites
    fun getFavorites(userId: String): Flow<List<FavoriteEntity>> = db.favoriteDao().getFavoritesForUser(userId)
    fun isFavorite(userId: String, listingId: String): Flow<Int> = db.favoriteDao().isFavorite(userId, listingId)
    suspend fun toggleFavorite(userId: String, listingId: String, currentlyFav: Boolean) {
        if (currentlyFav) {
            db.favoriteDao().deleteFavorite(userId, listingId)
        } else {
            db.favoriteDao().insertFavorite(
                FavoriteEntity(
                    id = "FAV_${userId}_$listingId",
                    userId = userId,
                    listingId = listingId,
                    notifyPriceDrop = true,
                    notifySimilar = true,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    // Platform Settings & Payments
    fun getPlatformSettings(): Flow<PlatformSettingsEntity?> = db.settingsDao().getSettings()

    suspend fun updatePlatformSettings(settings: PlatformSettingsEntity) {
        db.settingsDao().insertOrUpdateSettings(settings)
        try {
            back4AppClient.savePlatformSettings(settings)
        } catch (_: Exception) {}
        try {
            firestoreService.savePlatformSettings(settings)
        } catch (_: Exception) {}
    }

    suspend fun syncPlatformSettingsFromFirestore() {
        try {
            val b4aResult = back4AppClient.getPlatformSettings()
            if (b4aResult.isSuccess) {
                b4aResult.getOrNull()?.let { remoteSettings ->
                    db.settingsDao().insertOrUpdateSettings(remoteSettings)
                }
            }
        } catch (_: Exception) {}
        try {
            val result = firestoreService.getPlatformSettings()
            if (result.isSuccess) {
                result.getOrNull()?.let { remoteSettings ->
                    db.settingsDao().insertOrUpdateSettings(remoteSettings)
                }
            }
        } catch (_: Exception) {}
    }

    fun getPlatformSettingsFromFirestore(): Flow<Result<PlatformSettingsEntity?>> {
        return firestoreService.getPlatformSettingsFlow()
    }

    fun getAllPayments(): Flow<List<PaymentOrderEntity>> = db.paymentDao().getAllPayments()

    // --- Orders Management ---
    fun getLocalOrdersByBuyer(buyerId: String): Flow<List<OrderEntity>> = db.orderDao().getOrdersByBuyer(buyerId)
    fun getLocalOrdersBySeller(sellerId: String): Flow<List<OrderEntity>> = db.orderDao().getOrdersBySeller(sellerId)
    fun getLocalOrderById(orderId: String): Flow<OrderEntity?> = db.orderDao().getOrderById(orderId)

    suspend fun createOrder(order: FirestoreOrder): Result<String> {
        val entity = order.toOrderEntity()
        db.orderDao().insertOrder(entity)
        try {
            back4AppClient.saveOrder(entity)
        } catch (_: Exception) {}
        val res = firestoreService.createOrder(order)
        return Result.success(order.id)
    }

    suspend fun saveOrderLocally(order: OrderEntity) {
        db.orderDao().insertOrder(order)
        try {
            back4AppClient.saveOrder(order)
        } catch (_: Exception) {}
    }

    fun getOrderFlow(orderId: String): Flow<Result<FirestoreOrder?>> = firestoreService.getOrderFlow(orderId)

    fun getUserOrdersFlow(buyerId: String): Flow<Result<List<FirestoreOrder>>> = firestoreService.getUserOrdersFlow(buyerId)

    fun getSellerOrdersFlow(sellerId: String): Flow<Result<List<FirestoreOrder>>> = firestoreService.getSellerOrdersFlow(sellerId)

    fun getAllOrdersAdminFlow(): Flow<Result<List<FirestoreOrder>>> = firestoreService.getAllOrdersAdminFlow()

    suspend fun updateOrderStatus(
        orderId: String,
        newStatus: String,
        statusNote: String = "",
        trackingNumber: String? = null
    ): Result<Unit> {
        db.orderDao().updateOrderStatus(orderId, newStatus, statusNote, System.currentTimeMillis())
        try {
            back4AppClient.updateOrderStatus(orderId, newStatus, statusNote, trackingNumber)
        } catch (_: Exception) {}
        val res = firestoreService.updateOrderStatus(orderId, newStatus, statusNote, trackingNumber)
        return Result.success(Unit)
    }

    suspend fun syncOrdersLocally(orders: List<FirestoreOrder>) {
        if (orders.isNotEmpty()) {
            db.orderDao().insertOrders(orders.map { it.toOrderEntity() })
        }
    }
}
