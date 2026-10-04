package com.example.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object InitialDataSeeder {
    suspend fun seed(db: AppDatabase) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        // 1. Platform Settings
        val settings = PlatformSettingsEntity(
            id = "global",
            standardAdFeeDzd = 400,
            featuredAdFeeDzd = 600,
            urgentAdFeeDzd = 1000,
            adDurationDays = 30,
            autoPublishAfterPayment = true,
            isFreePromoActive = false
        )
        db.settingsDao().insertOrUpdateSettings(settings)
    }
}
