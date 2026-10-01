package com.example.ui.screens.search

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.components.OcaSearchBar
import com.example.ui.components.OcaSubTopBar
import com.example.ui.components.PrimaryButton
import com.example.ui.components.WilayaPickerBottomSheet
import com.example.ui.model.Category
import com.example.ui.model.ItemCondition
import com.example.ui.model.Wilaya
import com.example.ui.theme.OcaGreenPrimary

enum class SortOrder(val labelAr: String) {
    LATEST("الأحدث أولاً"),
    PRICE_LOW_TO_HIGH("السعر: من الأقل للأعلى"),
    PRICE_HIGH_TO_LOW("السعر: من الأعلى للأقل")
}

data class SearchFilterState(
    val query: String = "",
    val categoryId: String? = null,
    val wilayaCode: Int? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val condition: ItemCondition? = null,
    val sortOrder: SortOrder = SortOrder.LATEST
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    categories: List<Category>,
    initialFilter: SearchFilterState = SearchFilterState(),
    onApplyFilter: (SearchFilterState) -> Unit,
    onBackClick: () -> Unit
) {
    var query by remember { mutableStateOf(initialFilter.query) }
    var selectedCategoryId by remember { mutableStateOf(initialFilter.categoryId) }
    var selectedWilayaCode by remember { mutableStateOf(initialFilter.wilayaCode) }
    var minPriceText by remember { mutableStateOf(initialFilter.minPrice?.toLong()?.toString() ?: "") }
    var maxPriceText by remember { mutableStateOf(initialFilter.maxPrice?.toLong()?.toString() ?: "") }
    var selectedCondition by remember { mutableStateOf(initialFilter.condition) }
    var selectedSortOrder by remember { mutableStateOf(initialFilter.sortOrder) }
    var showWilayaSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            OcaSubTopBar(
                title = "البحث والفلاتر",
                onBackClick = onBackClick,
                actions = {
                    TextButton(
                        onClick = {
                            query = ""
                            selectedCategoryId = null
                            selectedWilayaCode = null
                            minPriceText = ""
                            maxPriceText = ""
                            selectedCondition = null
                            selectedSortOrder = SortOrder.LATEST
                        }
                    ) {
                        Text("مسح الكل", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Search Query
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "كلمة البحث",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                OcaSearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "ابحث بالاسم أو الموديل..."
                )
            }

            // Category Filter
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "التصنيف",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                ScrollableTabRow(
                    selectedTabIndex = categories.indexOfFirst { it.id == selectedCategoryId }.coerceAtLeast(0),
                    edgePadding = 0.dp,
                    indicator = {},
                    divider = {}
                ) {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { selectedCategoryId = null },
                        label = { Text("الكل") },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = {
                                selectedCategoryId = if (selectedCategoryId == cat.id) null else cat.id
                            },
                            label = { Text(cat.nameAr) },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
            }

            // Wilaya Filter
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "الولاية",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Surface(
                    onClick = { showWilayaSheet = true },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth().testTag("search_wilaya_btn")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = OcaGreenPrimary)
                            Text(
                                text = if (selectedWilayaCode != null) "ولاية رقم $selectedWilayaCode" else "كل الجزائر (جميع الولايات)",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
                }
            }

            // Price Range
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "نطاق السعر (دج)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = minPriceText,
                        onValueChange = { minPriceText = it },
                        label = { Text("السعر الأدنى") },
                        placeholder = { Text("مثال: 5000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxPriceText,
                        onValueChange = { maxPriceText = it },
                        label = { Text("السعر الأقصى") },
                        placeholder = { Text("مثال: 50000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Condition Filter
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "حالة السلعة",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ItemCondition.entries.forEach { cond ->
                        FilterChip(
                            selected = selectedCondition == cond,
                            onClick = {
                                selectedCondition = if (selectedCondition == cond) null else cond
                            },
                            label = { Text(cond.labelAr) }
                        )
                    }
                }
            }

            // Sorting
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ترتيب النتائج",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                SortOrder.entries.forEach { sort ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSortOrder = sort }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = selectedSortOrder == sort,
                            onClick = { selectedSortOrder = sort },
                            colors = RadioButtonDefaults.colors(selectedColor = OcaGreenPrimary)
                        )
                        Text(text = sort.labelAr, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Apply Button
            PrimaryButton(
                text = "عرض النتائج",
                onClick = {
                    val filter = SearchFilterState(
                        query = query.trim(),
                        categoryId = selectedCategoryId,
                        wilayaCode = selectedWilayaCode,
                        minPrice = minPriceText.toDoubleOrNull(),
                        maxPrice = maxPriceText.toDoubleOrNull(),
                        condition = selectedCondition,
                        sortOrder = selectedSortOrder
                    )
                    onApplyFilter(filter)
                },
                testTag = "apply_search_filter_btn"
            )
        }

        if (showWilayaSheet) {
            WilayaPickerBottomSheet(
                selectedWilayaCode = selectedWilayaCode,
                onWilayaSelected = { wilaya ->
                    selectedWilayaCode = wilaya?.code
                },
                onDismiss = { showWilayaSheet = false }
            )
        }
    }
}
