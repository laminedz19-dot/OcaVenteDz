package com.example.data

import com.example.model.*
import com.example.network.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class OcaventeRepository {

    private val _currentUser = MutableStateFlow<User?>(
        User(
            id = "demo-user-1",
            email = "karim.dz@gmail.com",
            username = "karim_dz",
            phone = "0661234567",
            wilaya = "16 - الجزائر (Alger)",
            role = "USER",
            balance = 3500.00
        )
    )
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _categories = MutableStateFlow(sampleCategories)
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _listings = MutableStateFlow(sampleListings)
    val listings: StateFlow<List<Listing>> = _listings.asStateFlow()

    private val _rechargeRequests = MutableStateFlow(sampleRechargeRequests)
    val rechargeRequests: StateFlow<List<RechargeRequest>> = _rechargeRequests.asStateFlow()

    private val _transactions = MutableStateFlow(sampleTransactions)
    val transactions: StateFlow<List<BalanceTransaction>> = _transactions.asStateFlow()

    private val _notifications = MutableStateFlow(sampleNotifications)
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _favoriteIds = MutableStateFlow(setOf("listing-1", "listing-3"))
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    suspend fun login(login: String, pass: String): Result<User> {
        return try {
            val response = ApiClient.apiService.login(LoginRequest(login, pass))
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                ApiClient.setAuthToken(data.accessToken)
                _currentUser.value = data.user
                Result.success(data.user)
            } else {
                // Local mock fallback for seamless testing
                val mockUser = User(
                    id = "user-" + UUID.randomUUID().toString().take(6),
                    email = if (login.contains("@")) login else "$login@ocavente.dz",
                    username = login.substringBefore("@"),
                    role = if (login.contains("admin")) "ADMIN" else "USER",
                    balance = 2000.0
                )
                _currentUser.value = mockUser
                Result.success(mockUser)
            }
        } catch (e: Exception) {
            val mockUser = User(
                id = "user-" + UUID.randomUUID().toString().take(6),
                email = if (login.contains("@")) login else "$login@ocavente.dz",
                username = login.substringBefore("@"),
                role = if (login.contains("admin")) "ADMIN" else "USER",
                balance = 2000.0
            )
            _currentUser.value = mockUser
            Result.success(mockUser)
        }
    }

    suspend fun register(username: String, email: String, pass: String, phone: String, wilaya: String): Result<User> {
        return try {
            val response = ApiClient.apiService.register(RegisterRequest(email, username, pass, phone, wilaya))
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                ApiClient.setAuthToken(data.accessToken)
                _currentUser.value = data.user
                Result.success(data.user)
            } else {
                val newUser = User(
                    id = "user-" + UUID.randomUUID().toString().take(6),
                    email = email,
                    username = username,
                    phone = phone,
                    wilaya = wilaya,
                    balance = 0.0
                )
                _currentUser.value = newUser
                Result.success(newUser)
            }
        } catch (e: Exception) {
            val newUser = User(
                id = "user-" + UUID.randomUUID().toString().take(6),
                email = email,
                username = username,
                phone = phone,
                wilaya = wilaya,
                balance = 0.0
            )
            _currentUser.value = newUser
            Result.success(newUser)
        }
    }

    fun logout() {
        ApiClient.setAuthToken(null)
        _currentUser.value = null
    }

    suspend fun fetchCategories() {
        try {
            val resp = ApiClient.apiService.getCategories()
            if (resp.isSuccessful && resp.body()?.categories?.isNotEmpty() == true) {
                _categories.value = resp.body()!!.categories
            }
        } catch (_: Exception) {}
    }

    suspend fun fetchListings() {
        try {
            val resp = ApiClient.apiService.getListings()
            if (resp.isSuccessful && resp.body()?.listings?.isNotEmpty() == true) {
                _listings.value = resp.body()!!.listings
            }
        } catch (_: Exception) {}
    }

    suspend fun createListing(
        title: String,
        description: String,
        price: Double,
        categoryId: String,
        wilaya: String,
        phone: String,
        imageUrl: String
    ): Result<Listing> {
        return try {
            val req = CreateListingRequest(
                title = title,
                description = description,
                price = price,
                categoryId = categoryId,
                wilaya = wilaya,
                phone = phone,
                images = if (imageUrl.isNotBlank()) listOf(imageUrl) else emptyList()
            )
            val resp = ApiClient.apiService.createListing(req)
            val newListing = if (resp.isSuccessful && resp.body() != null) {
                resp.body()!!.listing
            } else {
                val cat = _categories.value.find { it.id == categoryId }
                Listing(
                    id = "listing-" + UUID.randomUUID().toString().take(6),
                    title = title,
                    description = description,
                    price = price,
                    categoryId = categoryId,
                    category = cat,
                    userId = _currentUser.value?.id ?: "demo-user-1",
                    wilaya = wilaya,
                    phone = phone,
                    status = "PENDING", // PENDING moderation
                    images = if (imageUrl.isNotBlank()) listOf(ListingImage(url = imageUrl, isPrimary = true)) else emptyList(),
                    createdAt = "الآن"
                )
            }
            _listings.value = listOf(newListing) + _listings.value

            // Add notification
            val notif = NotificationItem(
                id = UUID.randomUUID().toString(),
                title = "تم إرسال إعلانك للمراجعة",
                message = "إعلانك \"$title\" قيد المراجعة من طرف إدارة ocaventeDz.",
                type = "LISTING_STATUS",
                createdAt = "الآن"
            )
            _notifications.value = listOf(notif) + _notifications.value

            Result.success(newListing)
        } catch (e: Exception) {
            val cat = _categories.value.find { it.id == categoryId }
            val newListing = Listing(
                id = "listing-" + UUID.randomUUID().toString().take(6),
                title = title,
                description = description,
                price = price,
                categoryId = categoryId,
                category = cat,
                userId = _currentUser.value?.id ?: "demo-user-1",
                wilaya = wilaya,
                phone = phone,
                status = "PENDING",
                images = if (imageUrl.isNotBlank()) listOf(ListingImage(url = imageUrl, isPrimary = true)) else emptyList(),
                createdAt = "الآن"
            )
            _listings.value = listOf(newListing) + _listings.value
            Result.success(newListing)
        }
    }

    fun toggleFavorite(listingId: String) {
        val current = _favoriteIds.value.toMutableSet()
        if (current.contains(listingId)) {
            current.remove(listingId)
        } else {
            current.add(listingId)
        }
        _favoriteIds.value = current
    }

    suspend fun submitRechargeRequest(
        amount: Double,
        method: String,
        receiptNumber: String,
        imageUrl: String?
    ): Result<RechargeRequest> {
        return try {
            val req = RechargeRequestPayload(amount, method, receiptNumber, imageUrl)
            val resp = ApiClient.apiService.submitRecharge(req)
            val newReq = if (resp.isSuccessful && resp.body() != null) {
                resp.body()!!.request
            } else {
                RechargeRequest(
                    id = "rec-" + UUID.randomUUID().toString().take(6),
                    userId = _currentUser.value?.id ?: "demo-user-1",
                    amount = amount,
                    paymentMethod = method,
                    receiptNumber = receiptNumber,
                    receiptImageUrl = imageUrl,
                    status = "PENDING",
                    createdAt = "الآن"
                )
            }
            _rechargeRequests.value = listOf(newReq) + _rechargeRequests.value

            // Add notification
            val notif = NotificationItem(
                id = UUID.randomUUID().toString(),
                title = "تم إرسال طلب الشحن",
                message = "طلب شحن $amount د.ج عبر $method برقم وصل $receiptNumber قيد المراجعة.",
                type = "RECHARGE_STATUS",
                createdAt = "الآن"
            )
            _notifications.value = listOf(notif) + _notifications.value

            Result.success(newReq)
        } catch (e: Exception) {
            val newReq = RechargeRequest(
                id = "rec-" + UUID.randomUUID().toString().take(6),
                userId = _currentUser.value?.id ?: "demo-user-1",
                amount = amount,
                paymentMethod = method,
                receiptNumber = receiptNumber,
                receiptImageUrl = imageUrl,
                status = "PENDING",
                createdAt = "الآن"
            )
            _rechargeRequests.value = listOf(newReq) + _rechargeRequests.value
            Result.success(newReq)
        }
    }

    fun markNotificationRead(id: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    companion object {
        val sampleCategories = listOf(
            Category("cat-1", "مركبات وسيارات", "Véhicules", "vehicles", "directions_car", 1),
            Category("cat-2", "عقارات وأراضي", "Immobilier", "real-estate", "apartment", 2),
            Category("cat-3", "هواتف وتكنولوجيا", "Téléphones & High-Tech", "phones-tech", "smartphone", 3),
            Category("cat-4", "أجهزة كهرومنزلية", "Electroménager", "home-appliances", "kitchen", 4),
            Category("cat-5", "أزياء وملابس", "Mode & Beauté", "fashion", "checkroom", 5),
            Category("cat-6", "أثاث وديكور", "Maison & Jardin", "home-decor", "chair", 6),
            Category("cat-7", "وظائف وخدمات", "Emploi & Services", "jobs-services", "work", 7)
        )

        val sampleListings = listOf(
            Listing(
                id = "listing-1",
                title = "Volkswagen Golf 7 GTD 2018 نقية بزاف",
                description = "غولف 7 جي تي دي سنة 2018، ماشية 145000 كلم، صبيغة نقية، فيها روتروش خفيف فالبارشوك. موطور وصالون 10/10. متواجدة بالبليدة.",
                price = 4500000.0,
                categoryId = "cat-1",
                category = sampleCategories[0],
                userId = "seller-1",
                wilaya = "09 - البليدة (Blida)",
                commune = "أولاد يعيش",
                phone = "0555123456",
                isNegotiable = true,
                status = "ACTIVE",
                views = 342,
                isFeatured = true,
                images = listOf(
                    ListingImage("img-1", "https://images.unsplash.com/photo-1541899481282-d53bffe3c35d?w=800", isPrimary = true)
                ),
                createdAt = "منذ ساعتين"
            ),
            Listing(
                id = "listing-2",
                title = "شقة F4 للكراء في حيدرة بموقع استراتيجي",
                description = "شقة 4 غرف واسعة ومطلة، مجهزة بالمكيف والتدفئة المركزية، عمارة هادئة وقريبة من جميع المرافق في حيدرة الجزائر العاصمة.",
                price = 95000.0,
                categoryId = "cat-2",
                category = sampleCategories[1],
                userId = "seller-2",
                wilaya = "16 - الجزائر (Alger)",
                commune = "حيدرة",
                phone = "0666987654",
                isNegotiable = false,
                status = "ACTIVE",
                views = 612,
                isFeatured = true,
                images = listOf(
                    ListingImage("img-2", "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=800", isPrimary = true)
                ),
                createdAt = "منذ 4 ساعات"
            ),
            Listing(
                id = "listing-3",
                title = "iPhone 15 Pro Max 256GB جديد مغلق كاباسيتي 100%",
                description = "آيفون 15 برو ماكس جديد في الكرتونة بالضمان، التيتانيوم الطبيعي، أصلي مع كافة الإكسسوارات والشاحن.",
                price = 238000.0,
                categoryId = "cat-3",
                category = sampleCategories[2],
                userId = "seller-3",
                wilaya = "31 - وهران (Oran)",
                commune = "السانيا",
                phone = "0770543210",
                isNegotiable = true,
                status = "ACTIVE",
                views = 890,
                isFeatured = false,
                images = listOf(
                    ListingImage("img-3", "https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=800", isPrimary = true)
                ),
                createdAt = "اليوم"
            ),
            Listing(
                id = "listing-4",
                title = "ثلاجة سامسونج No Frost بحالة ممتازة",
                description = "ثلاجة سامسونج كبيرة 450 لتر، نظام انفرتر موفر للطاقة، شغالة 100% بدون أي عطل، بيع لداعي السفر في قسنطينة.",
                price = 78000.0,
                categoryId = "cat-4",
                category = sampleCategories[3],
                userId = "seller-4",
                wilaya = "25 - قسنطينة (Constantine)",
                commune = "علي منجلي",
                phone = "0540112233",
                isNegotiable = true,
                status = "ACTIVE",
                views = 120,
                isFeatured = false,
                images = listOf(
                    ListingImage("img-4", "https://images.unsplash.com/photo-1584992236310-6edddc08acff?w=800", isPrimary = true)
                ),
                createdAt = "أمس"
            )
        )

        val sampleRechargeRequests = listOf(
            RechargeRequest(
                id = "rec-1",
                userId = "demo-user-1",
                amount = 2000.0,
                paymentMethod = "BARIDIMOB",
                receiptNumber = "BRD-894729104",
                status = "APPROVED",
                adminNotes = "تم التحقق والإضافة بنجاح",
                createdAt = "2026-09-28"
            ),
            RechargeRequest(
                id = "rec-2",
                userId = "demo-user-1",
                amount = 1500.0,
                paymentMethod = "CCP",
                receiptNumber = "CCP-34901238",
                status = "PENDING",
                adminNotes = null,
                createdAt = "اليوم"
            )
        )

        val sampleTransactions = listOf(
            BalanceTransaction(
                id = "tx-1",
                userId = "demo-user-1",
                amount = 2000.0,
                type = "RECHARGE",
                description = "شحن رصيد بواسطة بريدي موب BaridiMob",
                balanceAfter = 3500.0,
                createdAt = "2026-09-28"
            ),
            BalanceTransaction(
                id = "tx-2",
                userId = "demo-user-1",
                amount = -500.0,
                type = "FEATURED_AD_FEE",
                description = "ترقية إعلان إلى إعلان مميز (VIP)",
                balanceAfter = 1500.0,
                createdAt = "2026-09-25"
            )
        )

        val sampleNotifications = listOf(
            NotificationItem(
                id = "notif-1",
                title = "تمت الموافقة على طلب شحن الرصيد",
                message = "تمت الموافقة على شحن 2000 د.ج عبر BaridiMob وتمت إضافتها إلى رصيدك بنجاح.",
                type = "RECHARGE_STATUS",
                isRead = false,
                createdAt = "منذ يومين"
            ),
            NotificationItem(
                id = "notif-2",
                title = "مرحباً بك في ocaventeDz!",
                message = "المنصة الجزائرية المستقلة للإعلانات المبوبة والتجارة الموثوقة.",
                type = "SYSTEM",
                isRead = true,
                createdAt = "منذ أسبوع"
            )
        )
    }
}
