package com.example.ui.screens.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.components.*
import com.example.ui.model.*
import com.example.ui.theme.OcaGreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAdScreen(
    categories: List<Category>,
    defaultPhone: String,
    defaultWilayaCode: Int,
    onCreateAd: (Listing) -> Unit,
    onBackClick: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var isNegotiable by remember { mutableStateOf(true) }
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id ?: "phones") }
    var selectedCategoryName by remember { mutableStateOf(categories.firstOrNull()?.nameAr ?: "هواتف") }
    var selectedWilayaCode by remember { mutableIntStateOf(defaultWilayaCode) }
    var selectedWilayaName by remember {
        mutableStateOf(WilayasData.allWilayas.find { it.code == defaultWilayaCode }?.nameAr ?: "الجزائر")
    }
    var commune by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf(defaultPhone) }
    var condition by remember { mutableStateOf(ItemCondition.LIKE_NEW) }
    var showWilayaSheet by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPreviewMode by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            OcaSubTopBar(
                title = if (isPreviewMode) "معاينة الإعلان" else "إضافة إعلان جديد",
                onBackClick = {
                    if (isPreviewMode) isPreviewMode = false else onBackClick()
                },
                actions = {
                    TextButton(onClick = { isPreviewMode = !isPreviewMode }) {
                        Text(
                            text = if (isPreviewMode) "تعديل" else "معاينة",
                            color = OcaGreenPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (isPreviewMode) {
            // Preview Mode
            val previewListing = Listing(
                id = "preview-${System.currentTimeMillis()}",
                title = title.ifBlank { "عنوان تجريبي للإعلان" },
                price = priceText.toDoubleOrNull() ?: 15000.0,
                isNegotiable = isNegotiable,
                categoryId = selectedCategoryId,
                categoryName = selectedCategoryName,
                wilayaCode = selectedWilayaCode,
                wilayaName = selectedWilayaName,
                commune = commune,
                description = description.ifBlank { "وصف تفصيلي للسلعة المعروضة للبيع" },
                condition = condition,
                createdAt = "الآن",
                sellerId = "current-user",
                sellerName = "أنت (البائع)",
                sellerPhone = phone,
                sellerRating = 5.0f,
                isFeatured = false
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "هكذا سيظهر إعلانك للمشترين على OcaVenteDz",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                ListingCard(
                    listing = previewListing,
                    isFavorite = false,
                    onFavoriteClick = {},
                    onClick = {}
                )

                Spacer(modifier = Modifier.height(16.dp))

                PrimaryButton(
                    text = "نشر الإعلان الآن",
                    icon = Icons.Default.Publish,
                    onClick = {
                        onCreateAd(previewListing)
                        showSuccessDialog = true
                    },
                    testTag = "preview_publish_btn"
                )
            }
        } else {
            // Form Mode
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Photo upload placeholder area
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(OcaGreenPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = OcaGreenPrimary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("أضف صور السلعة (حتى 5 صور)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        Text("الصور الواضحة تزيد فرص البيع بنسبة 80%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; errorMessage = null },
                    label = { Text("عنوان الإعلان *") },
                    placeholder = { Text("مثال: هاتف iPhone 14 Pro Max نظيف جداً") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("ad_title_input")
                )

                // Category Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("اختر التصنيف *", style = MaterialTheme.typography.titleSmall)
                    ScrollableTabRow(
                        selectedTabIndex = categories.indexOfFirst { it.id == selectedCategoryId }.coerceAtLeast(0),
                        edgePadding = 0.dp,
                        indicator = {},
                        divider = {}
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = {
                                    selectedCategoryId = cat.id
                                    selectedCategoryName = cat.nameAr
                                },
                                label = { Text(cat.nameAr) },
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }
                    }
                }

                // Price and Negotiable
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it; errorMessage = null },
                        label = { Text("السعر (دج) *") },
                        placeholder = { Text("مثال: 45000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.3f).testTag("ad_price_input")
                    )

                    FilterChip(
                        selected = isNegotiable,
                        onClick = { isNegotiable = !isNegotiable },
                        label = { Text(if (isNegotiable) "قابل للتفاوض" else "سعر ثابت") },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Item Condition
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("حالة السلعة", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ItemCondition.entries.take(3).forEach { cond ->
                            FilterChip(
                                selected = condition == cond,
                                onClick = { condition = cond },
                                label = { Text(cond.labelAr) }
                            )
                        }
                    }
                }

                // Wilaya & Commune
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        onClick = { showWilayaSheet = true },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$selectedWilayaCode - $selectedWilayaName",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    OutlinedTextField(
                        value = commune,
                        onValueChange = { commune = it },
                        label = { Text("البلدية") },
                        placeholder = { Text("البلدية أو الحي") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Quick communes suggestion chips from selected wilaya
                val currentWilayaObj = remember(selectedWilayaCode) {
                    WilayasData.allWilayas.find { it.code == selectedWilayaCode }
                }
                if (currentWilayaObj != null && currentWilayaObj.communes.isNotEmpty()) {
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        currentWilayaObj.communes.take(12).forEach { comm ->
                            SuggestionChip(
                                onClick = { commune = comm },
                                label = { Text(comm, style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }
                }

                // Phone
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; errorMessage = null },
                    label = { Text("رقم هاتف التواصل *") },
                    placeholder = { Text("05xx xx xx xx") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("ad_phone_input")
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it; errorMessage = null },
                    label = { Text("تفاصيل ووصف السلعة *") },
                    placeholder = { Text("أدخل مواصفات السلعة، مدة الاستعمال، الملحقات، وسبب البيع...") },
                    minLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("ad_desc_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Actions: Preview & Publish
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SecondaryButton(
                        text = "معاينة",
                        onClick = { isPreviewMode = true },
                        modifier = Modifier.weight(1f)
                    )

                    PrimaryButton(
                        text = "نشر الإعلان",
                        icon = Icons.Default.Publish,
                        onClick = {
                            if (title.isBlank() || priceText.isBlank() || description.isBlank()) {
                                errorMessage = "يرجى ملء جميع الحقول الإلزامية ذات العلامة (*)"
                            } else {
                                val newListing = Listing(
                                    id = "list-${System.currentTimeMillis()}",
                                    title = title.trim(),
                                    price = priceText.toDoubleOrNull() ?: 0.0,
                                    isNegotiable = isNegotiable,
                                    categoryId = selectedCategoryId,
                                    categoryName = selectedCategoryName,
                                    wilayaCode = selectedWilayaCode,
                                    wilayaName = selectedWilayaName,
                                    commune = commune.trim(),
                                    description = description.trim(),
                                    condition = condition,
                                    createdAt = "الآن",
                                    sellerId = "current-user",
                                    sellerName = "أمين الجزائري",
                                    sellerPhone = phone,
                                    sellerRating = 4.9f,
                                    isFeatured = false,
                                    status = ListingStatus.PUBLISHED
                                )
                                onCreateAd(newListing)
                                showSuccessDialog = true
                            }
                        },
                        modifier = Modifier.weight(1.5f),
                        testTag = "submit_ad_btn"
                    )
                }
            }
        }

        if (showWilayaSheet) {
            WilayaPickerBottomSheet(
                selectedWilayaCode = selectedWilayaCode,
                onWilayaSelected = { wilaya ->
                    if (wilaya != null) {
                        selectedWilayaCode = wilaya.code
                        selectedWilayaName = wilaya.nameAr
                        if (commune.isBlank() && wilaya.communes.isNotEmpty()) {
                            commune = wilaya.communes.first()
                        }
                    }
                },
                onCommuneSelected = { wilaya, comm ->
                    selectedWilayaCode = wilaya.code
                    selectedWilayaName = wilaya.nameAr
                    commune = comm
                },
                onDismiss = { showWilayaSheet = false }
            )
        }

        InfoDialog(
            show = showSuccessDialog,
            title = "تم نشر الإعلان بنجاح!",
            message = "إعلانك متاح الآن لجميع مستخدمي OcaVenteDz في ولاية $selectedWilayaName وكل الجزائر.",
            buttonText = "العودة للرئيسية",
            onDismiss = {
                showSuccessDialog = false
                onBackClick()
            }
        )
    }
}
