package com.example.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.ListingEntity
import com.example.data.models.AlgeriaWilayas
import com.example.data.models.CategoriesData
import com.example.ui.components.WilayaPickerSheet
import com.example.ui.components.formatDzd
import com.example.ui.components.formatTimeAgo
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.UrgentRed
import com.example.ui.viewmodel.MarketplaceViewModel

private enum class QuickFilter {
    ALL, URGENT, FEATURED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MarketplaceViewModel,
    onAdClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSellClick: () -> Unit
) {
    val allListings by viewModel.publishedListings.collectAsState()
    val selectedWilayaCode by viewModel.selectedWilaya.collectAsState()
    var showWilayaSheet by remember { mutableStateOf(false) }
    var activeQuickFilter by remember { mutableStateOf(QuickFilter.ALL) }
    var isGridView by remember { mutableStateOf(false) }

    val activeWilaya = remember(selectedWilayaCode) {
        selectedWilayaCode?.let { AlgeriaWilayas.findByCode(it) }
    }

    // Filter listings based on wilaya & quick filter
    val displayedListings = remember(allListings, selectedWilayaCode, activeQuickFilter) {
        allListings.filter { ad ->
            val matchWilaya = selectedWilayaCode == null || ad.wilayaCode == selectedWilayaCode
            val matchFilter = when (activeQuickFilter) {
                QuickFilter.ALL -> true
                QuickFilter.URGENT -> ad.isUrgent
                QuickFilter.FEATURED -> ad.isFeatured
            }
            matchWilaya && matchFilter
        }
    }

    val urgentListings = remember(allListings) {
        allListings.filter { it.isUrgent || it.isFeatured }
    }

    Box(modifier = Modifier.fillMaxSize().testTag("home_screen")) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Search Bar & Wilaya selector
            item {
                HomeSearchBar(
                    activeWilayaName = activeWilaya?.nameAr,
                    onSearchClick = onSearchClick,
                    onWilayaClick = { showWilayaSheet = true },
                    onClearWilaya = { viewModel.selectedWilaya.value = null }
                )
            }

            // Quick Status Filter Chips
            item {
                QuickFilterBar(
                    activeFilter = activeQuickFilter,
                    onFilterSelected = { activeQuickFilter = it },
                    activeWilayaName = activeWilaya?.nameAr,
                    onOpenWilayas = { showWilayaSheet = true }
                )
            }

            // Promotional Banner
            item {
                HomeHeroBanner(onSellClick = onSellClick)
            }

            // Categories Section
            item {
                CategoriesSection(onCategoryClick = onCategoryClick)
            }

            // Urgent & Featured carousel
            if (urgentListings.isNotEmpty() && activeQuickFilter == QuickFilter.ALL) {
                item {
                    UrgentFeaturedSection(
                        listings = urgentListings,
                        onAdClick = onAdClick
                    )
                }
            }

            // Section Header & View Switcher
            item {
                SectionHeader(
                    count = displayedListings.size,
                    isGridView = isGridView,
                    onToggleView = { isGridView = !isGridView },
                    filterName = when (activeQuickFilter) {
                        QuickFilter.ALL -> activeWilaya?.let { "في ${it.nameAr}" } ?: "الكل"
                        QuickFilter.URGENT -> "عاجلة 🔥"
                        QuickFilter.FEATURED -> "مميزة ⭐"
                    }
                )
            }

            // Listings List or Grid View
            if (displayedListings.isEmpty()) {
                item {
                    EmptyListingsState(
                        hasFilters = selectedWilayaCode != null || activeQuickFilter != QuickFilter.ALL,
                        onResetFilters = {
                            viewModel.selectedWilaya.value = null
                            activeQuickFilter = QuickFilter.ALL
                        },
                        onSellClick = onSellClick
                    )
                }
            } else if (!isGridView) {
                items(displayedListings, key = { it.id }) { ad ->
                    ListingListItem(
                        ad = ad,
                        onClick = { onAdClick(ad.id) }
                    )
                }
            } else {
                // Two items per row in grid view
                val chunkedList = displayedListings.chunked(2)
                items(chunkedList, key = { it.first().id }) { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (ad in rowItems) {
                            Box(modifier = Modifier.weight(1f)) {
                                ListingGridCard(
                                    ad = ad,
                                    onClick = { onAdClick(ad.id) }
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

        // Full Wilaya Selection Bottom Sheet
        if (showWilayaSheet) {
            WilayaPickerSheet(
                selectedWilayaCode = selectedWilayaCode,
                selectedCommune = null,
                onDismiss = { showWilayaSheet = false },
                onWilayaAndCommuneSelected = { wilaya, _ ->
                    viewModel.selectedWilaya.value = wilaya.code
                    showWilayaSheet = false
                }
            )
        }
    }
}

@Composable
private fun HomeSearchBar(
    activeWilayaName: String?,
    onSearchClick: () -> Unit,
    onWilayaClick: () -> Unit,
    onClearWilaya: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search field container
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clickable(onClick = onSearchClick)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "ابحث عن سيارات، عقارات، هواتف...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Wilaya Quick Button
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (activeWilayaName != null) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .height(48.dp)
                    .clickable(onClick = onWilayaClick)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Wilaya",
                        tint = if (activeWilayaName != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = activeWilayaName ?: "كل الولايات",
                        color = if (activeWilayaName != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    if (activeWilayaName != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color.White,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onClearWilaya() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickFilterBar(
    activeFilter: QuickFilter,
    onFilterSelected: (QuickFilter) -> Unit,
    activeWilayaName: String?,
    onOpenWilayas: () -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = activeFilter == QuickFilter.ALL,
                onClick = { onFilterSelected(QuickFilter.ALL) },
                label = { Text("جميع العروض", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldPrimary,
                    selectedLabelColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }
        item {
            FilterChip(
                selected = activeFilter == QuickFilter.URGENT,
                onClick = { onFilterSelected(QuickFilter.URGENT) },
                label = { Text("عاجلة 🔥", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = UrgentRed,
                    selectedLabelColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }
        item {
            FilterChip(
                selected = activeFilter == QuickFilter.FEATURED,
                onClick = { onFilterSelected(QuickFilter.FEATURED) },
                label = { Text("مميزة ⭐", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GoldSecondary,
                    selectedLabelColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }
        item {
            FilterChip(
                selected = activeWilayaName != null,
                onClick = onOpenWilayas,
                label = {
                    Text(
                        text = if (activeWilayaName != null) "📍 $activeWilayaName" else "تحديد الولاية ▾",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldDark,
                    selectedLabelColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

@Composable
private fun HomeHeroBanner(onSellClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            EmeraldDark,
                            EmeraldPrimary
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = GoldSecondary,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.ElectricBolt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "بيع واشري بكل ثقة",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "انشر إعلانك مجاناً ليصل إلى آلاف المشترين عبر 69 ولاية",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onSellClick,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldSecondary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.PostAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("انشر الآن", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CategoriesSection(onCategoryClick: (String) -> Unit) {
    Column(modifier = Modifier.padding(top = 10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "الأقسام الرئيسية",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(CategoriesData.allCategories) { cat ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onCategoryClick(cat.id) }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    CategoriesData.getCategoryIcon(cat.iconName),
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = cat.nameAr,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UrgentFeaturedSection(
    listings: List<ListingEntity>,
    onAdClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(top = 14.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Whatshot, contentDescription = null, tint = UrgentRed, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("عروض مميزة وعاجلة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(listings, key = { it.id }) { ad ->
                val firstImg = ad.imagesJson.split(",").firstOrNull()?.trim() ?: ""
                Card(
                    modifier = Modifier
                        .width(200.dp)
                        .clickable { onAdClick(ad.id) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (firstImg.isNotBlank()) {
                                AsyncImage(
                                    model = firstImg,
                                    contentDescription = ad.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            if (ad.isUrgent) {
                                Surface(
                                    color = UrgentRed,
                                    shape = RoundedCornerShape(bottomStart = 8.dp),
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Text(
                                        "عاجل",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                ad.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                formatDzd(ad.priceDzd),
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "${ad.wilayaName} • ${formatTimeAgo(ad.createdAt)}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    count: Int,
    isGridView: Boolean,
    onToggleView: () -> Unit,
    filterName: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "الإعلانات المعروضة ($count)",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = filterName,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(
            onClick = onToggleView,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                contentDescription = "Toggle view",
                tint = EmeraldPrimary
            )
        }
    }
}

@Composable
private fun ListingListItem(
    ad: ListingEntity,
    onClick: () -> Unit
) {
    val firstImg = ad.imagesJson.split(",").firstOrNull()?.trim() ?: ""
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (firstImg.isNotBlank()) {
                    AsyncImage(
                        model = firstImg,
                        contentDescription = ad.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (ad.isUrgent) {
                    Surface(
                        color = UrgentRed,
                        shape = RoundedCornerShape(bottomStart = 8.dp),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text("عاجل", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(92.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        ad.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        formatDzd(ad.priceDzd),
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = if (ad.commune.isNotBlank()) "${ad.wilayaName} - ${ad.commune}" else ad.wilayaName,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        formatTimeAgo(ad.createdAt),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ListingGridCard(
    ad: ListingEntity,
    onClick: () -> Unit
) {
    val firstImg = ad.imagesJson.split(",").firstOrNull()?.trim() ?: ""
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (firstImg.isNotBlank()) {
                    AsyncImage(
                        model = firstImg,
                        contentDescription = ad.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (ad.isUrgent) {
                    Surface(
                        color = UrgentRed,
                        shape = RoundedCornerShape(bottomStart = 8.dp),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text("عاجل", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                    }
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    ad.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    formatDzd(ad.priceDzd),
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    ad.wilayaName,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyListingsState(
    hasFilters: Boolean,
    onResetFilters: () -> Unit,
    onSellClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (hasFilters) "لا توجد إعلانات مطابقة لهذا الفلتر" else "لا توجد إعلانات منشورة بعد",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (hasFilters) "جرّب اختيار ولاية أخرى أو إلغاء الفلاتر لمشاهدة جميع العروض" else "كن أول من ينشر إعلاناً في التطبيق!",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (hasFilters) {
            OutlinedButton(
                onClick = onResetFilters,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("عرض جميع الإعلانات")
            }
        } else {
            Button(
                onClick = onSellClick,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("نشر إعلان جديد")
            }
        }
    }
}
