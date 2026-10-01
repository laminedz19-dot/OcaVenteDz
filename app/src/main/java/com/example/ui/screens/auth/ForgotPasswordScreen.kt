package com.example.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.OcaSubTopBar
import com.example.ui.components.PrimaryButton
import com.example.ui.theme.OcaGreenPrimary

@Composable
fun ForgotPasswordScreen(
    onBackClick: () -> Unit
) {
    var emailOrPhone by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            OcaSubTopBar(
                title = "استرجاع الحساب",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isSubmitted) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "نسيت كلمة المرور؟",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "أدخل بريدك الإلكتروني أو رقم هاتفك المسجل وسنرسل لك رمز التأكيد لإعادة تعيين كلمة المرور.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = emailOrPhone,
                    onValueChange = { emailOrPhone = it },
                    label = { Text("البريد الإلكتروني أو الهاتف") },
                    placeholder = { Text("example@mail.com أو 05xxxxxxxx") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forgot_pass_input")
                )

                PrimaryButton(
                    text = "إرسال رمز الاسترجاع",
                    enabled = emailOrPhone.isNotBlank(),
                    onClick = { isSubmitted = true },
                    testTag = "send_reset_code_btn"
                )
            } else {
                Spacer(modifier = Modifier.height(40.dp))
                Icon(
                    imageVector = Icons.Default.MarkEmailRead,
                    contentDescription = null,
                    tint = OcaGreenPrimary,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "تم إرسال تعليمات الاسترجاع",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "إذا كان الحساب $emailOrPhone مسجلاً لدينا، فستصلك رسالة تحتوي على رابط ورمز لإعادة تعيين كلمة المرور.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                PrimaryButton(
                    text = "العودة لتسجيل الدخول",
                    onClick = onBackClick,
                    testTag = "back_to_login_btn"
                )
            }
        }
    }
}
