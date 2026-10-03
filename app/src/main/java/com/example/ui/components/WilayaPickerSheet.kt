package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.models.AlgeriaWilayas
import com.example.data.models.Wilaya
import com.example.ui.theme.EmeraldPrimary

private data class CachedWilayaItem(
    val wilaya: Wilaya,
    val codeStr: String,
    val searchKey: String
)

// Pre-cached list for ultra-fast instant rendering without allocations
private val preloadedWilayas: List<CachedWilayaItem> by lazy {
    AlgeriaWilayas.list.map { w ->
        val codeStr = if (w.code < 10) "0${w.code}" else "${w.code}"
        CachedWilayaItem(
            wilaya = w,
            codeStr = codeStr,
            searchKey = "${w.nameAr} ${w.nameFr} $codeStr ${w.code}".lowercase()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WilayaPickerSheet(
    selectedWilayaCode: Int?,
    selectedCommune: String?,
    onDismiss: () -> Unit,
    onWilayaAndCommuneSelected: (Wilaya, String) -> Unit
) {
    // Fast full-screen or bottom-anchored dialog for immediate 0ms presentation
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .clickable(enabled = false, onClick = {}) // prevent dismissing when tapping inside
                ,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 16.dp
            ) {
                var searchQuery by remember { mutableStateOf("") }
                var communeSearchQuery by remember { mutableStateOf("") }
                var currentSelectedWilaya by remember {
                    mutableStateOf(
                        if (selectedCommune != null && selectedWilayaCode != null) {
                            AlgeriaWilayas.findByCode(selectedWilayaCode)
                        } else null
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Drag Handle indicator
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 40.dp, height = 4.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                                    shape = CircleShape
                                )
                        )
                    }

                    // Top Bar Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (currentSelectedWilaya != null) {
                                IconButton(
                                    onClick = {
                                        currentSelectedWilaya = null
                                        communeSearchQuery = ""
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "العودة للولايات",
                                        tint = EmeraldPrimary
                                    )
                                }
                            }
                            Text(
                                text = if (currentSelectedWilaya == null) "اختر الولاية (69 ولاية)" else "اختر البلدية - ${currentSelectedWilaya?.nameAr}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    AnimatedContent(
                        targetState = currentSelectedWilaya,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "WilayaCommunePickerTransition"
                    ) { wilayaTarget ->
                        if (wilayaTarget == null) {
                            // --- Step 1: Instant Wilaya Selection ---
                            Column(modifier = Modifier.fillMaxSize()) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("ابحث باسم الولاية أو رقمها...") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = EmeraldPrimary
                                        )
                                    },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = "مسح"
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldPrimary
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                val filteredWilayas = remember(searchQuery) {
                                    val q = searchQuery.trim().lowercase()
                                    if (q.isBlank()) preloadedWilayas
                                    else preloadedWilayas.filter { it.searchKey.contains(q) }
                                }

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(
                                        items = filteredWilayas,
                                        key = { it.wilaya.code }
                                    ) { item ->
                                        val wilaya = item.wilaya
                                        val isSelected = wilaya.code == selectedWilayaCode

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (selectedCommune == null) {
                                                        // Filter mode (e.g. from HomeScreen or GuestBrowseScreen) -> Instant select & close!
                                                        onWilayaAndCommuneSelected(wilaya, "")
                                                        onDismiss()
                                                    } else {
                                                        // Mode where commune is needed -> move to step 2 instantly
                                                        currentSelectedWilaya = wilaya
                                                    }
                                                }
                                                .padding(vertical = 11.dp, horizontal = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.padding(end = 12.dp)
                                                ) {
                                                    Text(
                                                        text = item.codeStr,
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                    )
                                                }

                                                Column {
                                                    Text(
                                                        text = wilaya.nameAr,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                    Text(
                                                        text = wilaya.nameFr,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = EmeraldPrimary
                                                )
                                            }
                                        }
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    }
                                }
                            }
                        } else {
                            // --- Step 2: Instant Commune Selection ---
                            Column(modifier = Modifier.fillMaxSize()) {
                                OutlinedTextField(
                                    value = communeSearchQuery,
                                    onValueChange = { communeSearchQuery = it },
                                    placeholder = { Text("ابحث في بلديات ${wilayaTarget.nameAr}...") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = EmeraldPrimary
                                        )
                                    },
                                    trailingIcon = {
                                        if (communeSearchQuery.isNotEmpty()) {
                                            IconButton(onClick = { communeSearchQuery = "" }) {
                                                Icon(
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = "مسح"
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldPrimary
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                val filteredCommunes = remember(wilayaTarget, communeSearchQuery) {
                                    val q = communeSearchQuery.trim()
                                    if (q.isBlank()) wilayaTarget.communes
                                    else wilayaTarget.communes.filter { it.contains(q, ignoreCase = true) }
                                }

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    // Option: Select all communes of this wilaya
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onWilayaAndCommuneSelected(wilayaTarget, "")
                                                    onDismiss()
                                                }
                                                .padding(vertical = 12.dp, horizontal = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationCity,
                                                contentDescription = null,
                                                tint = EmeraldPrimary,
                                                modifier = Modifier.padding(end = 10.dp)
                                            )
                                            Text(
                                                text = "كل بلديات ولاية ${wilayaTarget.nameAr}",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldPrimary
                                            )
                                        }
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    }

                                    items(
                                        items = filteredCommunes,
                                        key = { it }
                                    ) { commune ->
                                        val isSelected = commune == selectedCommune
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onWilayaAndCommuneSelected(wilayaTarget, commune)
                                                    onDismiss()
                                                }
                                                .padding(vertical = 12.dp, horizontal = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.LocationCity,
                                                    contentDescription = null,
                                                    tint = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(end = 10.dp)
                                                )
                                                Text(
                                                    text = commune,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }

                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = EmeraldPrimary
                                                )
                                            }
                                        }
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
