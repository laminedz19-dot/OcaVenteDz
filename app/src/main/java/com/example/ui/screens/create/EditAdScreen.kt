package com.example.ui.screens.create

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.components.InfoDialog
import com.example.ui.components.OcaSubTopBar
import com.example.ui.components.PrimaryButton
import com.example.ui.model.Listing
import com.example.ui.model.ListingStatus

@Composable
fun EditAdScreen(
    listing: Listing,
    onSaveAd: (Listing) -> Unit,
    onBackClick: () -> Unit
) {
    var title by remember { mutableStateOf(listing.title) }
    var priceText by remember { mutableStateOf(listing.price.toLong().toString()) }
    var description by remember { mutableStateOf(listing.description) }
    var isNegotiable by remember { mutableStateOf(listing.isNegotiable) }
    var status by remember { mutableStateOf(listing.status) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            OcaSubTopBar(
                title = "تعديل الإعلان",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان الإعلان") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("edit_ad_title")
            )

            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text("السعر (دج)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("edit_ad_price")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = isNegotiable,
                    onClick = { isNegotiable = true },
                    label = { Text("قابل للتفاوض") }
                )
                FilterChip(
                    selected = !isNegotiable,
                    onClick = { isNegotiable = false },
                    label = { Text("سعر ثابت") }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("حالة الإعلان", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ListingStatus.entries.forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(s.labelAr) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("الوصف") },
                minLines = 4,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("edit_ad_desc")
            )

            Spacer(modifier = Modifier.height(12.dp))

            PrimaryButton(
                text = "حفظ التعديلات",
                icon = Icons.Default.Save,
                onClick = {
                    val updated = listing.copy(
                        title = title,
                        price = priceText.toDoubleOrNull() ?: listing.price,
                        description = description,
                        isNegotiable = isNegotiable,
                        status = status
                    )
                    onSaveAd(updated)
                    showSuccessDialog = true
                },
                testTag = "save_ad_changes_btn"
            )
        }

        InfoDialog(
            show = showSuccessDialog,
            title = "تم التعديل بنجاح",
            message = "تم تحديث بيانات إعلانك على OcaVenteDz.",
            onDismiss = {
                showSuccessDialog = false
                onBackClick()
            }
        )
    }
}
