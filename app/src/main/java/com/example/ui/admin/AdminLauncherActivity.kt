package com.example.ui.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.model.ListingStatus
import com.example.ui.model.RechargeRequest
import com.example.ui.model.UserProfile
import com.example.ui.model.WilayasData
import com.example.ui.theme.OcaVenteDzTheme

class AdminLauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OcaVenteDzTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var listingsState by remember { mutableStateOf(WilayasData.sampleListings) }
                    var rechargeRequestsState by remember {
                        mutableStateOf(
                            listOf(
                                RechargeRequest(
                                    id = "rec-1",
                                    amount = 5000.0,
                                    method = "بريدي موب BaridiMob",
                                    status = "قيد المراجعة",
                                    date = "2026-10-01 10:15",
                                    transactionRef = "BRD-8829102",
                                    receiptImageUrl = "https://images.unsplash.com/photo-1554224155-8d04cb21cd6c?w=800"
                                ),
                                RechargeRequest(
                                    id = "rec-2",
                                    amount = 12000.0,
                                    method = "حساب البريد الجاري CCP",
                                    status = "قيد المراجعة",
                                    date = "2026-10-01 09:30",
                                    transactionRef = "CCP-9921827",
                                    receiptImageUrl = "https://images.unsplash.com/photo-1554224154-26032ffc0d07?w=800"
                                )
                            )
                        )
                    }

                    var usersState by remember {
                        mutableStateOf(
                            listOf(
                                UserProfile(
                                    id = "usr-1",
                                    name = "أمين الجزائري",
                                    email = "amine@example.dz",
                                    phone = "0550123456",
                                    wilayaCode = 16,
                                    wilayaName = "الجزائر",
                                    balance = 5200.0,
                                    rating = 4.9f,
                                    listingsCount = 2
                                ),
                                UserProfile(
                                    id = "usr-2",
                                    name = "محمد وهراني",
                                    email = "mohamed@example.dz",
                                    phone = "0661987654",
                                    wilayaCode = 31,
                                    wilayaName = "وهران",
                                    balance = 1500.0,
                                    rating = 4.8f,
                                    listingsCount = 1
                                ),
                                UserProfile(
                                    id = "usr-3",
                                    name = "ياسين سطايفي",
                                    email = "yacine@example.dz",
                                    phone = "0770334455",
                                    wilayaCode = 19,
                                    wilayaName = "سطيف",
                                    balance = 3400.0,
                                    rating = 5.0f,
                                    listingsCount = 1
                                )
                            )
                        )
                    }

                    var currentAdminScreen by remember { mutableStateOf<String>("dashboard") }

                    when (currentAdminScreen) {
                        "dashboard" -> {
                            AdminDashboardScreen(
                                listings = listingsState,
                                rechargeRequests = rechargeRequestsState,
                                onNavigateToListingsAdmin = { currentAdminScreen = "listings" },
                                onNavigateToRechargesAdmin = { currentAdminScreen = "recharges" },
                                onNavigateToUsersAdmin = { currentAdminScreen = "users" },
                                onApproveListing = { id ->
                                    listingsState = listingsState.map {
                                        if (it.id == id) it.copy(status = ListingStatus.PUBLISHED) else it
                                    }
                                },
                                onRejectListing = { id ->
                                    listingsState = listingsState.map {
                                        if (it.id == id) it.copy(status = ListingStatus.REJECTED) else it
                                    }
                                },
                                onApproveRecharge = { id ->
                                    rechargeRequestsState = rechargeRequestsState.map {
                                        if (it.id == id) it.copy(status = "مقبول") else it
                                    }
                                },
                                onBackClick = { finish() }
                            )
                        }
                        "recharges" -> {
                            AdminRechargeScreen(
                                requests = rechargeRequestsState,
                                onApprove = { id ->
                                    rechargeRequestsState = rechargeRequestsState.map {
                                        if (it.id == id) it.copy(status = "مقبول") else it
                                    }
                                },
                                onReject = { id ->
                                    rechargeRequestsState = rechargeRequestsState.map {
                                        if (it.id == id) it.copy(status = "مرفوض") else it
                                    }
                                },
                                onBackClick = { currentAdminScreen = "dashboard" }
                            )
                        }
                        "listings" -> {
                            AdminListingsScreen(
                                listings = listingsState,
                                onApprove = { id ->
                                    listingsState = listingsState.map {
                                        if (it.id == id) it.copy(status = ListingStatus.PUBLISHED) else it
                                    }
                                },
                                onReject = { id ->
                                    listingsState = listingsState.map {
                                        if (it.id == id) it.copy(status = ListingStatus.REJECTED) else it
                                    }
                                },
                                onDelete = { id ->
                                    listingsState = listingsState.filter { it.id != id }
                                },
                                onBackClick = { currentAdminScreen = "dashboard" }
                            )
                        }
                        "users" -> {
                            AdminUsersScreen(
                                users = usersState,
                                onBackClick = { currentAdminScreen = "dashboard" }
                            )
                        }
                        else -> {
                            AdminDashboardScreen(
                                listings = listingsState,
                                rechargeRequests = rechargeRequestsState,
                                onNavigateToListingsAdmin = { currentAdminScreen = "listings" },
                                onNavigateToRechargesAdmin = { currentAdminScreen = "recharges" },
                                onNavigateToUsersAdmin = { currentAdminScreen = "users" },
                                onApproveListing = { id ->
                                    listingsState = listingsState.map {
                                        if (it.id == id) it.copy(status = ListingStatus.PUBLISHED) else it
                                    }
                                },
                                onRejectListing = { id ->
                                    listingsState = listingsState.map {
                                        if (it.id == id) it.copy(status = ListingStatus.REJECTED) else it
                                    }
                                },
                                onApproveRecharge = { id ->
                                    rechargeRequestsState = rechargeRequestsState.map {
                                        if (it.id == id) it.copy(status = "مقبول") else it
                                    }
                                },
                                onBackClick = { finish() }
                            )
                        }
                    }
                }
            }
        }
    }
}
