package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.OcaVenteTheme
import com.example.ui.viewmodel.MarketplaceViewModel
import kotlinx.coroutines.flow.collectLatest

class AdminActivity : ComponentActivity() {

    private val viewModel: MarketplaceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            OcaVenteTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    val snackbarHostState = remember { SnackbarHostState() }
                    var isAuthenticated by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        viewModel.uiEvent.collectLatest { msg ->
                            snackbarHostState.showSnackbar(msg)
                        }
                    }

                    // Restore and validate the Supabase admin session on startup.
                    LaunchedEffect(Unit) {
                        viewModel.restoreAdminSession {
                            isAuthenticated = true
                        }
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            AnimatedContent(
                                targetState = isAuthenticated,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "AdminAuthTransition"
                            ) { authed ->
                                if (authed) {
                                    BackHandler {
                                        viewModel.repository.authService.signOut()
                                        viewModel.exitAdminSession()
                                        isAuthenticated = false
                                    }
                                    AdminDashboardScreen(
                                        viewModel = viewModel,
                                        onBack = {
                                            viewModel.repository.authService.signOut()
                                            viewModel.exitAdminSession()
                                            isAuthenticated = false
                                            finish()
                                        }
                                    )
                                } else {
                                    AdminLoginGate(
                                        viewModel = viewModel,
                                        onSuccess = {
                                            isAuthenticated = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * بوابة دخول المشرف عبر المصادقة المحلية/Supabase.
 * Authenticates via the configured application account
 * using the configured application authentication. No local PIN or hardcoded credentials.
 */
@Composable
fun AdminLoginGate(
    viewModel: MarketplaceViewModel,
    onSuccess: () -> Unit
) {
    var mode by remember { mutableStateOf(0) } // 0: OTP Email, 1: Password
    var email by remember { mutableStateOf("laminedz.19@gmail.com") }
    var otpToken by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successNotice by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("admin_login_gate"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Shield & Admin Icon
        Surface(
            shape = CircleShape,
            color = EmeraldPrimary.copy(alpha = 0.12f),
            modifier = Modifier.size(90.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(54.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "OcaVenteDz إشراف (Admin App)",
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "بوابة الإدارة المركزية وحماية شحن الرصيد\nالمصادقة السحابية الصارمة عبر Supabase Auth",
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Toggle Tabs
        androidx.compose.material3.TabRow(
            selectedTabIndex = mode,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            androidx.compose.material3.Tab(
                selected = mode == 0,
                onClick = { mode = 0; errorMessage = null; successNotice = null },
                text = { Text("رمز البريد (OTP)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
            )
            androidx.compose.material3.Tab(
                selected = mode == 1,
                onClick = { mode = 1; errorMessage = null; successNotice = null },
                text = { Text("كلمة المرور", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = GoldSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (mode == 0) "دخول المشرف برمز التحقق (OTP)" else "تسجيل دخول المشرف بكلمة المرور",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = null
                    },
                    label = { Text("البريد الإلكتروني للمسؤول") },
                    placeholder = { Text("laminedz.19@gmail.com") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    leadingIcon = {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && !otpSent
                )

                if (mode == 0) {
                    // OTP Mode
                    if (otpSent) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "تم إرسال رمز التحقق إلى بريدك. أدخل الرمز (6 أرقام):",
                            fontSize = 12.sp,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = otpToken,
                            onValueChange = {
                                if (it.length <= 8) otpToken = it.filter { ch -> ch.isDigit() }
                                errorMessage = null
                            },
                            label = { Text("رمز التحقق (6 أرقام)") },
                            placeholder = { Text("123456") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (otpToken.length < 4) {
                                    errorMessage = "يرجى إدخال رمز التحقق المستلم"
                                    return@Button
                                }
                                isLoading = true
                                errorMessage = null
                                viewModel.loginAdminWithOtp(
                                    email = email.trim(),
                                    token = otpToken.trim(),
                                    onSuccess = {
                                        isLoading = false
                                        onSuccess()
                                    },
                                    onError = { err ->
                                        isLoading = false
                                        errorMessage = err
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري التحقق والدخول...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            } else {
                                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تأكيد الرمز والدخول", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(
                                onClick = {
                                    isLoading = true
                                    errorMessage = null
                                    viewModel.sendEmailOtp(
                                        email = email.trim(),
                                        shouldCreateUser = false,
                                        onSuccess = {
                                            isLoading = false
                                            successNotice = "تمت إعادة إرسال الرمز بنجاح."
                                        },
                                        onError = {
                                            isLoading = false
                                            errorMessage = it
                                        }
                                    )
                                },
                                enabled = !isLoading
                            ) {
                                Text("إعادة إرسال الرمز")
                            }

                            TextButton(
                                onClick = {
                                    otpSent = false
                                    otpToken = ""
                                },
                                enabled = !isLoading
                            ) {
                                Text("تغيير البريد")
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                                    errorMessage = "يرجى إدخال بريد إلكتروني صحيح"
                                    return@Button
                                }
                                isLoading = true
                                errorMessage = null
                                viewModel.sendEmailOtp(
                                    email = email.trim(),
                                    shouldCreateUser = false,
                                    onSuccess = {
                                        isLoading = false
                                        otpSent = true
                                        successNotice = "تم إرسال رمز المصادقة بنجاح إلى بريدك."
                                    },
                                    onError = {
                                        isLoading = false
                                        errorMessage = it
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري الإرسال...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            } else {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إرسال رمز المصادقة إلى البريد", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                } else {
                    // Password Mode
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text("كلمة المرور") },
                        placeholder = { Text("••••••••") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        isError = errorMessage != null,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                errorMessage = "يرجى ملء البريد الإلكتروني وكلمة المرور"
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null
                            viewModel.loginAdmin(
                                email = email.trim(),
                                pass = password,
                                onSuccess = {
                                    isLoading = false
                                    onSuccess()
                                },
                                onError = { err ->
                                    isLoading = false
                                    errorMessage = err
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري التحقق من الصلاحيات...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("دخول لوحة الإدارة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (successNotice != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = successNotice!!,
                        color = EmeraldPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Security Footnote
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "محمي بواسطة مصادقة التطبيق Claims (admin: true) • لا توجد أسرار محلية",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
