package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.remote.back4app.Back4AppClient
import com.example.ui.admin.*
import com.example.ui.components.MainTab
import com.example.ui.components.OcaBottomNavigationBar
import com.example.ui.model.*
import com.example.ui.navigation.AppScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.auth.*
import com.example.ui.screens.categories.CategoriesScreen
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.chat.ConversationsScreen
import com.example.ui.screens.create.CreateAdScreen
import com.example.ui.screens.create.EditAdScreen
import com.example.ui.screens.details.AdDetailsScreen
import com.example.ui.screens.favorites.FavoritesScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.legal.LegalInfoScreen
import com.example.ui.screens.listings.MyListingsScreen
import com.example.ui.screens.notifications.NotificationsScreen
import com.example.ui.screens.offers.OffersScreen
import com.example.ui.screens.orders.OrdersScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.profile.SellerProfileScreen
import com.example.ui.screens.search.SearchResultsScreen
import com.example.ui.screens.search.SearchScreen
import com.example.ui.screens.search.SearchFilterState
import com.example.ui.screens.security.SecurityCenterScreen
import com.example.ui.screens.settings.Back4AppSettingsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.wallet.WalletScreen
import kotlinx.coroutines.launch

@Composable
fun MainAppRoot(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit
) {
    // Force RTL for authentic Arabic experience
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        // App Navigation Stack
        var backStack by remember { mutableStateOf(listOf<AppScreen>(AppScreen.Splash)) }
        val currentScreen = backStack.lastOrNull() ?: AppScreen.Splash

        fun navigateTo(screen: AppScreen) {
            backStack = backStack + screen
        }

        fun navigateBack() {
            if (backStack.size > 1) {
                backStack = backStack.dropLast(1)
            }
        }

        fun navigateAndReplace(screen: AppScreen) {
            backStack = listOf(screen)
        }

        // Active Bottom Nav Tab for Main screen
        var currentTab by remember { mutableStateOf(MainTab.HOME) }

        // Core App Data (OcaVenteDz Local UI State)
        var listings by remember { mutableStateOf(WilayasData.sampleListings) }
        val categories = remember { WilayasData.defaultCategories }
        var favoriteIds by remember { mutableStateOf(setOf("list-1", "list-3")) }
        var currentWilaya by remember { mutableStateOf<Wilaya?>(null) }

        var userProfile by remember {
            mutableStateOf(
                UserProfile(
                    id = "current-user",
                    name = "أمين الجزائري",
                    email = "amine@ocavente.dz",
                    phone = "0550123456",
                    wilayaCode = 16,
                    wilayaName = "الجزائر",
                    balance = 3500.0,
                    memberSince = "جانفي 2024",
                    isVerified = true,
                    rating = 4.9f,
                    reviewsCount = 18,
                    listingsCount = 2
                )
            )
        }

        var conversations by remember {
            mutableStateOf(
                listOf(
                    Conversation(
                        id = "conv-1",
                        listingId = "list-1",
                        listingTitle = "Samsung Galaxy S23 Ultra",
                        listingPrice = 118000.0,
                        otherUserId = "usr-buyer-1",
                        otherUserName = "رضوان بلقاسم",
                        lastMessage = "سلام عليكم أخي، تقبل 110 آلاف ونتلاقاو غدوا في باب الزوار؟",
                        lastMessageTime = "14:30",
                        unreadCount = 1,
                        messages = listOf(
                            ChatMessage("m1", "usr-buyer-1", "سلام عليكم أخي، الهاتف مازال متوفر؟", "14:20", false),
                            ChatMessage("m2", "current-user", "وعليكم السلام، نعم مازال متوفر وخويا راه كالجديد تماماً.", "14:25", true),
                            ChatMessage("m3", "usr-buyer-1", "سلام عليكم أخي، تقبل 110 آلاف ونتلاقاو غدوا في باب الزوار؟", "14:30", false)
                        )
                    ),
                    Conversation(
                        id = "conv-2",
                        listingId = "list-2",
                        listingTitle = "Renault Clio 4 GT Line",
                        listingPrice = 2250000.0,
                        otherUserId = "usr-2",
                        otherUserName = "محمد وهراني",
                        lastMessage = "مرحبا بيك في وهران خويا شوف الكليو وسوقها",
                        lastMessageTime = "أمس",
                        unreadCount = 0,
                        messages = listOf(
                            ChatMessage("m10", "current-user", "مساء الخير، هل السيارة فيها أي روتوش في القصعة؟", "أمس", true),
                            ChatMessage("m11", "usr-2", "مرحبا بيك في وهران خويا شوف الكليو وسوقها", "أمس", false)
                        )
                    )
                )
            )
        }

        var notifications by remember {
            mutableStateOf(
                listOf(
                    AppNotification(
                        id = "notif-1",
                        title = "عرض سعر جديد!",
                        body = "قدم رضوان بلقاسم عرضاً بقيمة 110,000 دج على هاتف سامسونغ S23 Ultra.",
                        time = "منذ 10 دقائق",
                        isRead = false,
                        type = NotificationType.OFFER
                    ),
                    AppNotification(
                        id = "notif-2",
                        title = "تم شحن المحفظة بنجاح",
                        body = "تمت الموافقة على شحن رصيدك بقيمة 2,000 دج عبر بريدي موب.",
                        time = "منذ ساعتين",
                        isRead = false,
                        type = NotificationType.WALLET
                    ),
                    AppNotification(
                        id = "notif-3",
                        title = "إعلانك نشط الآن",
                        body = "تمت مراجعة إعلانك بنجاح وهو الآن متاح للجميع على منصة OcaVenteDz.",
                        time = "منذ يوم",
                        isRead = true,
                        type = NotificationType.ADMIN
                    )
                )
            )
        }

        var transactions by remember {
            mutableStateOf(
                listOf(
                    WalletTransaction("tx-1", "شحن رصيد - BaridiMob", 2000.0, true, "2026-09-28", "MOB-994821", "مكتملة"),
                    WalletTransaction("tx-2", "تمييز إعلان لمدة 7 أيام", 500.0, false, "2026-09-25", "FEAT-11234", "مكتملة"),
                    WalletTransaction("tx-3", "شحن رصيد - CCP الجزائر", 2000.0, true, "2026-09-10", "CCP-004381", "مكتملة")
                )
            )
        }

        var rechargeRequests by remember {
            mutableStateOf(
                listOf(
                    RechargeRequest("req-1", 1500.0, "بريدي موب BaridiMob", "قيد المراجعة", "اليوم 11:20", "TX-88392019"),
                    RechargeRequest("req-2", 2000.0, "بريدي موب BaridiMob", "مقبول", "منذ يومين", "TX-77401923")
                )
            )
        }

        var offers by remember {
            mutableStateOf(
                listOf(
                    Offer(
                        id = "off-1",
                        listingId = "list-1",
                        listingTitle = "Samsung Galaxy S23 Ultra 256GB",
                        originalPrice = 118000.0,
                        offerAmount = 110000.0,
                        buyerName = "رضوان بلقاسم",
                        status = "معلق",
                        date = "اليوم 14:30"
                    )
                )
            )
        }

        var orders by remember {
            mutableStateOf(
                listOf(
                    Order(
                        id = "ord-1",
                        listingTitle = "MacBook Pro M1 16GB",
                        price = 165000.0,
                        status = "في طريق التوصيل",
                        trackingNumber = "DZ-YAL-88391",
                        sellerName = "ياسين سطايفي",
                        buyerName = "أمين الجزائري",
                        date = "2026-09-29"
                    )
                )
            )
        }

        // Search Filter State for Search Results
        var currentSearchFilter by remember { mutableStateOf(SearchFilterState()) }

        // Back Handler handling
        BackHandler(enabled = backStack.size > 1) {
            navigateBack()
        }

        when (val screen = currentScreen) {
            is AppScreen.Splash -> {
                SplashScreen(
                    onSplashFinished = {
                        navigateAndReplace(AppScreen.AuthLanding)
                    }
                )
            }

            is AppScreen.AuthLanding -> {
                AuthLandingScreen(
                    onNavigateToLogin = { navigateTo(AppScreen.Login) },
                    onNavigateToRegister = { navigateTo(AppScreen.Register) }
                )
            }

            is AppScreen.Login -> {
                LoginScreen(
                    onLoginSuccess = { navigateAndReplace(AppScreen.Main(MainTab.HOME)) },
                    onNavigateToRegister = { navigateTo(AppScreen.Register) },
                    onNavigateToForgotPassword = { navigateTo(AppScreen.ForgotPassword) },
                    onBackClick = { navigateBack() }
                )
            }

            is AppScreen.Register -> {
                RegisterScreen(
                    onRegisterSuccess = { navigateAndReplace(AppScreen.Main(MainTab.HOME)) },
                    onNavigateToLogin = { navigateTo(AppScreen.Login) },
                    onBackClick = { navigateBack() }
                )
            }

            is AppScreen.ForgotPassword -> {
                ForgotPasswordScreen(onBackClick = { navigateBack() })
            }

            is AppScreen.ChangePassword -> {
                ChangePasswordScreen(onBackClick = { navigateBack() })
            }

            is AppScreen.Main -> {
                Scaffold(
                    bottomBar = {
                        OcaBottomNavigationBar(
                            currentTab = currentTab,
                            onTabSelected = { tab -> currentTab = tab },
                            unreadMessagesCount = conversations.sumOf { it.unreadCount }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            MainTab.HOME -> {
                                HomeScreen(
                                    listings = listings,
                                    categories = categories,
                                    favoriteIds = favoriteIds,
                                    currentWilaya = currentWilaya,
                                    onWilayaChanged = { currentWilaya = it },
                                    onListingClick = { listing ->
                                        navigateTo(AppScreen.AdDetails(listing.id))
                                    },
                                    onToggleFavorite = { id ->
                                        favoriteIds = if (favoriteIds.contains(id)) favoriteIds - id else favoriteIds + id
                                    },
                                    onCategoryClick = { cat ->
                                        currentSearchFilter = SearchFilterState(categoryId = cat.id)
                                        navigateTo(AppScreen.SearchResults(currentSearchFilter))
                                    },
                                    onSearchClick = {
                                        navigateTo(AppScreen.SearchFilter(SearchFilterState(wilayaCode = currentWilaya?.code)))
                                    },
                                    onViewAllCategories = {
                                        navigateTo(AppScreen.Categories)
                                    },
                                    onViewAllListings = {
                                        currentSearchFilter = SearchFilterState(wilayaCode = currentWilaya?.code)
                                        navigateTo(AppScreen.SearchResults(currentSearchFilter))
                                    },
                                    onNotificationsClick = {
                                        navigateTo(AppScreen.Notifications)
                                    },
                                    unreadNotificationsCount = notifications.count { !it.isRead }
                                )
                            }

                            MainTab.SEARCH -> {
                                SearchScreen(
                                    categories = categories,
                                    initialFilter = currentSearchFilter,
                                    onApplyFilter = { filter ->
                                        currentSearchFilter = filter
                                        navigateTo(AppScreen.SearchResults(filter))
                                    },
                                    onBackClick = { currentTab = MainTab.HOME }
                                )
                            }

                            MainTab.CREATE -> {
                                CreateAdScreen(
                                    categories = categories,
                                    defaultPhone = userProfile.phone,
                                    defaultWilayaCode = userProfile.wilayaCode,
                                    onCreateAd = { newListing ->
                                        listings = listOf(newListing) + listings
                                        userProfile = userProfile.copy(listingsCount = userProfile.listingsCount + 1)
                                        currentTab = MainTab.HOME
                                        coroutineScope.launch {
                                            Back4AppClient.syncListing(context, newListing)
                                        }
                                    },
                                    onBackClick = { currentTab = MainTab.HOME }
                                )
                            }

                            MainTab.CHAT -> {
                                ConversationsScreen(
                                    conversations = conversations,
                                    onConversationClick = { conv ->
                                        navigateTo(AppScreen.Chat(conv.id))
                                    },
                                    onBackClick = { currentTab = MainTab.HOME }
                                )
                            }

                            MainTab.PROFILE -> {
                                ProfileScreen(
                                    userProfile = userProfile,
                                    onNavigateToEditProfile = { navigateTo(AppScreen.EditProfile) },
                                    onNavigateToMyListings = { navigateTo(AppScreen.MyListings) },
                                    onNavigateToFavorites = { navigateTo(AppScreen.Favorites) },
                                    onNavigateToWallet = { navigateTo(AppScreen.Wallet) },
                                    onNavigateToNotifications = { navigateTo(AppScreen.Notifications) },
                                    onNavigateToOffers = { navigateTo(AppScreen.Offers) },
                                    onNavigateToOrders = { navigateTo(AppScreen.Orders) },
                                    onNavigateToSecurity = { navigateTo(AppScreen.Security) },
                                    onNavigateToSettings = { navigateTo(AppScreen.Settings) },
                                    onNavigateToLegal = { navigateTo(AppScreen.Legal) },
                                    onNavigateToAdmin = { navigateTo(AppScreen.AdminDashboard) },
                                    onLogout = { navigateAndReplace(AppScreen.AuthLanding) }
                                )
                            }
                        }
                    }
                }
            }

            is AppScreen.AdDetails -> {
                val listing = listings.find { it.id == screen.listingId } ?: listings.first()
                val similar = listings.filter { it.categoryId == listing.categoryId && it.id != listing.id }
                AdDetailsScreen(
                    listing = listing,
                    similarListings = similar,
                    isFavorite = favoriteIds.contains(listing.id),
                    onToggleFavorite = {
                        favoriteIds = if (favoriteIds.contains(listing.id)) favoriteIds - listing.id else favoriteIds + listing.id
                    },
                    onContactSellerChat = {
                        val existingConv = conversations.find { it.listingId == listing.id }
                        if (existingConv != null) {
                            navigateTo(AppScreen.Chat(existingConv.id))
                        } else {
                            val newConv = Conversation(
                                id = "conv-${System.currentTimeMillis()}",
                                listingId = listing.id,
                                listingTitle = listing.title,
                                listingPrice = listing.price,
                                otherUserId = listing.sellerId,
                                otherUserName = listing.sellerName,
                                lastMessage = "سلام عليكم، هل الإعلان متوفر؟",
                                lastMessageTime = "الآن",
                                unreadCount = 0,
                                messages = listOf(
                                    ChatMessage("m-${System.currentTimeMillis()}", "current-user", "سلام عليكم، هل الإعلان متوفر؟", "الآن", true)
                                )
                            )
                            conversations = listOf(newConv) + conversations
                            navigateTo(AppScreen.Chat(newConv.id))
                        }
                    },
                    onViewSellerProfile = { sellerId ->
                        navigateTo(AppScreen.SellerProfile(sellerId))
                    },
                    onListingClick = { target ->
                        navigateTo(AppScreen.AdDetails(target.id))
                    },
                    onBackClick = { navigateBack() }
                )
            }

            is AppScreen.Categories -> {
                CategoriesScreen(
                    categories = categories,
                    onCategoryClick = { cat ->
                        currentSearchFilter = SearchFilterState(categoryId = cat.id)
                        navigateTo(AppScreen.SearchResults(currentSearchFilter))
                    },
                    onBackClick = { navigateBack() }
                )
            }

            is AppScreen.SearchResults -> {
                SearchResultsScreen(
                    filter = screen.filter,
                    allListings = listings,
                    favoriteIds = favoriteIds,
                    onToggleFavorite = { id ->
                        favoriteIds = if (favoriteIds.contains(id)) favoriteIds - id else favoriteIds + id
                    },
                    onListingClick = { target ->
                        navigateTo(AppScreen.AdDetails(target.id))
                    },
                    onOpenFilter = {
                        navigateTo(AppScreen.SearchFilter(screen.filter))
                    },
                    onBackClick = { navigateBack() }
                )
            }

            is AppScreen.SearchFilter -> {
                SearchScreen(
                    categories = categories,
                    initialFilter = screen.initialFilter,
                    onApplyFilter = { filter ->
                        currentSearchFilter = filter
                        navigateTo(AppScreen.SearchResults(filter))
                    },
                    onBackClick = { navigateBack() }
                )
            }

            is AppScreen.Favorites -> {
                val favList = listings.filter { favoriteIds.contains(it.id) }
                FavoritesScreen(
                    favoriteListings = favList,
                    onToggleFavorite = { id ->
                        favoriteIds = favoriteIds - id
                    },
                    onListingClick = { target ->
                        navigateTo(AppScreen.AdDetails(target.id))
                    },
                    onExploreClick = {
                        navigateBack()
                        currentTab = MainTab.HOME
                    },
                    onBackClick = { navigateBack() }
                )
            }

            is AppScreen.MyListings -> {
                val myList = listings.filter { it.sellerId == "current-user" || it.id in listOf("list-1", "list-6") }
                MyListingsScreen(
                    userListings = myList,
                    onListingClick = { target -> navigateTo(AppScreen.AdDetails(target.id)) },
                    onEditListing = { target -> navigateTo(AppScreen.EditAd(target.id)) },
                    onDeleteListing = { id ->
                        listings = listings.filter { it.id != id }
                    },
                    onCreateAdClick = {
                        navigateBack()
                        currentTab = MainTab.CREATE
                    },
                    onBackClick = { navigateBack() }
                )
            }

            is AppScreen.EditAd -> {
                val listing = listings.find { it.id == screen.listingId } ?: listings.first()
                EditAdScreen(
                    listing = listing,
                    onSaveAd = { updated ->
                        listings = listings.map { if (it.id == updated.id) updated else it }
                        navigateBack()
                    },
                    onBackClick = { navigateBack() }
                )
            }

            is AppScreen.Chat -> {
                val conv = conversations.find { it.id == screen.conversationId } ?: conversations.first()
                ChatScreen(
                    conversation = conv,
                    onSendMessage = { text ->
                        val newMsg = ChatMessage(
                            id = "msg-${System.currentTimeMillis()}",
                            senderId = "current-user",
                            text = text,
                            timestamp = "الآن",
                            isMine = true
                        )
                        val updatedConv = conv.copy(
                            messages = conv.messages + newMsg,
                            lastMessage = text,
                            lastMessageTime = "الآن",
                            unreadCount = 0
                        )
                        conversations = conversations.map { if (it.id == conv.id) updatedConv else it }
                    },
                    onBackClick = { navigateBack() }
                )
            }

            is AppScreen.Offers -> {
                OffersScreen(
                    offers = offers,
                    onAcceptOffer = { id ->
                        offers = offers.map { if (it.id == id) it.copy(status = "مقبول") else it }
                    },
                    onRejectOffer = { id ->
                        offers = offers.map { if (it.id == id) it.copy(status = "مرفوض") else it }
                    },
                    onBackClick = { navigateBack() }
                )
            }

            is AppScreen.Orders -> {
                OrdersScreen(orders = orders, onBackClick = { navigateBack() })
            }

            is AppScreen.Wallet -> {
                WalletScreen(
                    balance = userProfile.balance,
                    transactions = transactions,
                    rechargeRequests = rechargeRequests,
                    onRequestRecharge = { amount, method, ref, receiptUri ->
                        val newReq = RechargeRequest(
                            id = "req-${System.currentTimeMillis()}",
                            amount = amount,
                            method = method,
                            status = "قيد المراجعة والتحقق اليدوي",
                            date = "الآن",
                            transactionRef = ref,
                            receiptImageUrl = receiptUri,
                            destinationAccount = "007999990008761821 Clé 94"
                        )
                        rechargeRequests = listOf(newReq) + rechargeRequests
                        coroutineScope.launch {
                            Back4AppClient.submitRechargeRequest(context, newReq)
                        }
                    },
                    onBackClick = { navigateBack() }
                )
            }

            is AppScreen.Notifications -> {
                NotificationsScreen(
                    notifications = notifications,
                    onNotificationClick = { clicked ->
                        notifications = notifications.map { if (it.id == clicked.id) it.copy(isRead = true) else it }
                        when (clicked.type) {
                            NotificationType.OFFER -> navigateTo(AppScreen.Offers)
                            NotificationType.WALLET -> navigateTo(AppScreen.Wallet)
                            NotificationType.CHAT -> {
                                val firstConv = conversations.firstOrNull()
                                if (firstConv != null) navigateTo(AppScreen.Chat(firstConv.id))
                            }
                            else -> {}
                        }
                    },
                    onMarkAllAsRead = {
                        notifications = notifications.map { it.copy(isRead = true) }
                    },
                    onBackClick = { navigateBack() }
                )
            }

            is AppScreen.EditProfile -> {
                EditProfileScreen(
                    userProfile = userProfile,
                    onSaveProfile = { updated -> userProfile = updated },
                    onBackClick = { navigateBack() }
                )
            }

            is AppScreen.SellerProfile -> {
                val targetListing = listings.find { it.sellerId == screen.sellerId } ?: listings.first()
                val sellerListings = listings.filter { it.sellerId == targetListing.sellerId }
                SellerProfileScreen(
                    sellerName = targetListing.sellerName,
                    sellerPhone = targetListing.sellerPhone,
                    sellerWilaya = targetListing.wilayaName,
                    sellerRating = targetListing.sellerRating,
                    sellerListings = sellerListings,
                    favoriteIds = favoriteIds,
                    onToggleFavorite = { id ->
                        favoriteIds = if (favoriteIds.contains(id)) favoriteIds - id else favoriteIds + id
                    },
                    onListingClick = { target -> navigateTo(AppScreen.AdDetails(target.id)) },
                    onContactChat = {
                        val existingConv = conversations.find { it.otherUserId == targetListing.sellerId }
                        if (existingConv != null) navigateTo(AppScreen.Chat(existingConv.id))
                        else {
                            val newConv = Conversation(
                                id = "conv-${System.currentTimeMillis()}",
                                listingId = targetListing.id,
                                listingTitle = targetListing.title,
                                listingPrice = targetListing.price,
                                otherUserId = targetListing.sellerId,
                                otherUserName = targetListing.sellerName,
                                lastMessage = "سلام عليكم",
                                lastMessageTime = "الآن",
                                unreadCount = 0,
                                messages = emptyList()
                            )
                            conversations = listOf(newConv) + conversations
                            navigateTo(AppScreen.Chat(newConv.id))
                        }
                    },
                    onBackClick = { navigateBack() }
                )
            }

            is AppScreen.Security -> {
                SecurityCenterScreen(
                    userEmail = userProfile.email,
                    userPhone = userProfile.phone,
                    onNavigateToChangePassword = { navigateTo(AppScreen.ChangePassword) },
                    onBackClick = { navigateBack() }
                )
            }

            is AppScreen.Settings -> {
                SettingsScreen(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = onToggleDarkMode,
                    onNavigateToBack4App = { navigateTo(AppScreen.Back4AppSettings) },
                    onBackClick = { navigateBack() }
                )
            }

            is AppScreen.Back4AppSettings -> {
                Back4AppSettingsScreen(onBackClick = { navigateBack() })
            }

            is AppScreen.Legal -> {
                LegalInfoScreen(onBackClick = { navigateBack() })
            }

            is AppScreen.AdminDashboard -> {
                AdminDashboardScreen(
                    listings = listings,
                    rechargeRequests = rechargeRequests,
                    onNavigateToListingsAdmin = { navigateTo(AppScreen.AdminListings) },
                    onNavigateToRechargesAdmin = { navigateTo(AppScreen.AdminRecharge) },
                    onNavigateToUsersAdmin = { navigateTo(AppScreen.AdminUsers) },
                    onApproveListing = { id ->
                        listings = listings.map { if (it.id == id) it.copy(status = ListingStatus.PUBLISHED) else it }
                    },
                    onRejectListing = { id ->
                        listings = listings.map { if (it.id == id) it.copy(status = ListingStatus.REJECTED) else it }
                    },
                    onApproveRecharge = { id ->
                        val req = rechargeRequests.find { it.id == id }
                        rechargeRequests = rechargeRequests.map { if (it.id == id) it.copy(status = "مقبول") else it }
                        if (req != null) {
                            userProfile = userProfile.copy(balance = userProfile.balance + req.amount)
                            val newTx = WalletTransaction(
                                id = "tx-${System.currentTimeMillis()}",
                                title = "شحن رصيد - ${req.method}",
                                amount = req.amount,
                                isCredit = true,
                                date = "الآن",
                                reference = req.transactionRef
                            )
                            transactions = listOf(newTx) + transactions
                        }
                    },
                    onBackClick = { navigateBack() }
                )
            }

            is AppScreen.AdminListings -> {
                AdminListingsScreen(
                    listings = listings,
                    onApprove = { id ->
                        listings = listings.map { if (it.id == id) it.copy(status = ListingStatus.PUBLISHED) else it }
                    },
                    onReject = { id ->
                        listings = listings.map { if (it.id == id) it.copy(status = ListingStatus.REJECTED) else it }
                    },
                    onDelete = { id ->
                        listings = listings.filter { it.id != id }
                    },
                    onBackClick = { navigateBack() }
                )
            }

            is AppScreen.AdminRecharge -> {
                AdminRechargeScreen(
                    requests = rechargeRequests,
                    onApprove = { id ->
                        val req = rechargeRequests.find { it.id == id }
                        rechargeRequests = rechargeRequests.map { if (it.id == id) it.copy(status = "مقبول") else it }
                        if (req != null) {
                            userProfile = userProfile.copy(balance = userProfile.balance + req.amount)
                        }
                    },
                    onReject = { id ->
                        rechargeRequests = rechargeRequests.map { if (it.id == id) it.copy(status = "مرفوض") else it }
                    },
                    onBackClick = { navigateBack() }
                )
            }

            is AppScreen.AdminUsers -> {
                val allUsers = listOf(
                    userProfile,
                    UserProfile(
                        id = "usr-1",
                        name = "أمين الجزائري",
                        email = "amine.alg@mail.dz",
                        phone = "0550123456",
                        wilayaCode = 16,
                        wilayaName = "الجزائر",
                        avatarUrl = "",
                        balance = 3500.0,
                        memberSince = "2024",
                        isVerified = true,
                        rating = 4.9f,
                        reviewsCount = 24,
                        listingsCount = 6
                    ),
                    UserProfile(
                        id = "usr-2",
                        name = "محمد وهراني",
                        email = "mohamed.oran@mail.dz",
                        phone = "0661987654",
                        wilayaCode = 31,
                        wilayaName = "وهران",
                        avatarUrl = "",
                        balance = 1200.0,
                        memberSince = "2023",
                        isVerified = true,
                        rating = 4.8f,
                        reviewsCount = 15,
                        listingsCount = 3
                    ),
                    UserProfile(
                        id = "usr-3",
                        name = "ياسين سطايفي",
                        email = "yacine.setif@mail.dz",
                        phone = "0770334455",
                        wilayaCode = 19,
                        wilayaName = "سطيف",
                        avatarUrl = "",
                        balance = 5000.0,
                        memberSince = "2025",
                        isVerified = true,
                        rating = 5.0f,
                        reviewsCount = 32,
                        listingsCount = 8
                    )
                )
                AdminUsersScreen(users = allUsers, onBackClick = { navigateBack() })
            }
        }
    }
}
