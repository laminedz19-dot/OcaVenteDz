package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.ui.model.Category
import com.example.ui.model.Listing
import com.example.ui.model.Wilaya
import com.example.ui.theme.OcaAmberTertiary
import com.example.ui.theme.OcaGreenDark
import com.example.ui.theme.OcaGreenPrimary
import com.example.ui.theme.OcaNavySecondary

@Composable
fun HomeScreen(
    listings: List<Listing>,
    categories: List<Category>,
    favoriteIds: Set<String>,
    currentWilaya: Wilaya?,
    onWilayaChanged: (Wilaya?) -> Unit,
    onListingClick: (Listing) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onCategoryClick: (Category) -> Unit,
    onSearchClick: () -> Unit,
    onViewAllCategories: () -> Unit,
    onViewAllListings: () -> Unit,
    onNotificationsClick: () -> Unit,
    unreadNotificationsCount: Int = 2
) {
    var showWilayaSheet by remember { mutableStateOf(false) }

    // Filter listings if wilaya selected
    val displayListings = remember(listings, currentWilaya) {
        if (currentWilaya == null) listings
        else listings.filter { it.wilayaCode == currentWilaya.code }
    }

    val featuredListings = remember(displayListings) {
        displayListings.filter { it.isFeatured }
    }

    val latestListings = remember(displayListings) {
        displayListings
    }

    Scaffold(
        topBar = {
            OcaMainTopBar(
                currentWilaya = currentWilaya?.nameAr ?: "كل الجزائر",
                onWilayaClick = { showWilayaSheet = true },
                onNotificationsClick = onNotificationsClick,
                unreadNotificationsCount = unreadNotificationsCount
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("home_screen_list"),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Search Bar Header
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    OcaSearchBar(
                        query = "",
                        onQueryChange = {},
                        readOnly = true,
                        onClick = onSearchClick,
                        onFilterClick = onSearchClick
                    )
                }
            }

            // Promotional Algerian Marketplace Hero Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(OcaNavySecondary, Color(0xFF0F766E))
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = OcaAmberTertiary,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "OcaVenteDz",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Text(
                                        text = "انشر إعلانك بكل بساطة",
                                        color = Color.White.copy(alpha = 0.95f),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }

                                Text(
                                    text = "تصفح آلاف الإعلانات في كل الولايات",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                )

                                Text(
                                    text = "سيارات، هواتف، أجهزة وأثاث بأفضل الأسعار مباشرة من البائعين",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // Categories Header & Carousel
            item {
                Column {
                    SectionHeader(
                        title = "التصنيفات الرئيسية",
                        actionText = "جميع التصنيفات",
                        onActionClick = onViewAllCategories
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(categories, key = { it.id }) { category ->
                            CategoryCircleItem(
                                category = category,
                                isSelected = false,
                                onClick = { onCategoryClick(category) }
                            )
                        }
                    }
                }
            }

            // Featured Listings (Horizontal cards)
            if (featuredListings.isNotEmpty()) {
                item {
                    Column {
                        SectionHeader(
                            title = "إعلانات مميزة ⭐",
                            actionText = null
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(featuredListings, key = { "feat_${it.id}" }) { listing ->
                                Box(modifier = Modifier.width(220.dp)) {
                                    ListingCard(
                                        listing = listing,
                                        isFavorite = favoriteIds.contains(listing.id),
                                        onFavoriteClick = { onToggleFavorite(listing.id) },
                                        onClick = { onListingClick(listing) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Latest Listings Section
            item {
                SectionHeader(
                    title = if (currentWilaya != null) "إعلانات ولاية ${currentWilaya.nameAr}" else "أحدث الإعلانات في الجزائر",
                    actionText = if (latestListings.size > 4) "مشاهدة المزيد" else null,
                    onActionClick = onViewAllListings
                )
            }

            // Grid of Latest Listings
            if (latestListings.isEmpty()) {
                item {
                    EmptyState(
                        title = "لا توجد إعلانات حالياً في هذه الولاية",
                        message = "كن أول من ينشر إعلاناً في ${currentWilaya?.nameAr ?: "هذه المنطقة"} أو قم بتغيير الولاية للبحث في كل الجزائر.",
                        actionText = "عرض كل الجزائر",
                        onActionClick = { onWilayaChanged(null) }
                    )
                }
            } else {
                // Display in pairs (2 columns)
                val chunkedListings = latestListings.chunked(2)
                items(chunkedListings) { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
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
                        // If odd number in row, fill with empty spacer
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Wilaya Picker BottomSheet
        if (showWilayaSheet) {
            WilayaPickerBottomSheet(
                selectedWilayaCode = currentWilaya?.code,
                onWilayaSelected = { wilaya ->
                    onWilayaChanged(wilaya)
                },
                onDismiss = { showWilayaSheet = false }
            )
        }
    }
}
