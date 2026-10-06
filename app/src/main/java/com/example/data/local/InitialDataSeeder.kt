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

        // 1.1 Master Admin User (Lamine DZ) - Pre-activated & verified
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

        // 2. Demo Sellers for Catalog Showcase
        val sellerKarim = UserEntity(
            id = "seller_karim",
            phone = "+213 000 00 00 03",
            email = "seller-karim@ocaventedz.dz",
            name = "كريم وهران لقطع الغيار والسيارات",
            avatarUrl = "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=200",
            wilaya = "وهران",
            commune = "السانية",
            bio = "محل معتمد لبيع قطع الغيار والسيارات السياحية بوهران.",
            sellerRating = 4.8,
            reviewsCount = 29,
            adsCount = 12,
            createdAt = now - (90L * 24 * 3600 * 1000),
            isVerified = true,
            verificationRequested = false,
            isBanned = false,
            role = "USER"
        )
        db.userDao().insertUser(sellerKarim)

        val sellerYacine = UserEntity(
            id = "seller_yacine",
            phone = "+213 000 00 00 04",
            email = "seller-yacine@ocaventedz.dz",
            name = "ياسين سطيف إلكترونيك",
            avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
            wilaya = "سطيف",
            commune = "العلمة",
            bio = "استيراد وبيع الهواتف الذكية والإلكترونيات الأصلية بالجملة والتجزئة.",
            sellerRating = 5.0,
            reviewsCount = 64,
            adsCount = 8,
            createdAt = now - (150L * 24 * 3600 * 1000),
            isVerified = true,
            verificationRequested = false,
            isBanned = false,
            role = "USER"
        )
        db.userDao().insertUser(sellerYacine)

        val sellerAmine = UserEntity(
            id = "seller_amine",
            phone = "+213 000 00 00 05",
            email = "seller-amine@ocaventedz.dz",
            name = "أمين العقارية الجزائر",
            avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200",
            wilaya = "الجزائر العاصمة",
            commune = "حيدرة",
            bio = "وكالة عقارية معتمدة للكراء والبيع بالعاصمة وضواحيها.",
            sellerRating = 4.9,
            reviewsCount = 38,
            adsCount = 5,
            createdAt = now - (180L * 24 * 3600 * 1000),
            isVerified = true,
            verificationRequested = false,
            isBanned = false,
            role = "USER"
        )
        db.userDao().insertUser(sellerAmine)

        // 3. Sample Realistic Algerian Listings
        val sampleListings = listOf(
            ListingEntity(
                id = "list_1",
                userId = "seller_yacine",
                userName = "ياسين سطيف إلكترونيك",
                userPhone = "+213 000 00 00 04",
                isPhoneVisible = true,
                title = "iPhone 15 Pro Max 256GB تيتانيوم أزرق أصلي",
                description = "آيفون 15 برو ماكس بطارية 100%، كابا أصلي غير مفتوح مع العلبة وكابل الشحن الأصلي. خالي من الخدوش مع فاتورة شراء.",
                categoryId = "electronics",
                categoryNameAr = "الهواتف والإلكترونيات",
                subcategory = "هواتف ذكية",
                priceDzd = 185000,
                isNegotiable = true,
                condition = "LIKE_NEW",
                wilayaCode = 19,
                wilayaName = "سطيف",
                commune = "العلمة",
                imagesJson = "https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=800,https://images.unsplash.com/photo-1695048065052-19266ad8e4b5?w=800",
                videoUrl = "",
                status = "PUBLISHED",
                rejectionReason = "",
                packageType = "FEATURED",
                publishingFeeDzd = 400,
                isPaid = true,
                isFeatured = true,
                isUrgent = false,
                viewsCount = 340,
                createdAt = now - (3 * 3600 * 1000),
                expiresAt = now + (27L * 24 * 3600 * 1000)
            ),
            ListingEntity(
                id = "list_2",
                userId = "seller_karim",
                userName = "كريم وهران لقطع الغيار والسيارات",
                userPhone = "+213 000 00 00 03",
                isPhoneVisible = true,
                title = "Volkswagen Golf 7 GTD 2.0 TDI 2018 نقية بزاف",
                description = "قولف 7 جي تي دي موديل 2018، ماشية 120 ألف كم حقيقي، سبيغة نقية، محرك 10/10 وعلبة سرعات DSG، صيانة دورية بالوثائق.",
                categoryId = "vehicles",
                categoryNameAr = "السيارات والدراجات",
                subcategory = "سيارات سياحية",
                priceDzd = 4450000,
                isNegotiable = true,
                condition = "USED",
                wilayaCode = 31,
                wilayaName = "وهران",
                commune = "السانية",
                imagesJson = "https://images.unsplash.com/photo-1541899481282-d53bffe3c35d?w=800,https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=800",
                videoUrl = "",
                status = "PUBLISHED",
                rejectionReason = "",
                packageType = "URGENT",
                publishingFeeDzd = 600,
                isPaid = true,
                isFeatured = true,
                isUrgent = true,
                viewsCount = 980,
                createdAt = now - (1 * 3600 * 1000),
                expiresAt = now + (29L * 24 * 3600 * 1000)
            ),
            ListingEntity(
                id = "list_3",
                userId = "seller_amine",
                userName = "أمين العقارية الجزائر",
                userPhone = "+213 000 00 00 05",
                isPhoneVisible = true,
                title = "شقة F3 للكراء بحيدرة قريبة من كل المرافق",
                description = "شقة 3 غرف في الطابق الثاني مجهزة بنظام تدفئة مركزي ومكيف، قريبة من المرافق والمحلات. عقد موثق سنوي، متوفرة فورًا.",
                categoryId = "real_estate",
                categoryNameAr = "العقارات",
                subcategory = "شقق للكراء",
                priceDzd = 85000,
                isNegotiable = false,
                condition = "LIKE_NEW",
                wilayaCode = 16,
                wilayaName = "الجزائر العاصمة",
                commune = "حيدرة",
                imagesJson = "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=800,https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=800",
                videoUrl = "",
                status = "PUBLISHED",
                rejectionReason = "",
                packageType = "STANDARD",
                publishingFeeDzd = 400,
                isPaid = true,
                isFeatured = false,
                isUrgent = false,
                viewsCount = 512,
                createdAt = now - (12 * 3600 * 1000),
                expiresAt = now + (18L * 24 * 3600 * 1000)
            )
        )
        db.listingDao().insertListings(sampleListings)
    }
}
