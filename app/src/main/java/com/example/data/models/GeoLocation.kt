package com.example.data.models

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Geolocation and administrative location data for Algeria.
 */
@IgnoreExtraProperties
data class GeoLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val wilayaCode: Int = 16,
    val wilayaName: String = "الجزائر",
    val commune: String = "",
    val streetAddress: String = "",
    val postalCode: String = ""
) {
    fun toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)

    companion object {
        fun fromGeoPoint(
            geoPoint: GeoPoint?,
            wilayaCode: Int = 16,
            wilayaName: String = "الجزائر",
            commune: String = "",
            address: String = ""
        ): GeoLocation {
            return GeoLocation(
                latitude = geoPoint?.latitude ?: 0.0,
                longitude = geoPoint?.longitude ?: 0.0,
                wilayaCode = wilayaCode,
                wilayaName = wilayaName,
                commune = commune,
                streetAddress = address
            )
        }
    }
}
