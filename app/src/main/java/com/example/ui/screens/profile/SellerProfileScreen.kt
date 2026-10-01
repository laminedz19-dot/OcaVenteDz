package com.example.ui.screens.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.*
import com.example.ui.model.Listing
import com.example.ui.theme.OcaGreenPrimary

@Composable
fun SellerProfileScreen(
    sellerName: String,
    sellerPhone: String,
    sellerWilaya: String,
    sellerRating: Float,
    sellerListings: List<Listing>,
    favoriteIds: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onListingClick: (Listing) -> Unit,
    onContactChat: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            OcaSubTopBar(
                title = "ملف البائع",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Seller Header Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        UserAvatar(name = sellerName, size = 72.dp)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = sellerName,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "موثق",
                                    tint = OcaGreenPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "ولاية $sellerWilaya • بائع معتمد على OcaVenteDz",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Rating Card
                        RatingCard(
                            rating = sellerRating,
                            totalReviews = 18
                        )

                        // Contact Buttons (Chat & Call)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onContactChat,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OcaGreenPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ChatBubble, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مراسلة")
                            }

                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:$sellerPhone")
                                    }
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("اتصال")
                            }
                        }
                    }
                }
            }

            // Seller Listings Header
            item {
                SectionHeader(
                    title = "إعلانات البائع (${sellerListings.size})",
                    actionText = null
                )
            }

            // Grid of Listings
            if (sellerListings.isEmpty()) {
                item {
                    EmptyState(
                        title = "لا توجد إعلانات أخرى",
                        message = "لا توجد إعلانات نشطة لهذا البائع حالياً."
                    )
                }
            } else {
                val chunks = sellerListings.chunked(2)
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
