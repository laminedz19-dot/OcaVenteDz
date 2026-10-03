package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.FavoriteEntity
import com.example.data.local.ListingEntity
import com.example.data.local.OrderEntity
import com.example.data.local.PaymentOrderEntity
import com.example.data.local.PlatformSettingsEntity
import com.example.data.local.ReportEntity
import com.example.data.local.ReviewEntity
import com.example.data.local.UserEntity
import com.example.data.local.WalletEntity
import com.example.data.local.WalletTransactionEntity
import com.example.data.local.TopUpRequestEntity
import com.example.data.repository.MarketplaceRepository
import com.example.data.remote.supabase.SupabaseSessionStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import android.util.Log
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class MarketplaceViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionPrefs = application.getSharedPreferences("ocavente_user_session", Context.MODE_PRIVATE)

    fun getSavedUserId(): String {
        return sessionPrefs.getString("logged_in_user_id", "") ?: ""
    }

    fun saveLoggedInUserId(userId: String) {
        if (userId.isNotBlank()) {
            sessionPrefs.edit().putString("logged_in_user_id", userId).apply()
        }
    }

    fun clearSavedUserId() {
        sessionPrefs.edit().remove("logged_in_user_id").apply()
    }

    // Declare state before init blocks: coroutines launched from init may start immediately.
    private val _currentUserId = MutableStateFlow(
        if (SupabaseSessionStore.hasSession()) SupabaseSessionStore.userId().orEmpty() else ""
    )
    val currentUserId = _currentUserId.asStateFlow()

    private val db = AppDatabase.getDatabase(application)
    val repository = MarketplaceRepository(db)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (db.listingDao().getCount() == 0) {
                    com.example.data.local.InitialDataSeeder.seed(db)
                }
            } catch (e: Exception) {
                Log.w("MarketplaceViewModel", "Initial seeding check error: ${e.message}")
            }
        }
        viewModelScope.launch {
            val currentSettings = db.settingsDao().getSettingsDirect()
            if (currentSettings == null || (currentSettings.standardAdFeeDzd == 100 && currentSettings.featuredAdFeeDzd == 200)) {
                repository.updatePlatformSettings(
                    (currentSettings ?: PlatformSettingsEntity()).copy(
                        standardAdFeeDzd = 400,
                        featuredAdFeeDzd = 600,
                        urgentAdFeeDzd = 1000
                    )
                )
            }
            try {
                repository.syncPlatformSettingsFromCloud()
            } catch (_: Exception) {}
            try {
                repository.syncListingsFromCloud()
            } catch (_: Exception) {}

            restoreSavedSession()
        }
    }

    private suspend fun restoreSavedSession() {
        if (!SupabaseSessionStore.hasSession()) {
            clearSavedUserId()
            _currentUserId.value = ""
            return
        }
        val result = repository.authService.restoreSession()
        if (result.isFailure) {
            val err = result.exceptionOrNull()
            Log.w("MarketplaceViewModel", "Session restore deferred: ${err?.message}")
            if (err?.message?.contains("انتهت جلسة", ignoreCase = true) == true || !SupabaseSessionStore.hasSession()) {
                clearSavedUserId()
                _currentUserId.value = ""
            }
            return
        }
        val uid = result.getOrNull()?.uid?.takeIf { it.isNotBlank() && it != "deleted" }
        if (uid == null) {
            clearSavedUserId()
            _currentUserId.value = ""
            return
        }
        repository.supabaseClient.getAllUsers().getOrNull()
            ?.firstOrNull { it.id == uid }
            ?.let { repository.saveUser(it) }
        saveLoggedInUserId(uid)
        _currentUserId.value = uid
    }

    // Current User & Session

    // Admin state remains disabled until a server-backed identity provider is configured.
    data class AdminAuditLog(
        val id: String = UUID.randomUUID().toString(),
        val action: String,
        val details: String,
        val timestamp: Long = System.currentTimeMillis(),
        val status: String = "SUCCESS"
    )

    private val _isAdminSessionActive = MutableStateFlow(false)
    val isAdminSessionActive: StateFlow<Boolean> = _isAdminSessionActive.asStateFlow()

    private val _adminAuditLogs = MutableStateFlow<List<AdminAuditLog>>(
        listOf(
            AdminAuditLog(
                action = "تهيئة البوابة المشفرة",
                details = "تم تشغيل نظام أمان OcaVenteDz وعزل صلاحيات الإشراف بنجاح"
            ),
            AdminAuditLog(
                action = "تحديث السياسات",
                details = "تفعيل المراقبة الفورية للسلع المحظورة والاحتيال الرقمي"
            )
        )
    )
    val adminAuditLogs: StateFlow<List<AdminAuditLog>> = _adminAuditLogs.asStateFlow()

    fun authenticateAdmin(): Boolean {
        emitMessage("دخول الإدارة غير متاح قبل إعداد مصادقة خادمية حقيقية.")
        return false
    }

    fun exitAdminSession() {
        _isAdminSessionActive.value = false
        _currentUserId.value = ""
        logAdminAction("إنهاء جلسة الإشراف", "تم إغلاق وحدة التحكم الإدارية والعودة إلى واجهة المتجر العامة")
        emitMessage("تم إغلاق جلسة الإدارة وتأمين البوابة بنجاح")
    }

    fun logAdminAction(action: String, details: String) {
        val log = AdminAuditLog(action = action, details = details)
        _adminAuditLogs.value = listOf(log) + _adminAuditLogs.value
    }

    // Language ("ar" / "fr")
    private val _language = MutableStateFlow("ar")
    val language = _language.asStateFlow()

    // Image Upload Progress Flow
    private val _imageUploadProgress = MutableStateFlow<Float?>(null)
    val imageUploadProgress: StateFlow<Float?> = _imageUploadProgress.asStateFlow()

    // Notification / Toast message event
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent.asSharedFlow()

    // User & Data Flows
    val currentUser: StateFlow<UserEntity?> = _currentUserId.combine(repository.getAllUsers()) { id, users ->
        users.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val publishedListings: StateFlow<List<ListingEntity>> = repository.getPublishedListings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminListings: StateFlow<List<ListingEntity>> = repository.getAllListingsAdmin()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myListings: StateFlow<List<ListingEntity>> = _currentUserId.combine(repository.getAllListingsAdmin()) { id, listings ->
        listings.filter { it.userId == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentWallet: StateFlow<WalletEntity?> = _currentUserId.flatMapLatest { id ->
        repository.getWallet(id)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val wallet: StateFlow<WalletEntity?> = _currentUserId.flatMapLatest { id ->
        repository.getWallet(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val walletTransactions: StateFlow<List<WalletTransactionEntity>> = _currentUserId.flatMapLatest { id ->
        repository.getWalletTransactions(id)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    sealed interface TopUpSyncState {
        data object Loading : TopUpSyncState
        data class Success(val requests: List<TopUpRequestEntity>) : TopUpSyncState
        data class Error(val message: String) : TopUpSyncState
    }

    val userTopUpSyncState: StateFlow<TopUpSyncState> = _currentUserId.flatMapLatest { id ->
        if (id.isBlank() || id == "deleted" || id == "admin_super") {
            kotlinx.coroutines.flow.flowOf(TopUpSyncState.Success(emptyList()))
        } else {
            repository.getUserTopUpRequestsSync(id).map { res ->
                if (res.isSuccess) {
                    TopUpSyncState.Success(res.getOrNull().orEmpty())
                } else {
                    val errMsg = res.exceptionOrNull()?.message ?: "خطأ في مزامنة طلبات الشحن من السحابة"
                    TopUpSyncState.Error(errMsg)
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TopUpSyncState.Loading)

    val userTopUpRequests: StateFlow<List<TopUpRequestEntity>> = _currentUserId.flatMapLatest { id ->
        if (id.isBlank() || id == "deleted") {
            repository.getAllTopUpRequests()
        } else {
            repository.getUserTopUpRequests(id)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTopUpRequests: StateFlow<List<TopUpRequestEntity>> = repository.getAllTopUpRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val platformSettings: StateFlow<PlatformSettingsEntity> = repository.getPlatformSettings()
        .combine(MutableStateFlow(PlatformSettingsEntity())) { settings, default ->
            settings ?: default
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlatformSettingsEntity())

    val allUsers: StateFlow<List<UserEntity>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPayments: StateFlow<List<PaymentOrderEntity>> = repository.getAllPayments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReports: StateFlow<List<ReportEntity>> = repository.getAllReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<FavoriteEntity>> = _currentUserId.flatMapLatest { id ->
        repository.getFavorites(id)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Orders StateFlows & Management ---
    sealed interface OrderListUiState {
        data object Loading : OrderListUiState
        data class Success(val orders: List<OrderEntity>) : OrderListUiState
        data class Error(val message: String) : OrderListUiState
    }

    val myPurchases: StateFlow<OrderListUiState> = _currentUserId.flatMapLatest { id ->
        if (id.isBlank() || id == "deleted") {
            kotlinx.coroutines.flow.flowOf(OrderListUiState.Success(emptyList()))
        } else {
            repository.getUserOrdersFlow(id).map { res ->
                if (res.isSuccess) {
                    val list = res.getOrNull().orEmpty()
                    viewModelScope.launch { repository.syncOrdersLocally(list) }
                    OrderListUiState.Success(list)
                } else {
                    OrderListUiState.Error(res.exceptionOrNull()?.message ?: "خطأ في تحميل طلبات الشراء")
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OrderListUiState.Loading)

    val mySalesOrders: StateFlow<OrderListUiState> = _currentUserId.flatMapLatest { id ->
        if (id.isBlank() || id == "deleted") {
            kotlinx.coroutines.flow.flowOf(OrderListUiState.Success(emptyList()))
        } else {
            repository.getSellerOrdersFlow(id).map { res ->
                if (res.isSuccess) {
                    val list = res.getOrNull().orEmpty()
                    viewModelScope.launch { repository.syncOrdersLocally(list) }
                    OrderListUiState.Success(list)
                } else {
                    OrderListUiState.Error(res.exceptionOrNull()?.message ?: "خطأ في تحميل الطلبيات الواردة")
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OrderListUiState.Loading)

    fun placeOrder(
        listing: ListingEntity,
        quantity: Int,
        buyerName: String,
        buyerPhone: String,
        buyerWilaya: String,
        buyerCommune: String,
        buyerAddress: String,
        deliveryFeeDzd: Int = 0,
        paymentMethod: String = "COD",
        buyerNotes: String = "",
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val uid = _currentUserId.value
        if (uid.isBlank()) {
            onError("يرجى تسجيل الدخول أولاً لإتمام الطلب.")
            return
        }
        val unitPrice = listing.priceDzd.toInt()
        val total = (unitPrice * quantity) + deliveryFeeDzd
        val orderNumber = "SQ-${System.currentTimeMillis().toString().takeLast(6)}"

        val order = OrderEntity(
            id = UUID.randomUUID().toString(),
            orderNumber = orderNumber,
            listingId = listing.id,
            listingTitle = listing.title,
            listingImageUrl = listing.imagesJson.split(",").firstOrNull().orEmpty(),
            sellerId = listing.userId,
            sellerName = listing.userName,
            sellerPhone = listing.userPhone,
            buyerId = uid,
            buyerName = buyerName.trim(),
            buyerPhone = buyerPhone.trim(),
            buyerWilaya = buyerWilaya,
            buyerCommune = buyerCommune,
            buyerAddress = buyerAddress.trim(),
            quantity = quantity,
            unitPriceDzd = unitPrice,
            deliveryFeeDzd = deliveryFeeDzd,
            totalAmountDzd = total,
            paymentMethod = paymentMethod,
            isPaid = paymentMethod == "WALLET",
            status = "PENDING",
            buyerNotes = buyerNotes.trim(),
            trackingNumber = "",
            statusNote = "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            val res = repository.createOrder(order)
            if (res.isSuccess) {
                emitMessage("تم تأكيد وإرسال طلب الشراء بنجاح! رقم الطلب: $orderNumber")
                onSuccess(res.getOrNull() ?: order.id)
            } else {
                val err = res.exceptionOrNull()?.message ?: "فشل في تسجيل الطلب"
                onError(err)
            }
        }
    }

    fun updateOrderStatus(
        orderId: String,
        newStatus: String,
        statusNote: String = "",
        trackingNumber: String? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.updateOrderStatus(orderId, newStatus, statusNote, trackingNumber)
            if (res.isSuccess) {
                emitMessage("تم تحديث حالة الطلب إلى: ${newStatus}")
                onSuccess()
            } else {
                val err = res.exceptionOrNull()?.message ?: "فشل في تحديث حالة الطلب"
                onError(err)
            }
        }
    }

    // Search & Filter State
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<String?>(null)
    val selectedWilaya = MutableStateFlow<Int?>(null)
    val selectedCommune = MutableStateFlow<String?>(null)
    val selectedCondition = MutableStateFlow<String?>(null)
    val minPrice = MutableStateFlow<Long?>(null)
    val maxPrice = MutableStateFlow<Long?>(null)
    val onlyNegotiable = MutableStateFlow(false)
    val sortBy = MutableStateFlow("NEWEST") // "NEWEST", "PRICE_ASC", "PRICE_DESC"

    // Search Result
    val filteredListings: StateFlow<List<ListingEntity>> = combine(
        publishedListings,
        searchQuery,
        selectedCategory,
        selectedWilaya,
        selectedCondition,
        minPrice,
        maxPrice,
        onlyNegotiable,
        sortBy
    ) { params ->
        @Suppress("UNCHECKED_CAST")
        val list = params[0] as List<ListingEntity>
        val query = params[1] as String
        val cat = params[2] as String?
        val wilaya = params[3] as Int?
        val condition = params[4] as String?
        val minP = params[5] as Long?
        val maxP = params[6] as Long?
        val neg = params[7] as Boolean
        val sort = params[8] as String

        list.filter { listing ->
            val matchesQuery = query.isBlank() ||
                    listing.title.contains(query, ignoreCase = true) ||
                    listing.description.contains(query, ignoreCase = true) ||
                    listing.commune.contains(query, ignoreCase = true) ||
                    listing.wilayaName.contains(query, ignoreCase = true)

            val matchesCat = cat == null || listing.categoryId == cat
            val matchesWilaya = wilaya == null || listing.wilayaCode == wilaya
            val matchesCondition = condition == null || listing.condition == condition
            val matchesMinPrice = minP == null || listing.priceDzd >= minP
            val matchesMaxPrice = maxP == null || listing.priceDzd <= maxP
            val matchesNeg = !neg || listing.isNegotiable

            matchesQuery && matchesCat && matchesWilaya && matchesCondition && matchesMinPrice && matchesMaxPrice && matchesNeg
        }.let { filtered ->
            when (sort) {
                "PRICE_ASC" -> filtered.sortedBy { it.priceDzd }
                "PRICE_DESC" -> filtered.sortedByDescending { it.priceDzd }
                else -> filtered.sortedWith(compareByDescending<ListingEntity> { it.isUrgent }.thenByDescending { it.isFeatured }.thenByDescending { it.createdAt })
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setLanguage(lang: String) {
        _language.value = lang
    }

    fun normalizeAlgerianPhone(input: String): String {
        var digits = input.filter { it.isDigit() }
        if (input.trim().startsWith("+")) {
            if (digits.startsWith("213")) {
                digits = "0" + digits.removePrefix("213")
            }
        } else if (digits.startsWith("00213")) {
            digits = "0" + digits.removePrefix("00213")
        } else if (digits.startsWith("213") && digits.length == 11) {
            digits = "0" + digits.removePrefix("213")
        } else if ((digits.startsWith("5") || digits.startsWith("6") || digits.startsWith("7")) && digits.length == 9) {
            digits = "0$digits"
        }
        return digits
    }

    fun registerUser(
        name: String,
        phone: String,
        email: String = "",
        wilaya: String,
        commune: String,
        password: String = "",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanName = name.trim()
        val normalizedPhone = normalizeAlgerianPhone(phone)
        val cleanWilaya = wilaya.trim()
        val cleanCommune = commune.trim()

        when {
            cleanName.length < 2 -> { onError("يرجى إدخال الاسم الكامل."); return }
            normalizedPhone.length != 10 || !normalizedPhone.startsWith("0") -> {
                onError("يرجى إدخال رقم هاتف جزائري صحيح (مثال: 0555123456 أو 06/07)."); return
            }
            password.length < 6 -> { onError("يجب أن تتكون كلمة المرور من 6 أحرف على الأقل."); return }
            cleanWilaya.isEmpty() || cleanCommune.isEmpty() -> { onError("يرجى اختيار الولاية والبلدية."); return }
        }

        viewModelScope.launch {
            try {
                val authPhone = "+213" + normalizedPhone.removePrefix("0")
                val authResult = repository.authService.registerWithPhone(authPhone, password, cleanName)
                if (authResult.isFailure) {
                    val errMsg = authResult.exceptionOrNull()?.message ?: "فشل تسجيل الحساب عبر الخدمة السحابية."
                    withContext(Dispatchers.Main) { onError(errMsg) }
                    return@launch
                }

                val authUser = authResult.getOrNull()
                val realUid = authUser?.uid
                if (realUid.isNullOrBlank()) {
                    withContext(Dispatchers.Main) { onError("تعذر الحصول على معرّف حساب حقيقي من الخدمة السحابية.") }
                    return@launch
                }

                val newUser = UserEntity(
                    id = realUid,
                    phone = normalizedPhone,
                    email = "",
                    name = cleanName,
                    avatarUrl = "",
                    wilaya = cleanWilaya,
                    commune = cleanCommune,
                    bio = "عضو في OcaVenteDz.",
                    sellerRating = 5.0,
                    reviewsCount = 0,
                    adsCount = 0,
                    createdAt = System.currentTimeMillis(),
                    isVerified = false,
                    verificationRequested = false,
                    isBanned = false,
                    role = "USER"
                )
                repository.saveUser(newUser)
                repository.createEmptyWallet(realUid)
                saveLoggedInUserId(realUid)
                _currentUserId.value = realUid
                emitMessage("تم إنشاء الحساب بنجاح. مرحبًا بك في OcaVenteDz!")
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("MarketplaceViewModel", "Error registering user: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "حدث خطأ غير متوقع. يرجى المحاولة مرة أخرى.")
                }
            }
        }
    }

    fun loginUser(
        identifier: String,
        password: String = "",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanIdentifier = identifier.trim()
        if (cleanIdentifier.isEmpty()) {
            onError("يرجى إدخال رقم الهاتف.")
            return
        }
        if (password.isBlank()) {
            onError("يرجى إدخال كلمة المرور.")
            return
        }

        viewModelScope.launch {
            val normalizedPhone = normalizeAlgerianPhone(cleanIdentifier)
            val isPhoneLogin = normalizedPhone.length == 10 && normalizedPhone.startsWith("0")
            val loginTarget = if (isPhoneLogin) "" else if (android.util.Patterns.EMAIL_ADDRESS.matcher(cleanIdentifier.lowercase()).matches()) {
                cleanIdentifier.lowercase()
            } else {
                onError("يرجى إدخال رقم هاتف جزائري صحيح (مثال: 0555123456 أو 06/07).")
                return@launch
            }
            val authResult = if (isPhoneLogin) {
                repository.authService.loginWithPhone("+213" + normalizedPhone.removePrefix("0"), password)
            } else {
                repository.authService.loginWithEmail(loginTarget, password)
            }
            if (authResult.isFailure) {
                val errMsg = authResult.exceptionOrNull()?.message ?: "رقم الهاتف أو كلمة المرور غير صحيحة."
                withContext(Dispatchers.Main) { onError(errMsg) }
                return@launch
            }

            val authUser = authResult.getOrNull()
            val uid = authUser?.uid
            if (uid.isNullOrBlank()) {
                withContext(Dispatchers.Main) { onError("تعذر التحقق من جلسة المستخدم.") }
                return@launch
            }

            val localUser = repository.getUserDirect(uid) ?: UserEntity(
                id = uid,
                phone = if (normalizedPhone.length == 10) normalizedPhone else "",
                email = if (!isPhoneLogin) cleanIdentifier else "",
                name = authUser.displayName ?: "مستخدم OcaVenteDz",
                avatarUrl = "",
                wilaya = "الجزائر",
                commune = "الجزائر الوسطى",
                bio = "عضو في OcaVenteDz",
                sellerRating = 5.0,
                reviewsCount = 0,
                adsCount = 0,
                createdAt = System.currentTimeMillis(),
                isVerified = false,
                verificationRequested = false,
                isBanned = false,
                role = "USER"
            )
            if (localUser.isBanned) {
                withContext(Dispatchers.Main) {
                    onError("هذا الحساب موقوف حاليًا. يرجى التواصل مع الإدارة.")
                }
                return@launch
            }
            repository.saveUser(localUser)
            saveLoggedInUserId(uid)
            _currentUserId.value = uid
            emitMessage("تم تسجيل الدخول بنجاح. مرحبًا ${localUser.name}!")
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    fun logoutUser(onLoggedOut: () -> Unit = {}) {
        clearSavedUserId()
        repository.authService.signOut()
        _currentUserId.value = ""
        emitMessage("تم تسجيل الخروج بنجاح.")
        onLoggedOut()
    }

    fun requestPasswordReset(
        identifier: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanIdentifier = identifier.trim()
        if (cleanIdentifier.isEmpty()) {
            onError("يرجى إدخال رقم الهاتف.")
            return
        }

        val normalizedPhone = normalizeAlgerianPhone(cleanIdentifier)
        if (normalizedPhone.length == 10 && normalizedPhone.startsWith("0")) {
            onError("استعادة كلمة المرور عبر رقم الهاتف ستتوفر عبر رمز SMS قريبًا. استخدم البريد الإلكتروني إن كان مرتبطًا بالحساب.")
            return
        }
        val targetEmail = if (android.util.Patterns.EMAIL_ADDRESS.matcher(cleanIdentifier.lowercase()).matches()) {
            cleanIdentifier.lowercase()
        } else {
            onError("يرجى إدخال بريد إلكتروني صحيح لاستعادة كلمة المرور.")
            return
        }

        viewModelScope.launch {
            val authResult = repository.authService.sendPasswordReset(targetEmail)
            if (authResult.isSuccess) {
                withContext(Dispatchers.Main) {
                    onSuccess("تم تقديم طلب استعادة كلمة المرور للحساب المرتبط برقم الهاتف بنجاح.")
                }
            } else {
                val errMsg = authResult.exceptionOrNull()?.message ?: "فشل تقديم طلب استعادة كلمة المرور."
                withContext(Dispatchers.Main) {
                    onError(errMsg)
                }
            }
        }
    }

    fun updateCurrentUserProfile(
        name: String,
        phone: String,
        email: String,
        wilaya: String,
        commune: String,
        bio: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanName = name.trim()
        val cleanPhone = phone.trim()
        val cleanEmail = email.trim().lowercase()
        if (cleanName.length < 2) { onError("يرجى إدخال الاسم الكامل."); return }
        if (cleanPhone.length < 9) { onError("يرجى إدخال رقم هاتف صحيح."); return }
        if (cleanEmail.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            onError("صيغة البريد الإلكتروني غير صحيحة.")
            return
        }

        viewModelScope.launch {
            val current = repository.getUserDirect(_currentUserId.value)
                ?: run { onError("تعذر العثور على الحساب الحالي."); return@launch }
            val duplicatePhone = repository.findUserByPhoneOrEmail(cleanPhone)
            val duplicateEmail = if (cleanEmail.isNotEmpty()) repository.findUserByPhoneOrEmail(cleanEmail) else null
            if ((duplicatePhone != null && duplicatePhone.id != current.id) ||
                (duplicateEmail != null && duplicateEmail.id != current.id)) {
                onError("رقم الهاتف أو البريد الإلكتروني مستخدم من حساب آخر.")
                return@launch
            }
            repository.updateUser(current.copy(
                name = cleanName,
                phone = cleanPhone,
                email = cleanEmail,
                wilaya = wilaya.trim().ifBlank { current.wilaya },
                commune = commune.trim().ifBlank { current.commune },
                bio = bio.trim()
            ))
            emitMessage("تم تحديث الملف الشخصي بنجاح.")
            onSuccess()
        }
    }

    fun changePassword(
        currentPassword: String,
        newPassword: String,
        confirmation: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        when {
            currentPassword.isBlank() -> { onError("يرجى إدخال كلمة المرور الحالية."); return }
            newPassword.length < 6 -> { onError("يجب أن تتكون كلمة المرور الجديدة من 6 أحرف على الأقل."); return }
            newPassword != confirmation -> { onError("تأكيد كلمة المرور غير مطابق."); return }
        }

        viewModelScope.launch {
            val result = repository.authService.changeCurrentPassword(currentPassword, newPassword)
            result.onSuccess {
                withContext(Dispatchers.Main) { onSuccess("تم تغيير كلمة المرور بنجاح.") }
            }.onFailure { err ->
                withContext(Dispatchers.Main) { onError(err.message ?: "فشل تغيير كلمة المرور.") }
            }
        }
    }

    fun socialAuthUnavailable(provider: String) {
        emitMessage("تسجيل الدخول عبر $provider غير متاح حالياً؛ يرجى استخدام البريد الإلكتروني.")
    }

    fun resetFilters() {
        searchQuery.value = ""
        selectedCategory.value = null
        selectedWilaya.value = null
        selectedCommune.value = null
        selectedCondition.value = null
        minPrice.value = null
        maxPrice.value = null
        onlyNegotiable.value = false
        sortBy.value = "NEWEST"
    }

    fun toggleFavorite(listingId: String, currentFav: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(_currentUserId.value, listingId, currentFav)
            emitMessage(if (!currentFav) "تمت إضافة الإعلان إلى المفضلة" else "تمت إزالة الإعلان من المفضلة")
        }
    }

    fun deleteCurrentAccount() {
        val uid = _currentUserId.value
        clearSavedUserId()
        viewModelScope.launch {
            if (uid.isNotBlank() && uid != "deleted") {
                repository.deleteUserData(uid)
            }
            repository.authService.signOut()
            _currentUserId.value = ""
            emitMessage("تم حذف بيانات الحساب بنجاح.")
        }
    }

    // Top up wallet
    fun topUpWallet(amount: Int, provider: String, reference: String = "") {
        emitMessage("يرجى تقديم طلب الشحن وإرفاق وصل التحويل للمراجعة والاعتماد.")
    }

    /**
     * Reads the current user's balance directly from قاعدة البيانات and returns it via callback.
     */
    fun getCurrentUserBalance(onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val uid = _currentUserId.value
            val res = repository.getCurrentUserBalance(uid)
            val balance = res.getOrDefault(0)
            withContext(Dispatchers.Main) {
                onResult(balance)
            }
        }
    }

    fun submitTopUpRequest(
        amount: Int,
        provider: String,
        reference: String,
        receiptImageUri: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val uid = _currentUserId.value
        if (uid.isBlank() || uid == "deleted") {
            val msg = "جلسة تسجيل الدخول غير صالحة. يرجى تسجيل الدخول من جديد."
            onError(msg)
            emitMessage(msg)
            return
        }
        if (amount < 200) {
            val msg = "الحد الأدنى للشحن هو 200 دج."
            onError(msg)
            emitMessage(msg)
            return
        }
        if (receiptImageUri.isBlank() && reference.isBlank()) {
            val msg = "يرجى إرفاق صورة الوصل أو إدخال رقم مرجع التحويل."
            onError(msg)
            emitMessage(msg)
            return
        }

        emitMessage("جاري إرسال طلب شحن الرصيد وحفظ الوصل...")

        viewModelScope.launch {
            try {
                val result = repository.submitTopUpRequest(
                    context = getApplication(),
                    amount = amount,
                    provider = provider,
                    reference = reference,
                    receiptImageUriString = receiptImageUri
                )
                result.onSuccess { msg ->
                    emitMessage(msg)
                    withContext(Dispatchers.Main) { onSuccess() }
                }.onFailure { err ->
                    val errorMsg = err.message ?: "فشل إرسال طلب الشحن"
                    emitMessage(errorMsg)
                    withContext(Dispatchers.Main) { onError(errorMsg) }
                }
            } catch (t: Throwable) {
                val errorMsg = t.message ?: "حدث خطأ أثناء معالجة الطلب"
                emitMessage(errorMsg)
                withContext(Dispatchers.Main) { onError(errorMsg) }
            }
        }
    }

    fun resolveReceiptUrl(storagePathOrUrl: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val resolved = repository.supabaseClient.createSignedReceiptUrl(storagePathOrUrl).getOrNull()
            withContext(Dispatchers.Main) { onResult(resolved) }
        }
    }

    fun approveTopUpRequest(requestId: String, adminNote: String = "") {
        viewModelScope.launch {
            val result = repository.approveTopUpRequest(requestId, adminNote)
            result.onSuccess { msg ->
                emitMessage(msg)
            }.onFailure { err ->
                emitMessage(err.message ?: "فشلت العملية")
            }
        }
    }

    fun rejectTopUpRequest(requestId: String, reason: String = "") {
        viewModelScope.launch {
            val result = repository.rejectTopUpRequest(requestId, reason)
            result.onSuccess { msg ->
                emitMessage(msg)
            }.onFailure { err ->
                emitMessage(err.message ?: "فشلت العملية")
            }
        }
    }

    // Publish Ad Flow with Fee Payment
    fun createAndPublishListing(
        title: String,
        description: String,
        categoryId: String,
        categoryNameAr: String,
        subcategory: String,
        priceDzd: Long,
        isNegotiable: Boolean,
        condition: String,
        wilayaCode: Int,
        wilayaName: String,
        commune: String,
        images: List<String>,
        videoUrl: String,
        packageType: String, // "STANDARD", "FEATURED", "URGENT"
        paymentMethod: String, // "WALLET", "EDAHABIA", "CIB", "BARIDIMOB"
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val user = repository.getUserDirect(_currentUserId.value) ?: UserEntity(
                id = _currentUserId.value,
                phone = "+213 000 00 00 01",
                email = "user-demo@ocaventedz.invalid",
                name = "مستخدم OcaVenteDz",
                avatarUrl = "",
                wilaya = wilayaName,
                commune = commune,
                bio = "",
                sellerRating = 5.0,
                reviewsCount = 0,
                adsCount = 1,
                createdAt = System.currentTimeMillis(),
                isVerified = false,
                verificationRequested = false,
                isBanned = false,
                role = "USER"
            )

            val listingId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            var imageUploadError: String? = null
            val uploadedImages = images.mapIndexed { index, imgStr ->
                if (imgStr.startsWith("content://") || imgStr.startsWith("file://")) {
                    try {
                        val uploadRes = repository.supabaseClient.uploadListingImage(
                            getApplication(), user.id, Uri.parse(imgStr), "listing_${listingId}_$index"
                        )
                        if (uploadRes.isSuccess) uploadRes.getOrNull().orEmpty()
                        else {
                            imageUploadError = uploadRes.exceptionOrNull()?.message ?: "فشل رفع صورة الإعلان"
                            ""
                        }
                    } catch (error: Exception) {
                        imageUploadError = error.message ?: "فشل رفع صورة الإعلان"
                        ""
                    }
                } else {
                    imgStr
                }
            }
            if (imageUploadError != null) {
                emitMessage(imageUploadError!!)
                withContext(Dispatchers.Main) { onError(imageUploadError!!) }
                return@launch
            }
            _imageUploadProgress.value = null

            val draftListing = ListingEntity(
                id = listingId,
                userId = user.id,
                userName = user.name,
                userPhone = user.phone,
                isPhoneVisible = true,
                title = title,
                description = description,
                categoryId = categoryId,
                categoryNameAr = categoryNameAr,
                subcategory = subcategory,
                priceDzd = priceDzd,
                isNegotiable = isNegotiable,
                condition = condition,
                wilayaCode = wilayaCode,
                wilayaName = wilayaName,
                commune = commune,
                imagesJson = if (uploadedImages.isNotEmpty()) uploadedImages.joinToString(",") else "https://images.unsplash.com/photo-1526738549149-8e07eca6c147?w=800",
                videoUrl = videoUrl,
                status = "PAYMENT_PENDING",
                rejectionReason = "",
                packageType = packageType,
                publishingFeeDzd = 0,
                isPaid = false,
                isFeatured = packageType != "STANDARD",
                isUrgent = packageType == "URGENT",
                viewsCount = 0,
                createdAt = now,
                expiresAt = now + (30L * 24 * 3600 * 1000)
            )

            repository.saveListing(draftListing)

            // Execute Payment
            val payResult = repository.processAdPayment(listingId, user.id, packageType, paymentMethod)
            payResult.onSuccess {
                val updatedListing = repository.getListingDirect(listingId)
                val statusText = if (updatedListing?.status == "PUBLISHED") "وتم نشره مباشرة للعامة!" else "وهو قيد المراجعة الإدارية."
                emitMessage("تم دفع رسوم الإعلان بنجاح $statusText")
                onSuccess(listingId)
            }.onFailure { err ->
                repository.updateListingStatus(listingId, "PAYMENT_FAILED", "فشل الدفع: " + err.message)
                val errorMsg = "تعذر إتمام الدفع: " + (err.message ?: "خطأ غير معروف")
                emitMessage(errorMsg)
                withContext(Dispatchers.Main) {
                    onError(errorMsg)
                }
            }
        }
    }

    suspend fun uploadAdImages(vararg unused: Any): Result<List<String>> = Result.failure(IllegalStateException("رفع الصور المتعدد غير متاح حالياً."))

    suspend fun deleteAdImage(imageStoragePathOrUrl: String): Result<Unit> =
        repository.supabaseClient.deleteListingMedia(imageStoragePathOrUrl)

    suspend fun deleteAllAdImages(listingId: String): Result<Int> {
        val images = repository.getListingDirect(listingId)?.imagesJson.orEmpty()
            .split(",").map(String::trim).filter(String::isNotBlank)
        var deleted = 0
        images.forEach { if (deleteAdImage(it).isSuccess) deleted++ }
        return Result.success(deleted)
    }

    // Chat Actions
    fun sendMessage(listingId: String, receiverId: String, content: String) {
        viewModelScope.launch {
            repository.sendMessage(listingId, _currentUserId.value, receiverId, content)
        }
    }

    fun sendPriceOffer(listingId: String, receiverId: String, offerAmount: Long) {
        viewModelScope.launch {
            val content = "أقدم لك عرض شراء بقيمة ${String.format("%,d", offerAmount)} دج"
            repository.sendMessage(listingId, _currentUserId.value, receiverId, content, isOffer = true, offerAmount = offerAmount)
            emitMessage("تم إرسال عرض السعر للبائع")
        }
    }

    fun respondToOffer(messageId: String, accept: Boolean) {
        viewModelScope.launch {
            val status = if (accept) "ACCEPTED" else "REJECTED"
            repository.updateOfferStatus(messageId, status)
            emitMessage(if (accept) "تم قبول العرض!" else "تم رفض العرض.")
        }
    }

    // Reviews
    fun submitReview(sellerId: String, listingId: String, rating: Int, comment: String) {
        viewModelScope.launch {
            val user = repository.getUserDirect(_currentUserId.value)
            val buyerName = user?.name ?: "مشتري"
            val res = repository.addReview(sellerId, _currentUserId.value, buyerName, listingId, rating, comment)
            res.onSuccess { msg -> emitMessage(msg) }
                .onFailure { err -> emitMessage(err.message ?: "فشل التقييم") }
        }
    }

    // Reports
    fun submitReport(listingId: String, sellerId: String, reason: String, comment: String) {
        viewModelScope.launch {
            repository.submitReport(_currentUserId.value, listingId, sellerId, reason, comment)
            emitMessage("شكرًا لتعاونك، تم استلام البلاغ وسيتم فحصه من الإدارة.")
        }
    }

    // Admin Controls
    private fun requireAdminSession(): Boolean {
        if (!_isAdminSessionActive.value) {
            emitMessage("هذه العملية تتطلب جلسة إدارة موثقة.")
            return false
        }
        return true
    }

    fun adminApproveListing(listingId: String) {
        if (!requireAdminSession()) return
        viewModelScope.launch {
            repository.updateListingStatus(listingId, "PUBLISHED")
            logAdminAction("قبول إعلان", "تمت مراجعة الإعلان ($listingId) وقبوله للنشر العام")
            emitMessage("تم قبول الإعلان ونشره بنجاح")
        }
    }

    fun adminRejectListing(listingId: String, reason: String) {
        if (!requireAdminSession()) return
        viewModelScope.launch {
            repository.updateListingStatus(listingId, "REJECTED", reason)
            logAdminAction("رفض إعلان", "تم رفض الإعلان ($listingId) للسبب: $reason")
            emitMessage("تم رفض الإعلان مع توضيح السبب للبائع")
        }
    }

    fun adminDeleteListing(listingId: String) {
        if (!requireAdminSession()) return
        viewModelScope.launch {
            repository.deleteListing(listingId)
            logAdminAction("حذف إعلان نهائياً", "تم حذف الإعلان ($listingId) من قاعدة البيانات")
            emitMessage("تم حذف الإعلان نهائياً")
        }
    }

    fun adminUpdateSettings(standardFee: Int, featuredFee: Int, urgentFee: Int, durationDays: Int, autoPublish: Boolean) {
        if (!requireAdminSession()) return
        if (standardFee <= 0 || featuredFee <= 0 || urgentFee <= 0 || durationDays !in 1..365) {
            emitMessage("قيم الأسعار والمدة غير صالحة.")
            return
        }
        viewModelScope.launch {
            val current = platformSettings.value
            val updated = current.copy(
                standardAdFeeDzd = standardFee,
                featuredAdFeeDzd = featuredFee,
                urgentAdFeeDzd = urgentFee,
                adDurationDays = durationDays,
                autoPublishAfterPayment = autoPublish
            )
            repository.updatePlatformSettings(updated)
            logAdminAction("تعديل إعدادات الرسوم", "عادي: $standardFee دج، مميز: $featuredFee دج، عاجل: $urgentFee دج")
            emitMessage("تم حفظ إعدادات الرسوم والنشر بنجاح")
        }
    }

    fun adminUpdateOfficialAccount(
        rip: String,
        key: String,
        holder: String,
        provider: String,
        instructions: String
    ) {
        if (!requireAdminSession()) return
        val cleanRip = rip.trim().replace(" ", "")
        val cleanKey = key.trim().replace(" ", "")
        if (cleanRip.length < 10 || cleanKey.isEmpty()) {
            emitMessage("يرجى إدخال رقم حساب (RIP) ومفتاح صالحين.")
            return
        }
        viewModelScope.launch {
            val current = platformSettings.value
            val updated = current.copy(
                officialRip = cleanRip,
                officialKey = cleanKey,
                officialAccountHolder = holder.trim(),
                officialProviderName = provider.trim(),
                officialInstructions = instructions.trim()
            )
            repository.updatePlatformSettings(updated)
            logAdminAction("تعديل الحساب الرسمي", "RIP: $cleanRip، المفتاح: $cleanKey، الجهة: $provider")
            emitMessage("تم تحديث الحساب الرسمي بنجاح وتطبيقه على كافة المستخدمين ✓")
        }
    }

    fun adminToggleUserBan(userId: String, currentBan: Boolean) {
        if (!requireAdminSession()) return
        viewModelScope.launch {
            repository.updateBanStatus(userId, !currentBan)
            logAdminAction(if (!currentBan) "حظر مستخدم" else "إلغاء حظر مستخدم", "المستخدم المعني: $userId")
            emitMessage(if (!currentBan) "تم حظر الحساب بنجاح" else "تم إلغاء حظر الحساب")
        }
    }

    fun adminToggleVerification(userId: String, currentVerif: Boolean) {
        if (!requireAdminSession()) return
        viewModelScope.launch {
            repository.updateVerification(userId, !currentVerif)
            logAdminAction(if (!currentVerif) "توثيق حساب شارة زرقاء" else "إلغاء توثيق حساب", "المستخدم المعني: $userId")
            emitMessage(if (!currentVerif) "تم توثيق الحساب ومنح الشارة الزرقاء" else "تم إلغاء التوثيق")
        }
    }

    fun requestVerification() {
        viewModelScope.launch {
            repository.requestVerification(_currentUserId.value)
            emitMessage("تم إرسال طلب التوثيق للإدارة، ستتم مراجعته خلال 24 ساعة")
        }
    }

    fun markAdAsSold(listingId: String) {
        viewModelScope.launch {
            if (repository.markListingAsSold(listingId, _currentUserId.value)) {
                emitMessage("مبروك! تم تغيير حالة الإعلان إلى 'تم البيع'")
            } else {
                emitMessage("لا يمكن تغيير هذا الإعلان: الملكية غير متطابقة.")
            }
        }
    }

    fun emitMessage(msg: String) {
        viewModelScope.launch {
            _uiEvent.emit(msg)
        }
    }

    fun getAdminPin(): String {
        val prefs = getApplication<Application>().getSharedPreferences("ocavente_admin_prefs", android.content.Context.MODE_PRIVATE)
        return prefs.getString("admin_pin_code", "2026") ?: "2026"
    }

    fun updateAdminPin(oldPin: String, newPin: String): Boolean {
        val current = getAdminPin()
        if (oldPin.trim() != current.trim()) {
            emitMessage("الرمز السري الحالي غير صحيح!")
            return false
        }
        if (newPin.trim().length < 4) {
            emitMessage("يجب أن يتكون الرمز الجديد من 4 أرقام على الأقل!")
            return false
        }
        val prefs = getApplication<Application>().getSharedPreferences("ocavente_admin_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("admin_pin_code", newPin.trim()).apply()
        logAdminAction("تغيير رمز الدخول للإدارة", "تم تغيير رمز الإشراف PIN بنجاح")
        emitMessage("تم تحديث رمز دخول الإشراف بنجاح")
        return true
    }

    fun resetAdminPinToDefault() {
        val prefs = getApplication<Application>().getSharedPreferences("ocavente_admin_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("admin_pin_code", "2026").apply()
        logAdminAction("استعادة رمز الإدارة الافتراضي", "تمت استعادة 2026")
        emitMessage("تمت استعادة الرمز الافتراضي (2026)")
    }
}
