package com.example.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.back4app.Back4AppClient
import com.example.data.remote.back4app.Back4AppConfig
import com.example.ui.components.OcaSubTopBar
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import com.example.ui.theme.OcaAmberTertiary
import com.example.ui.theme.OcaGreenPrimary
import com.example.ui.theme.OcaNavySecondary
import kotlinx.coroutines.launch

@Composable
fun Back4AppSettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var appId by remember { mutableStateOf(Back4AppConfig.getAppId(context)) }
    var restKey by remember { mutableStateOf(Back4AppConfig.getRestKey(context)) }
    var serverUrl by remember { mutableStateOf(Back4AppConfig.getServerUrl(context)) }

    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testSuccess by remember { mutableStateOf<Boolean?>(null) }

    Scaffold(
        topBar = {
            OcaSubTopBar(
                title = "ربط التطبيق مع Back4App",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cloud Status Header Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (Back4AppConfig.isConfigured(context)) OcaGreenPrimary.copy(alpha = 0.12f)
                        else OcaAmberTertiary.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (Back4AppConfig.isConfigured(context)) OcaGreenPrimary else OcaAmberTertiary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (Back4AppConfig.isConfigured(context)) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = if (Back4AppConfig.isConfigured(context)) OcaGreenPrimary else OcaAmberTertiary,
                        modifier = Modifier.size(36.dp)
                    )
                    Column {
                        Text(
                            text = if (Back4AppConfig.isConfigured(context)) "Back4App متصل ومهيأ 🟢" else "بانتظار إدخال مفاتيح السحابة 🟡",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (Back4AppConfig.isConfigured(context)) "تتم مزامنة الإعلانات وطلبات الشحن مع سحابة Parse / Back4App."
                                   else "أدخل Application ID و REST API Key لربط قاعدة البيانات السحابية.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Credentials Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "مفاتيح الربط مع سحابة Parse / Back4App",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    OutlinedTextField(
                        value = appId,
                        onValueChange = { appId = it },
                        label = { Text("Application ID") },
                        placeholder = { Text("مثال: 9gK1X...") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = restKey,
                        onValueChange = { restKey = it },
                        label = { Text("REST API Key") },
                        placeholder = { Text("مثال: vP4mQ...") },
                        leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("Server URL") },
                        placeholder = { Text(Back4AppConfig.DEFAULT_SERVER_URL) },
                        leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (testResult != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (testSuccess == true) OcaGreenPrimary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (testSuccess == true) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (testSuccess == true) OcaGreenPrimary else MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = testResult!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (testSuccess == true) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SecondaryButton(
                            text = if (isTesting) "جاري الاختبار..." else "اختبار الاتصال",
                            icon = Icons.Default.NetworkCheck,
                            enabled = !isTesting && appId.isNotBlank() && restKey.isNotBlank(),
                            onClick = {
                                isTesting = true
                                testResult = null
                                // Temporarily save to test
                                Back4AppConfig.saveConfig(context, appId, restKey, serverUrl)
                                coroutineScope.launch {
                                    val res = Back4AppClient.testConnection(context)
                                    isTesting = false
                                    if (res.isSuccess) {
                                        testSuccess = true
                                        testResult = res.getOrNull()
                                    } else {
                                        testSuccess = false
                                        testResult = res.exceptionOrNull()?.message ?: "فشل الاتصال"
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        PrimaryButton(
                            text = "حفظ الإعدادات",
                            icon = Icons.Default.Save,
                            onClick = {
                                Back4AppConfig.saveConfig(context, appId, restKey, serverUrl)
                                Toast.makeText(context, "تم حفظ إعدادات Back4App بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Instructions Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "كيفية ربط التطبيق مع Back4App ☁️",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    InstructionStep(
                        step = "1",
                        text = "سجّل حساباً مجانياً على https://www.back4app.com وأنشئ تطبيقاً جديداً (New App)."
                    )
                    InstructionStep(
                        step = "2",
                        text = "توجه إلى App Settings ثم Security & Keys وانسخ كلاً من Application ID و REST API Key."
                    )
                    InstructionStep(
                        step = "3",
                        text = "الصق المفاتيح في الحقول أعلاه واضغط على زر 'اختبار الاتصال' للتحقق، ثم احفظ الإعدادات."
                    )
                    InstructionStep(
                        step = "4",
                        text = "يتم دعم جداول (Classes): 'Listing' للإعلانات، و'RechargeRequest' لطلبات شحن الرصيد والوصولات للمراجعة اليدوية."
                    )
                }
            }
        }
    }
}

@Composable
private fun InstructionStep(step: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = OcaGreenPrimary,
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = step,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
