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

        val adminUser1 = UserEntity(
            id = "admin_laminedz_19",
            phone = "0555123456",
            email = "laminedz.19@gmail.com",
            name = "المشرف العام (Lamine DZ)",
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
            wilaya = "16 - الجزائر العاصمة",
            commune = "الجزائر الوسطى",
            bio = "المشرف والمسؤول العام عن منصة OcaVenteDz.",
            sellerRating = 5.0,
            reviewsCount = 100,
            adsCount = 20,
            createdAt = now - (365L * 24 * 3600 * 1000),
            isVerified = true,
            verificationRequested = false,
            isBanned = false,
            role = "ADMIN"
        )
        db.userDao().insertUser(adminUser1)
        db.walletDao().insertOrUpdateWallet(WalletEntity(userId = "admin_laminedz_19", balanceDzd = 50000, updatedAt = now))

        val adminUser2 = adminUser1.copy(
            id = "admin_laminedz_19_alt",
            email = "laminedz19@gmail.com"
        )
        db.userDao().insertUser(adminUser2)
        db.walletDao().insertOrUpdateWallet(WalletEntity(userId = "admin_laminedz_19_alt", balanceDzd = 50000, updatedAt = now))
    }
}
