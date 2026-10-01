package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.MainViewModel

@Composable
fun AuthDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var loginInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("0550123456") }
    var wilayaInput by remember { mutableStateOf("16 - الجزائر (Alger)") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isRegisterMode) "إنشاء حساب في ocaventeDz" else "تسجيل الدخول",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isRegisterMode) {
                    OutlinedTextField(
                        value = loginInput,
                        onValueChange = { loginInput = it },
                        label = { Text("البريد الإلكتروني أو اسم المستخدم") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("كلمة المرور") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("اسم المستخدم") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("البريد الإلكتروني") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("كلمة المرور (6 أحرف على الأقل)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("رقم الهاتف") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
                    Text(
                        text = if (isRegisterMode) "لديك حساب بالفعل؟ سجل دخولك" else "ليس لديك حساب؟ أنشئ حساباً الآن",
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isRegisterMode) {
                        if (loginInput.isNotBlank() && passwordInput.isNotBlank()) {
                            viewModel.login(loginInput, passwordInput)
                            onDismiss()
                        }
                    } else {
                        if (emailInput.isNotBlank() && usernameInput.isNotBlank() && passwordInput.isNotBlank()) {
                            viewModel.register(usernameInput, emailInput, passwordInput, phoneInput, wilayaInput)
                            onDismiss()
                        }
                    }
                }
            ) {
                Text(if (isRegisterMode) "تسجيل حساب" else "دخول")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
