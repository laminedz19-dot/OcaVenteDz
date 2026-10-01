package com.example.ui.screens.offers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.EmptyState
import com.example.ui.components.OcaSubTopBar
import com.example.ui.components.formatPriceDZD
import com.example.ui.model.Offer
import com.example.ui.theme.OcaAmberTertiary
import com.example.ui.theme.OcaGreenPrimary

@Composable
fun OffersScreen(
    offers: List<Offer>,
    onAcceptOffer: (String) -> Unit,
    onRejectOffer: (String) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            OcaSubTopBar(
                title = "العروض والتفاوض (${offers.size})",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        if (offers.isEmpty()) {
            EmptyState(
                title = "لا توجد عروض أسعار حالياً",
                message = "عندما يقدم المشترون عروض أسعار على إعلاناتك المعروضة للتفاوض، ستظهر طلباتهم هنا للموافقة أو الرفض.",
                icon = Icons.Default.LocalOffer,
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(offers, key = { it.id }) { offer ->
                    OfferItemCard(
                        offer = offer,
                        onAccept = { onAcceptOffer(offer.id) },
                        onReject = { onRejectOffer(offer.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OfferItemCard(
    offer: Offer,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "عرض من: ${offer.buyerName}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = offer.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = offer.listingTitle,
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("السعر الأصلي", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatPriceDZD(offer.originalPrice), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                }

                Column {
                    Text("العرض المقترح", style = MaterialTheme.typography.bodySmall, color = OcaGreenPrimary)
                    Text(formatPriceDZD(offer.offerAmount), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = OcaGreenPrimary))
                }
            }

            if (offer.status == "معلق") {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onReject) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("رفض العرض", color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(containerColor = OcaGreenPrimary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("قبول")
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (offer.status == "مقبول") OcaGreenPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "الحالة: ${offer.status}",
                        color = if (offer.status == "مقبول") OcaGreenPrimary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
