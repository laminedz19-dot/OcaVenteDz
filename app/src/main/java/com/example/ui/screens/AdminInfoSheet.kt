package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminInfoSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(48.dp)
                ) {
                    Image(
                        painter = painterResource(id = com.example.R.drawable.app_logo),
                        contentDescription = "OcaVenteDz Logo",
                        modifier = Modifier.fillMaxSize().padding(2.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "بوابة إدارة OcaVenteDz Admin",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "بيع وشراء المستعمل بكل ثقة",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🛡️ تطبيق الإدارة المستقل:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("• تم بناء موديول كامل مستقل باسم :admin", fontSize = 12.sp)
                    Text("• Package ID: dz.ocavente.admin", fontSize = 12.sp)
                    Text("• يشتمل على: لوحة الإحصائيات، قبول ورفض الإعلانات مع ذكر السبب، مراجعة طلبات شحن الرصيد والتحقق منها وإضافتها تلقائياً عبر المعاملات المالية الآمنة، إدارة المستخدمين وتغيير رتبهم وحظرهم، وسجل العمليات Audit Logs.", fontSize = 12.sp)
                    Text("• لا وجود لأي Firebase SDK في التطبيقين.", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = {
                    try {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage("dz.ocavente.admin")
                        if (launchIntent != null) {
                            context.startActivity(launchIntent)
                        }
                    } catch (_: Exception) {}
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Launch, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("فتح تطبيق المشرف المستقل (dz.ocavente.admin)")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
