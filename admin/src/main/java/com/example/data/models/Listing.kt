package com.example.data.models

import com.example.data.local.ListingEntity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Firestore data model representing a Marketplace Advertisement matching the Firestore schema.
 */
@IgnoreExtraProperties
data class Listing(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val sellerName: String = "",
    val sellerPhone: String = "",
    val isPhoneVisible: Boolean = true,
    val title: String = "",
    val description: String = "",
    val categoryId: String = "",
    val categoryNameAr: String = "",
    val categoryNameFr: String = "",
    val subcategory: String = "",
    val priceDzd: Long = 0L,
    val isNegotiable: Boolean = false,
    val condition: ItemCondition = ItemCondition.USED,
    val images: List<ListingImage> = emptyList(),
    val imageUrls: List<String> = emptyList(),
    val videoUrl: String? = null,
    val status: ListingStatus = ListingStatus.PAYMENT_PENDING,
    val packageType: ListingPackageType = ListingPackageType.STANDARD,
    val publishingFeeDzd: Int = 100,
    val isPaid: Boolean = false,
    val isFeatured: Boolean = false,
    val isUrgent: Boolean = false,
    val rejectionReason: String? = null,
    val wilayaCode: Int = 16,
    val wilayaName: String = "الجزائر",
    val commune: String = "الجزائر الوسطى",
    val address: String = "",
    val geoPoint: GeoPoint? = null,
    val geoLocation: GeoLocation? = null,
    val viewsCount: Int = 0,
    val favoritesCount: Int = 0,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null,
    val publishedAt: Timestamp? = null,
    val expiresAt: Timestamp? = null
) {
    /**
     * Converts Firestore Listing model to local Room ListingEntity
     */
    fun toListingEntity(): ListingEntity {
        val extractedImages = if (imageUrls.isNotEmpty()) {
            imageUrls
        } else {
            images.map { it.url }.filter { it.isNotBlank() }
        }

        return ListingEntity(
            id = id,
            userId = userId,
            userName = sellerName,
            userPhone = sellerPhone,
            isPhoneVisible = isPhoneVisible,
            title = title,
            description = description,
            categoryId = categoryId,
            categoryNameAr = categoryNameAr,
            subcategory = subcategory,
            priceDzd = priceDzd,
            isNegotiable = isNegotiable,
            condition = condition.name,
            wilayaCode = wilayaCode,
            wilayaName = wilayaName,
            commune = commune,
            imagesJson = extractedImages.joinToString(","),
            videoUrl = videoUrl ?: "",
            status = status.name,
            rejectionReason = rejectionReason ?: "",
            packageType = packageType.name,
            publishingFeeDzd = publishingFeeDzd,
            isPaid = isPaid,
            isFeatured = isFeatured,
            isUrgent = isUrgent,
            viewsCount = viewsCount,
            createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis(),
            expiresAt = expiresAt?.toDate()?.time ?: 0L
        )
    }

    companion object {
        fun fromListingEntity(entity: ListingEntity): Listing {
            val imgList = entity.imagesJson.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            val listingImages = imgList.mapIndexed { index, url ->
                ListingImage(
                    id = "img_${entity.id}_$index",
                    url = url,
                    thumbnailUrl = url,
                    isPrimary = index == 0,
                    order = index
                )
            }

            return Listing(
                id = entity.id,
                userId = entity.userId,
                sellerName = entity.userName,
                sellerPhone = entity.userPhone,
                isPhoneVisible = entity.isPhoneVisible,
                title = entity.title,
                description = entity.description,
                categoryId = entity.categoryId,
                categoryNameAr = entity.categoryNameAr,
                subcategory = entity.subcategory,
                priceDzd = entity.priceDzd,
                isNegotiable = entity.isNegotiable,
                condition = ItemCondition.fromString(entity.condition),
                images = listingImages,
                imageUrls = imgList,
                videoUrl = entity.videoUrl.ifEmpty { null },
                status = ListingStatus.fromString(entity.status),
                rejectionReason = entity.rejectionReason.ifEmpty { null },
                packageType = ListingPackageType.fromString(entity.packageType),
                publishingFeeDzd = entity.publishingFeeDzd,
                isPaid = entity.isPaid,
                isFeatured = entity.isFeatured,
                isUrgent = entity.isUrgent,
                viewsCount = entity.viewsCount,
                wilayaCode = entity.wilayaCode,
                wilayaName = entity.wilayaName,
                commune = entity.commune,
                createdAt = Timestamp(Date(entity.createdAt)),
                expiresAt = if (entity.expiresAt > 0) Timestamp(Date(entity.expiresAt)) else null
            )
        }
    }
}
