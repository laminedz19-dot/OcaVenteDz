package com.example.ui.screens.legal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalInfoScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الشروط القانونية والخصوصية", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        modifier = Modifier.testTag("legal_info_screen")
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "شروط الاستخدام والنشر في OcaVenteDz",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "1. يلتزم المستخدم بعدم نشر أي منتجات محظورة قانوناً في الجمهورية الجزائرية الديمقراطية الشعبية.\n\n" +
                                   "2. يتحمل المعلن كامل المسؤولية القانونية والأخلاقية عن صحة المعلومات والصور المنشورة.\n\n" +
                                   "3. تحتفظ إدارة OcaVenteDz بحق رفض أو حذف أي إعلان مخالف للشروط أو يثير شبهة احتيال دون إشعار مسبق.\n\n" +
                                   "4. منصة OcaVenteDz هي وسيط رقمي حر يربط بين البائع والمشتري ولا تتقاضى أي نسبة من قيمة البيع النهائي بين الأفراد.",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "سياسة حماية البيانات والخصوصية",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• يتم تخزين بيانات المستخدمين ومحافظهم بشكل آمن ومشفر محلياً.\n\n" +
                                   "• لا نقوم بمشاركة أرقام الهواتف أو المعلومات الشخصية مع أي طرف ثالث لأغراض تجارية.\n\n" +
                                   "• يمكن للمستخدم طلب حذف حسابه وبياناته بالكامل في أي وقت من إعدادات الملف الشخصي.",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
