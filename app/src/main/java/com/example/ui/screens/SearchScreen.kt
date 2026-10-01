package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AlgerianWilayas
import com.example.ui.components.ListingCard
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: MainViewModel) {
    BackHandler {
        viewModel.navigateTo(AppScreen.HOME)
    }

    val query by viewModel.searchQuery.collectAsState()
    val selectedWilaya by viewModel.selectedWilaya.collectAsState()
    val selectedCatId by viewModel.selectedCategoryId.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val listings by viewModel.filteredListings.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()

    var showWilayaDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("بحث متقدم في ocaventeDz", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.HOME) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // Search Input
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("ابحث بالعنوان أو الوصف...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )

            // Wilaya Selector Button
            OutlinedButton(
                onClick = { showWilayaDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "الولاية: $selectedWilaya", maxLines = 1)
            }

            // Categories Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCatId == null,
                        onClick = { viewModel.setSelectedCategory(null) },
                        label = { Text("كل التصنيفات") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                items(categories) { cat ->
                    val isSelected = selectedCatId == cat.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setSelectedCategory(cat.id) },
                        label = { Text(cat.nameAr) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Results count
            Text(
                text = "النتائج (${listings.size})",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            // Results list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (listings.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لم يتم العثور على أي إعلان يطابق بحثك",
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    items(listings) { item ->
                        ListingCard(
                            listing = item,
                            isFavorite = favoriteIds.contains(item.id),
                            onToggleFavorite = { viewModel.toggleFavorite(item.id) },
                            onClick = { viewModel.openListingDetail(item) }
                        )
                    }
                }
            }
        }
    }

    if (showWilayaDialog) {
        AlertDialog(
            onDismissRequest = { showWilayaDialog = false },
            title = { Text("اختر الولاية") },
            text = {
                val wilayasList = listOf("كل الولايات (58 ولاية)") + AlgerianWilayas.all
                LazyColumn(modifier = Modifier.height(350.dp)) {
                    items(wilayasList) { w ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setSelectedWilaya(w)
                                    showWilayaDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedWilaya == w,
                                onClick = {
                                    viewModel.setSelectedWilaya(w)
                                    showWilayaDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = w, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWilayaDialog = false }) { Text("إغلاق") }
            }
        )
    }
}
