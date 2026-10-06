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
    var identifier by remember { mutableStateOf("") }
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
            Spacer(modifier = Modifier.height(30.dp))
            Icon(
                imageVector = Icons.Default.LockReset,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.height(52.dp)
            )
            Text(
                text = "هل نسيت كلمة المرور؟",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "أدخل بريدك الإلكتروني المسجل لإرسال رابط استعادة كلمة المرور.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = identifier,
                onValueChange = { identifier = it; feedback = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("البريد الإلكتروني") },
                placeholder = { Text("example@gmail.com") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                enabled = !isSubmitting
            )

            feedback?.let {
                Text(
                    text = it,
                    color = if (isError) MaterialTheme.colorScheme.error else EmeraldPrimary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = {
                    val cleanEmail = identifier.trim().lowercase()
                    if (cleanEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                        feedback = "يرجى إدخال بريد إلكتروني صالح (مثال: example@gmail.com)"
                        isError = true
                        return@Button
                    }
                    isSubmitting = true
                    feedback = null
                    viewModel.requestPasswordReset(
                        identifier = cleanEmail,
                        onSuccess = {
                            isSubmitting = false
                            isError = false
                            feedback = it
                        },
                        onError = {
                            isSubmitting = false
                            isError = true
                            feedback = it
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
                    Text("إرسال رابط الاستعادة", fontWeight = FontWeight.Bold)
                }
            }

            TextButton(onClick = onLogin, enabled = !isSubmitting) {
                Text("العودة إلى تسجيل الدخول")
            }
            Text(
                text = "ستتلقى رسالة على بريدك الإلكتروني تتضمن رابطاً أو تعليمات لاستعادة كلمة المرور.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}
