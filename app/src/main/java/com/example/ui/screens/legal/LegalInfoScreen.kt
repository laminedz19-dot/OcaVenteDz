package com.example.ui.screens.legal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.OcaSubTopBar
import com.example.ui.theme.OcaGreenPrimary

@Composable
fun LegalInfoScreen(
    onBackClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("شروط الاستخدام", "سياسة الخصوصية", "عن OcaVenteDz")

    Scaffold(
        topBar = {
            OcaSubTopBar(
                title = "المعلومات القانونية والشروط",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    0 -> TermsOfUseContent()
                    1 -> PrivacyPolicyContent()
                    2 -> AboutContent()
                }
            }
        }
    }
}

@Composable
private fun TermsOfUseContent() {
    LegalSection(
        title = "1. قبول الشروط",
        content = "باستخدامك لتطبيق OcaVenteDz، فإنك توافق على الالتزام الكامل بشروط الاستخدام المعمول بها والقوانين الجزائرية المنظمة للمعاملات التجارية الإلكترونية وحماية المستهلك."
    )
    LegalSection(
        title = "2. مسؤولية المحتوى والإعلانات",
        content = "يتحمل المعلن المسؤولية القانونية الكاملة عن صحة ودقة المعلومات والصور المنشورة في إعلانه. يحظر نشر أي سلع ممنوعة قانوناً أو مسروقة أو تخالف النظام العام والآداب في الجمهورية الجزائرية الديمقراطية الشعبية."
    )
    LegalSection(
        title = "3. التعاملات المالية والتفاوض",
        content = "منصة OcaVenteDz هي مساحة تواصل وإعلان بين البائع والمشتري. ننصح دائماً بمعاينة السلعة والتأكد من سلامتها قبل تسليم المبالغ النقدية للبائع في الأماكن العامة."
    )
    LegalSection(
        title = "4. الرصيد والشحن",
        content = "أي رصيد يتم شحنه عبر بريدي موب أو CCP مخصص لترقية وتمييز الإعلانات داخل المنصة ولا يمكن استرداده نقداً إلا وفق الشروط المحددة."
    )
}

@Composable
private fun PrivacyPolicyContent() {
    LegalSection(
        title = "1. البيانات التي نجمعها",
        content = "نجمع فقط المعلومات الضرورية لتشغيل حسابك وعرض إعلاناتك، بما في ذلك الاسم، رقم الهاتف للتواصل مع المشترين، والولاية لتسهيل وصول المشترين المحليين إليك."
    )
    LegalSection(
        title = "2. حماية الخصوصية",
        content = "نلتزم بحماية سرية بياناتك الشخصية وعدم مشاركتها أو بيعها لأي أطراف ثالثة لأغراض إعلانية غير مصرح بها."
    )
    LegalSection(
        title = "3. الأمان والتشفير",
        content = "نستخدم تقنيات التشفير الحديثة لضمان أمان كلمات المرور والمحادثات المتبادلة بين مستخدمي المنصة."
    )
}

@Composable
private fun AboutContent() {
    LegalSection(
        title = "من نحن - OcaVenteDz",
        content = "OcaVenteDz هي منصة إعلانات مبوبة جزائرية رائدة ومتخصصة في تسهيل بيع وشراء السلع المستعملة والجديدة عبر كامل التراب الوطني في 69 ولاية، من سيارات وعقارات وإلكترونيات وهواتف بأعلى درجات الأمان والسهولة."
    )
    LegalSection(
        title = "معلومات الإصدار والتطوير",
        content = "الإصدار: 1.0.0 Stable\nبرمجة وتطوير: راهم محمد لمين\nالبريد الإلكتروني للدعم: contact@ocavente.dz\nجميع الحقوق محفوظة @2026 OcaVenteDz."
    )
}

@Composable
private fun LegalSection(title: String, content: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = OcaGreenPrimary
                )
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
