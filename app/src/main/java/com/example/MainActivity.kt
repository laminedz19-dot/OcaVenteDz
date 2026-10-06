package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.components.OcaVenteBottomBar
import com.example.ui.components.OcaVenteTopBar
import com.example.ui.screens.auth.AuthLandingScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.ForgotPasswordScreen
import com.example.ui.screens.auth.ChangePasswordScreen
import com.example.ui.screens.auth.EditProfileScreen
import com.example.ui.screens.auth.RegisterScreen
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.create.CreateAdScreen
import com.example.ui.screens.details.AdDetailsScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.legal.LegalInfoScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.profile.SellerProfileScreen
import com.example.ui.screens.search.SearchScreen
import com.example.ui.screens.security.SecurityCenterScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.guest.GuestBrowseScreen
import com.example.ui.screens.guest.GuestAdDetailsScreen
import com.example.ui.theme.OcaVenteTheme
import com.example.ui.viewmodel.MarketplaceViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay

sealed class Screen {
    data class MainTab(val tab: String) : Screen() // "home", "search", "create", "chat", "profile"
    data class AdDetails(val listingId: String) : Screen()
    data class ChatConversation(val listingId: String, val sellerId: String) : Screen()
    data class SellerProfile(val sellerId: String) : Screen()
    object AuthLanding : Screen()
    object GuestBrowse : Screen()
    data class GuestAdDetails(val listingId: String) : Screen()
    object SecurityCenter : Screen()
    object LegalInfo : Screen()
    object Register : Screen()
    object Login : Screen()
    object ForgotPassword : Screen()
    object EditProfile : Screen()
    object ChangePassword : Screen()
}

class MainActivity : ComponentActivity() {

    private val viewModel: MarketplaceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            OcaVenteTheme {
                val currentLang by viewModel.language.collectAsState()
                val currentUserId by viewModel.currentUserId.collectAsState()
                val layoutDirection = if (currentLang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr

                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    OcaVenteApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun OcaVenteApp(viewModel: MarketplaceViewModel) {
    val currentUserId by viewModel.currentUserId.collectAsState()
    var currentScreen by remember {
        mutableStateOf<Screen>(if (viewModel.currentUserId.value.isNotBlank()) Screen.MainTab("home") else Screen.AuthLanding)
    }
    var showSplash by remember { mutableStateOf(true) }
    val currentLang by viewModel.language.collectAsState()
    val isAdminSessionActive by viewModel.isAdminSessionActive.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotBlank() && currentUserId != "deleted") {
            if (currentScreen is Screen.AuthLanding || currentScreen is Screen.Login || currentScreen is Screen.Register) {
                currentScreen = Screen.MainTab("home")
            }
        }
    }

    LaunchedEffect(Unit) {
        // Show the splash screen for 8 seconds as requested
        delay(8_000L)
        showSplash = false
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Handle back button behavior for sub-screens
    BackHandler(enabled = currentScreen !is Screen.AuthLanding) {
        when (currentScreen) {
            is Screen.MainTab -> {
                if ((currentScreen as Screen.MainTab).tab != "home") {
                    currentScreen = Screen.MainTab("home")
                } else if (currentUserId.isBlank()) {
                    currentScreen = Screen.AuthLanding
                }
            }
            is Screen.GuestBrowse -> currentScreen = Screen.AuthLanding
            is Screen.GuestAdDetails -> currentScreen = Screen.GuestBrowse
            is Screen.AdDetails -> currentScreen = Screen.MainTab("home")
            is Screen.ChatConversation -> currentScreen = Screen.MainTab("chat")
            is Screen.SellerProfile -> currentScreen = Screen.MainTab("home")
            is Screen.AuthLanding -> { /* Keep on AuthLanding */ }
            is Screen.SecurityCenter -> currentScreen = Screen.MainTab("profile")
            is Screen.LegalInfo -> currentScreen = Screen.MainTab("profile")
            is Screen.Register -> currentScreen = Screen.AuthLanding
            is Screen.Login -> currentScreen = Screen.AuthLanding
            is Screen.ForgotPassword -> currentScreen = Screen.Login
            is Screen.EditProfile -> currentScreen = Screen.MainTab("profile")
            is Screen.ChangePassword -> currentScreen = Screen.MainTab("profile")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (currentScreen is Screen.MainTab) {
                    OcaVenteTopBar(
                        currentUserId = currentUserId,
                        currentLang = currentLang,
                        onToggleLang = {
                            viewModel.setLanguage(if (currentLang == "ar") "fr" else "ar")
                        },
                        onLogoClick = {
                            currentScreen = Screen.MainTab("home")
                        },
                        onSecurityClick = {
                            currentScreen = Screen.SecurityCenter
                        }
                    )
                }
            },
            bottomBar = {
                if (currentScreen is Screen.MainTab) {
                    OcaVenteBottomBar(
                        currentTab = (currentScreen as Screen.MainTab).tab,
                        onTabSelected = { newTab ->
                            if ((newTab == "create" || newTab == "chat") && currentUserId.isBlank()) {
                                viewModel.emitMessage("يرجى تسجيل الدخول ببريدك الإلكتروني للمتابعة")
                                currentScreen = Screen.Login
                            } else {
                                currentScreen = Screen.MainTab(newTab)
                            }
                        }
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screen_transition"
                ) { screen ->
                    when (screen) {
                        is Screen.MainTab -> {
                            when (screen.tab) {
                                "home" -> HomeScreen(
                                    viewModel = viewModel,
                                    onAdClick = { id -> currentScreen = Screen.AdDetails(id) },
                                    onCategoryClick = { catId ->
                                        viewModel.selectedCategory.value = catId
                                        currentScreen = Screen.MainTab("search")
                                    },
                                    onSearchClick = { currentScreen = Screen.MainTab("search") },
                                    onSellClick = {
                                        if (currentUserId.isBlank()) {
                                            viewModel.emitMessage("يرجى تسجيل الدخول ببريدك الإلكتروني لنشر إعلانك")
                                            currentScreen = Screen.Login
                                        } else {
                                            currentScreen = Screen.MainTab("create")
                                        }
                                    }
                                )
                                "search" -> SearchScreen(
                                    viewModel = viewModel,
                                    onAdClick = { id -> currentScreen = Screen.AdDetails(id) }
                                )
                                "create" -> CreateAdScreen(
                                    viewModel = viewModel,
                                    onFinished = { newId ->
                                        currentScreen = Screen.AdDetails(newId)
                                    },
                                    onCancel = { currentScreen = Screen.MainTab("home") }
                                )
                                "chat" -> {
                                    val allListings by viewModel.adminListings.collectAsState()
                                    val demoListing = allListings.firstOrNull()
                                    ChatScreen(
                                        listingId = demoListing?.id ?: "list_1",
                                        sellerId = demoListing?.userId ?: "user_yacine",
                                        viewModel = viewModel,
                                        onBack = { currentScreen = Screen.MainTab("home") }
                                    )
                                }
                                "profile" -> ProfileScreen(
                                    viewModel = viewModel,
                                    onAdClick = { id -> currentScreen = Screen.AdDetails(id) },
                                    onOpenLegal = { currentScreen = Screen.LegalInfo },
                                    onOpenSecurity = { currentScreen = Screen.SecurityCenter },
                                    onEditProfile = { currentScreen = Screen.EditProfile },
                                    onChangePassword = { currentScreen = Screen.ChangePassword },
                                    onLogout = { currentScreen = Screen.AuthLanding },
                                    onLogin = { currentScreen = Screen.Login }
                                )
                            }
                        }

                        is Screen.AdDetails -> {
                            AdDetailsScreen(
                                listingId = screen.listingId,
                                viewModel = viewModel,
                                onBack = { currentScreen = Screen.MainTab("home") },
                                onOpenChat = { lId, sId ->
                                    if (currentUserId.isBlank()) {
                                        viewModel.emitMessage("يرجى تسجيل الدخول ببريدك الإلكتروني للتواصل مع البائع")
                                        currentScreen = Screen.Login
                                    } else {
                                        currentScreen = Screen.ChatConversation(lId, sId)
                                    }
                                },
                                onOpenSellerProfile = { sId ->
                                    currentScreen = Screen.SellerProfile(sId)
                                },
                                onOpenSecurity = {
                                    currentScreen = Screen.SecurityCenter
                                }
                            )
                        }

                        is Screen.ChatConversation -> {
                            ChatScreen(
                                listingId = screen.listingId,
                                sellerId = screen.sellerId,
                                viewModel = viewModel,
                                onBack = { currentScreen = Screen.AdDetails(screen.listingId) }
                            )
                        }

                        is Screen.SellerProfile -> {
                            SellerProfileScreen(
                                sellerId = screen.sellerId,
                                viewModel = viewModel,
                                onBack = { currentScreen = Screen.MainTab("home") },
                                onAdClick = { id -> currentScreen = Screen.AdDetails(id) },
                                onOpenChat = { lId, sId -> currentScreen = Screen.ChatConversation(lId, sId) }
                            )
                        }

                        is Screen.AuthLanding -> {
                            AuthLandingScreen(
                                onBrowseAds = { currentScreen = Screen.GuestBrowse },
                                onRegister = { currentScreen = Screen.Register },
                                onLogin = { currentScreen = Screen.Login }
                            )
                        }

                        is Screen.GuestBrowse -> {
                            GuestBrowseScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = Screen.AuthLanding },
                                onAdClick = { id -> currentScreen = Screen.GuestAdDetails(id) }
                            )
                        }

                        is Screen.GuestAdDetails -> {
                            GuestAdDetailsScreen(
                                listingId = screen.listingId,
                                viewModel = viewModel,
                                onBack = { currentScreen = Screen.GuestBrowse },
                                onLoginClick = { currentScreen = Screen.Login }
                            )
                        }

                        is Screen.SecurityCenter -> {
                            SecurityCenterScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = Screen.MainTab("profile") },
                                onOpenLegal = { currentScreen = Screen.LegalInfo }
                            )
                        }

                        is Screen.LegalInfo -> {
                            LegalInfoScreen(
                                onBack = { currentScreen = Screen.MainTab("profile") }
                            )
                        }

                        is Screen.Register -> {
                            RegisterScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = Screen.AuthLanding },
                                onRegistered = { currentScreen = Screen.MainTab("home") },
                                onLogin = { currentScreen = Screen.Login }
                            )
                        }

                        is Screen.Login -> {
                            LoginScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = Screen.AuthLanding },
                                onLoggedIn = { currentScreen = Screen.MainTab("home") },
                                onRegister = { currentScreen = Screen.Register },
                                onForgotPassword = { currentScreen = Screen.ForgotPassword }
                            )
                        }

                        is Screen.ForgotPassword -> {
                            ForgotPasswordScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = Screen.Login },
                                onLogin = { currentScreen = Screen.Login }
                            )
                        }

                        is Screen.EditProfile -> {
                            val user by viewModel.currentUser.collectAsState()
                            if (user == null) {
                                currentScreen = Screen.MainTab("profile")
                            } else {
                                EditProfileScreen(
                                    user = user!!,
                                    viewModel = viewModel,
                                    onBack = { currentScreen = Screen.MainTab("profile") },
                                    onSaved = { currentScreen = Screen.MainTab("profile") }
                                )
                            }
                        }

                        is Screen.ChangePassword -> {
                            ChangePasswordScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = Screen.MainTab("profile") }
                            )
                        }
                    }
                }
            }
        }

        // Overlay splash screen: animates out seamlessly without breaking composable hierarchy
        androidx.compose.animation.AnimatedVisibility(
            visible = showSplash,
            exit = androidx.compose.animation.fadeOut(
                animationSpec = androidx.compose.animation.core.tween(400)
            )
        ) {
            SplashScreen()
        }
    }
}
