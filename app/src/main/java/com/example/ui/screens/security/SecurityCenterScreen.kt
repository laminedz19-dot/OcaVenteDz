package com.example.ui.screens.security

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.InfoDialog
import com.example.ui.components.OcaSubTopBar
import com.example.ui.screens.profile.DeleteAccountDialog
import com.example.ui.theme.OcaGreenPrimary

@Composable
fun SecurityCenterScreen(
    userEmail: String,
    userPhone: String,
    onNavigateToChangePassword: () -> Unit,
    onBackClick: () -> Unit
) {
    var twoFactorEnabled by remember { mutableStateOf(false) }
    var biometricEnabled by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogMsg by remember { mutableStateOf("") }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            OcaSubTopBar(
                title = "مركز الأمان والخصوصية",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Account Status Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = OcaGreenPrimary.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = OcaGreenPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "حسابك محمي وآمن",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = OcaGreenPrimary
                            )
                        )
                        Text(
                            text = "رقم الهاتف والبريد الإلكتروني موثقان في الجزائر",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Password Section
            SecurityGroupCard(title = "تسجيل الدخول وكلمة المرور") {
                SecurityRowItem(
                    title = "تغيير كلمة المرور",
                    subtitle = "آخر تحديث منذ 3 أشهر",
                    onClick = onNavigateToChangePassword,
                    testTag = "change_password_item"
                )
            }

            // Security toggles
            SecurityGroupCard(title = "الحماية الإضافية") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("التحقق بخطوتين (2FA)", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                        Text("إرسال رمز SMS إلى هاتفك عند تسجيل الدخول من جهاز جديد", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = twoFactorEnabled,
                        onCheckedChange = {
                            twoFactorEnabled = it
                            dialogMsg = if (it) "تم تفعيل التحقق بخطوتين بنجاح" else "تم إيقاف التحقق بخطوتين"
                            showDialog = true
                        }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("تسجيل الدخول بالبصمة", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                        Text("فتح التطبيق بسرعة باستخدام بصمة الإصبع أو الوجه", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = { biometricEnabled = it }
                    )
                }
            }

            // Active Sessions
            SecurityGroupCard(title = "الأجهزة المتصلة والجلسات النشطة") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = OcaGreenPrimary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("هاتف أندرويد الحالي (هذا الجهاز)", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        Text("الجزائر العاصمة • نشط الآن", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = OcaGreenPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "الجهاز الحالي",
                            color = OcaGreenPrimary,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Danger zone
            SecurityGroupCard(title = "منطقة الخطر") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDeleteAccountDialog = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Column {
                        Text("حذف الحساب نهائياً", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.error)
                        Text("حذف جميع الإعلانات والبيانات نهائياً", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        InfoDialog(
            show = showDialog,
            title = "مركز الأمان",
            message = dialogMsg,
            onDismiss = { showDialog = false }
        )

        DeleteAccountDialog(
            show = showDeleteAccountDialog,
            onConfirm = {
                showDeleteAccountDialog = false
                dialogMsg = "تم إرسال طلب حذف الحساب، سيتم إعلامك بالبريد الإلكتروني."
                showDialog = true
            },
            onDismiss = { showDeleteAccountDialog = false }
        )
    }
}

@Composable
private fun SecurityGroupCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
            Column(content = content)
        }
    }
}

@Composable
private fun SecurityRowItem(title: String, subtitle: String, onClick: () -> Unit, testTag: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}
