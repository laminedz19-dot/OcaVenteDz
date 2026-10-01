package com.example.ui.screens.details

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.ListingEntity
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatDzd
import com.example.ui.components.formatTimeAgo
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.UrgentRed
import com.example.ui.theme.VerifiedBlue
import com.example.ui.viewmodel.MarketplaceViewModel

@Composable
fun AdDetailsScreen(
    listingId: String,
    viewModel: MarketplaceViewModel,
    onBack: () -> Unit,
    onOpenChat: (listingId: String, sellerId: String) -> Unit,
    onOpenSellerProfile: (sellerId: String) -> Unit = {},
    onOpenSecurity: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentUserId by viewModel.currentUserId.collectAsState()
    val allListings by viewModel.adminListings.collectAsState()
    val listing = allListings.find { it.id == listingId }
    val favorites by viewModel.favorites.collectAsState()
    val isFav = favorites.any { it.listingId == listingId }

    val allUsers by viewModel.allUsers.collectAsState()
    val seller = allUsers.find { it.id == listing?.userId }

    // Dialogs state
    var showOfferDialog by remember { mutableStateOf(false) }
    var offerAmountText by remember { mutableStateOf("") }

    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("FRAUD") }
    var reportComment by remember { mutableStateOf("") }

    var showReviewDialog by remember { mutableStateOf(false) }
    var reviewRating by remember { mutableIntStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }

    if (listing == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("الإعلان غير متوفر أو تم حذفه.", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = onBack) { Text("العودة") }
            }
        }
        return
    }

    val images = remember(listing.imagesJson) {
        listing.imagesJson.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
    var selectedImageIndex by remember { mutableIntStateOf(0) }
    val isOwner = listing.userId == currentUserId

    // Dialog: Send Price Offer
    if (showOfferDialog) {
        AlertDialog(
            onDismissRequest = { showOfferDialog = false },
            title = { Text("تقديم عرض سعر للبائع", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("السعر المطلوب: ${formatDzd(listing.priceDzd)}", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = offerAmountText,
                        onValueChange = { offerAmountText = it },
                        label = { Text("عرضك بالدينار الجزائري (دج)") },
                        placeholder = { Text("مثال: ${listing.priceDzd - (listing.priceDzd * 0.1).toLong()}") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = offerAmountText.toLongOrNull() ?: 0
                        if (amount > 0) {
                            viewModel.sendPriceOffer(listing.id, listing.userId, amount)
                            showOfferDialog = false
                            onOpenChat(listing.id, listing.userId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("إرسال العرض")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOfferDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Dialog: Report Listing
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("الإبلاغ عن هذا الإعلان", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("اختر سبب الإبلاغ لمراجعة الإدارة ومكافحة الاحتيال:", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    val reasons = listOf(
                        "FRAUD" to "احتيال أو نصب",
                        "PROHIBITED_ITEM" to "منتج محظور قانونياً في الجزائر",
                        "FALSE_INFO" to "معلومات كاذبة أو مضللة",
                        "DUPLICATE" to "إعلان مكرر",
                        "FAKE_PRICE" to "سعر وهمي أو غير حقيقي",
                        "INAPPROPRIATE" to "محتوى أو صور غير مناسبة",
                        "OTHER" to "سبب آخر"
                    )

                    reasons.forEach { (code, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reportReason = code }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(selected = reportReason == code, onClick = { reportReason = code })
                            Text(label, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reportComment,
                        onValueChange = { reportComment = it },
                        placeholder = { Text("تفاصيل إضافية عن المخالفة...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitReport(listing.id, listing.userId, reportReason, reportComment)
                        showReportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UrgentRed)
                ) {
                    Text("إرسال البلاغ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Dialog: Add Review
    if (showReviewDialog) {
        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            title = { Text("تقييم البائع ${seller?.name ?: ""}", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("كيف كانت تجربتك في التعامل معه؟", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // 5-Star Row
                    Row(horizontalArrangement = Arrangement.Center) {
                        for (i in 1..5) {
                            IconButton(onClick = { reviewRating = i }) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "$i نجوم",
                                    tint = if (i <= reviewRating) GoldSecondary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = reviewComment,
                        onValueChange = { reviewComment = it },
                        placeholder = { Text("اكتب رأيك بأمانة (السلعة، المصداقية، المواعيد)...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitReview(listing.userId, listing.id, reviewRating, reviewComment)
                        showReviewDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("إرسال التقييم")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReviewDialog = false }) { Text("إلغاء") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ad_details_screen"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Top Image Gallery Carousel
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
            ) {
                AsyncImage(
                    model = images.getOrNull(selectedImageIndex) ?: "",
                    contentDescription = listing.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top navigation overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Share
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            IconButton(onClick = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "شاهد إعلان '${listing.title}' بسعر ${formatDzd(listing.priceDzd)} على OcaVenteDz")
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = Color.White)
                            }
                        }

                        // Bookmark
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            IconButton(onClick = { viewModel.toggleFavorite(listing.id, isFav) }) {
                                Icon(
                                    imageVector = if (isFav) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "مفضلة",
                                    tint = if (isFav) GoldLight else Color.White
                                )
                            }
                        }
                    }
                }

                // Thumbnails indicator if multiple images
                if (images.size > 1) {
                    LazyRow(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(images.indices.toList()) { index ->
                            Box(
                                modifier = Modifier
                                    .size(if (selectedImageIndex == index) 10.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(if (selectedImageIndex == index) Color.White else Color.White.copy(alpha = 0.5f))
                                    .clickable { selectedImageIndex = index }
                            )
                        }
                    }
                }
            }
        }

        // Status Banner if not normal published
        if (listing.status != "PUBLISHED") {
            item {
                Surface(
                    color = when (listing.status) {
                        "UNDER_REVIEW" -> Color(0xFFFFF8E1)
                        "REJECTED" -> Color(0xFFFFEBEE)
                        "SOLD" -> Color(0xFFECEFF1)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusBadge(listing.status)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (listing.status) {
                                    "UNDER_REVIEW" -> "هذا الإعلان قيد المراجعة الإدارية وسينشر قريباً."
                                    "REJECTED" -> "تم رفض هذا الإعلان من طرف المشرف."
                                    "SOLD" -> "تم بيع هذا المنتج وإغلاق الإعلان."
                                    else -> "حالة الإعلان: ${listing.status}"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        if (listing.status == "REJECTED" && listing.rejectionReason.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("سبب الرفض: ${listing.rejectionReason}", color = UrgentRed, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Title & Price Section
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                // Category & Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${listing.categoryNameAr} > ${listing.subcategory}",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formatTimeAgo(listing.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Price & Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDzd(listing.priceDzd),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = EmeraldPrimary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (listing.isNegotiable) {
                            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp)) {
                                Text("قابل للتفاوض", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = when (listing.condition) {
                                    "NEW" -> "جديد بالعلبة"
                                    "LIKE_NEW" -> "كالجديد"
                                    else -> "مستعمل"
                                },
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Location
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${listing.commune}، ولاية ${listing.wilayaName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                // Description
                Text(
                    text = "وصف الإعلان",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = listing.description,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 22.sp
                )

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                // Seller Card with link to detailed seller profile
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "معلومات البائع",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "عرض الملف والتقييمات ←",
                        color = EmeraldPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onOpenSellerProfile(seller?.id ?: listing.userId) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenSellerProfile(seller?.id ?: listing.userId) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = EmeraldPrimary,
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = (seller?.name ?: listing.userName).take(1),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = seller?.name ?: listing.userName,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        if (seller?.isVerified == true) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = "موثوق",
                                                tint = VerifiedBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = GoldSecondary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "${seller?.sellerRating ?: 5.0} (${seller?.reviewsCount ?: 0} تقييم)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "• ${seller?.adsCount ?: 1} إعلان",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (!isOwner) {
                                OutlinedButton(
                                    onClick = { showReviewDialog = true },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("تقييم", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Safe Trading Tip Card linking to Security Center
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenSecurity),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldDark.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldPrimary,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("دليل المعاملات الآمنة في الجزائر 🛡️", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EmeraldPrimary)
                            Text("لا تدفع عبر بريدي موب قبل فحص السلعة. التقي في مكان عام. اضغط للمزيد.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions for Buyer vs Owner
                if (isOwner) {
                    Text("خيارات صاحب الإعلان:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.markAdAsSold(listing.id) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("تحديد كـ تم البيع ✓")
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.adminDeleteListing(listing.id)
                                onBack()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = UrgentRed)
                        ) {
                            Text("حذف")
                        }
                    }
                } else {
                    // Contact Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // In-App Chat button
                        Button(
                            onClick = { onOpenChat(listing.id, listing.userId) },
                            modifier = Modifier.weight(1f).height(50.dp).testTag("chat_seller_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("محادثة البائع", fontWeight = FontWeight.Bold)
                        }

                        // Send Price Offer button
                        OutlinedButton(
                            onClick = { showOfferDialog = true },
                            modifier = Modifier.height(50.dp).testTag("offer_price_button")
                        ) {
                            Icon(Icons.Default.LocalOffer, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تقديم عرض", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Call Button (If seller enabled phone visibility)
                    if (listing.isPhoneVisible && listing.userPhone.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${listing.userPhone.replace(" ", "")}")
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("اتصال هاتفي (${listing.userPhone})", color = EmeraldPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Report Listing Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showReportDialog = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Report, contentDescription = null, tint = UrgentRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("الإبلاغ عن احتيال أو محتوى غير لائق في هذا الإعلان", color = UrgentRed, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
