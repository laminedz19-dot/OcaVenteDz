package com.example.ui.screens.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.UrgentRed
import com.example.ui.viewmodel.MarketplaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityCenterScreen(
    viewModel: MarketplaceViewModel,
    onBack: () -> Unit,
    onOpenLegal: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مركز الأمان ومكافحة الاحتيال", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        modifier = Modifier.testTag("security_center_screen")
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Main Protection Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldDark)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "دليلك للتسوق الآمن في الجزائر",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "منصة OcaVenteDz تشجع التعاملات المباشرة وجهًا لوجه والفحص الدقيق للسلع قبل الدفع.",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Key Advice Items
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SecurityTip(
                            icon = Icons.Default.Handshake,
                            title = "المقابلة في أماكن عامة ومزدحمة",
                            desc = "احرص دائمًا على الالتقاء بالبائع أو المشتري في مكان عام معروف نهارًا."
                        )
                        Divider()
                        SecurityTip(
                            icon = Icons.Default.CheckCircle,
                            title = "معاينة السلعة وتجربتها قبل الدفع",
                            desc = "لا تقم بتحويل أي مبلغ عربون مسبق عبر بريدي موب أو CCP قبل رؤية وفحص المنتج بنفسك."
                        )
                        Divider()
                        SecurityTip(
                            icon = Icons.Default.Block,
                            title = "تجنب الروابط الخارجية والمشبوهة",
                            desc = "لا تدخل بيانات بطاقتك الذهبية أو كلمة سر بريدي موب في أي رابط يرسله لك شخص في الرسائل."
                        )
                        Divider()
                        SecurityTip(
                            icon = Icons.Default.Report,
                            title = "الإبلاغ الفوري عن الإعلانات المشبوهة",
                            desc = "استخدم زر 'إبلاغ عن هذا الإعلان' في أي وقت لمساعدة فريق الإشراف على مراجعته."
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = onOpenLegal,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("شروط الاستخدام وسياسة الخصوصية")
                }
            }
        }
    }
}

@Composable
private fun SecurityTip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
        }
    }
}
