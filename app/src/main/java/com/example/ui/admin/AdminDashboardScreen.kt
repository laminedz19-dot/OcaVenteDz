package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.OcaSubTopBar
import com.example.ui.components.StatCard
import com.example.ui.components.formatPriceDZD
import com.example.ui.model.Listing
import com.example.ui.model.ListingStatus
import com.example.ui.model.RechargeRequest
import com.example.ui.theme.OcaAmberTertiary
import com.example.ui.theme.OcaGreenPrimary
import com.example.ui.theme.OcaNavySecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    listings: List<Listing>,
    rechargeRequests: List<RechargeRequest>,
    onNavigateToListingsAdmin: () -> Unit,
    onNavigateToRechargesAdmin: () -> Unit,
    onNavigateToUsersAdmin: () -> Unit,
    onApproveListing: (String) -> Unit,
    onRejectListing: (String) -> Unit,
    onApproveRecharge: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val pendingListings = remember(listings) {
        listings.filter { it.status == ListingStatus.PENDING }
    }

    val pendingRecharges = remember(rechargeRequests) {
        rechargeRequests.filter { it.status == "قيد المراجعة" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = OcaNavySecondary
                        ) {
                            Text(
                                text = "Admin",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = "لوحة إدارة OcaVenteDz",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Stats Grid 2x2
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "إجمالي الإعلانات",
                            value = "${listings.size}",
                            subtitle = "في كل الجزائر",
                            icon = Icons.Default.Inventory2,
                            color = OcaGreenPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "مراجعات الإعلانات",
                            value = "${pendingListings.size}",
                            subtitle = "قيد الانتظار",
                            icon = Icons.Default.PendingActions,
                            color = OcaAmberTertiary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "طلبات الشحن",
                            value = "${pendingRecharges.size}",
                            subtitle = "بريدي موب وCCP",
                            icon = Icons.Default.Payment,
                            color = Color(0xFF0284C7),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "المستخدمون النشطون",
                            value = "2,480",
                            subtitle = "عبر 69 ولاية",
                            icon = Icons.Default.People,
                            color = OcaNavySecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Quick Navigation Hub
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        AdminHubItem(icon = Icons.Default.ListAlt, label = "كل الإعلانات", onClick = onNavigateToListingsAdmin)
                        AdminHubItem(icon = Icons.Default.AccountBalanceWallet, label = "طلبات الشحن", onClick = onNavigateToRechargesAdmin)
                        AdminHubItem(icon = Icons.Default.Group, label = "المستخدمين", onClick = onNavigateToUsersAdmin)
                    }
                }
            }

            // Pending Recharges Action Card
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "طلبات شحن معلقة (${pendingRecharges.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(onClick = onNavigateToRechargesAdmin) {
                        Text("إدارة الكل", color = OcaGreenPrimary)
                    }
                }
            }

            if (pendingRecharges.isEmpty()) {
                item {
                    Text(
                        text = "لا توجد طلبات شحن معلقة حالياً.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(pendingRecharges.take(2), key = { it.id }) { req ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "طريقة الدفع: ${req.method}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = formatPriceDZD(req.amount),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = OcaGreenPrimary
                                    )
                                )
                            }
                            Text(
                                text = "رقم المعاملة: ${req.transactionRef} • ${req.date}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = { onApproveRecharge(req.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = OcaGreenPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("الموافقة والشحن")
                                }
                            }
                        }
                    }
                }
            }

            // Pending Listings Action Card
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "إعلانات بحاجة لمراجعة (${pendingListings.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(onClick = onNavigateToListingsAdmin) {
                        Text("عرض الكل", color = OcaGreenPrimary)
                    }
                }
            }

            if (pendingListings.isEmpty()) {
                item {
                    Text(
                        text = "جميع الإعلانات مراجعة ومعتمدة.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(pendingListings.take(2), key = { it.id }) { listing ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = listing.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${listing.categoryName} • ولاية ${listing.wilayaName} • ${formatPriceDZD(listing.price)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { onRejectListing(listing.id) }) {
                                    Text("رفض", color = MaterialTheme.colorScheme.error)
                                }
                                Button(
                                    onClick = { onApproveListing(listing.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = OcaGreenPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("موافقة ونشر")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminHubItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(OcaGreenPrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = OcaGreenPrimary, modifier = Modifier.size(22.dp))
        }
        Text(text = label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
    }
}
