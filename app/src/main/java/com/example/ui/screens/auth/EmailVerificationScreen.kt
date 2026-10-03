package com.example.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Button
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.MarketplaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationScreen(
    email: String,
    viewModel: MarketplaceViewModel,
    onBack: () -> Unit,
    onVerified: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("تأكيد البريد الإلكتروني", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "رجوع") } }
        )
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.height(58.dp))
            Spacer(modifier = Modifier.height(14.dp))
            Text("أدخل رمز التحقق", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("أرسلنا رمزًا من 6 أرقام إلى:\n$email", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(18.dp))
            OutlinedTextField(
                value = code,
                onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) { code = it; feedback = null } },
                label = { Text("رمز التحقق") },
                placeholder = { Text("123456") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting
            )
            feedback?.let { Text(it, color = if (isError) MaterialTheme.colorScheme.error else EmeraldPrimary, fontSize = 12.sp, textAlign = TextAlign.Center) }
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = {
                    isSubmitting = true
                    viewModel.verifyRegistrationEmail(email, code, onSuccess = { isSubmitting = false; onVerified() }, onError = { isSubmitting = false; isError = true; feedback = it })
                },
                enabled = !isSubmitting && code.length == 6,
                modifier = Modifier.fillMaxWidth()
            ) { if (isSubmitting) CircularProgressIndicator(modifier = Modifier.height(20.dp)) else Text("تأكيد البريد") }
            TextButton(
                onClick = { viewModel.resendRegistrationEmailCode(email, onSuccess = { isError = false; feedback = it }, onError = { isError = true; feedback = it }) },
                enabled = !isSubmitting
            ) { Text("إعادة إرسال الرمز") }
        }
    }
}
