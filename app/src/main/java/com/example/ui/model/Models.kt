package com.example.ui.model

enum class ListingStatus(val labelAr: String) {
    PUBLISHED("منشور"),
    PENDING("قيد المراجعة"),
    REJECTED("مرفوض"),
    SOLD("تم البيع")
}

enum class ItemCondition(val labelAr: String) {
    NEW("جديد كلياً"),
    LIKE_NEW("شبه جديد"),
    GOOD("حالة جيدة"),
    FAIR("مستعمل مقبول"),
    FOR_PARTS("قطع غيار")
}

data class Listing(
    val id: String,
    val title: String,
    val price: Double,
    val isNegotiable: Boolean = true,
    val categoryId: String,
    val categoryName: String,
    val wilayaCode: Int,
    val wilayaName: String,
    val commune: String = "",
    val description: String,
    val condition: ItemCondition = ItemCondition.LIKE_NEW,
    val imageUrls: List<String> = emptyList(),
    val createdAt: String,
    val sellerId: String,
    val sellerName: String,
    val sellerPhone: String,
    val sellerAvatar: String = "",
    val sellerRating: Float = 4.8f,
    val isFeatured: Boolean = false,
    val status: ListingStatus = ListingStatus.PUBLISHED,
    val viewsCount: Int = 120
)

data class Category(
    val id: String,
    val nameAr: String,
    val nameFr: String,
    val iconName: String,
    val listingCount: Int = 0
)

data class Wilaya(
    val code: Int,
    val nameAr: String,
    val nameFr: String,
    val communes: List<String> = emptyList()
)

data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val wilayaCode: Int,
    val wilayaName: String,
    val avatarUrl: String = "",
    val balance: Double = 3500.0,
    val memberSince: String = "جانفي 2024",
    val isVerified: Boolean = true,
    val rating: Float = 4.9f,
    val reviewsCount: Int = 18,
    val listingsCount: Int = 12
)

data class ChatMessage(
    val id: String,
    val senderId: String,
    val text: String,
    val timestamp: String,
    val isMine: Boolean
)

data class Conversation(
    val id: String,
    val listingId: String,
    val listingTitle: String,
    val listingPrice: Double,
    val otherUserId: String,
    val otherUserName: String,
    val otherUserAvatar: String = "",
    val lastMessage: String,
    val lastMessageTime: String,
    val unreadCount: Int = 0,
    val messages: List<ChatMessage> = emptyList()
)

data class AppNotification(
    val id: String,
    val title: String,
    val body: String,
    val time: String,
    val isRead: Boolean = false,
    val type: NotificationType = NotificationType.INFO
)

enum class NotificationType {
    OFFER,
    CHAT,
    WALLET,
    ADMIN,
    INFO
}

data class WalletTransaction(
    val id: String,
    val title: String,
    val amount: Double,
    val isCredit: Boolean,
    val date: String,
    val reference: String,
    val status: String = "مكتملة"
)

data class RechargeRequest(
    val id: String,
    val amount: Double,
    val method: String, // بريدي موب BaridiMob, CCP, CIB
    val status: String, // قيد المراجعة والتحقق اليدوي, مقبول, مرفوض
    val date: String,
    val transactionRef: String,
    val receiptImageUrl: String = "",
    val destinationAccount: String = "007999990008761821 Clé 94"
)

data class Offer(
    val id: String,
    val listingId: String,
    val listingTitle: String,
    val originalPrice: Double,
    val offerAmount: Double,
    val buyerName: String,
    val status: String, // معلق, مقبول, مرفوض
    val date: String
)

data class Order(
    val id: String,
    val listingTitle: String,
    val price: Double,
    val status: String,
    val trackingNumber: String,
    val sellerName: String,
    val buyerName: String,
    val date: String
)
