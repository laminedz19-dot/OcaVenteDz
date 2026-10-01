package com.example.ui.screens.profile

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DeleteAccountDialog(
    show: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = "حذف الحساب نهائياً", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف حسابك من منصة OcaVenteDz؟ سيتم إزالة جميع إعلاناتك ورسائلك ورصيدك نهائياً دون إمكانية استرجاعها."
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = "تأكيد الحذف")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = "إلغاء")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}
