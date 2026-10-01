package com.example.data.remote.firestore

import com.example.data.local.ListingEntity
import com.example.data.local.OrderEntity
import com.example.data.local.PaymentOrderEntity
import com.example.data.local.PlatformSettingsEntity
import com.example.data.local.TopUpRequestEntity
import com.example.data.local.UserEntity
import com.example.data.local.WalletEntity
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Firestore data model for the "users" collection.
 * Includes no-arg default constructor required by Firestore.
 */
@IgnoreExtraProperties
data class FirestoreUser(
    @DocumentId
    val id: String = "",
    val phone: String = "",
    val email: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val wilaya: String = "",
    val commune: String = "",
    val bio: String = "",
    val sellerRating: Double = 5.0,
    val reviewsCount: Int = 0,
    val adsCount: Int = 0,
    val isVerified: Boolean = false,
    val verificationRequested: Boolean = false,
    val isBanned: Boolean = false,
    val role: String = "USER", // "USER", "ADMIN"
    @ServerTimestamp
    val createdAt: Date? = null
) {
    fun toUserEntity(): UserEntity {
        return UserEntity(
            id = id,
            phone = phone,
            email = email,
            name = name,
            avatarUrl = avatarUrl,
            wilaya = wilaya,
            commune = commune,
            bio = bio,
            sellerRating = sellerRating,
            reviewsCount = reviewsCount,
            adsCount = adsCount,
            createdAt = createdAt?.time ?: System.currentTimeMillis(),
            isVerified = isVerified,
            verificationRequested = verificationRequested,
            isBanned = isBanned,
            role = role
        )
    }

    companion object {
        fun fromUserEntity(entity: UserEntity): FirestoreUser {
            return FirestoreUser(
                id = entity.id,
                phone = entity.phone,
                email = entity.email,
                name = entity.name,
                avatarUrl = entity.avatarUrl,
                wilaya = entity.wilaya,
                commune = entity.commune,
                bio = entity.bio,
                sellerRating = entity.sellerRating,
                reviewsCount = entity.reviewsCount,
                adsCount = entity.adsCount,
                isVerified = entity.isVerified,
                verificationRequested = entity.verificationRequested,
                isBanned = entity.isBanned,
                role = entity.role,
                createdAt = Date(entity.createdAt)
            )
        }
    }
}

/**
 * Firestore data model for the "listings" collection.
 * Includes no-arg default constructor required by Firestore.
 */
@IgnoreExtraProperties
data class FirestoreListing(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val isPhoneVisible: Boolean = true,
    val title: String = "",
    val description: String = "",
    val categoryId: String = "",
    val categoryNameAr: String = "",
    val subcategory: String = "",
    val priceDzd: Long = 0L,
    val isNegotiable: Boolean = false,
    val condition: String = "USED", // "NEW", "LIKE_NEW", "USED"
    val wilayaCode: Int = 16,
    val wilayaName: String = "الجزائر العاصمة",
    val commune: String = "حيدرة",
    val images: List<String> = emptyList(),
    val videoUrl: String = "",
    val status: String = "PAYMENT_PENDING", // "DRAFT", "PAYMENT_PENDING", "PAYMENT_FAILED", "PAID", "UNDER_REVIEW", "PUBLISHED", "REJECTED", "EXPIRED", "SOLD"
    val rejectionReason: String = "",
    val packageType: String = "STANDARD", // "STANDARD", "FEATURED", "URGENT"
    val publishingFeeDzd: Int = 100,
    val isPaid: Boolean = false,
    val isFeatured: Boolean = false,
    val isUrgent: Boolean = false,
    val viewsCount: Int = 0,
    @ServerTimestamp
    val createdAt: Date? = null,
    val expiresAt: Long = 0L
) {
    fun toListingEntity(): ListingEntity {
        return ListingEntity(
            id = id,
            userId = userId,
            userName = userName,
            userPhone = userPhone,
            isPhoneVisible = isPhoneVisible,
            title = title,
            description = description,
            categoryId = categoryId,
            categoryNameAr = categoryNameAr,
            subcategory = subcategory,
            priceDzd = priceDzd,
            isNegotiable = isNegotiable,
            condition = condition,
            wilayaCode = wilayaCode,
            wilayaName = wilayaName,
            commune = commune,
            imagesJson = images.joinToString(","),
            videoUrl = videoUrl,
            status = status,
            rejectionReason = rejectionReason,
            packageType = packageType,
            publishingFeeDzd = publishingFeeDzd,
            isPaid = isPaid,
            isFeatured = isFeatured,
            isUrgent = isUrgent,
            viewsCount = viewsCount,
            createdAt = createdAt?.time ?: System.currentTimeMillis(),
            expiresAt = expiresAt
        )
    }

    companion object {
        fun fromListingEntity(entity: ListingEntity): FirestoreListing {
            val imgList = entity.imagesJson.split(",").map { it.trim() }.filter { it.isNotBlank() }
            return FirestoreListing(
                id = entity.id,
                userId = entity.userId,
                userName = entity.userName,
                userPhone = entity.userPhone,
                isPhoneVisible = entity.isPhoneVisible,
                title = entity.title,
                description = entity.description,
                categoryId = entity.categoryId,
                categoryNameAr = entity.categoryNameAr,
                subcategory = entity.subcategory,
                priceDzd = entity.priceDzd,
                isNegotiable = entity.isNegotiable,
                condition = entity.condition,
                wilayaCode = entity.wilayaCode,
                wilayaName = entity.wilayaName,
                commune = entity.commune,
                images = imgList,
                videoUrl = entity.videoUrl,
                status = entity.status,
                rejectionReason = entity.rejectionReason,
                packageType = entity.packageType,
                publishingFeeDzd = entity.publishingFeeDzd,
                isPaid = entity.isPaid,
                isFeatured = entity.isFeatured,
                isUrgent = entity.isUrgent,
                viewsCount = entity.viewsCount,
                createdAt = Date(entity.createdAt),
                expiresAt = entity.expiresAt
            )
        }
    }
}

/**
 * Firestore data model for the "payments" collection.
 */
@IgnoreExtraProperties
data class FirestorePayment(
    @DocumentId
    val paymentId: String = "",
    val userId: String = "",
    val listingId: String = "",
    val amount: Int = 0,
    val currency: String = "DZD",
    val status: String = "SUCCESS",
    val provider: String = "EDAHABIA", // "EDAHABIA", "CIB", "BARIDIMOB", "WALLET"
    val transactionReference: String = "",
    @ServerTimestamp
    val createdAt: Date? = null,
    val completedAt: Long = 0L
) {
    fun toPaymentOrderEntity(): PaymentOrderEntity {
        return PaymentOrderEntity(
            paymentId = paymentId,
            userId = userId,
            listingId = listingId,
            amount = amount,
            currency = currency,
            status = status,
            provider = provider,
            transactionReference = transactionReference,
            createdAt = createdAt?.time ?: System.currentTimeMillis(),
            completedAt = completedAt
        )
    }
}

/**
 * Firestore data model for the "topUpRequests" collection.
 * Required fields: id, userId, userName, userPhone, amountDzd, provider,
 * reference, receiptImageUri, status, adminNote, createdAt, reviewedAt.
 */
@IgnoreExtraProperties
data class FirestoreTopUpRequest(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val amountDzd: Int = 0,
    val provider: String = "BARIDIMOB", // "BARIDIMOB", "CCP", "EDAHABIA", "CIB"
    val reference: String = "",
    val receiptImageUri: String = "",
    val status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED"
    val adminNote: String = "",
    @ServerTimestamp
    val createdAt: Date? = null,
    val reviewedAt: Date? = null
) {
    fun toTopUpRequestEntity(): TopUpRequestEntity {
        return TopUpRequestEntity(
            id = id,
            userId = userId,
            userName = userName,
            userPhone = userPhone,
            amountDzd = amountDzd,
            provider = provider,
            reference = reference,
            receiptImageUri = receiptImageUri,
            status = status,
            adminNote = adminNote,
            createdAt = createdAt?.time ?: System.currentTimeMillis(),
            reviewedAt = reviewedAt?.time ?: 0L
        )
    }

    companion object {
        fun fromTopUpRequestEntity(entity: TopUpRequestEntity): FirestoreTopUpRequest {
            return FirestoreTopUpRequest(
                id = entity.id,
                userId = entity.userId,
                userName = entity.userName,
                userPhone = entity.userPhone,
                amountDzd = entity.amountDzd,
                provider = entity.provider,
                reference = entity.reference,
                receiptImageUri = entity.receiptImageUri,
                status = entity.status,
                adminNote = entity.adminNote,
                createdAt = if (entity.createdAt > 0) Date(entity.createdAt) else null,
                reviewedAt = if (entity.reviewedAt > 0) Date(entity.reviewedAt) else null
            )
        }
    }
}

/**
 * Firestore data model for the "wallets" collection.
 * Manages user balance and wallet status directly on Firestore.
 */
@IgnoreExtraProperties
data class FirestoreWallet(
    @DocumentId
    val userId: String = "",
    val balanceDzd: Int = 0,
    val pendingBalanceDzd: Int = 0,
    val currency: String = "DZD",
    val isActive: Boolean = true,
    @ServerTimestamp
    val updatedAt: Date? = null
) {
    fun toWalletEntity(): WalletEntity {
        return WalletEntity(
            userId = userId,
            balanceDzd = balanceDzd,
            updatedAt = updatedAt?.time ?: System.currentTimeMillis()
        )
    }

    companion object {
        fun fromWalletEntity(entity: WalletEntity): FirestoreWallet {
            return FirestoreWallet(
                userId = entity.userId,
                balanceDzd = entity.balanceDzd,
                updatedAt = Date(entity.updatedAt)
            )
        }
    }
}

/**
 * Firestore data model for the "settings" collection (document: "platform" or "global").
 * Keeps fees and official BaridiMob account synchronized across all devices in real-time.
 */
@IgnoreExtraProperties
data class FirestoreSettings(
    @DocumentId
    val id: String = "global",
    val standardAdFeeDzd: Int = 400,
    val featuredAdFeeDzd: Int = 600,
    val urgentAdFeeDzd: Int = 1000,
    val adDurationDays: Int = 30,
    val autoPublishAfterPayment: Boolean = true,
    val isFreePromoActive: Boolean = false,
    val officialRip: String = "007999990008761821",
    val officialKey: String = "94",
    val officialAccountHolder: String = "OcaVenteDz - الحساب المعتمد",
    val officialProviderName: String = "بريدي موب / CCP",
    val officialInstructions: String = "يرجى تحويل المبلغ بدقة، ثم أخذ لقطة شاشة للوصل وإرفاقها مع كتابة رقم العملية.",
    @ServerTimestamp
    val updatedAt: Date? = null
) {
    fun toPlatformSettingsEntity(): PlatformSettingsEntity {
        return PlatformSettingsEntity(
            id = if (id.isNotBlank()) id else "global",
            standardAdFeeDzd = standardAdFeeDzd,
            featuredAdFeeDzd = featuredAdFeeDzd,
            urgentAdFeeDzd = urgentAdFeeDzd,
            adDurationDays = adDurationDays,
            autoPublishAfterPayment = autoPublishAfterPayment,
            isFreePromoActive = isFreePromoActive,
            officialRip = officialRip,
            officialKey = officialKey,
            officialAccountHolder = officialAccountHolder,
            officialProviderName = officialProviderName,
            officialInstructions = officialInstructions
        )
    }

    companion object {
        fun fromPlatformSettingsEntity(entity: PlatformSettingsEntity): FirestoreSettings {
            return FirestoreSettings(
                id = if (entity.id.isNotBlank()) entity.id else "global",
                standardAdFeeDzd = entity.standardAdFeeDzd,
                featuredAdFeeDzd = entity.featuredAdFeeDzd,
                urgentAdFeeDzd = entity.urgentAdFeeDzd,
                adDurationDays = entity.adDurationDays,
                autoPublishAfterPayment = entity.autoPublishAfterPayment,
                isFreePromoActive = entity.isFreePromoActive,
                officialRip = entity.officialRip,
                officialKey = entity.officialKey,
                officialAccountHolder = entity.officialAccountHolder,
                officialProviderName = entity.officialProviderName,
                officialInstructions = entity.officialInstructions
            )
        }
    }
}

/**
 * Order status constants and helpers.
 */
object OrderStatus {
    const val PENDING = "PENDING"       // قيد المراجعة / في انتظار تأكيد البائع
    const val CONFIRMED = "CONFIRMED"   // تم التأكيد من البائع وجاري التجهيز
    const val SHIPPED = "SHIPPED"       // قيد الشحن / خرج للتوصيل مع شركة التوصيل
    const val DELIVERED = "DELIVERED"   // تم الاستلام بنجاح
    const val CANCELLED = "CANCELLED"   // تم الإلغاء من قبل المشتري
    const val REJECTED = "REJECTED"     // مرفوض من قبل البائع (نفاذ الكمية أو تعذر التوصيل)

    fun getDisplayName(status: String, lang: String = "ar"): String {
        return when (status) {
            PENDING -> if (lang == "ar") "قيد الانتظار" else "En attente"
            CONFIRMED -> if (lang == "ar") "مؤكد" else "Confirmée"
            SHIPPED -> if (lang == "ar") "قيد التوصيل" else "En livraison"
            DELIVERED -> if (lang == "ar") "تم التسليم" else "Livrée"
            CANCELLED -> if (lang == "ar") "ملغاة" else "Annulée"
            REJECTED -> if (lang == "ar") "مرفوضة" else "Refusée"
            else -> status
        }
    }
}

/**
 * Supported payment methods for orders.
 */
object OrderPaymentMethod {
    const val COD = "COD"               // الدفع عند الاستلام
    const val WALLET = "WALLET"         // خصم من رصيد المحفظة
    const val BARIDIMOB = "BARIDIMOB"   // تحويل بريدي موب

    fun getDisplayName(method: String, lang: String = "ar"): String {
        return when (method) {
            COD -> if (lang == "ar") "الدفع عند الاستلام" else "Paiement à la livraison"
            WALLET -> if (lang == "ar") "رصيد المحفظة" else "Solde du portefeuille"
            BARIDIMOB -> if (lang == "ar") "بريدي موب / CCP" else "BaridiMob / CCP"
            else -> method
        }
    }
}

/**
 * Firestore data model for the "orders" collection.
 * Tracks purchase orders between buyers and sellers, including delivery and payment status.
 */
@IgnoreExtraProperties
data class FirestoreOrder(
    @DocumentId
    val id: String = "",
    val orderNumber: String = "",
    val listingId: String = "",
    val listingTitle: String = "",
    val listingImageUrl: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val sellerPhone: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val buyerPhone: String = "",
    val buyerWilaya: String = "",
    val buyerCommune: String = "",
    val buyerAddress: String = "",
    val quantity: Int = 1,
    val unitPriceDzd: Int = 0,
    val deliveryFeeDzd: Int = 0,
    val totalAmountDzd: Int = 0,
    val paymentMethod: String = "COD", // "COD", "WALLET", "BARIDIMOB"
    val isPaid: Boolean = false,
    val status: String = "PENDING", // "PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED", "REJECTED"
    val trackingNumber: String = "",
    val buyerNotes: String = "",
    val statusNote: String = "",
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null,
    val deliveredAt: Long = 0L,
    val cancelledAt: Long = 0L
) {
    fun toOrderEntity(): OrderEntity {
        return OrderEntity(
            id = id,
            orderNumber = orderNumber,
            listingId = listingId,
            listingTitle = listingTitle,
            listingImageUrl = listingImageUrl,
            sellerId = sellerId,
            sellerName = sellerName,
            sellerPhone = sellerPhone,
            buyerId = buyerId,
            buyerName = buyerName,
            buyerPhone = buyerPhone,
            buyerWilaya = buyerWilaya,
            buyerCommune = buyerCommune,
            buyerAddress = buyerAddress,
            quantity = quantity,
            unitPriceDzd = unitPriceDzd,
            deliveryFeeDzd = deliveryFeeDzd,
            totalAmountDzd = totalAmountDzd,
            paymentMethod = paymentMethod,
            isPaid = isPaid,
            status = status,
            trackingNumber = trackingNumber,
            buyerNotes = buyerNotes,
            statusNote = statusNote,
            createdAt = createdAt?.time ?: System.currentTimeMillis(),
            updatedAt = updatedAt?.time ?: System.currentTimeMillis(),
            deliveredAt = deliveredAt,
            cancelledAt = cancelledAt
        )
    }

    companion object {
        fun fromOrderEntity(entity: OrderEntity): FirestoreOrder {
            return FirestoreOrder(
                id = entity.id,
                orderNumber = entity.orderNumber,
                listingId = entity.listingId,
                listingTitle = entity.listingTitle,
                listingImageUrl = entity.listingImageUrl,
                sellerId = entity.sellerId,
                sellerName = entity.sellerName,
                sellerPhone = entity.sellerPhone,
                buyerId = entity.buyerId,
                buyerName = entity.buyerName,
                buyerPhone = entity.buyerPhone,
                buyerWilaya = entity.buyerWilaya,
                buyerCommune = entity.buyerCommune,
                buyerAddress = entity.buyerAddress,
                quantity = entity.quantity,
                unitPriceDzd = entity.unitPriceDzd,
                deliveryFeeDzd = entity.deliveryFeeDzd,
                totalAmountDzd = entity.totalAmountDzd,
                paymentMethod = entity.paymentMethod,
                isPaid = entity.isPaid,
                status = entity.status,
                trackingNumber = entity.trackingNumber,
                buyerNotes = entity.buyerNotes,
                statusNote = entity.statusNote,
                createdAt = Date(entity.createdAt),
                updatedAt = Date(entity.updatedAt),
                deliveredAt = entity.deliveredAt,
                cancelledAt = entity.cancelledAt
            )
        }
    }
}
