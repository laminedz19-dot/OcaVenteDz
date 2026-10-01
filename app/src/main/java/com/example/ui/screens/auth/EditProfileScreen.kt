package com.example.ui.screens.auth

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
import com.example.ui.components.InfoDialog
import com.example.ui.components.OcaSubTopBar
import com.example.ui.components.PrimaryButton
import com.example.ui.components.UserAvatar
import com.example.ui.components.WilayaPickerBottomSheet
import com.example.ui.model.UserProfile
import com.example.ui.model.Wilaya
import com.example.ui.model.WilayasData
import com.example.ui.theme.OcaGreenPrimary

@Composable
fun EditProfileScreen(
    userProfile: UserProfile,
    onSaveProfile: (UserProfile) -> Unit,
    onBackClick: () -> Unit
) {
    var name by remember { mutableStateOf(userProfile.name) }
    var email by remember { mutableStateOf(userProfile.email) }
    var phone by remember { mutableStateOf(userProfile.phone) }
    var selectedWilayaCode by remember { mutableIntStateOf(userProfile.wilayaCode) }
    var selectedWilayaName by remember { mutableStateOf(userProfile.wilayaName) }
    var showWilayaSheet by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            OcaSubTopBar(
                title = "تعديل الملف الشخصي",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Avatar Header
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UserAvatar(name = name, size = 80.dp)
                    Text(
                        text = "تغيير الصورة الشخصية",
                        color = OcaGreenPrimary,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("الاسم الكامل") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("edit_name_input")
            )

            // Phone
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("رقم الهاتف") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("edit_phone_input")
            )

            // Email (read-only or editable)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("البريد الإلكتروني") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("edit_email_input")
            )

            // Wilaya
            Surface(
                onClick = { showWilayaSheet = true },
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().testTag("edit_wilaya_selector")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = OcaGreenPrimary
                        )
                        Text(
                            text = "$selectedWilayaCode - $selectedWilayaName",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            PrimaryButton(
                text = "حفظ التغييرات",
                onClick = {
                    val updated = userProfile.copy(
                        name = name,
                        email = email,
                        phone = phone,
                        wilayaCode = selectedWilayaCode,
                        wilayaName = selectedWilayaName
                    )
                    onSaveProfile(updated)
                    showSuccessDialog = true
                },
                testTag = "save_profile_btn"
            )
        }

        if (showWilayaSheet) {
            WilayaPickerBottomSheet(
                selectedWilayaCode = selectedWilayaCode,
                onWilayaSelected = { wilaya ->
                    if (wilaya != null) {
                        selectedWilayaCode = wilaya.code
                        selectedWilayaName = wilaya.nameAr
                    }
                },
                onDismiss = { showWilayaSheet = false }
            )
        }

        InfoDialog(
            show = showSuccessDialog,
            title = "تم الحفظ بنجاح",
            message = "تم تحديث معلومات ملفك الشخصي في OcaVenteDz بنجاح.",
            onDismiss = {
                showSuccessDialog = false
                onBackClick()
            }
        )
    }
}
