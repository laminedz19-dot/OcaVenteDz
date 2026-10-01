package com.example.ui.screens.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.components.EmptyState
import com.example.ui.components.ListingHorizontalCard
import com.example.ui.components.OcaSubTopBar
import com.example.ui.model.Listing

@Composable
fun FavoritesScreen(
    favoriteListings: List<Listing>,
    onToggleFavorite: (String) -> Unit,
    onListingClick: (Listing) -> Unit,
    onExploreClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            OcaSubTopBar(
                title = "قائمة المفضلة (${favoriteListings.size})",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        if (favoriteListings.isEmpty()) {
            EmptyState(
                title = "قائمة المفضلة فارغة",
                message = "لم تقم بحفظ أي إعلان بعد. اضغط على أيقونة القلب في أي إعلان لحفظه هنا والرجوع إليه لاحقاً.",
                icon = Icons.Default.FavoriteBorder,
                actionText = "استكشف الإعلانات",
                onActionClick = onExploreClick,
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(favoriteListings, key = { it.id }) { listing ->
                    ListingHorizontalCard(
                        listing = listing,
                        isFavorite = true,
                        onFavoriteClick = { onToggleFavorite(listing.id) },
                        onClick = { onListingClick(listing) }
                    )
                }
            }
        }
    }
}
