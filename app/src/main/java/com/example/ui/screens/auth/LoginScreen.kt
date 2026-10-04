package com.example.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.MarketplaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: MarketplaceViewModel,
    onBack: () -> Unit,
    onLoggedIn: () -> Unit,
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit
) {
    var loginMode by remember { mutableStateOf(0) } // 0: OTP Email, 1: Password
    var email by remember { mutableStateOf("") }
    var otpToken by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }

    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successNotice by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تسجيل الدخول", fontWeight = FontWeight.Bold) },
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
            Spacer(modifier = Modifier.height(20.dp))
            Icon(
                imageVector = Icons.Default.Login,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.height(52.dp)
            )
            Text(
                text = "مرحبًا بعودتك",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (loginMode == 0)
                    "سجل الدخول فوراً عبر رمز المصادقة المباشر المرسل إلى بريدك الإلكتروني."
                else
                    "أدخل رقم هاتفك أو بريدك وكلمة المرور لتسجيل الدخول إلى حسابك.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )

            // Switch Mode Tabs
            androidx.compose.material3.TabRow(
                selectedTabIndex = loginMode,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                androidx.compose.material3.Tab(
                    selected = loginMode == 0,
                    onClick = { loginMode = 0; errorMessage = null; successNotice = null },
                    text = { Text("رمز البريد (OTP)", fontWeight = FontWeight.SemiBold) }
                )
                androidx.compose.material3.Tab(
                    selected = loginMode == 1,
                    onClick = { loginMode = 1; errorMessage = null; successNotice = null },
                    text = { Text("كلمة المرور", fontWeight = FontWeight.SemiBold) }
                )
            }

            if (loginMode == 0) {
                // --- OTP VIA EMAIL MODE ---
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("البريد الإلكتروني") },
                    placeholder = { Text("example@domain.dz") },
                    leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    enabled = !isSubmitting && !otpSent
                )

                if (otpSent) {
                    androidx.compose.material3.Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "تم إرسال رمز المصادقة إلى بريدك:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = email,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "يرجى فحص صندوق الوارد أو مجلد الرسائل غير المرغوب فيها (Spam).",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "💡 ملاحظة: إذا وصلتك رسالة بها رابط (Sign in) بدون رمز 6 أرقام، فهذا يعني أن قالب البريد في Supabase يحتاج لتفعيل {{ .Token }}. يمكنك التبديل فوراً لتبويب 'كلمة المرور' للدخول السريع.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    OutlinedTextField(
                        value = otpToken,
                        onValueChange = {
                            if (it.length <= 8) otpToken = it.filter { ch -> ch.isDigit() }
                            errorMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("رمز التحقق (6 أرقام)") },
                        placeholder = { Text("123456") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        enabled = !isSubmitting
                    )

                    Button(
                        onClick = {
                            if (otpToken.length < 4) {
                                errorMessage = "يرجى إدخال رمز التحقق المكون من 6 أرقام."
                                return@Button
                            }
                            isSubmitting = true
                            errorMessage = null
                            viewModel.verifyEmailOtpAndLogin(
                                email = email,
                                token = otpToken,
                                onSuccess = {
                                    isSubmitting = false
                                    onLoggedIn()
                                },
                                onError = {
                                    isSubmitting = false
                                    errorMessage = it
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.height(22.dp))
                        } else {
                            Text("تأكيد الرمز وتسجيل الدخول", fontWeight = FontWeight.Bold)
                        }
                    }

                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                isSubmitting = true
                                errorMessage = null
                                viewModel.sendEmailOtp(
                                    email = email,
                                    shouldCreateUser = true,
                                    onSuccess = {
                                        isSubmitting = false
                                        successNotice = "تمت إعادة إرسال رمز المصادقة بنجاح."
                                    },
                                    onError = {
                                        isSubmitting = false
                                        errorMessage = it
                                    }
                                )
                            },
                            enabled = !isSubmitting
                        ) {
                            Text("إعادة إرسال الرمز")
                        }

                        TextButton(
                            onClick = {
                                otpSent = false
                                otpToken = ""
                                errorMessage = null
                                successNotice = null
                            },
                            enabled = !isSubmitting
                        ) {
                            Text("تغيير البريد")
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                                errorMessage = "يرجى إدخال بريد إلكتروني صحيح."
                                return@Button
                            }
                            isSubmitting = true
                            errorMessage = null
                            viewModel.sendEmailOtp(
                                email = email,
                                shouldCreateUser = true,
                                onSuccess = {
                                    isSubmitting = false
                                    otpSent = true
                                    successNotice = "تم إرسال الرمز إلى بريدك الإلكتروني بنجاح."
                                },
                                onError = {
                                    isSubmitting = false
                                    errorMessage = it
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.height(22.dp))
                        } else {
                            Text("إرسال رمز المصادقة إلى البريد", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // --- PASSWORD MODE ---
                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it; errorMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("رقم الهاتف أو البريد الإلكتروني") },
                    placeholder = { Text("05 55 12 34 56 أو بريدك") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
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

                Button(
                    onClick = {
                        if (identifier.isBlank()) {
                            errorMessage = "يرجى إدخال رقم الهاتف أو البريد الإلكتروني"
                            return@Button
                        }
                        if (password.isBlank()) {
                            errorMessage = "يرجى إدخال كلمة المرور"
                            return@Button
                        }
                        isSubmitting = true
                        errorMessage = null
                        viewModel.loginUser(
                            identifier = identifier,
                            password = password,
                            onSuccess = {
                                isSubmitting = false
                                onLoggedIn()
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
                        Text("تسجيل الدخول", fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(onClick = onForgotPassword, enabled = !isSubmitting) {
                    Text("نسيت كلمة المرور؟ استعادتها")
                }
            }

            successNotice?.let {
                Text(it, color = EmeraldPrimary, fontSize = 13.sp, textAlign = TextAlign.Center)
            }

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(4.dp))

            TextButton(onClick = onRegister, enabled = !isSubmitting) {
                Text("ليس لديك حساب؟ إنشاء حساب جديد")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
