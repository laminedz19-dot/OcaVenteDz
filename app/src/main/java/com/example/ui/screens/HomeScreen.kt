package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AlgerianWilayas
import com.example.model.Category
import com.example.model.Listing
import com.example.ui.components.ListingCard
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenAdminSheet: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val listings by viewModel.filteredListings.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val selectedWilaya by viewModel.selectedWilaya.collectAsState()
    val selectedCatId by viewModel.selectedCategoryId.collectAsState()

    var showWilayaDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            shadowElevation = 2.dp,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Image(
                                painter = painterResource(id = com.example.R.drawable.app_logo),
                                contentDescription = "OcaVenteDz Logo",
                                modifier = Modifier.fillMaxSize().padding(2.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "OcaVente",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0A346C)
                                )
                                Text(
                                    text = "Dz",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF00A859)
                                )
                            }
                            Text(
                                text = "بيع وشراء المستعمل بكل ثقة",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = onOpenAdminSheet,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.secondary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Portal",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تطبيق الإدارة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Wilaya Selector & Search Bar
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Wilaya Filter Pill
                    OutlinedButton(
                        onClick = { showWilayaDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Wilaya",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedWilaya,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select Wilaya",
                            tint = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search Trigger Bar
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigateTo(AppScreen.SEARCH) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "ابحث عن سيارة، شقة، هاتف، أو خدمة...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // Hero Promo Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF008751),
                                        Color(0xFF005834)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF59E0B),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = "100% بنية مستقلة",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "انشر إعلانك مجاناً عبر ocaventeDz",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "تواصل مباشر عبر الهاتف أو الواتساب، شحن رصيد سهل ببريدي موب وCCP",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { viewModel.navigateTo(AppScreen.ADD_LISTING) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF008751)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("أضف إعلانك الآن", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Categories Row
            item {
                Column(modifier = Modifier.padding(top = 20.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "التصنيفات الرئيسية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedCatId != null) {
                            TextButton(onClick = { viewModel.setSelectedCategory(null) }) {
                                Text("عرض الكل", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(categories) { cat ->
                            val isSelected = selectedCatId == cat.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setSelectedCategory(cat.id) },
                                label = { Text(cat.nameAr, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (cat.slug) {
                                            "vehicles" -> Icons.Default.DirectionsCar
                                            "real-estate" -> Icons.Default.Apartment
                                            "phones-tech" -> Icons.Default.Smartphone
                                            "home-appliances" -> Icons.Default.Kitchen
                                            "fashion" -> Icons.Default.Checkroom
                                            "home-decor" -> Icons.Default.Weekend
                                            else -> Icons.Default.Category
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Listings Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "أحدث الإعلانات (${listings.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Listings Grid
            if (listings.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد إعلانات مطابقة في هذا التصنيف أو الولاية",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(listings.chunked(2)) { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (item in pair) {
                            Box(modifier = Modifier.weight(1f)) {
                                ListingCard(
                                    listing = item,
                                    isFavorite = favoriteIds.contains(item.id),
                                    onToggleFavorite = { viewModel.toggleFavorite(item.id) },
                                    onClick = { viewModel.openListingDetail(item) }
                                )
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    // Wilaya Picker Dialog
    if (showWilayaDialog) {
        AlertDialog(
            onDismissRequest = { showWilayaDialog = false },
            title = { Text("اختر الولاية (58 ولاية)") },
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
                                .padding(vertical = 12.dp),
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
                TextButton(onClick = { showWilayaDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }
}
