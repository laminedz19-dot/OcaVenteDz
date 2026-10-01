package com.example.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.formatDzd
import com.example.ui.components.formatTimeAgo
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.VerifiedBlue
import com.example.ui.viewmodel.MarketplaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerProfileScreen(
    sellerId: String,
    viewModel: MarketplaceViewModel,
    onBack: () -> Unit,
    onAdClick: (String) -> Unit,
    onOpenChat: (listingId: String, sellerId: String) -> Unit
) {
    val allUsers by viewModel.allUsers.collectAsState()
    val allListings by viewModel.publishedListings.collectAsState()

    val seller = allUsers.find { it.id == sellerId }
    val sellerAds = allListings.filter { it.userId == sellerId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(seller?.name ?: "ملف البائع", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        modifier = Modifier.testTag("seller_profile_screen")
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Seller Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = (seller?.name?.take(1) ?: "ب"),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = seller?.name ?: "بائع معتمد",
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp
                            )
                            if (seller?.isVerified == true) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Verified, contentDescription = "موثق", tint = VerifiedBlue, modifier = Modifier.size(18.dp))
                            }
                        }

                        Text(
                            text = "عضو في OcaVenteDz • ${sellerAds.size} إعلانات منشورة",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (sellerAds.isNotEmpty()) {
                            Button(
                                onClick = { onOpenChat(sellerAds.first().id, sellerId) },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مراسلة البائع")
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "إعلانات هذا البائع (${sellerAds.size}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            if (sellerAds.isEmpty()) {
                item {
                    Text("لا توجد إعلانات منشورة لهذا البائع حالياً.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(sellerAds, key = { it.id }) { ad ->
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
                                    .size(76.dp)
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
                                    .height(76.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(ad.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                    Text(formatDzd(ad.priceDzd), color = EmeraldPrimary, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                }
                                Text("${ad.wilayaName} • ${formatTimeAgo(ad.createdAt)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
