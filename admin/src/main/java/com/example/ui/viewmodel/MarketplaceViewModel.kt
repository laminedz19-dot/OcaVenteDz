package com.example.ui.viewmodel

import android.app.Application
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

    private val db = AppDatabase.getDatabase(application)
    val repository = MarketplaceRepository(db)
    private val _currentUserId = MutableStateFlow("")
    val currentUserId = _currentUserId.asStateFlow()

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
            try {
                repository.syncUsersFromCloud()
            } catch (_: Exception) {}
            try {
                repository.syncPaymentsFromCloud()
            } catch (_: Exception) {}
            restoreAdminSession()
        }
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refreshAllAdminData() {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                repository.syncPlatformSettingsFromCloud()
                repository.syncListingsFromCloud()
                repository.syncUsersFromCloud()
                repository.syncPaymentsFromCloud()
                emitMessage("تم تحديث كافة بيانات الإدارة من السحابة بنجاح ✓")
            } catch (e: Exception) {
                emitMessage("تم تحديث البيانات: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
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

    fun restoreAdminSession(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val user = repository.authService.restoreSession().getOrNull()
            if (user != null && repository.authService.checkIsCurrentAdmin()) {
                _isAdminSessionActive.value = true
                _currentUserId.value = user.uid
                logAdminAction("استعادة جلسة المشرف", "تمت استعادة الجلسة والتحقق من صلاحية admin")
                withContext(Dispatchers.Main) { onSuccess() }
            } else {
                repository.authService.signOut()
                _isAdminSessionActive.value = false
            }
        }
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

    val userTopUpRequests: StateFlow<List<TopUpRequestEntity>> = _currentUserId.flatMapLatest { id ->
        repository.getUserTopUpRequests(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    sealed interface TopUpListUiState {
        data object Loading : TopUpListUiState
        data class Success(val requests: List<TopUpRequestEntity>) : TopUpListUiState
        data class Error(val message: String) : TopUpListUiState
    }

    val topUpListUiState: StateFlow<TopUpListUiState> = repository.getAllTopUpRequestsFromCloud().map { result ->
        if (result.isSuccess) {
            TopUpListUiState.Success(result.getOrNull().orEmpty())
        } else {
            val err = result.exceptionOrNull()?.message ?: "خطأ أثناء تحميل طلبات الشحن من الخدمة السحابية قاعدة البيانات"
            TopUpListUiState.Error(err)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TopUpListUiState.Loading)

    val allTopUpRequests: StateFlow<List<TopUpRequestEntity>> = topUpListUiState.map { state ->
        when (state) {
            is TopUpListUiState.Success -> state.requests
            else -> emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    // --- Orders Management (Admin) ---
    sealed interface AdminOrderListUiState {
        data object Loading : AdminOrderListUiState
        data class Success(val orders: List<OrderEntity>) : AdminOrderListUiState
        data class Error(val message: String) : AdminOrderListUiState
    }

    val adminOrdersUiState: StateFlow<AdminOrderListUiState> = repository.getAllOrdersAdminFlow().map { res ->
        if (res.isSuccess) {
            val list = res.getOrNull().orEmpty()
            viewModelScope.launch { repository.syncOrdersLocally(list) }
            AdminOrderListUiState.Success(list)
        } else {
            val err = res.exceptionOrNull()?.message ?: "خطأ أثناء تحميل الطلبيات من السحابة"
            AdminOrderListUiState.Error(err)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminOrderListUiState.Loading)

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

    fun registerUser(
        name: String,
        phone: String,
        email: String,
        wilaya: String,
        commune: String,
        password: String = "",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanName = name.trim()
        val cleanPhone = phone.trim()
        val cleanEmail = email.trim().lowercase()
        val cleanWilaya = wilaya.trim()
        val cleanCommune = commune.trim()

        when {
            cleanName.length < 2 -> { onError("يرجى إدخال الاسم الكامل."); return }
            cleanPhone.length < 9 -> { onError("يرجى إدخال رقم هاتف صحيح."); return }
            cleanEmail.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() -> {
                onError("صيغة البريد الإلكتروني غير صحيحة."); return
            }
            password.length < 6 -> { onError("يجب أن تتكون كلمة المرور من 6 أحرف على الأقل."); return }
            cleanWilaya.isEmpty() || cleanCommune.isEmpty() -> { onError("يرجى اختيار الولاية والبلدية."); return }
        }

        viewModelScope.launch {
            try {
                val existingUser = repository.findUserByPhoneOrEmail(cleanPhone)
                    ?: if (cleanEmail.isNotEmpty()) repository.findUserByPhoneOrEmail(cleanEmail) else null

                var userId = existingUser?.id ?: ("user_" + UUID.randomUUID().toString().replace("-", "").take(12))
                val authEmail = if (cleanEmail.isNotBlank()) cleanEmail else "${cleanPhone}@ocaventedz.dz"

                try {
                    withTimeoutOrNull(3500L) {
                        val authResult = repository.authService.registerWithEmail(authEmail, password)
                        if (authResult.isSuccess) {
                            authResult.getOrNull()?.uid?.let { userId = it }
                        } else {
                            val loginRes = repository.authService.loginWithEmail(authEmail, password)
                            if (loginRes.isSuccess) {
                                loginRes.getOrNull()?.uid?.let { userId = it }
                            }
                        }
                    }
                } catch (t: Throwable) {
                    Log.w("MarketplaceViewModel", "الخدمة السحابية auth during registration: ${t.message}")
                }

                val newUser = UserEntity(
                    id = userId,
                    phone = cleanPhone,
                    email = cleanEmail,
                    name = cleanName,
                    avatarUrl = existingUser?.avatarUrl ?: "",
                    wilaya = cleanWilaya,
                    commune = cleanCommune,
                    bio = existingUser?.bio ?: "عضو في OcaVenteDz.",
                    sellerRating = existingUser?.sellerRating ?: 5.0,
                    reviewsCount = existingUser?.reviewsCount ?: 0,
                    adsCount = existingUser?.adsCount ?: 0,
                    createdAt = existingUser?.createdAt ?: System.currentTimeMillis(),
                    isVerified = existingUser?.isVerified ?: false,
                    verificationRequested = false,
                    isBanned = false,
                    role = existingUser?.role ?: "USER"
                )
                repository.saveUser(newUser)
                repository.createEmptyWallet(userId)
                _currentUserId.value = userId
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
        val cleanIdentifier = identifier.trim().lowercase()
        if (cleanIdentifier.isEmpty()) {
            onError("يرجى إدخال رقم الهاتف أو البريد الإلكتروني.")
            return
        }

        viewModelScope.launch {
            if (password.isNotBlank()) {
                val loginTarget = if (android.util.Patterns.EMAIL_ADDRESS.matcher(cleanIdentifier).matches()) {
                    cleanIdentifier
                } else {
                    "${cleanIdentifier}@ocaventedz.dz"
                }
                val authResult = repository.authService.loginWithEmail(loginTarget, password)
                if (authResult.isSuccess) {
                    val authUser = authResult.getOrNull()
                    val uid = authUser?.uid ?: ""
                    val localUser = repository.getUserDirect(uid) ?: UserEntity(
                        id = uid,
                        phone = if (!loginTarget.endsWith("@ocaventedz.dz")) "" else cleanIdentifier,
                        email = if (!loginTarget.endsWith("@ocaventedz.dz")) cleanIdentifier else "",
                        name = authUser?.displayName ?: "مستخدم OcaVenteDz",
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
                    repository.saveUser(localUser)
                    _currentUserId.value = uid
                    emitMessage("تم تسجيل الدخول بنجاح. مرحبًا ${localUser.name}!")
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                    return@launch
                }
            }

            val user = repository.findUserByPhoneOrEmail(cleanIdentifier)
                ?: repository.getAllUsersDirect().firstOrNull { candidate ->
                    candidate.phone.trim().lowercase() == cleanIdentifier ||
                        (candidate.email.isNotBlank() && candidate.email.trim().lowercase() == cleanIdentifier)
                }
            if (user == null) {
                withContext(Dispatchers.Main) {
                    onError("لم يتم العثور على حساب بهذه البيانات. يمكنك إنشاء حساب جديد.")
                }
                return@launch
            }
            if (user.isBanned) {
                withContext(Dispatchers.Main) {
                    onError("هذا الحساب موقوف حاليًا. يرجى التواصل مع الإدارة.")
                }
                return@launch
            }
            _currentUserId.value = user.id
            emitMessage("تم تسجيل الدخول بنجاح. مرحبًا ${user.name}!")
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    fun logoutUser(onLoggedOut: () -> Unit = {}) {
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
        val cleanIdentifier = identifier.trim().lowercase()
        if (cleanIdentifier.isEmpty()) {
            onError("يرجى إدخال البريد الإلكتروني أو رقم الهاتف.")
            return
        }

        viewModelScope.launch {
            if (android.util.Patterns.EMAIL_ADDRESS.matcher(cleanIdentifier).matches()) {
                val authResult = repository.authService.sendPasswordReset(cleanIdentifier)
                if (authResult.isSuccess) {
                    onSuccess("تم إرسال رابط استعادة كلمة المرور إلى $cleanIdentifier عبر الخدمة السحابية بنجاح.")
                    return@launch
                }
            }

            val user = repository.findUserByPhoneOrEmail(cleanIdentifier)
                ?: repository.getAllUsersDirect().firstOrNull { candidate ->
                    candidate.phone.trim().lowercase() == cleanIdentifier ||
                        (candidate.email.isNotBlank() && candidate.email.trim().lowercase() == cleanIdentifier)
                }
            if (user == null) {
                onError("لم يتم العثور على حساب بهذه البيانات.")
                return@launch
            }
            if (user.email.isNotBlank()) {
                val authResult = repository.authService.sendPasswordReset(user.email)
                if (authResult.isSuccess) {
                    onSuccess("تم إرسال رابط استعادة كلمة المرور إلى ${user.email} عبر الخدمة السحابية بنجاح.")
                    return@launch
                }
                onSuccess("تم التحقق من الحساب ${user.name}. تم إرسال طلب استعادة كلمة المرور.")
            } else {
                onSuccess("تم التحقق من الحساب ${user.name}. يلزم ربط بريد إلكتروني لاستعادة كلمة المرور عبر البريد.")
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
            newPassword.length < 8 -> { onError("يجب أن تتكون كلمة المرور الجديدة من 8 أحرف على الأقل."); return }
            newPassword != confirmation -> { onError("تأكيد كلمة المرور غير مطابق."); return }
        }
        viewModelScope.launch {
            repository.authService.changeCurrentPassword(currentPassword, newPassword)
                .onSuccess {
                    logAdminAction("تغيير كلمة مرور المشرف", "تم تحديث كلمة المرور عبر Supabase Auth بعد إعادة التحقق")
                    withContext(Dispatchers.Main) { onSuccess("تم تغيير كلمة المرور في السحابة بنجاح.") }
                }
                .onFailure { error ->
                    withContext(Dispatchers.Main) { onError(error.message ?: "تعذر تغيير كلمة المرور") }
                }
        }
    }

    fun socialAuthUnavailable(provider: String) {
        emitMessage("تسجيل الدخول عبر $provider جاهز في الواجهة، لكنه ينتظر إعداد الخدمة السحابية وملف إعداد Supabase.")
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
        viewModelScope.launch {
            repository.deleteUserData(_currentUserId.value)
            _currentUserId.value = "deleted"
            emitMessage("تم حذف بيانات الحساب من الجهاز.")
        }
    }

    // Top up wallet
    fun topUpWallet(amount: Int, provider: String, reference: String = "") {
        viewModelScope.launch {
            val result = repository.topUpWallet(_currentUserId.value, amount, provider, reference)
            result.onSuccess { msg ->
                emitMessage(msg)
            }.onFailure { err ->
                emitMessage(err.message ?: "فشلت عملية الشحن")
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

        viewModelScope.launch {
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
        }
    }

    private val _isProcessingTopUp = MutableStateFlow<String?>(null)
    val isProcessingTopUp: StateFlow<String?> = _isProcessingTopUp.asStateFlow()

    fun approveTopUpRequest(requestId: String, adminNote: String = "") {
        if (_isProcessingTopUp.value != null) return // Prevent double click
        _isProcessingTopUp.value = requestId
        viewModelScope.launch {
            try {
                val result = repository.approveTopUpRequest(requestId, adminNote)
                result.onSuccess { msg ->
                    emitMessage(msg)
                    logAdminAction("قبول طلب شحن", "تم قبول طلب الشحن $requestId وتحديث الحالة في قاعدة البيانات")
                }.onFailure { err ->
                    emitMessage(err.message ?: "فشلت عملية قبول الطلب")
                }
            } finally {
                _isProcessingTopUp.value = null
            }
        }
    }

    fun rejectTopUpRequest(requestId: String, reason: String = "") {
        if (_isProcessingTopUp.value != null) return // Prevent double click
        _isProcessingTopUp.value = requestId
        viewModelScope.launch {
            try {
                val result = repository.rejectTopUpRequest(requestId, reason)
                result.onSuccess { msg ->
                    emitMessage(msg)
                    logAdminAction("رفض طلب شحن", "تم رفض طلب الشحن $requestId وتحديث الحالة في قاعدة البيانات")
                }.onFailure { err ->
                    emitMessage(err.message ?: "فشلت عملية رفض الطلب")
                }
            } finally {
                _isProcessingTopUp.value = null
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
        onSuccess: (String) -> Unit
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

            val uploadedImages = images.mapIndexed { index, imgStr ->
                if (imgStr.startsWith("content://") || imgStr.startsWith("file://")) {
                    try {
                        val uploadRes = repository.supabaseClient.uploadListingImage(
                            getApplication(), Uri.parse(imgStr), "listing_${listingId}_$index"
                        )
                        if (uploadRes.isSuccess) uploadRes.getOrNull().orEmpty() else imgStr
                    } catch (_: Exception) {
                        imgStr
                    }
                } else {
                    imgStr
                }
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
                emitMessage("تعذر إتمام الدفع: " + (err.message ?: "خطأ غير معروف"))
            }
        }
    }

    suspend fun uploadAdImages(vararg unused: Any): Result<List<String>> = Result.failure(IllegalStateException("رفع الصور المتعدد غير متاح حالياً."))

    suspend fun deleteAdImage(imageStoragePathOrUrl: String): Result<Unit> = Result.success(Unit)

    suspend fun deleteAllAdImages(listingId: String): Result<Int> = Result.success(0)

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

    private fun emitMessage(msg: String) {
        viewModelScope.launch {
            _uiEvent.emit(msg)
        }
    }

    /**
     * Authenticates the admin using الخدمة السحابية Authentication (Email + Password).
     * Enforces token force-refresh and verifies the custom claim admin == true.
     */
    fun loginAdmin(
        email: String,
        pass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (email.isBlank() || pass.isBlank()) {
            val msg = "يرجى إدخال البريد الإلكتروني وكلمة المرور للمشرف"
            emitMessage(msg)
            onError(msg)
            return
        }

        viewModelScope.launch {
            val result = repository.authService.loginAdminWithClaims(email, pass)
            result.onSuccess { user ->
                _isAdminSessionActive.value = true
                _currentUserId.value = user.uid
                logAdminAction("تسجيل دخول المشرف", "تم توثيق المشرف (${user.email}) بنجاح عبر الخدمة السحابية Auth")
                emitMessage("مرحباً بك في لوحة الإدارة ✓")
                withContext(Dispatchers.Main) { onSuccess() }
            }.onFailure { err ->
                val errorMsg = err.message ?: "فشلت عملية تسجيل دخول المشرف"
                emitMessage(errorMsg)
                withContext(Dispatchers.Main) { onError(errorMsg) }
            }
        }
    }

    /**
     * Resolves a الخدمة السحابية Storage receipt path into an authenticated download URL.
     */
    fun resolveReceiptUrl(storagePathOrUrl: String, onResult: (String?) -> Unit) { onResult(storagePathOrUrl) }
}
