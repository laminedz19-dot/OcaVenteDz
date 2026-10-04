@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.MarketplaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    viewModel: MarketplaceViewModel,
    onBack: () -> Unit,
    onLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var otpToken by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }

    var isSubmitting by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("استعادة كلمة المرور", fontWeight = FontWeight.Bold) },
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
            Spacer(modifier = Modifier.height(24.dp))
            Icon(
                imageVector = Icons.Default.LockReset,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.height(52.dp)
            )
            Text(
                text = "استعادة كلمة المرور",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (!otpSent)
                    "أدخل بريدك الإلكتروني لإرسال رمز المصادقة المباشر (OTP)."
                else
                    "أدخل رمز التحقق المكون من 6 أرقام المستلم في بريدك وكلمة المرور الجديدة.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; feedback = null },
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
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "تم إرسال رمز الاستعادة إلى: $email",
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "فحص صندوق البريد والرسائل غير المرغوب فيها (Spam).",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = otpToken,
                    onValueChange = {
                        if (it.length <= 8) otpToken = it.filter { ch -> ch.isDigit() }
                        feedback = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("رمز التحقق (6 أرقام)") },
                    placeholder = { Text("123456") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = !isSubmitting
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; feedback = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("كلمة المرور الجديدة") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    enabled = !isSubmitting
                )

                OutlinedTextField(
                    value = confirmNewPassword,
                    onValueChange = { confirmNewPassword = it; feedback = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("تأكيد كلمة المرور الجديدة") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    enabled = !isSubmitting
                )

                Button(
                    onClick = {
                        if (otpToken.length < 4) {
                            feedback = "يرجى إدخال رمز التحقق المستلم."
                            isError = true
                            return@Button
                        }
                        if (newPassword.length < 6) {
                            feedback = "يجب أن تتكون كلمة المرور من 6 أحرف على الأقل."
                            isError = true
                            return@Button
                        }
                        if (newPassword != confirmNewPassword) {
                            feedback = "كلمتا المرور غير متطابقتين."
                            isError = true
                            return@Button
                        }

                        isSubmitting = true
                        feedback = null
                        viewModel.resetPasswordWithOtp(
                            email = email,
                            token = otpToken,
                            newPassword = newPassword,
                            onSuccess = {
                                isSubmitting = false
                                isError = false
                                feedback = "تم تعيين كلمة المرور الجديدة بنجاح! يمكنك الآن تسجيل الدخول."
                            },
                            onError = {
                                isSubmitting = false
                                isError = true
                                feedback = it
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
                        Text("تأكيد وتعيين كلمة المرور", fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(
                    onClick = {
                        otpSent = false
                        otpToken = ""
                        feedback = null
                    },
                    enabled = !isSubmitting
                ) {
                    Text("تغيير البريد الإلكتروني")
                }
            } else {
                Button(
                    onClick = {
                        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                            feedback = "يرجى إدخال بريد إلكتروني صحيح."
                            isError = true
                            return@Button
                        }
                        isSubmitting = true
                        feedback = null
                        viewModel.sendEmailOtp(
                            email = email,
                            shouldCreateUser = false,
                            onSuccess = {
                                isSubmitting = false
                                isError = false
                                otpSent = true
                                feedback = "تم إرسال رمز الاستعادة إلى بريدك بنجاح."
                            },
                            onError = {
                                isSubmitting = false
                                isError = true
                                feedback = it
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
                        Text("إرسال رمز الاستعادة إلى البريد", fontWeight = FontWeight.Bold)
                    }
                }
            }

            feedback?.let {
                Text(
                    text = it,
                    color = if (isError) MaterialTheme.colorScheme.error else EmeraldPrimary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            TextButton(onClick = onLogin, enabled = !isSubmitting) {
                Text("العودة إلى تسجيل الدخول")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
