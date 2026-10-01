package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import com.example.ui.screens.*
import com.example.ui.theme.OcaventeTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MainViewModel

data class NavItem(
    val screen: AppScreen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OcaventeTheme {
                OcaventeApp(viewModel)
            }
        }
    }
}

@Composable
fun OcaventeApp(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showAdminSheet by remember { mutableStateOf(false) }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val navItems = listOf(
        NavItem(AppScreen.HOME, "الرئيسية", Icons.Filled.Home, Icons.Outlined.Home),
        NavItem(AppScreen.SEARCH, "بحث", Icons.Filled.Search, Icons.Outlined.Search),
        NavItem(AppScreen.ADD_LISTING, "أضف إعلان", Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline),
        NavItem(AppScreen.WALLET, "المحفظة", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
        NavItem(AppScreen.PROFILE, "حسابي", Icons.Filled.Person, Icons.Outlined.Person)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentScreen != AppScreen.LISTING_DETAIL) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    navItems.forEach { item ->
                        val isSelected = currentScreen == item.screen
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.navigateTo(item.screen) },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            modifier = Modifier.testTag("nav_item_${item.screen.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onOpenAdminSheet = { showAdminSheet = true }
                )
                AppScreen.SEARCH -> SearchScreen(viewModel = viewModel)
                AppScreen.ADD_LISTING -> AddListingScreen(viewModel = viewModel)
                AppScreen.WALLET -> WalletScreen(viewModel = viewModel)
                AppScreen.PROFILE -> ProfileScreen(
                    viewModel = viewModel,
                    onOpenAuthDialog = { showAuthDialog = true }
                )
                AppScreen.LISTING_DETAIL -> {
                    BackHandler { viewModel.navigateTo(AppScreen.HOME) }
                    ListingDetailScreen(viewModel = viewModel)
                }
            }
        }
    }

    if (showAuthDialog) {
        AuthDialog(viewModel = viewModel, onDismiss = { showAuthDialog = false })
    }

    if (showAdminSheet) {
        AdminInfoSheet(onDismiss = { showAdminSheet = false })
    }
}
