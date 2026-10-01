package com.example.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.EmptyState
import com.example.ui.components.OcaSubTopBar
import com.example.ui.components.formatPriceDZD
import com.example.ui.model.Listing
import com.example.ui.model.ListingStatus
import com.example.ui.theme.OcaGreenPrimary

@Composable
fun AdminListingsScreen(
    listings: List<Listing>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onDelete: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf<ListingStatus?>(null) }

    val filtered = remember(listings, selectedFilter) {
        if (selectedFilter == null) listings
        else listings.filter { it.status == selectedFilter }
    }

    Scaffold(
        topBar = {
            OcaSubTopBar(
                title = "إدارة جميع الإعلانات (${listings.size})",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = if (selectedFilter == null) 0 else selectedFilter!!.ordinal + 1,
                edgePadding = 16.dp,
                divider = {}
            ) {
                Tab(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    text = { Text("الكل (${listings.size})") }
                )
                ListingStatus.entries.forEach { status ->
                    val count = listings.count { it.status == status }
                    Tab(
                        selected = selectedFilter == status,
                        onClick = { selectedFilter = status },
                        text = { Text("${status.labelAr} ($count)") }
                    )
                }
            }

            if (filtered.isEmpty()) {
                EmptyState(
                    title = "لا توجد إعلانات مطابقة",
                    message = "لا توجد إعلانات ضمن هذا الفلتر حالياً."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.id }) { item ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = item.status.labelAr,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "البائع: ${item.sellerName} (${item.sellerPhone}) • ${item.wilayaName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = formatPriceDZD(item.price),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = OcaGreenPrimary
                                    )
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { onDelete(item.id) }) {
                                        Text("حذف نهائي", color = MaterialTheme.colorScheme.error)
                                    }
                                    if (item.status != ListingStatus.REJECTED) {
                                        TextButton(onClick = { onReject(item.id) }) {
                                            Text("رفض")
                                        }
                                    }
                                    if (item.status != ListingStatus.PUBLISHED) {
                                        Button(
                                            onClick = { onApprove(item.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = OcaGreenPrimary)
                                        ) {
                                            Text("اعتماد ونشر")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
