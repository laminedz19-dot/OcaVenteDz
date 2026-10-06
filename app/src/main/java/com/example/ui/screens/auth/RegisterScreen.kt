package com.example.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.AlgeriaWilayas
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.MarketplaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: MarketplaceViewModel,
    onBack: () -> Unit,
    onRegistered: () -> Unit,
    onLogin: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var wilaya by remember { mutableStateOf("16 - الجزائر العاصمة") }
    var commune by remember { mutableStateOf("الجزائر الوسطى") }

    var isWilayaExpanded by remember { mutableStateOf(false) }
    var isCommuneExpanded by remember { mutableStateOf(false) }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showVerificationDialog by remember { mutableStateOf(false) }

    val selectedWilayaObj = remember(wilaya) {
        AlgeriaWilayas.list.find {
            "${it.code} - ${it.nameAr}".equals(wilaya.trim(), ignoreCase = true) ||
            it.nameAr.equals(wilaya.trim(), ignoreCase = true) ||
            wilaya.trim().startsWith("${it.code} ")
        }
    }

    val currentCommunes = selectedWilayaObj?.communes ?: emptyList()

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("إنشاء حساب بالبريد الإلكتروني", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            Icon(
                imageVector = Icons.Default.HowToReg,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.height(48.dp)
            )
            Text(
                text = "انضم إلى OcaVenteDz",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "أنشئ حسابك بالبريد الإلكتروني لبيع وشراء المنتجات بسهولة وأمان عبر كافة الولايات.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorMessage = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("الاسم الكامل") },
                placeholder = { Text("مثال: محمد بن علي") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                enabled = !isSubmitting
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("البريد الإلكتروني (لتسجيل الدخول)") },
                placeholder = { Text("example@gmail.com") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                enabled = !isSubmitting
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it; errorMessage = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("رقم الهاتف (للتواصل في الإعلانات - اختياري)") },
                placeholder = { Text("05 55 12 34 56") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                enabled = !isSubmitting
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("كلمة المرور") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                enabled = !isSubmitting
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; errorMessage = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("تأكيد كلمة المرور") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                enabled = !isSubmitting
            )

            // Wilaya: Select-only list of 69 wilayas (قائمة فقط دون إمكانية الإدراج)
            ExposedDropdownMenuBox(
                expanded = isWilayaExpanded,
                onExpandedChange = { if (!isSubmitting) isWilayaExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = wilaya,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    label = { Text("الولاية (اختر من القائمة - 69 ولاية)") },
                    leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isWilayaExpanded) },
                    singleLine = true,
                    enabled = !isSubmitting
                )
                ExposedDropdownMenu(
                    expanded = isWilayaExpanded,
                    onDismissRequest = { isWilayaExpanded = false },
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    AlgeriaWilayas.list.forEach { item ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${item.code} - ${item.nameAr}", fontWeight = FontWeight.SemiBold)
                                    Text(item.nameFr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            onClick = {
                                wilaya = "${item.code} - ${item.nameAr}"
                                if (item.communes.isNotEmpty()) {
                                    commune = item.communes.first()
                                }
                                isWilayaExpanded = false
                            }
                        )
                    }
                }
            }

            // Commune: Select-only list for selected wilaya
            if (currentCommunes.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = isCommuneExpanded,
                    onExpandedChange = { if (!isSubmitting) isCommuneExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = commune,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        label = { Text("البلدية (اختر من القائمة)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCommuneExpanded) },
                        singleLine = true,
                        enabled = !isSubmitting
                    )
                    ExposedDropdownMenu(
                        expanded = isCommuneExpanded,
                        onDismissRequest = { isCommuneExpanded = false },
                        modifier = Modifier.heightIn(max = 240.dp)
                    ) {
                        currentCommunes.forEach { commName ->
                            DropdownMenuItem(
                                text = { Text(commName) },
                                onClick = {
                                    commune = commName
                                    isCommuneExpanded = false
                                }
                            )
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = commune,
                    onValueChange = { commune = it; errorMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("البلدية") },
                    singleLine = true,
                    enabled = !isSubmitting
                )
            }

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, textAlign = TextAlign.Center)
            }

            Button(
                onClick = {
                    if (name.trim().length < 2) {
                        errorMessage = "يرجى إدخال الاسم الكامل"
                        return@Button
                    }
                    if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim().lowercase()).matches()) {
                        errorMessage = "يرجى إدخال بريد إلكتروني صحيح (مثال: example@gmail.com)"
                        return@Button
                    }
                    if (phone.isNotBlank() && phone.trim().length < 9) {
                        errorMessage = "يرجى إدخال رقم هاتف جزائري صحيح (مثال: 0555123456) أو تركه فارغاً"
                        return@Button
                    }
                    if (password.isBlank()) {
                        errorMessage = "يرجى إدخال كلمة المرور"
                        return@Button
                    }
                    if (password.length < 6) {
                        errorMessage = "يجب أن تتكون كلمة المرور من 6 أحرف على الأقل"
                        return@Button
                    }
                    if (password != confirmPassword) {
                        errorMessage = "كلمة المرور وتأكيد كلمة المرور غير متطابقين"
                        return@Button
                    }
                    if (wilaya.trim().isEmpty() || commune.trim().isEmpty()) {
                        errorMessage = "يرجى تحديد الولاية والبلدية"
                        return@Button
                    }

                    isSubmitting = true
                    errorMessage = null
                    val cleanEmail = email.trim().lowercase()
                    viewModel.registerUser(
                        name = name,
                        email = cleanEmail,
                        wilaya = wilaya,
                        commune = commune,
                        password = password,
                        phone = phone.trim(),
                        onSuccess = { requiresConfirmation ->
                            isSubmitting = false
                            if (requiresConfirmation) {
                                showVerificationDialog = true
                            } else {
                                onRegistered()
                            }
                        },
                        onError = {
                            isSubmitting = false
                            errorMessage = it
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.height(22.dp))
                } else {
                    Text("إنشاء الحساب", fontWeight = FontWeight.Bold)
                }
            }

            TextButton(onClick = onLogin, enabled = !isSubmitting) {
                Text("لديك حساب بالفعل؟ تسجيل الدخول")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showVerificationDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { /* require action */ },
            icon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.height(48.dp)
                )
            },
            title = {
                Text(
                    text = "تأكيد البريد الإلكتروني",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "تم إنشاء حسابك بنجاح وأرسلنا رابط التأكيد إلى بريدك الإلكتروني:",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = email.trim().lowercase(),
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "يرجى فتح صندوق الوارد والضغط على رابط التفعيل لتأكيد حسابك، ثم المتابعة لتسجيل الدخول بكلمة المرور.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showVerificationDialog = false
                        onLogin()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("الانتقال إلى تسجيل الدخول")
                }
            }
        )
    }
}
