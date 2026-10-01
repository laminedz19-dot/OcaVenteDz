package com.example.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object InitialDataSeeder {
    suspend fun seed(db: AppDatabase) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        // 1. Settings
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

        // 2. Demo Users
        val currentUser = UserEntity(
            id = "user_me",
            phone = "+213 000 00 00 01",
            email = "demo-user@ocaventedz.invalid",
            name = "محمد أمين دزيري",
            avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200",
            wilaya = "الجزائر العاصمة",
            commune = "حيدرة",
            bio = "بائع موثوق للأجهزة الإلكترونية والمنزلية الأصلية في العاصمة.",
            sellerRating = 4.9,
            reviewsCount = 18,
            adsCount = 4,
            createdAt = now - (60L * 24 * 3600 * 1000),
            isVerified = true,
            verificationRequested = false,
            isBanned = false,
            role = "USER"
        )
        db.userDao().insertUser(currentUser)

        val adminUser = UserEntity(
            id = "user_admin",
            phone = "+213 555 12 34 56",
            email = "laminedz.19@gmail.com",
            name = "المشرف العام (Lamine DZ)",
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
            wilaya = "الجزائر العاصمة",
            commune = "الجزائر الوسطى",
            bio = "الحساب الرسمي لإدارة ومنصة OcaVenteDz.",
            sellerRating = 5.0,
            reviewsCount = 42,
            adsCount = 0,
            createdAt = now - (120L * 24 * 3600 * 1000),
            isVerified = true,
            verificationRequested = false,
            isBanned = false,
            role = "ADMIN"
        )
        db.userDao().insertUser(adminUser)

        val sellerKarim = UserEntity(
            id = "user_karim",
            phone = "+213 000 00 00 03",
            email = "seller-karim@ocaventedz.invalid",
            name = "كريم وهران لقطع الغيار",
            avatarUrl = "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=200",
            wilaya = "وهران",
            commune = "السانية",
            bio = "مستورد وموزع قطع غيار ولوازم السيارات بوهران.",
            sellerRating = 4.8,
            reviewsCount = 31,
            adsCount = 12,
            createdAt = now - (90L * 24 * 3600 * 1000),
            isVerified = true,
            verificationRequested = false,
            isBanned = false,
            role = "USER"
        )
        db.userDao().insertUser(sellerKarim)

        val sellerYacine = UserEntity(
            id = "user_yacine",
            phone = "+213 000 00 00 04",
            email = "seller-yacine@ocaventedz.invalid",
            name = "ياسين سطيف إلكترونيك",
            avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
            wilaya = "سطيف",
            commune = "العلمة",
            bio = "هواتف ذكية وأجهزة كهرومنزلية جديدة وبالضمان.",
            sellerRating = 4.7,
            reviewsCount = 15,
            adsCount = 6,
            createdAt = now - (45L * 24 * 3600 * 1000),
            isVerified = false,
            verificationRequested = true,
            isBanned = false,
            role = "USER"
        )
        db.userDao().insertUser(sellerYacine)

        // 3. Start with no spendable balance; credits must come from a verified provider.
        val wallet = WalletEntity(
            userId = "user_me",
            balanceDzd = 0,
            updatedAt = now
        )
        db.walletDao().insertOrUpdateWallet(wallet)

        // 4. Sample Realistic Algerian Listings
        val sampleListings = listOf(
            ListingEntity(
                id = "list_1",
                userId = "user_yacine",
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
                publishingFeeDzd = 200,
                isPaid = true,
                isFeatured = true,
                isUrgent = false,
                viewsCount = 340,
                createdAt = now - (3 * 3600 * 1000),
                expiresAt = now + (27L * 24 * 3600 * 1000)
            ),
            ListingEntity(
                id = "list_2",
                userId = "user_karim",
                userName = "كريم وهران لقطع الغيار",
                userPhone = "+213 000 00 00 03",
                isPhoneVisible = true,
                title = "Volkswagen Golf 7 GTD 2.0 TDI 2018 نقية بزاف",
                description = "قولف 7 جي تي دي موديل 2018، ماشية 120 ألف كم حقيقي، سبيغة نقية فيها نقاوة فالباب الأيمن فقط، محرك 10/10 وعلبة سرعات DSG سيري، صيانة دورية بالوثائق.",
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
                publishingFeeDzd = 300,
                isPaid = true,
                isFeatured = true,
                isUrgent = true,
                viewsCount = 980,
                createdAt = now - (1 * 3600 * 1000),
                expiresAt = now + (29L * 24 * 3600 * 1000)
            ),
            ListingEntity(
                id = "list_3",
                userId = "user_me",
                userName = "محمد أمين دزيري",
                userPhone = "+213 000 00 00 01",
                isPhoneVisible = true,
                title = "شقة F3 للكراء بحيدرة قريبة من كل المرافق",
                description = "شقة 3 غرف في الطابق الثاني مجهزة بنظام تدفئة مركزي ومكيف، قريبة من السفارات والمحلات. عقد موثق سنوي، متوفرة فورًا.",
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
                publishingFeeDzd = 100,
                isPaid = true,
                isFeatured = false,
                isUrgent = false,
                viewsCount = 512,
                createdAt = now - (12 * 3600 * 1000),
                expiresAt = now + (25L * 24 * 3600 * 1000)
            ),
            ListingEntity(
                id = "list_4",
                userId = "user_yacine",
                userName = "ياسين سطيف إلكترونيك",
                userPhone = "+213 000 00 00 04",
                isPhoneVisible = true,
                title = "PlayStation 5 Slim 1TB مع يدين أصليين ولعبتين",
                description = "بلايستيشن 5 سليم جديد في العلبة لم يستعمل إلا للتجريب، معه يدين تحكم DualSense و شريطين FC24 و Spider-Man 2.",
                categoryId = "games",
                categoryNameAr = "الألعاب وأجهزة الفيديو",
                subcategory = "PlayStation",
                priceDzd = 98000,
                isNegotiable = true,
                condition = "NEW",
                wilayaCode = 19,
                wilayaName = "سطيف",
                commune = "سطيف",
                imagesJson = "https://images.unsplash.com/photo-1606813907291-d86efa9b94db?w=800",
                videoUrl = "",
                status = "PUBLISHED",
                rejectionReason = "",
                packageType = "FEATURED",
                publishingFeeDzd = 200,
                isPaid = true,
                isFeatured = true,
                isUrgent = false,
                viewsCount = 420,
                createdAt = now - (6 * 3600 * 1000),
                expiresAt = now + (28L * 24 * 3600 * 1000)
            ),
            ListingEntity(
                id = "list_5",
                userId = "user_karim",
                userName = "كريم وهران لقطع الغيار",
                userPhone = "+213 000 00 00 03",
                isPhoneVisible = false,
                title = "صالون مغربي عصري 7 مقاعد خشب زان فاخر",
                description = "صالون مغربي عصري مصنوع من خشب الزان الصلب، قماش مقاوم للبقع (Anti-tache) لون رمادي وذهبي، بحالة ممتازة كالجديد.",
                categoryId = "furniture",
                categoryNameAr = "الأثاث والأجهزة المنزلية",
                subcategory = "أثاث صالون ومجالس",
                priceDzd = 65000,
                isNegotiable = true,
                condition = "LIKE_NEW",
                wilayaCode = 31,
                wilayaName = "وهران",
                commune = "عين الترك",
                imagesJson = "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=800",
                videoUrl = "",
                status = "PUBLISHED",
                rejectionReason = "",
                packageType = "STANDARD",
                publishingFeeDzd = 100,
                isPaid = true,
                isFeatured = false,
                isUrgent = false,
                viewsCount = 180,
                createdAt = now - (20 * 3600 * 1000),
                expiresAt = now + (26L * 24 * 3600 * 1000)
            ),
            ListingEntity(
                id = "list_6",
                userId = "user_me",
                userName = "محمد أمين دزيري",
                userPhone = "+213 000 00 00 01",
                isPhoneVisible = true,
                title = "مجموعة أدوات بوش Bosch المهنية 18V كاملة",
                description = "صندوق معدات بوش أصلي يضم مثقاب احترافي وصاروخ مع بطاريتين ليثيوم 4Ah وشاحن سريع. مستوردة من ألمانيا بحالة الجديد.",
                categoryId = "tools",
                categoryNameAr = "أدوات العمل والمعدات",
                subcategory = "أدوات كهربائية",
                priceDzd = 38000,
                isNegotiable = true,
                condition = "LIKE_NEW",
                wilayaCode = 16,
                wilayaName = "الجزائر العاصمة",
                commune = "الشراقة",
                imagesJson = "https://images.unsplash.com/photo-1504148455328-c376907d081c?w=800",
                videoUrl = "",
                status = "PUBLISHED",
                rejectionReason = "",
                packageType = "STANDARD",
                publishingFeeDzd = 100,
                isPaid = true,
                isFeatured = false,
                isUrgent = false,
                viewsCount = 295,
                createdAt = now - (2 * 24 * 3600 * 1000),
                expiresAt = now + (28L * 24 * 3600 * 1000)
            ),
            ListingEntity(
                id = "list_7_review",
                userId = "user_karim",
                userName = "كريم وهران لقطع الغيار",
                userPhone = "+213 000 00 00 03",
                isPhoneVisible = true,
                title = "دراجة نارية TMAX 560 موديل 2022 ماشية قليل",
                description = "تي ماكس 560 نظيفة جداً، مع عادم Akrapovic أصلي ووثائق مسجلة ومفتاحين. قيد المراجعة الإدارية.",
                categoryId = "vehicles",
                categoryNameAr = "السيارات والدراجات",
                subcategory = "دراجات نارية",
                priceDzd = 2100000,
                isNegotiable = true,
                condition = "LIKE_NEW",
                wilayaCode = 31,
                wilayaName = "وهران",
                commune = "بئر الجير",
                imagesJson = "https://images.unsplash.com/photo-1558981403-c5f9899a28bc?w=800",
                videoUrl = "",
                status = "UNDER_REVIEW",
                rejectionReason = "",
                packageType = "FEATURED",
                publishingFeeDzd = 200,
                isPaid = true,
                isFeatured = true,
                isUrgent = false,
                viewsCount = 12,
                createdAt = now - (30 * 60 * 1000),
                expiresAt = now + (30L * 24 * 3600 * 1000)
            )
        )

        for (listing in sampleListings) {
            db.listingDao().insertListing(listing)
        }

        // 5. Initial Payment Orders
        val paymentOrder1 = PaymentOrderEntity(
            paymentId = "pay_dz_001",
            userId = "user_yacine",
            listingId = "list_1",
            amount = 200,
            currency = "DZD",
            status = "SUCCESS",
            provider = "EDAHABIA",
            transactionReference = "TXN_EDAHABIA_994821",
            createdAt = now - (3 * 3600 * 1000),
            completedAt = now - (3 * 3600 * 1000)
        )
        db.paymentDao().insertPayment(paymentOrder1)

        val paymentOrder2 = PaymentOrderEntity(
            paymentId = "pay_dz_002",
            userId = "user_karim",
            listingId = "list_2",
            amount = 300,
            currency = "DZD",
            status = "SUCCESS",
            provider = "BARIDIMOB",
            transactionReference = "TXN_BARIDI_772104",
            createdAt = now - (1 * 3600 * 1000),
            completedAt = now - (1 * 3600 * 1000)
        )
        db.paymentDao().insertPayment(paymentOrder2)

        // 6. Initial Reviews
        val review1 = ReviewEntity(
            id = "rev_1",
            sellerId = "user_me",
            buyerId = "user_yacine",
            buyerName = "ياسين سطيف إلكترونيك",
            listingId = "list_prev_1",
            rating = 5,
            comment = "إنسان قمة في الأخلاق والتعامل، السلعة كما في الإعلان تماماً، بارك الله فيك أخي.",
            timestamp = now - (10L * 24 * 3600 * 1000)
        )
        db.reviewDao().insertReview(review1)

        val review2 = ReviewEntity(
            id = "rev_2",
            sellerId = "user_karim",
            buyerId = "user_me",
            buyerName = "محمد أمين دزيري",
            listingId = "list_2",
            rating = 5,
            comment = "تعامل احترافي وسريع، سلعة أصلية ومطابقة للوصف. أنصح بالتعامل معه.",
            timestamp = now - (5L * 24 * 3600 * 1000)
        )
        db.reviewDao().insertReview(review2)

        // 7. Initial Chat & Offers
        val msg1 = ChatMessageEntity(
            id = "chat_1",
            listingId = "list_1",
            senderId = "user_me",
            receiverId = "user_yacine",
            content = "السلام عليكم أخي، هل الهاتف ما زال متوفراً؟ وهل تقبل التوصيل إلى العاصمة؟",
            timestamp = now - (2 * 3600 * 1000),
            isOffer = false,
            offerAmountDzd = 0,
            offerStatus = "NONE"
        )
        db.chatDao().insertMessage(msg1)

        val msg2 = ChatMessageEntity(
            id = "chat_2",
            listingId = "list_1",
            senderId = "user_yacine",
            receiverId = "user_me",
            content = "وعليكم السلام ورحمة الله، نعم أخي متوفر، ويمكن التوصيل مع شركة يالين أو الاستلام يدًا بيد.",
            timestamp = now - (100 * 60 * 1000),
            isOffer = false,
            offerAmountDzd = 0,
            offerStatus = "NONE"
        )
        db.chatDao().insertMessage(msg2)

        val msg3 = ChatMessageEntity(
            id = "chat_3",
            listingId = "list_1",
            senderId = "user_me",
            receiverId = "user_yacine",
            content = "أقدم لك عرض شراء بقيمة 175,000 دج كاش فوراً.",
            timestamp = now - (60 * 60 * 1000),
            isOffer = true,
            offerAmountDzd = 175000,
            offerStatus = "ACCEPTED"
        )
        db.chatDao().insertMessage(msg3)
    }
}
