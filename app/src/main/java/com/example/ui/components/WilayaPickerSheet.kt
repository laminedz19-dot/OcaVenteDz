package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
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
import com.example.ui.model.Wilaya
import com.example.ui.model.WilayasData
import com.example.ui.theme.OcaGreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WilayaPickerBottomSheet(
    selectedWilayaCode: Int?, // null means "All Algeria"
    onWilayaSelected: (Wilaya?) -> Unit,
    onDismiss: () -> Unit,
    onCommuneSelected: ((wilaya: Wilaya, commune: String) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var expandedWilayaCode by remember { mutableStateOf<Int?>(null) }

    val filteredWilayas = remember(searchQuery) {
        if (searchQuery.isBlank()) WilayasData.allWilayas
        else {
            WilayasData.allWilayas.filter { wilaya ->
                wilaya.nameAr.contains(searchQuery.trim(), ignoreCase = true) ||
                wilaya.nameFr.contains(searchQuery.trim(), ignoreCase = true) ||
                wilaya.code.toString().contains(searchQuery.trim()) ||
                wilaya.communes.any { it.contains(searchQuery.trim(), ignoreCase = true) }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "اختر الولاية (69 ولاية جزائرية)",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "تصفح الولايات مع عرض البلديات التابعة لها",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث برقم الولاية، الاسم، أو البلدية...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wilaya_search_field")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Option: All Algeria
            Surface(
                onClick = {
                    onWilayaSelected(null)
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                color = if (selectedWilayaCode == null) OcaGreenPrimary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = OcaGreenPrimary
                        )
                        Text(
                            text = "كل الجزائر (جميع الـ 69 ولاية)",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (selectedWilayaCode == null) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (selectedWilayaCode == null) OcaGreenPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (selectedWilayaCode == null) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = OcaGreenPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // List of Wilayas
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(filteredWilayas, key = { it.code }) { wilaya ->
                    val isSelected = selectedWilayaCode == wilaya.code
                    val isExpanded = expandedWilayaCode == wilaya.code

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) OcaGreenPrimary.copy(alpha = 0.06f) else Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onWilayaSelected(wilaya)
                                    if (onCommuneSelected == null) {
                                        onDismiss()
                                    } else {
                                        expandedWilayaCode = if (isExpanded) null else wilaya.code
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("wilaya_item_${wilaya.code}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) OcaGreenPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "%02d".format(wilaya.code),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = wilaya.nameAr,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isSelected) OcaGreenPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "(${wilaya.nameFr})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "${wilaya.communes.size} بلدية",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }

                                    // Display communes summary
                                    if (wilaya.communes.isNotEmpty()) {
                                        Text(
                                            text = "البلديات: ${wilaya.communes.take(4).joinToString("، ")}${if (wilaya.communes.size > 4) "..." else ""}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (wilaya.communes.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            expandedWilayaCode = if (isExpanded) null else wilaya.code
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ExpandMore,
                                            contentDescription = "عرض البلديات",
                                            tint = if (isExpanded) OcaGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = OcaGreenPrimary
                                    )
                                }
                            }
                        }

                        // Expanded Communes Chips
                        if (isExpanded && wilaya.communes.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 48.dp, end = 12.dp, bottom = 12.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "بلديات ولاية ${wilaya.nameAr} (${wilaya.communes.size} بلدية):",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    wilaya.communes.forEach { communeName ->
                                        SuggestionChip(
                                            onClick = {
                                                onWilayaSelected(wilaya)
                                                onCommuneSelected?.invoke(wilaya, communeName)
                                                onDismiss()
                                            },
                                            label = {
                                                Text(communeName, style = MaterialTheme.typography.bodySmall)
                                            },
                                            icon = {
                                                Icon(
                                                    Icons.Default.LocationCity,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = OcaGreenPrimary
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                }
            }
        }
    }
}
