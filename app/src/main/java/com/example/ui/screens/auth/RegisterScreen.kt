package com.example.ui.screens.auth

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.components.OcaSubTopBar
import com.example.ui.components.PrimaryButton
import com.example.ui.components.WilayaPickerBottomSheet
import com.example.ui.model.Wilaya
import com.example.ui.theme.OcaGreenPrimary

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onBackClick: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedWilaya by remember { mutableStateOf<Wilaya?>(null) }
    var commune by remember { mutableStateOf("") }
    var showWilayaSheet by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var agreeToTerms by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            OcaSubTopBar(
                title = "إنشاء حساب جديد",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "انضم إلى مجتمع OcaVenteDz",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "أنشئ حسابك وانشر إعلانك بكل بساطة وأمان",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Full Name
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it; errorMessage = null },
                label = { Text("الاسم الكامل") },
                placeholder = { Text("مثال: أمين الجزائري") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("register_name_input")
            )

            // Phone
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it; errorMessage = null },
                label = { Text("رقم الهاتف") },
                placeholder = { Text("05xx xx xx xx") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("register_phone_input")
            )

            // Email (Optional)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                label = { Text("البريد الإلكتروني (اختياري)") },
                placeholder = { Text("example@domain.dz (يمكن تركه فارغاً)") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("register_email_input")
            )

            // Wilaya Selector Field (69 Wilayas)
            Surface(
                onClick = { showWilayaSheet = true },
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().testTag("register_wilaya_selector")
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
                            text = selectedWilaya?.let { "${it.code} - ${it.nameAr}" } ?: "اختر ولايتك (من بين 69 ولاية)",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selectedWilaya != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Communes display and selector
            if (selectedWilaya != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "بلديات ولاية ${selectedWilaya?.nameAr}:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (commune.isNotEmpty()) {
                            Text(
                                text = "المختارة: $commune",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = OcaGreenPrimary
                            )
                        }
                    }

                    // Commune input or chip selection
                    OutlinedTextField(
                        value = commune,
                        onValueChange = { commune = it },
                        label = { Text("البلدية أو الدائرة") },
                        placeholder = { Text("اختر من القائمة أو اكتب بلديتك") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Communes quick selection chips
                    if (selectedWilaya!!.communes.isNotEmpty()) {
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            selectedWilaya!!.communes.forEach { comm ->
                                val isSelected = commune == comm
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { commune = comm },
                                    label = { Text(comm, style = MaterialTheme.typography.bodySmall) }
                                )
                            }
                        }
                    }
                }
            }

            // Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = { Text("كلمة المرور") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("register_password_input")
            )

            // Confirm Password
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; errorMessage = null },
                label = { Text("تأكيد كلمة المرور") },
                leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null) },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("register_confirm_password_input")
            )

            // Agree to Terms
            Row(
                modifier = Modifier.fillMaxWidth().clickable { agreeToTerms = !agreeToTerms },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = agreeToTerms,
                    onCheckedChange = { agreeToTerms = it },
                    colors = CheckboxDefaults.colors(checkedColor = OcaGreenPrimary)
                )
                Text(
                    text = "أوافق على شروط الاستخدام وسياسة الخصوصية الخاصة بـ OcaVenteDz",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Submit Button
            PrimaryButton(
                text = "إنشاء الحساب",
                enabled = agreeToTerms,
                onClick = {
                    if (fullName.isBlank() || phone.isBlank() || password.isBlank()) {
                        errorMessage = "يرجى تعبئة الحقول الإلزامية (الاسم، رقم الهاتف، وكلمة المرور)"
                    } else if (password != confirmPassword) {
                        errorMessage = "كلمتا المرور غير متطابقتين"
                    } else if (!agreeToTerms) {
                        errorMessage = "يرجى الموافقة على شروط الاستخدام"
                    } else {
                        onRegisterSuccess()
                    }
                },
                testTag = "register_submit_btn"
            )

            // Already have account
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "لديك حساب بالفعل؟",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onNavigateToLogin) {
                    Text(
                        text = "تسجيل الدخول",
                        color = OcaGreenPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (showWilayaSheet) {
            WilayaPickerBottomSheet(
                selectedWilayaCode = selectedWilaya?.code,
                onWilayaSelected = { wilaya ->
                    selectedWilaya = wilaya
                    if (wilaya != null && commune.isBlank() && wilaya.communes.isNotEmpty()) {
                        commune = wilaya.communes.first()
                    }
                },
                onCommuneSelected = { wilaya, comm ->
                    selectedWilaya = wilaya
                    commune = comm
                },
                onDismiss = { showWilayaSheet = false }
            )
        }
    }
}
