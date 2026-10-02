package com.example.ui.screens.create

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableStateListOf
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
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream
import com.example.data.local.ListingEntity
import com.example.data.model.AlgeriaWilayas
import com.example.data.model.CategoriesData
import com.example.data.model.MarketplaceCategory
import com.example.ui.components.AdCard
import com.example.ui.components.WilayaPickerSheet
import com.example.ui.components.formatDzd
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.UrgentRed
import com.example.ui.viewmodel.MarketplaceViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateAdScreen(
    viewModel: MarketplaceViewModel,
    onFinished: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val platformSettings by viewModel.platformSettings.collectAsState()
    val currentWallet by viewModel.currentWallet.collectAsState()
    val imageUploadProgress by viewModel.imageUploadProgress.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()

    var currentStep by remember { mutableIntStateOf(1) } // 1: Category, 2: Details, 3: Media & Location, 4: Preview, 5: Package & Payment

    // Form state
    var selectedCategory by remember { mutableStateOf<MarketplaceCategory?>(null) }
    var selectedSubcategory by remember { mutableStateOf("") }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var isNegotiable by remember { mutableStateOf(false) }
    var condition by remember { mutableStateOf("USED") } // "NEW", "LIKE_NEW", "USED"

    val images = remember { mutableStateListOf<String>() }
    var videoUrl by remember { mutableStateOf("") }

    var selectedWilayaCode by remember { mutableStateOf(16) } // Alger default
    var selectedWilayaName by remember { mutableStateOf("الجزائر العاصمة") }
    var selectedCommune by remember { mutableStateOf("حيدرة") }
    var showWilayaSheet by remember { mutableStateOf(false) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }

    // Camera and Photo Pickers
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            try {
                val file = File(context.cacheDir, "product_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                images.add(file.toURI().toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        uris.forEach { uri ->
            if (images.size < 10 && !images.contains(uri.toString())) {
                images.add(uri.toString())
            }
        }
    }

    // Step 5 Payment State
    var selectedPackageType by remember { mutableStateOf("STANDARD") } // "STANDARD", "FEATURED", "URGENT"
    var selectedPaymentMethod by remember { mutableStateOf("WALLET") } // "WALLET", "EDAHABIA", "CIB", "BARIDIMOB"
    var isProcessingPayment by remember { mutableStateOf(false) }
    var paymentCompleted by remember { mutableStateOf(false) }

    if (showWilayaSheet) {
        WilayaPickerSheet(
            selectedWilayaCode = selectedWilayaCode,
            selectedCommune = selectedCommune,
            onDismiss = { showWilayaSheet = false },
            onWilayaAndCommuneSelected = { wilaya, commune ->
                selectedWilayaCode = wilaya.code
                selectedWilayaName = wilaya.nameAr
                selectedCommune = commune
                showWilayaSheet = false
            }
        )
    }

    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إضافة صور للمنتج", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("اختر طريقة إضافة صورة السلعة:", style = MaterialTheme.typography.bodyMedium)

                    // Option 1: Camera
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPhotoSourceDialog = false
                                val permissionCheck = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                )
                                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                    cameraLauncher.launch(null)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("التقاط صورة بالكاميرا (CameraX)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("تصوير المنتج الآن عبر كاميرا الهاتف مباشرة", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Option 2: Gallery
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPhotoSourceDialog = false
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Collections, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("اختيار من معرض الصور", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("تحديد صورة محفوظة مسبقاً في هاتفك", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhotoSourceDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("create_ad_screen"),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 90.dp)
    ) {
        // Step Indicator Progress Bar
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (currentStep) {
                            1 -> "الخطوة 1: التصنيف"
                            2 -> "الخطوة 2: معلومات المنتج"
                            3 -> "الخطوة 3: الصور والموقع"
                            4 -> "الخطوة 4: معاينة الإعلان"
                            else -> "الخطوة 5: باقة النشر والدفع"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                    Text(
                        text = "$currentStep / 5",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 5-segment Progress bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 1..5) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (i <= currentStep) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        // STEP 1: CATEGORY & SUBCATEGORY
        if (currentStep == 1) {
            item {
                Text(
                    text = "اختر التصنيف الرئيسي للسلعة:",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            items(CategoriesData.allCategories) { cat ->
                val isSelected = selectedCategory?.id == cat.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { selectedCategory = cat; selectedSubcategory = "" },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(EmeraldPrimary)) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = CategoriesData.getCategoryIcon(cat.iconName),
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = cat.nameAr,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary)
                        }
                    }
                }
            }

            if (selectedCategory != null) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "اختر التصنيف الفرعي:",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedCategory!!.subcategoriesAr.forEach { sub ->
                            val isSubSelected = selectedSubcategory == sub
                            FilterChip(
                                selected = isSubSelected,
                                onClick = { selectedSubcategory = sub },
                                label = { Text(sub) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { currentStep = 2 },
                    enabled = selectedCategory != null && selectedSubcategory.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("step1_next_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("المتابعة إلى الخطوة 2", fontWeight = FontWeight.Bold)
                }
            }
        }

        // STEP 2: PRODUCT DETAILS
        if (currentStep == 2) {
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان الإعلان *") },
                    placeholder = { Text("مثال: iPhone 14 Pro Max 256GB نظيف") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ad_title_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف المنتج بالتفصيل *") },
                    placeholder = { Text("المواصفات، الملحقات، العيوب إن وجدت، إمكانية التوصيل...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .testTag("ad_desc_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("السعر بالدينار الجزائري (دج) *") },
                    placeholder = { Text("مثال: 45000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ad_price_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isNegotiable = !isNegotiable }
                ) {
                    Checkbox(
                        checked = isNegotiable,
                        onCheckedChange = { isNegotiable = it }
                    )
                    Text("السعر قابل للتفاوض", style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("حالة السلعة:", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "NEW" to "جديد بالعلبة",
                        "LIKE_NEW" to "كالجديد",
                        "USED" to "مستعمل بحالة جيدة"
                    ).forEach { (code, label) ->
                        val isSelected = condition == code
                        FilterChip(
                            selected = isSelected,
                            onClick = { condition = code },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Algerian Law Compliance & Prohibited Items Safety Filter
                val prohibitedKeywords = listOf(
                    "درون", "drone", "مفرقعات", "ألعاب نارية", "بارود",
                    "عملة صعبة", "سوق سوداء", "أورو سكوار", "سلاح", "سلاح أبيض",
                    "كاميرا تجسس", "تجسس", "أجهزة تنصت", "ذهب غير مدموغ", "أدوية مهربة"
                )
                val detectedProhibited = prohibitedKeywords.firstOrNull {
                    title.contains(it, ignoreCase = true) || description.contains(it, ignoreCase = true)
                }

                if (detectedProhibited != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = UrgentRed, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تنبيه قانوني وأمني (القوانين والتشريعات الجزائرية)", color = UrgentRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "تم رصد كلمة '$detectedProhibited' التي تقع ضمن المواد المحظورة أو الخاضعة لتراخيص أمنية مقيدة في الجزائر. يرجى تعديل الإعلان فوراً لتفادي رفض النشر أو الملاحقة القانونية.",
                                color = UrgentRed,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { currentStep = 1 },
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text("السابق")
                    }
                    Button(
                        onClick = { currentStep = 3 },
                        enabled = title.isNotBlank() && description.isNotBlank() && priceText.toLongOrNull() != null && detectedProhibited == null,
                        modifier = Modifier.weight(1f).height(50.dp).testTag("step2_next_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("المتابعة (الصور)")
                    }
                }
            }
        }

        // STEP 3: PHOTOS & LOCATION
        if (currentStep == 3) {
            item {
                Text(
                    text = "صور المنتج (من 1 إلى 10 صور):",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "الإعلانات المرفقة بصور حقيقية تباع أسرع بـ 5 أضعاف",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Photos Grid
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(images) { imgUrl ->
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        ) {
                            AsyncImage(
                                model = imgUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { if (images.size > 1) images.remove(imgUrl) },
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    // Add Photo button
                    if (images.size < 10) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(90.dp)
                                    .clickable { showPhotoSourceDialog = true }
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = EmeraldPrimary)
                                    Text("+ صورة", fontSize = 11.sp, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Short Video URL (Optional)
                OutlinedTextField(
                    value = videoUrl,
                    onValueChange = { videoUrl = it },
                    label = { Text("رابط فيديو قصير للمنتج (اختياري)") },
                    placeholder = { Text("YouTube Shorts / TikTok / رابط فيديو") },
                    leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Wilaya & Commune
                Text("موقع السلعة في الجزائر:", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showWilayaSheet = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "$selectedCommune، ولاية $selectedWilayaName",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "انقر لتغيير الولاية أو البلدية",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text("تغيير", color = EmeraldPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { currentStep = 2 },
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text("السابق")
                    }
                    Button(
                        onClick = { currentStep = 4 },
                        modifier = Modifier.weight(1f).height(50.dp).testTag("step3_next_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("معاينة الإعلان")
                    }
                }
            }
        }

        // STEP 4: PREVIEW AD
        if (currentStep == 4) {
            item {
                Text(
                    text = "معاينة الإعلان كما سيظهر للجميع:",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Realistic preview card
                val previewListing = ListingEntity(
                    id = "preview",
                    userId = currentUserId,
                    userName = "المستخدم الحالي",
                    userPhone = "",
                    isPhoneVisible = true,
                    title = title,
                    description = description,
                    categoryId = selectedCategory?.id ?: "other",
                    categoryNameAr = selectedCategory?.nameAr ?: "",
                    subcategory = selectedSubcategory,
                    priceDzd = priceText.toLongOrNull() ?: 0,
                    isNegotiable = isNegotiable,
                    condition = condition,
                    wilayaCode = selectedWilayaCode,
                    wilayaName = selectedWilayaName,
                    commune = selectedCommune,
                    imagesJson = images.joinToString(","),
                    videoUrl = videoUrl,
                    status = "DRAFT",
                    rejectionReason = "",
                    packageType = selectedPackageType,
                    publishingFeeDzd = 0,
                    isPaid = false,
                    isFeatured = selectedPackageType != "STANDARD",
                    isUrgent = selectedPackageType == "URGENT",
                    viewsCount = 0,
                    createdAt = System.currentTimeMillis(),
                    expiresAt = System.currentTimeMillis() + (30L * 24 * 3600 * 1000)
                )

                AdCard(
                    listing = previewListing,
                    isFavorite = false,
                    onFavoriteClick = {},
                    onClick = {}
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("تفاصيل الوصف في المعاينة:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(description, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { currentStep = 3 },
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text("تعديل")
                    }
                    Button(
                        onClick = { currentStep = 5 },
                        modifier = Modifier.weight(1f).height(50.dp).testTag("step4_next_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("متابعة إلى الدفع 💳", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // STEP 5: PACKAGE & PAYMENT (CRITICAL FEATURE)
        if (currentStep == 5) {
            item {
                Text(
                    text = "اختر باقة نشر الإعلان وطريقة الدفع:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "وفقًا لسياسة OcaVenteDz، تُدفع رسوم النشر لضمان جدية الإعلانات ومكافحة الاحتيال.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Packages options loaded dynamically from database PlatformSettings
                // 1. Standard
                val standardFee = platformSettings.standardAdFeeDzd
                val featuredFee = platformSettings.featuredAdFeeDzd
                val urgentFee = platformSettings.urgentAdFeeDzd

                val packages = listOf(
                    Triple("STANDARD", "إعلان عادي ($standardFee دج)", "نشر لمدة 30 يوم مع ظهور كامل في البحث والتصنيفات."),
                    Triple("FEATURED", "إعلان مميز ⭐ ($featuredFee دج)", "شارة مميز ذهبية + ظهور في الصفحة الرئيسية ومقدمة النتائج."),
                    Triple("URGENT", "إعلان عاجل 🔥 ($urgentFee دج)", "شارة عاجل حمراء ملفتة + تثبيت في أعلى نتائج البحث وسرعة بيع مضاعفة.")
                )

                packages.forEach { (type, name, desc) ->
                    val isSelected = selectedPackageType == type
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedPackageType = type },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(EmeraldPrimary)) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedPackageType = type }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Required Publishing Fee Amount Banner
                val currentFee = when (selectedPackageType) {
                    "FEATURED" -> featuredFee
                    "URGENT" -> urgentFee
                    else -> standardFee
                }

                Surface(
                    color = EmeraldDark,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("المبلغ الإجمالي للدفع:", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            Text("$currentFee دج", color = GoldLight, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }

                        Surface(
                            color = GoldSecondary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "دفع آمن 100%",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Select Payment Method
                Text("طريقة الدفع:", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                val walletBalance = currentWallet?.balanceDzd ?: 0

                val paymentMethods = listOf(
                    Triple("WALLET", "رصيد المحفظة (الرصيد: $walletBalance دج)", Icons.Default.AccountBalanceWallet),
                    Triple("EDAHABIA", "البطاقة الذهبية بريد الجزائر", Icons.Default.CreditCard),
                    Triple("CIB", "البطاقة البنكية CIB", Icons.Default.CreditCard),
                    Triple("BARIDIMOB", "تطبيق بريدي موب BaridiMob", Icons.Default.PhoneAndroid)
                )

                paymentMethods.forEach { (method, label, icon) ->
                    val isSelected = selectedPaymentMethod == method
                    val isWalletDisabled = method == "WALLET" && walletBalance < currentFee

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isWalletDisabled) { selectedPaymentMethod = method },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedPaymentMethod = method },
                                    enabled = !isWalletDisabled
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(icon, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (isWalletDisabled) {
                                        Text("الرصيد غير كافٍ، يرجى شحن المحفظة أو اختيار بطاقة", color = UrgentRed, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Pay & Submit Button
                Button(
                    onClick = {
                        isProcessingPayment = true
                        viewModel.createAndPublishListing(
                            title = title,
                            description = description,
                            categoryId = selectedCategory?.id ?: "other",
                            categoryNameAr = selectedCategory?.nameAr ?: "أخرى",
                            subcategory = selectedSubcategory,
                            priceDzd = priceText.toLongOrNull() ?: 0,
                            isNegotiable = isNegotiable,
                            condition = condition,
                            wilayaCode = selectedWilayaCode,
                            wilayaName = selectedWilayaName,
                            commune = selectedCommune,
                            images = images,
                            videoUrl = videoUrl,
                            packageType = selectedPackageType,
                            paymentMethod = selectedPaymentMethod,
                            onSuccess = { newListingId ->
                                isProcessingPayment = false
                                paymentCompleted = true
                                onFinished(newListingId)
                            },
                            onError = { _ ->
                                isProcessingPayment = false
                            }
                        )
                    },
                    enabled = !isProcessingPayment,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("pay_and_publish_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    if (isProcessingPayment) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        val progressText = if (imageUploadProgress != null) {
                            "جاري رفع الصور إلى التخزين السحابي (${(imageUploadProgress!! * 100).toInt()}%)..."
                        } else {
                            "جاري معالجة ونشر الإعلان..."
                        }
                        Text(progressText)
                    } else {
                        Icon(Icons.Default.Payment, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تأكيد ودفع $currentFee دج ونشر الإعلان", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}
