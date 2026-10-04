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
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
            Spacer(modifier = Modifier.height(30.dp))
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
                text = "أدخل رقم هاتفك وكلمة المرور لتسجيل الدخول إلى حسابك.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = identifier,
                onValueChange = { identifier = it; errorMessage = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("رقم الهاتف") },
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

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, textAlign = TextAlign.Center)
            }

            Button(
                onClick = {
                    if (identifier.isBlank()) {
                        errorMessage = "يرجى إدخال رقم الهاتف"
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

            TextButton(onClick = onRegister, enabled = !isSubmitting) {
                Text("ليس لديك حساب؟ إنشاء حساب جديد")
            }
            TextButton(onClick = onForgotPassword, enabled = !isSubmitting) {
                Text("نسيت كلمة المرور؟ استعادتها")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
