package com.example.ui.screens.listings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.EmptyState
import com.example.ui.components.OcaSubTopBar
import com.example.ui.components.formatPriceDZD
import com.example.ui.model.Listing
import com.example.ui.model.ListingStatus
import com.example.ui.theme.OcaAmberTertiary
import com.example.ui.theme.OcaGreenPrimary

@Composable
fun MyListingsScreen(
    userListings: List<Listing>,
    onListingClick: (Listing) -> Unit,
    onEditListing: (Listing) -> Unit,
    onDeleteListing: (String) -> Unit,
    onCreateAdClick: () -> Unit,
    onBackClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(ListingStatus.PUBLISHED) }
    var itemToDelete by remember { mutableStateOf<Listing?>(null) }

    val filteredListings = remember(userListings, selectedTab) {
        userListings.filter { it.status == selectedTab }
    }

    Scaffold(
        topBar = {
            OcaSubTopBar(
                title = "إعلاناتي (${userListings.size})",
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateAdClick,
                containerColor = OcaGreenPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_listing")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة إعلان جديد")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Status Tabs
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                ListingStatus.entries.forEach { status ->
                    val count = userListings.count { it.status == status }
                    Tab(
                        selected = selectedTab == status,
                        onClick = { selectedTab = status },
                        text = {
                            Text(
                                text = "${status.labelAr} ($count)",
                                fontWeight = if (selectedTab == status) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            if (filteredListings.isEmpty()) {
                EmptyState(
                    title = "لا توجد إعلانات ${selectedTab.labelAr}",
                    message = "ليس لديك إعلانات مسجلة في هذا القسم حالياً.",
                    icon = Icons.Default.Inventory2,
                    actionText = "أضف إعلانك الآن",
                    onActionClick = onCreateAdClick
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredListings, key = { it.id }) { listing ->
                        MyListingItemCard(
                            listing = listing,
                            onView = { onListingClick(listing) },
                            onEdit = { onEditListing(listing) },
                            onDelete = { itemToDelete = listing }
                        )
                    }
                }
            }
        }

        ConfirmDialog(
            show = itemToDelete != null,
            title = "حذف الإعلان",
            message = "هل أنت متأكد من رغبتك في حذف الإعلان '${itemToDelete?.title}'؟ لا يمكن التراجع عن هذا الإجراء.",
            confirmText = "حذف",
            isDestructive = true,
            onConfirm = {
                itemToDelete?.let { onDeleteListing(it.id) }
                itemToDelete = null
            },
            onDismiss = { itemToDelete = null }
        )
    }
}

@Composable
private fun MyListingItemCard(
    listing: Listing,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (listing.status) {
                        ListingStatus.PUBLISHED -> OcaGreenPrimary.copy(alpha = 0.15f)
                        ListingStatus.PENDING -> OcaAmberTertiary.copy(alpha = 0.15f)
                        ListingStatus.REJECTED -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        ListingStatus.SOLD -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    }
                ) {
                    Text(
                        text = listing.status.labelAr,
                        color = when (listing.status) {
                            ListingStatus.PUBLISHED -> OcaGreenPrimary
                            ListingStatus.PENDING -> OcaAmberTertiary
                            ListingStatus.REJECTED -> MaterialTheme.colorScheme.error
                            ListingStatus.SOLD -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = listing.createdAt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = listing.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Text(
                text = formatPriceDZD(listing.price),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = OcaGreenPrimary
                )
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Action row (View, Edit, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }

                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تعديل")
                }

                Button(
                    onClick = onView,
                    colors = ButtonDefaults.buttonColors(containerColor = OcaGreenPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("عرض")
                }
            }
        }
    }
}
