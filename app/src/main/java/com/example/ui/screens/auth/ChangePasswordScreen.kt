package com.example.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.components.InfoDialog
import com.example.ui.components.OcaSubTopBar
import com.example.ui.components.PrimaryButton

@Composable
fun ChangePasswordScreen(
    onBackClick: () -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            OcaSubTopBar(
                title = "تغيير كلمة المرور",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "اختر كلمة مرور قوية",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "يجب أن تحتوي كلمة المرور على 8 أحرف على الأقل، مع مزيج من الحروف والأرقام لضمان حماية حسابك.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = oldPassword,
                onValueChange = { oldPassword = it; errorMessage = null },
                label = { Text("كلمة المرور الحالية") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("current_pass_input")
            )

            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it; errorMessage = null },
                label = { Text("كلمة المرور الجديدة") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("new_pass_input")
            )

            OutlinedTextField(
                value = confirmNewPassword,
                onValueChange = { confirmNewPassword = it; errorMessage = null },
                label = { Text("تأكيد كلمة المرور الجديدة") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("confirm_new_pass_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            PrimaryButton(
                text = "حفظ كلمة المرور الجديدة",
                onClick = {
                    if (oldPassword.isBlank() || newPassword.isBlank()) {
                        errorMessage = "يرجى إدخال جميع الحقول"
                    } else if (newPassword != confirmNewPassword) {
                        errorMessage = "كلمتا المرور الجديدتان غير متطابقتين"
                    } else if (newPassword.length < 6) {
                        errorMessage = "يجب أن تكون كلمة المرور 6 أحرف على الأقل"
                    } else {
                        showDialog = true
                    }
                },
                testTag = "save_new_pass_btn"
            )
        }

        InfoDialog(
            show = showDialog,
            title = "تم بنجاح",
            message = "تم تغيير كلمة المرور الخاصة بحسابك بنجاح.",
            onDismiss = {
                showDialog = false
                onBackClick()
            }
        )
    }
}
