package com.example.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

/**
 * Metadata and storage information for an ad image matching Firestore schema.
 */
@IgnoreExtraProperties
data class ListingImage(
    val id: String = "",
    val url: String = "",
    val thumbnailUrl: String = "",
    val storagePath: String = "",
    val isPrimary: Boolean = false,
    val order: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    @ServerTimestamp
    val uploadedAt: Timestamp? = null
)
