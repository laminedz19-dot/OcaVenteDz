package com.example.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.EmptyState
import com.example.ui.components.ListingCard
import com.example.ui.components.OcaSubTopBar
import com.example.ui.model.Listing
import com.example.ui.theme.OcaGreenPrimary

@Composable
fun SearchResultsScreen(
    filter: SearchFilterState,
    allListings: List<Listing>,
    favoriteIds: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onListingClick: (Listing) -> Unit,
    onOpenFilter: () -> Unit,
    onBackClick: () -> Unit
) {
    val filteredListings = remember(filter, allListings) {
        allListings.filter { item ->
            val matchQuery = if (filter.query.isBlank()) true
            else item.title.contains(filter.query, ignoreCase = true) || item.description.contains(filter.query, ignoreCase = true)

            val matchCategory = if (filter.categoryId == null) true
            else item.categoryId == filter.categoryId

            val matchWilaya = if (filter.wilayaCode == null) true
            else item.wilayaCode == filter.wilayaCode

            val matchMin = if (filter.minPrice == null) true
            else item.price >= filter.minPrice

            val matchMax = if (filter.maxPrice == null) true
            else item.price <= filter.maxPrice

            val matchCondition = if (filter.condition == null) true
            else item.condition == filter.condition

            matchQuery && matchCategory && matchWilaya && matchMin && matchMax && matchCondition
        }.let { list ->
            when (filter.sortOrder) {
                SortOrder.LATEST -> list
                SortOrder.PRICE_LOW_TO_HIGH -> list.sortedBy { it.price }
                SortOrder.PRICE_HIGH_TO_LOW -> list.sortedByDescending { it.price }
            }
        }
    }

    Scaffold(
        topBar = {
            OcaSubTopBar(
                title = if (filter.query.isNotBlank()) "نتائج: ${filter.query}" else "نتائج البحث",
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = onOpenFilter) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "تعديل الفلاتر",
                            tint = OcaGreenPrimary
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Summary Header
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "عُثر على ${filteredListings.size} إعلان",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = filter.sortOrder.labelAr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (filteredListings.isEmpty()) {
                EmptyState(
                    title = "لم يتم العثور على أي نتائج",
                    message = "جرب تغيير مصطلحات البحث أو توسيع نطاق السعر والولاية.",
                    actionText = "تعديل الفلاتر",
                    onActionClick = onOpenFilter
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val chunks = filteredListings.chunked(2)
                    items(chunks) { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (item in rowItems) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ListingCard(
                                        listing = item,
                                        isFavorite = favoriteIds.contains(item.id),
                                        onFavoriteClick = { onToggleFavorite(item.id) },
                                        onClick = { onListingClick(item) }
                                    )
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
