package com.example.admin.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AdminUser(
    val id: String,
    val email: String,
    val username: String,
    val phone: String? = null,
    val wilaya: String = "16 - الجزائر (Alger)",
    val role: String = "USER",
    val balance: Double = 0.0,
    val isBlocked: Boolean = false,
    val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class AdminListing(
    val id: String,
    val title: String,
    val description: String,
    val price: Double,
    val categoryName: String = "عام",
    val sellerUsername: String = "مستخدم",
    val sellerPhone: String = "0550123456",
    val wilaya: String = "16 - الجزائر",
    val status: String = "PENDING", // PENDING, ACTIVE, REJECTED
    val rejectionReason: String? = null,
    val imageUrl: String? = null,
    val createdAt: String = "اليوم"
)

@JsonClass(generateAdapter = true)
data class AdminRechargeRequest(
    val id: String,
    val userId: String,
    val username: String,
    val userPhone: String,
    val currentBalance: Double,
    val amount: Double,
    val paymentMethod: String, // BARIDIMOB, CCP
    val receiptNumber: String,
    val receiptImageUrl: String? = null,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val adminNotes: String? = null,
    val createdAt: String = "اليوم"
)

@JsonClass(generateAdapter = true)
data class AdminDashboardStats(
    val totalUsers: Int,
    val totalActiveListings: Int,
    val pendingListingsCount: Int,
    val pendingRechargeCount: Int,
    val totalRechargeVolume: Double
)

@JsonClass(generateAdapter = true)
data class AdminAuditLog(
    val id: String,
    val adminUsername: String,
    val action: String,
    val targetType: String,
    val details: String,
    val timestamp: String
)
