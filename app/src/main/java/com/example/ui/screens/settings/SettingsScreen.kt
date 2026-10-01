package com.example.ui.screens.settings

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.InfoDialog
import com.example.ui.components.OcaSubTopBar
import com.example.ui.theme.OcaGreenPrimary

@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onNavigateToBack4App: () -> Unit = {},
    onBackClick: () -> Unit
) {
    var pushNotifications by remember { mutableStateOf(true) }
    var chatSound by remember { mutableStateOf(true) }
    var selectedLanguage by remember { mutableStateOf("العربية (الجزائر)") }
    var showLangDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            OcaSubTopBar(
                title = "الإعدادات العامة",
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
            // Appearance
            SettingsCard(title = "المظهر والعرض") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("الوضع الليلي / الداكن", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                        Text("تقليل استهلاك البطارية وإراحة العين في الإضاءة الخافتة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = onToggleDarkMode,
                        modifier = Modifier.testTag("dark_mode_switch")
                    )
                }
            }

            // Language
            SettingsCard(title = "اللغة والمنطقة") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLangDialog = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("لغة التطبيق", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                        Text(selectedLanguage, style = MaterialTheme.typography.bodySmall, color = OcaGreenPrimary)
                    }
                    Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                }
            }

            // Notifications
            SettingsCard(title = "تفضيلات الإشعارات") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("إشعارات التطبيق الفورية", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                        Text("استلام تنبيهات عند وصول رسالة جديدة أو عرض سعر", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = pushNotifications,
                        onCheckedChange = { pushNotifications = it }
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
                        Text("أصوات المحادثات", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                        Text("تشغيل نغمة خفيفة عند إرسال واستقبال الرسائل", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = chatSound,
                        onCheckedChange = { chatSound = it }
                    )
                }
            }

            // Back4App Cloud Backend Card
            SettingsCard(title = "الربط السحابي (Back4App Cloud)") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToBack4App() }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "إعدادات مزامنة Back4App",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "ضبط Application ID ومفتاح REST ومزامنة قاعدة البيانات",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = OcaGreenPrimary)
                }
            }

            // App Version Info
            SettingsCard(title = "حول التطبيق") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("OcaVenteDz Android", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                    Text("الإصدار: 1.0.0 (Build 2026)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("برمجة وتطوير: راهم محمد لمين", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = OcaGreenPrimary))
                    Text("تطبيق بيع وشراء المستعمل والجديد في الـ 69 ولاية جزائرية بكل أمان وثقة.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("جميع الحقوق محفوظة @2026 OcaVenteDz", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (showLangDialog) {
            AlertDialog(
                onDismissRequest = { showLangDialog = false },
                title = { Text("اختر لغة التطبيق") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedLanguage = "العربية (الجزائر)"
                                    showLangDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(selected = selectedLanguage.startsWith("العربية"), onClick = null)
                            Text("العربية (الجزائر)")
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedLanguage = "Français (Algérie)"
                                    showLangDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(selected = selectedLanguage.startsWith("Français"), onClick = null)
                            Text("Français (Algérie)")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLangDialog = false }) {
                        Text("إغلاق")
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
            Column(content = content)
        }
    }
}
