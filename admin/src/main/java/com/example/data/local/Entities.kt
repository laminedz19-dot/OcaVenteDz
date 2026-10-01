package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val phone: String,
    val email: String,
    val name: String,
    val avatarUrl: String,
    val wilaya: String,
    val commune: String,
    val bio: String,
    val sellerRating: Double,
    val reviewsCount: Int,
    val adsCount: Int,
    val createdAt: Long,
    val isVerified: Boolean,
    val verificationRequested: Boolean,
    val isBanned: Boolean,
    val role: String // "USER", "ADMIN"
)

@Entity(tableName = "listings")
data class ListingEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val userPhone: String,
    val isPhoneVisible: Boolean,
    val title: String,
    val description: String,
    val categoryId: String,
    val categoryNameAr: String,
    val subcategory: String,
    val priceDzd: Long,
    val isNegotiable: Boolean,
    val condition: String, // "NEW", "LIKE_NEW", "USED"
    val wilayaCode: Int,
    val wilayaName: String,
    val commune: String,
    val imagesJson: String, // Comma-separated or serialized URLs
    val videoUrl: String,
    val status: String, // "DRAFT", "PAYMENT_PENDING", "PAYMENT_FAILED", "PAID", "UNDER_REVIEW", "PUBLISHED", "REJECTED", "EXPIRED", "SOLD", "DELETED"
    val rejectionReason: String,
    val packageType: String, // "STANDARD", "FEATURED", "URGENT"
    val publishingFeeDzd: Int,
    val isPaid: Boolean,
    val isFeatured: Boolean,
    val isUrgent: Boolean,
    val viewsCount: Int,
    val createdAt: Long,
    val expiresAt: Long
)

@Entity(tableName = "payment_orders")
data class PaymentOrderEntity(
    @PrimaryKey val paymentId: String,
    val userId: String,
    val listingId: String,
    val amount: Int,
    val currency: String,
    val status: String, // "SUCCESS", "PENDING", "FAILED"
    val provider: String, // "EDAHABIA", "CIB", "BARIDIMOB", "WALLET"
    val transactionReference: String,
    val createdAt: Long,
    val completedAt: Long
)

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey val userId: String,
    val balanceDzd: Int,
    val updatedAt: Long
)

@Entity(tableName = "wallet_transactions")
data class WalletTransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String, // "TOPUP", "AD_PAYMENT", "PROMOTION", "REFUND", "BONUS"
    val amount: Int,
    val description: String,
    val referenceId: String,
    val timestamp: Long
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val listingId: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val timestamp: Long,
    val isOffer: Boolean,
    val offerAmountDzd: Long,
    val offerStatus: String // "PENDING", "ACCEPTED", "REJECTED", "NONE"
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey val id: String,
    val sellerId: String,
    val buyerId: String,
    val buyerName: String,
    val listingId: String,
    val rating: Int,
    val comment: String,
    val timestamp: Long
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val reporterId: String,
    val reportedListingId: String,
    val reportedUserId: String,
    val reason: String, // "FRAUD", "PROHIBITED_ITEM", "FALSE_INFO", "DUPLICATE", "FAKE_PRICE", "INAPPROPRIATE", "OTHER"
    val comment: String,
    val timestamp: Long,
    val status: String // "PENDING", "RESOLVED", "DISMISSED"
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val listingId: String,
    val notifyPriceDrop: Boolean,
    val notifySimilar: Boolean,
    val timestamp: Long
)

@Entity(tableName = "platform_settings")
data class PlatformSettingsEntity(
    @PrimaryKey val id: String = "global",
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
    val officialInstructions: String = "يرجى تحويل المبلغ بدقة، ثم أخذ لقطة شاشة للوصل وإرفاقها مع كتابة رقم العملية."
)

@Entity(tableName = "top_up_requests")
data class TopUpRequestEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val userPhone: String,
    val amountDzd: Int,
    val provider: String, // "BARIDIMOB", "CCP", "EDAHABIA", "CIB"
    val reference: String,
    val receiptImageUri: String, // image uri or path
    val status: String, // "PENDING", "APPROVED", "REJECTED"
    val adminNote: String,
    val createdAt: Long,
    val reviewedAt: Long
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val orderNumber: String,
    val listingId: String,
    val listingTitle: String,
    val listingImageUrl: String,
    val sellerId: String,
    val sellerName: String,
    val sellerPhone: String,
    val buyerId: String,
    val buyerName: String,
    val buyerPhone: String,
    val buyerWilaya: String,
    val buyerCommune: String,
    val buyerAddress: String,
    val quantity: Int,
    val unitPriceDzd: Int,
    val deliveryFeeDzd: Int,
    val totalAmountDzd: Int,
    val paymentMethod: String, // "COD", "WALLET", "BARIDIMOB"
    val isPaid: Boolean,
    val status: String, // "PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED", "REJECTED"
    val trackingNumber: String,
    val buyerNotes: String,
    val statusNote: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deliveredAt: Long = 0L,
    val cancelledAt: Long = 0L
)
