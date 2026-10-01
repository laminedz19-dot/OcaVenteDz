package com.example.data.models

import com.example.data.local.UserEntity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Firestore data model representing a User matching the Firestore schema.
 */
@IgnoreExtraProperties
data class User(
    @DocumentId
    val id: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val wilayaCode: Int = 16,
    val wilaya: String = "الجزائر",
    val commune: String = "الجزائر الوسطى",
    val location: GeoPoint? = null,
    val geoLocation: GeoLocation? = null,
    val role: UserRole = UserRole.USER,
    val status: UserStatus = UserStatus.ACTIVE,
    val verificationStatus: VerificationStatus = VerificationStatus.UNVERIFIED,
    val isVerified: Boolean = false,
    val sellerRating: Double = 5.0,
    val reviewsCount: Int = 0,
    val activeAdsCount: Int = 0,
    val totalAdsCount: Int = 0,
    val walletBalanceDzd: Long = 0L,
    val preferredLanguage: String = "ar",
    val isBanned: Boolean = false,
    val banReason: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null,
    val lastActiveAt: Timestamp? = null
) {
    /**
     * Converts Firestore User model to local Room UserEntity
     */
    fun toUserEntity(): UserEntity {
        return UserEntity(
            id = id,
            phone = phoneNumber,
            email = email,
            name = displayName.ifEmpty { "مستخدم OcaVenteDz" },
            avatarUrl = photoUrl,
            wilaya = wilaya,
            commune = commune,
            bio = bio,
            sellerRating = sellerRating,
            reviewsCount = reviewsCount,
            adsCount = totalAdsCount,
            createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis(),
            isVerified = isVerified || verificationStatus == VerificationStatus.VERIFIED,
            verificationRequested = verificationStatus == VerificationStatus.PENDING,
            isBanned = isBanned || status == UserStatus.BANNED,
            role = role.name
        )
    }

    companion object {
        fun fromUserEntity(entity: UserEntity): User {
            return User(
                id = entity.id,
                phoneNumber = entity.phone,
                email = entity.email,
                displayName = entity.name,
                photoUrl = entity.avatarUrl,
                bio = entity.bio,
                wilaya = entity.wilaya,
                commune = entity.commune,
                role = UserRole.fromString(entity.role),
                status = if (entity.isBanned) UserStatus.BANNED else UserStatus.ACTIVE,
                isVerified = entity.isVerified,
                verificationStatus = when {
                    entity.isVerified -> VerificationStatus.VERIFIED
                    entity.verificationRequested -> VerificationStatus.PENDING
                    else -> VerificationStatus.UNVERIFIED
                },
                sellerRating = entity.sellerRating,
                reviewsCount = entity.reviewsCount,
                totalAdsCount = entity.adsCount,
                activeAdsCount = entity.adsCount,
                createdAt = Timestamp(Date(entity.createdAt))
            )
        }
    }
}
