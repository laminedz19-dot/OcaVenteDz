package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.OcaventeRepository
import com.example.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppScreen {
    HOME,
    SEARCH,
    ADD_LISTING,
    WALLET,
    PROFILE,
    LISTING_DETAIL
}

class MainViewModel(
    val repository: OcaventeRepository = OcaventeRepository()
) : ViewModel() {

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedListing = MutableStateFlow<Listing?>(null)
    val selectedListing: StateFlow<Listing?> = _selectedListing.asStateFlow()

    // Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedWilaya = MutableStateFlow("كل الولايات (58 ولاية)")
    val selectedWilaya: StateFlow<String> = _selectedWilaya.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    // UI feedback
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Repos flows
    val currentUser = repository.currentUser
    val categories = repository.categories
    val allListings = repository.listings
    val rechargeRequests = repository.rechargeRequests
    val transactions = repository.transactions
    val notifications = repository.notifications
    val favoriteIds = repository.favoriteIds

    // Filtered listings derived state
    val filteredListings: StateFlow<List<Listing>> = combine(
        allListings,
        _searchQuery,
        _selectedWilaya,
        _selectedCategoryId
    ) { listings, query, wilaya, catId ->
        listings.filter { listing ->
            val matchesQuery = query.isBlank() ||
                    listing.title.contains(query, ignoreCase = true) ||
                    listing.description.contains(query, ignoreCase = true)

            val matchesWilaya = wilaya.startsWith("كل الولايات") ||
                    listing.wilaya.contains(wilaya.substringAfter("-").substringBefore("(").trim()) ||
                    listing.wilaya == wilaya

            val matchesCategory = catId == null || listing.categoryId == catId

            matchesQuery && matchesWilaya && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.fetchCategories()
            repository.fetchListings()
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun openListingDetail(listing: Listing) {
        _selectedListing.value = listing
        _currentScreen.value = AppScreen.LISTING_DETAIL
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun setSelectedWilaya(wilaya: String) {
        _selectedWilaya.value = wilaya
    }

    fun setSelectedCategory(catId: String?) {
        _selectedCategoryId.value = if (_selectedCategoryId.value == catId) null else catId
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    fun toggleFavorite(listingId: String) {
        repository.toggleFavorite(listingId)
    }

    fun login(login: String, pass: String) {
        viewModelScope.launch {
            val res = repository.login(login, pass)
            if (res.isSuccess) {
                _userMessage.value = "مرحباً بك، تم تسجيل الدخول بنجاح!"
            } else {
                _userMessage.value = "فشل تسجيل الدخول: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    fun register(username: String, email: String, pass: String, phone: String, wilaya: String) {
        viewModelScope.launch {
            val res = repository.register(username, email, pass, phone, wilaya)
            if (res.isSuccess) {
                _userMessage.value = "تم إنشاء الحساب بنجاح!"
            } else {
                _userMessage.value = "فشل التسجيل"
            }
        }
    }

    fun logout() {
        repository.logout()
        _userMessage.value = "تم تسجيل الخروج"
    }

    fun submitListing(
        title: String,
        description: String,
        price: Double,
        categoryId: String,
        wilaya: String,
        phone: String,
        imageUrl: String
    ) {
        viewModelScope.launch {
            val res = repository.createListing(title, description, price, categoryId, wilaya, phone, imageUrl)
            if (res.isSuccess) {
                _userMessage.value = "تم إضافة الإعلان بنجاح! هو الآن قيد مراجعة الإدارة."
                _currentScreen.value = AppScreen.HOME
            } else {
                _userMessage.value = "حدث خطأ أثناء إضافة الإعلان"
            }
        }
    }

    fun submitRecharge(
        amount: Double,
        method: String,
        receiptNumber: String,
        receiptUrl: String?
    ) {
        viewModelScope.launch {
            val res = repository.submitRechargeRequest(amount, method, receiptNumber, receiptUrl)
            if (res.isSuccess) {
                _userMessage.value = "تم إرسال طلب الشحن بنجاح! سيتم التحقق منه وإضافة الرصيد فوراً."
            } else {
                _userMessage.value = "فشل إرسال طلب الشحن"
            }
        }
    }

    fun markNotificationRead(id: String) {
        repository.markNotificationRead(id)
    }
}
