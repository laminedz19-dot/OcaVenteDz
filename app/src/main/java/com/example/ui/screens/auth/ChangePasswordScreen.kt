@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MarketplaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    viewModel: MarketplaceViewModel,
    onBack: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("تغيير كلمة المرور", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "رجوع") } }
        )
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary)
            Text("حماية حسابك", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("أدخل كلمة المرور الحالية والجديدة.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, fontSize = 13.sp)
            OutlinedTextField(currentPassword, { currentPassword = it; message = null }, Modifier.fillMaxWidth(), label = { Text("كلمة المرور الحالية") }, visualTransformation = PasswordVisualTransformation())
            OutlinedTextField(newPassword, { newPassword = it; message = null }, Modifier.fillMaxWidth(), label = { Text("كلمة المرور الجديدة") }, visualTransformation = PasswordVisualTransformation())
            OutlinedTextField(confirmation, { confirmation = it; message = null }, Modifier.fillMaxWidth(), label = { Text("تأكيد كلمة المرور الجديدة") }, visualTransformation = PasswordVisualTransformation())
            message?.let { Text(it, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center, fontSize = 13.sp) }
            Button(
                onClick = {
                    viewModel.changePassword(currentPassword, newPassword, confirmation,
                        onSuccess = { isError = false; message = it },
                        onError = { isError = true; message = it })
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("حفظ كلمة المرور الجديدة", fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
