package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AlgerianWilayas
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddListingScreen(viewModel: MainViewModel) {
    BackHandler {
        viewModel.navigateTo(AppScreen.HOME)
    }

    val categories by viewModel.categories.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var title by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id ?: "") }
    var priceText by remember { mutableStateOf("") }
    var selectedWilaya by remember { mutableStateOf(currentUser?.wilaya ?: "16 - الجزائر (Alger)") }
    var phone by remember { mutableStateOf(currentUser?.phone ?: "0550123456") }
    var description by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1549399542-7e3f8b79c341?w=800") }

    var showWilayaDialog by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إضافة إعلان جديد", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Moderation Info Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "يتم إرسال إعلانك إلى المشرف للمراجعة والقبول قبل نشره للمستخدمين لضمان جودة المحتوى.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان الإعلان *") },
                placeholder = { Text("مثال: رونو كليو 4 سنة 2019 نقية") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Category Selector
            ExposedDropdownMenuBox(
                expanded = showCategoryMenu,
                onExpandedChange = { showCategoryMenu = !showCategoryMenu }
            ) {
                val currentCatName = categories.find { it.id == selectedCategoryId }?.nameAr ?: "اختر التصنيف"
                OutlinedTextField(
                    value = currentCatName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("التصنيف *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = showCategoryMenu,
                    onDismissRequest = { showCategoryMenu = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.nameAr) },
                            onClick = {
                                selectedCategoryId = cat.id
                                showCategoryMenu = false
                            }
                        )
                    }
                }
            }

            // Price in DZD
            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text("السعر بالدينار الجزائري (د.ج) *") },
                placeholder = { Text("مثال: 1850000") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = { Text("د.ج", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp)) }
            )

            // Wilaya Picker
            OutlinedTextField(
                value = selectedWilaya,
                onValueChange = {},
                readOnly = true,
                label = { Text("الولاية *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showWilayaDialog = true },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Phone number
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("رقم الهاتف للتواصل *") },
                placeholder = { Text("05XX / 06XX / 07XX") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(12.dp)
            )

            // Image URL
            OutlinedTextField(
                value = imageUrl,
                onValueChange = { imageUrl = it },
                label = { Text("رابط صورة الإعلان (URL)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.AddPhotoAlternate, contentDescription = null) }
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("تفاصيل ووصف الإعلان *") },
                placeholder = { Text("اذكر حالة السلعة، المميزات، والخصائص بالتفصيل...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                shape = RoundedCornerShape(12.dp),
                maxLines = 6
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Submit Button
            val isFormValid = title.isNotBlank() && priceText.isNotBlank() && description.isNotBlank()
            Button(
                onClick = {
                    val price = priceText.toDoubleOrNull() ?: 0.0
                    viewModel.submitListing(
                        title = title,
                        description = description,
                        price = price,
                        categoryId = if (selectedCategoryId.isNotBlank()) selectedCategoryId else categories.firstOrNull()?.id ?: "cat-1",
                        wilaya = selectedWilaya,
                        phone = phone,
                        imageUrl = imageUrl
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = isFormValid
            ) {
                Text("نشر الإعلان ومراجعته", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showWilayaDialog) {
        AlertDialog(
            onDismissRequest = { showWilayaDialog = false },
            title = { Text("اختر الولاية (58 ولاية)") },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.height(350.dp)) {
                    items(AlgerianWilayas.all.size) { idx ->
                        val w = AlgerianWilayas.all[idx]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedWilaya = w
                                    showWilayaDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedWilaya == w,
                                onClick = {
                                    selectedWilaya = w
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
                TextButton(onClick = { showWilayaDialog = false }) { Text("إلغاء") }
            }
        )
    }
}
