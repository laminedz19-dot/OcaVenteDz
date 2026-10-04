package com.example.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.models.CategoriesData
import com.example.ui.components.formatDzd
import com.example.ui.components.formatTimeAgo
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.MarketplaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MarketplaceViewModel,
    onAdClick: (String) -> Unit
) {
    val listings by viewModel.publishedListings.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCat by viewModel.selectedCategory.collectAsState()
    val selectedWilaya by viewModel.selectedWilaya.collectAsState()

    var queryText by remember(searchQuery) { mutableStateOf(searchQuery) }

    val filteredListings = listings.filter { ad ->
        val matchesQuery = queryText.isBlank() || ad.title.contains(queryText, ignoreCase = true) || ad.description.contains(queryText, ignoreCase = true)
        val matchesCat = selectedCat == null || ad.categoryId == selectedCat
        val matchesWilaya = selectedWilaya == null || ad.wilayaCode == selectedWilaya
        matchesQuery && matchesCat && matchesWilaya
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("search_screen")
    ) {
        // Search Input Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                OutlinedTextField(
                    value = queryText,
                    onValueChange = {
                        queryText = it
                        viewModel.searchQuery.value = it
                    },
                    label = { Text("بحث في OcaVenteDz") },
                    placeholder = { Text("اسم السلعة، الموديل، الولاية...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldPrimary) },
                    trailingIcon = {
                        if (queryText.isNotBlank()) {
                            IconButton(onClick = {
                                queryText = ""
                                viewModel.searchQuery.value = ""
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Categories Filters
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCat == null,
                            onClick = { viewModel.selectedCategory.value = null },
                            label = { Text("الكل") }
                        )
                    }
                    items(CategoriesData.allCategories) { cat ->
                        FilterChip(
                            selected = selectedCat == cat.id,
                            onClick = {
                                viewModel.selectedCategory.value = if (selectedCat == cat.id) null else cat.id
                            },
                            label = { Text(cat.nameAr) }
                        )
                    }
                }
            }
        }

        // Search Results
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "النتائج (${filteredListings.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (selectedCat != null || selectedWilaya != null || queryText.isNotBlank()) {
                        TextButton(
                            onClick = {
                                queryText = ""
                                viewModel.searchQuery.value = ""
                                viewModel.selectedCategory.value = null
                                viewModel.selectedWilaya.value = null
                            }
                        ) {
                            Text("إعادة ضبط الفلاتر", fontSize = 12.sp, color = EmeraldPrimary)
                        }
                    }
                }
            }

            if (filteredListings.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("لم يتم العثور على نتائج تطابق بحثك", fontWeight = FontWeight.Bold)
                            Text("جرّب كلمات بحث أخرى أو قم بإلغاء بعض الفلاتر.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(filteredListings, key = { it.id }) { ad ->
                    val firstImg = ad.imagesJson.split(",").firstOrNull()?.trim() ?: ""
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAdClick(ad.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(modifier = Modifier.padding(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                if (firstImg.isNotBlank()) {
                                    AsyncImage(
                                        model = firstImg,
                                        contentDescription = ad.title,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(80.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(ad.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                    Text(formatDzd(ad.priceDzd), color = EmeraldPrimary, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(ad.wilayaName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatTimeAgo(ad.createdAt), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
