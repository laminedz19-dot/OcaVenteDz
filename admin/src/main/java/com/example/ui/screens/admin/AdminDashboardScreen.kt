package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.FilterChip
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.local.TopUpRequestEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ListingEntity
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatDzd
import com.example.ui.components.formatTimeAgo
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.UrgentRed
import com.example.ui.theme.VerifiedBlue
import com.example.ui.viewmodel.MarketplaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: MarketplaceViewModel,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    // 0: Moderation/Ads, 1: Pricing Settings, 2: Users, 3: Payments, 4: Analytics & Reports

    val adminListings by viewModel.adminListings.collectAsState()
    val platformSettings by viewModel.platformSettings.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val allPayments by viewModel.allPayments.collectAsState()
    val allReports by viewModel.allReports.collectAsState()
    val adminAuditLogs by viewModel.adminAuditLogs.collectAsState()
    val allTopUpRequests by viewModel.allTopUpRequests.collectAsState()
    val topUpListUiState by viewModel.topUpListUiState.collectAsState()
    val isProcessingTopUp by viewModel.isProcessingTopUp.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val clipboardManager = LocalClipboardManager.current
    var ripInput by remember(platformSettings) { mutableStateOf(platformSettings.officialRip) }
    var keyInput by remember(platformSettings) { mutableStateOf(platformSettings.officialKey) }
    var holderInput by remember(platformSettings) { mutableStateOf(platformSettings.officialAccountHolder) }
    var providerInput by remember(platformSettings) { mutableStateOf(platformSettings.officialProviderName) }
    var instructionsInput by remember(platformSettings) { mutableStateOf(platformSettings.officialInstructions) }
    var testCopySuccess by remember { mutableStateOf(false) }

    var selectedTopUpFilter by remember { mutableStateOf("PENDING") }
    var previewReceiptUrl by remember { mutableStateOf<String?>(null) }
    var rejectTopUpTarget by remember { mutableStateOf<TopUpRequestEntity?>(null) }
    var rejectTopUpReason by remember { mutableStateOf("") }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var currentPasswordInput by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var passwordChangeError by remember { mutableStateOf<String?>(null) }
    var passwordChangeLoading by remember { mutableStateOf(false) }

    // Rejection Dialog state
    var rejectingAdId by remember { mutableStateOf<String?>(null) }
    var rejectionReasonText by remember { mutableStateOf("محتوى مخالف لشروط النشر") }

    // Dynamic Pricing Form State
    var standardFeeText by remember(platformSettings) { mutableStateOf(platformSettings.standardAdFeeDzd.toString()) }
    var featuredFeeText by remember(platformSettings) { mutableStateOf(platformSettings.featuredAdFeeDzd.toString()) }
    var urgentFeeText by remember(platformSettings) { mutableStateOf(platformSettings.urgentAdFeeDzd.toString()) }
    var durationDaysText by remember(platformSettings) { mutableStateOf(platformSettings.adDurationDays.toString()) }
    var autoPublishState by remember(platformSettings) { mutableStateOf(platformSettings.autoPublishAfterPayment) }

    // Search Users
    var userSearchQuery by remember { mutableStateOf("") }

    if (rejectingAdId != null) {
        AlertDialog(
            onDismissRequest = { rejectingAdId = null },
            title = { Text("رفض الإعلان وتحديد السبب", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("سيتم إشعار المعلن بسبب الرفض لتصحيحه:", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectionReasonText,
                        onValueChange = { rejectionReasonText = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        rejectingAdId?.let { viewModel.adminRejectListing(it, rejectionReasonText) }
                        rejectingAdId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UrgentRed)
                ) {
                    Text("تأكيد الرفض")
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectingAdId = null }) { Text("إلغاء") }
            }
        )
    }

    // Receipt Preview Dialog
    if (previewReceiptUrl != null) {
        Dialog(onDismissRequest = { previewReceiptUrl = null }) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("صورة وصل التحويل (بريدي موب / CCP)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        IconButton(onClick = { previewReceiptUrl = null }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    AsyncImage(
                        model = previewReceiptUrl,
                        contentDescription = "صورة الوصل",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.05f)),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { previewReceiptUrl = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إغلاق المعاينة")
                    }
                }
            }
        }
    }

    // Reject TopUp Reason Dialog
    if (rejectTopUpTarget != null) {
        val target = rejectTopUpTarget!!
        AlertDialog(
            onDismissRequest = { rejectTopUpTarget = null },
            title = { Text("رفض طلب شحن الرصيد", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "حدد سبب الرفض لطلب ${target.userName} بمبلغ ${target.amountDzd} دج:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    listOf(
                        "لم يصل المبلغ إلى الحساب بعد",
                        "صورة الوصل غير واضحة أو ناقصة",
                        "رقم العملية أو المرجع غير مطابق",
                        "تم استخدام هذا الوصل سابقاً"
                    ).forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable { rejectTopUpReason = preset }
                        ) {
                            Text(preset, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = rejectTopUpReason,
                        onValueChange = { rejectTopUpReason = it },
                        label = { Text("سبب الرفض") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rejectTopUpRequest(target.id, rejectTopUpReason)
                        rejectTopUpTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UrgentRed)
                ) {
                    Text("تأكيد الرفض")
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectTopUpTarget = null }) { Text("إلغاء") }
            }
        )
    }

    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = { if (!passwordChangeLoading) showChangePinDialog = false },
            title = { Text("تغيير كلمة مرور المشرف", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("الحساب: laminedz.19@gmail.com", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = currentPasswordInput,
                        onValueChange = { currentPasswordInput = it; passwordChangeError = null },
                        label = { Text("كلمة المرور الحالية") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it; passwordChangeError = null },
                        label = { Text("كلمة المرور الجديدة (8 أحرف على الأقل)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPasswordInput,
                        onValueChange = { confirmPasswordInput = it; passwordChangeError = null },
                        label = { Text("تأكيد كلمة المرور الجديدة") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    passwordChangeError?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(error, color = UrgentRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !passwordChangeLoading,
                    onClick = {
                        passwordChangeLoading = true
                        passwordChangeError = null
                        viewModel.changePassword(
                            currentPasswordInput,
                            newPasswordInput,
                            confirmPasswordInput,
                            onSuccess = {
                                passwordChangeLoading = false
                                currentPasswordInput = ""
                                newPasswordInput = ""
                                confirmPasswordInput = ""
                                showChangePinDialog = false
                            },
                            onError = {
                                passwordChangeLoading = false
                                passwordChangeError = it
                            }
                        )
                    }
                ) {
                    if (passwordChangeLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("حفظ كلمة المرور")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_dashboard_screen")
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = GoldSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("لوحة إدارة OcaVenteDz المعزولة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("جلسة إشراف مشفرة ومؤمنة", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "رجوع وإغلاق الجلسة")
                }
            },
            actions = {
                IconButton(
                    onClick = { viewModel.refreshAllAdminData() },
                    enabled = !isRefreshing
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = EmeraldPrimary
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "تحديث البيانات",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = UrgentRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .testTag("admin_exit_session_button")
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إنهاء الجلسة والقفل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        // Security Notice Banner for Isolated Admin Session
        Surface(
            color = GoldSecondary.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = GoldDark, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "وحدة الإشراف المعزولة: جميع العمليات تخضع للتدقيق الرقمي ومسجلة في السجل الأمني",
                    fontSize = 11.sp,
                    color = GoldDark,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = EmeraldPrimary,
            edgePadding = 12.dp
        ) {
            val pendingTopUps = allTopUpRequests.count { it.status == "PENDING" }
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("شحن الأرصدة والوصولات ($pendingTopUps)", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("الحساب الرسمي (BaridiMob/CCP)", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("مراجعة الإعلانات (${adminListings.count { it.status == "UNDER_REVIEW" }})", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("إعدادات الأسعار", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 4,
                onClick = { selectedTab = 4 },
                text = { Text("المستخدمون (${allUsers.size})", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 5,
                onClick = { selectedTab = 5 },
                text = { Text("المدفوعات (${allPayments.size})", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 6,
                onClick = { selectedTab = 6 },
                text = { Text("البلاغات (${allReports.size})", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 7,
                onClick = { selectedTab = 7 },
                text = { Text("سجل التدقيق الأمني (${adminAuditLogs.size})", fontSize = 12.sp) }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // TAB 0: TOP-UP REQUESTS & RECEIPTS (DIRECT الخدمة السحابية STREAM)
            if (selectedTab == 0) {
                when (val state = topUpListUiState) {
                    is MarketplaceViewModel.TopUpListUiState.Loading -> {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = EmeraldPrimary,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "جاري مزامنة واسترجاع طلبات الشحن من الخدمة السحابية...",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    is MarketplaceViewModel.TopUpListUiState.Error -> {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = UrgentRed.copy(alpha = 0.1f)),
                                border = BorderStroke(1.dp, UrgentRed.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = UrgentRed, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("خطأ في الاتصال أو الصلاحيات بـ الخدمة السحابية", fontWeight = FontWeight.Bold, color = UrgentRed, fontSize = 13.sp)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = state.message,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "ملاحظة: إذا ظهر خطأ PERMISSION_DENIED، تأكد من تسجيل دخول المشرف بحساب يحمل صلاحية admin == true في الخدمة السحابية.",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    is MarketplaceViewModel.TopUpListUiState.Success -> {
                        val requests = state.requests
                        val pendingCount = requests.count { it.status == "PENDING" }
                        val approvedCount = requests.count { it.status == "APPROVED" }
                        val rejectedCount = requests.count { it.status == "REJECTED" }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedTopUpFilter == "PENDING",
                                    onClick = { selectedTopUpFilter = "PENDING" },
                                    label = { Text("قيد الانتظار ($pendingCount)") }
                                )
                                FilterChip(
                                    selected = selectedTopUpFilter == "APPROVED",
                                    onClick = { selectedTopUpFilter = "APPROVED" },
                                    label = { Text("المقبولة ($approvedCount)") }
                                )
                                FilterChip(
                                    selected = selectedTopUpFilter == "REJECTED",
                                    onClick = { selectedTopUpFilter = "REJECTED" },
                                    label = { Text("المرفوضة ($rejectedCount)") }
                                )
                                FilterChip(
                                    selected = selectedTopUpFilter == "ALL",
                                    onClick = { selectedTopUpFilter = "ALL" },
                                    label = { Text("الكل (${requests.size})") }
                                )
                            }
                        }

                        val filteredRequests = when (selectedTopUpFilter) {
                            "PENDING" -> requests.filter { it.status == "PENDING" }
                            "APPROVED" -> requests.filter { it.status == "APPROVED" }
                            "REJECTED" -> requests.filter { it.status == "REJECTED" }
                            else -> requests
                        }

                        if (filteredRequests.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = if (selectedTopUpFilter == "PENDING") "لا توجد طلبات شحن معلقة حالياً في الخدمة السحابية ✓" else "لا توجد طلبات في هذا القسم.",
                                        modifier = Modifier.padding(16.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(filteredRequests, key = { it.id }) { req ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(
                                        1.dp,
                                        when (req.status) {
                                            "PENDING" -> GoldSecondary.copy(alpha = 0.5f)
                                            "APPROVED" -> EmeraldPrimary.copy(alpha = 0.4f)
                                            else -> UrgentRed.copy(alpha = 0.3f)
                                        }
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(req.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = req.userPhone.ifBlank { "بدون هاتف" },
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "+ ${req.amountDzd} دج",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 16.sp,
                                                    color = if (req.status == "APPROVED") EmeraldPrimary else GoldSecondary
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = when (req.status) {
                                                        "PENDING" -> GoldSecondary.copy(alpha = 0.15f)
                                                        "APPROVED" -> EmeraldPrimary.copy(alpha = 0.15f)
                                                        else -> UrgentRed.copy(alpha = 0.15f)
                                                    }
                                                ) {
                                                    Text(
                                                        text = when (req.status) {
                                                            "PENDING" -> "قيد المراجعة ⏳"
                                                            "APPROVED" -> "تم الشحن ✓"
                                                            else -> "مرفوض ✕"
                                                        },
                                                        color = when (req.status) {
                                                            "PENDING" -> GoldSecondary
                                                            "APPROVED" -> EmeraldPrimary
                                                            else -> UrgentRed
                                                        },
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("الوسيلة: ${req.provider}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(formatTimeAgo(req.createdAt), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        if (req.reference.isNotBlank()) {
                                            Text("رقم المرجع: ${req.reference}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = EmeraldDark)
                                        }

                                        if (req.adminNote.isNotBlank()) {
                                            Text("ملاحظة المشرف: ${req.adminNote}", fontSize = 11.sp, color = if (req.status == "REJECTED") UrgentRed else EmeraldPrimary)
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        if (req.receiptImageUri.isNotBlank()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                    .clickable {
                                                        viewModel.resolveReceiptUrl(req.receiptImageUri) { resolved ->
                                                            previewReceiptUrl = resolved
                                                        }
                                                    }
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                AsyncImage(
                                                    model = req.receiptImageUri,
                                                    contentDescription = "صورة الوصل",
                                                    modifier = Modifier
                                                        .size(54.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color.Black.copy(alpha = 0.08f)),
                                                    contentScale = ContentScale.Crop
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("وصل الدفع مرفق ✓", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EmeraldDark)
                                                    Text("اضغط هنا لتكبير ومعاينة الوصل بدقة عالية", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Icon(Icons.Default.Visibility, contentDescription = "معاينة", tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                                            }
                                        } else {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("لم يُرفق العميل صورة وصل (اعتمد على رقم المرجع أعلاه)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (req.receiptImageUri.isNotBlank()) {
                                                OutlinedButton(
                                                    onClick = {
                                                        viewModel.resolveReceiptUrl(req.receiptImageUri) { resolved ->
                                                            previewReceiptUrl = resolved
                                                        }
                                                    },
                                                    modifier = Modifier.height(36.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                                ) {
                                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("معاينة الوصل", fontSize = 11.sp)
                                                }
                                            }

                                            if (req.status == "PENDING") {
                                                val isOperating = isProcessingTopUp == req.id
                                                val isAnyOperating = isProcessingTopUp != null

                                                Button(
                                                    onClick = { viewModel.approveTopUpRequest(req.id) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                                    modifier = Modifier.weight(1f).height(36.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                                    enabled = !isAnyOperating
                                                ) {
                                                    if (isOperating) {
                                                        CircularProgressIndicator(
                                                            color = MaterialTheme.colorScheme.onPrimary,
                                                            modifier = Modifier.size(16.dp),
                                                            strokeWidth = 2.dp
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("جاري الاعتماد...", fontSize = 11.sp)
                                                    } else {
                                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("قبول وشحن", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        rejectTopUpTarget = req
                                                        rejectTopUpReason = ""
                                                    },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = UrgentRed),
                                                    modifier = Modifier.height(36.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                                    enabled = !isAnyOperating
                                                ) {
                                                    Text("رفض", fontSize = 11.sp, color = UrgentRed)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TAB 1: OFFICIAL ACCOUNT MANAGEMENT (Material 3)
            if (selectedTab == 1) {
                item {
                    // Card 1: Live Customer View Preview
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = EmeraldPrimary.copy(alpha = 0.08f)
                        ),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "المعاينة الحالية لدى الزبائن",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = EmeraldPrimary
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = GoldSecondary
                                ) {
                                    Text(
                                        text = platformSettings.officialProviderName,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = platformSettings.officialAccountHolder,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("رقم الحساب الجاري (RIP):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = platformSettings.officialRip,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("المفتاح (Clé):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = platformSettings.officialKey,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = EmeraldPrimary
                                    )
                                }
                            }

                            if (platformSettings.officialInstructions.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "التعليمات: ${platformSettings.officialInstructions}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(platformSettings.officialRip))
                                    testCopySuccess = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    if (testCopySuccess) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (testCopySuccess) "تم نسخ رقم الحساب بنجاح ✓" else "تجربة نسخ الحساب (RIP)",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                item {
                    // Card 2: Edit Form (Material Design 3)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تعديل بيانات الحساب الرسمي للشحن",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            Text(
                                text = "تعديل هذا الحساب يغيّر فورياً بيانات الشحن الظاهرة في محافظ جميع المستخدمين.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                            )

                            // RIP Field
                            OutlinedTextField(
                                value = ripInput,
                                onValueChange = { ripInput = it },
                                label = { Text("رقم الحساب الجاري (RIP)") },
                                placeholder = { Text("مثال: 007999990008761821") },
                                leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Key Field
                            OutlinedTextField(
                                value = keyInput,
                                onValueChange = { keyInput = it },
                                label = { Text("المفتاح (Clé)") },
                                placeholder = { Text("مثال: 94") },
                                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Account Holder
                            OutlinedTextField(
                                value = holderInput,
                                onValueChange = { holderInput = it },
                                label = { Text("اسم صاحب الحساب أو الهيئة") },
                                placeholder = { Text("مثال: OcaVenteDz - الحساب المعتمد") },
                                leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Provider Name
                            OutlinedTextField(
                                value = providerInput,
                                onValueChange = { providerInput = it },
                                label = { Text("اسم وسيلة الدفع / البنك") },
                                placeholder = { Text("مثال: بريدي موب / CCP") },
                                leadingIcon = { Icon(Icons.Default.Payment, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Instructions Field
                            OutlinedTextField(
                                value = instructionsInput,
                                onValueChange = { instructionsInput = it },
                                label = { Text("تعليمات الدفع وإرشادات الوصل") },
                                placeholder = { Text("تعليمات تظهر للمستخدم عند فتح نافذة الشحن...") },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.adminUpdateOfficialAccount(
                                            rip = ripInput,
                                            key = keyInput,
                                            holder = holderInput,
                                            provider = providerInput,
                                            instructions = instructionsInput
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("حفظ وتطبيق فوراً", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        ripInput = "007999990008761821"
                                        keyInput = "94"
                                        holderInput = "OcaVenteDz - الحساب المعتمد"
                                        providerInput = "بريدي موب / CCP"
                                        instructionsInput = "يرجى تحويل المبلغ بدقة، ثم أخذ لقطة شاشة للوصل وإرفاقها مع كتابة رقم العملية."
                                    },
                                    modifier = Modifier.height(44.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("استعادة الافتراضي", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                item {
                    // PIN Management Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, GoldSecondary.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = GoldSecondary.copy(alpha = 0.15f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Key, contentDescription = null, tint = GoldDark, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("كلمة مرور الإشراف السحابية", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("محميّة عبر Supabase Auth ويمكن تحديثها من داخل التطبيق", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("كلمة المرور الحالية فعّالة للحساب السحابي", fontSize = 12.sp, color = EmeraldDark, fontWeight = FontWeight.SemiBold)
                                    Text("تتم إعادة التحقق منها قبل حفظ كلمة المرور الجديدة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Button(
                                    onClick = { showChangePinDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldSecondary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تغيير كلمة المرور", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                item {
                    // Card 3: Security & Operational Advice Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "إرشادات الأمان والتحقق المالي (BaridiMob)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = EmeraldDark
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "• تأكد دائمًا من مطابقة رقم العملية في الوصل مع إشعارات تطبيق بريدي موب قبل الضغط على 'قبول وشحن'.\n" +
                                       "• يمكنك تغيير الحساب البنكي أو البريدي في أي وقت وسيتم تحديثه في ثوانٍ لدى كافة المستخدمين.\n" +
                                       "• لا تقم بقبول أي وصل مستعمل مسبقاً أو غير واضح الختم والتاريخ.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // TAB 2: MODERATION & ADS
            if (selectedTab == 2) {
                val underReview = adminListings.filter { it.status == "UNDER_REVIEW" }
                val otherAds = adminListings.filter { it.status != "UNDER_REVIEW" }

                item {
                    Text(
                        text = "الإعلانات المدفوعة بانتظار المراجعة (${underReview.size}):",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (underReview.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "لا توجد إعلانات معلقة بانتظار المراجعة حالياً ✓",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(underReview) { ad ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StatusBadge(ad.status)
                                    Text(formatDzd(ad.priceDzd), color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                val firstImage = remember(ad.imagesJson) {
                                    ad.imagesJson.split(",").map { it.trim() }.firstOrNull { it.isNotBlank() }
                                }
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    if (firstImage != null) {
                                        AsyncImage(
                                            model = firstImage,
                                            contentDescription = "صورة الإعلان",
                                            modifier = Modifier
                                                .size(68.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.Black.copy(alpha = 0.05f)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ad.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("المعلن: ${ad.userName} • ${ad.wilayaName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(ad.description, fontSize = 11.sp, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.adminApproveListing(ad.id) },
                                        modifier = Modifier.weight(1f).height(38.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("قبول ونشر")
                                    }
                                    OutlinedButton(
                                        onClick = { rejectingAdId = ad.id },
                                        modifier = Modifier.weight(1f).height(38.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = UrgentRed)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("رفض مع سبب")
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "باقي الإعلانات في المنصة (${otherAds.size}):",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                items(otherAds) { ad ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StatusBadge(ad.status)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(ad.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                }
                                Text("بواسطة ${ad.userName} • ${formatDzd(ad.priceDzd)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { viewModel.adminDeleteListing(ad.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = UrgentRed)
                            }
                        }
                    }
                }
            }

            // TAB 3: PRICING SETTINGS (Change fees without app update)
            if (selectedTab == 3) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "تعديل رسوم النشر والخدمات (بدون تحديث التطبيق)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "يتم حفظ هذه القيم مباشرة في قاعدة البيانات وتطبيقها فوراً على جميع المستخدمين.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = standardFeeText,
                                onValueChange = { standardFeeText = it },
                                label = { Text("سعر نشر الإعلان العادي (دج)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = featuredFeeText,
                                onValueChange = { featuredFeeText = it },
                                label = { Text("سعر باقة الإعلان المميز (دج)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = urgentFeeText,
                                onValueChange = { urgentFeeText = it },
                                label = { Text("سعر باقة الإعلان العاجل (دج)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = durationDaysText,
                                onValueChange = { durationDaysText = it },
                                label = { Text("مدة بقاء الإعلان بالأيام") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("النشر الآلي فور نجاح الدفع", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("إذا تم تعطيله ينتقل الإعلان لقيد المراجعة أولاً", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = autoPublishState,
                                    onCheckedChange = { autoPublishState = it }
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = {
                                    viewModel.adminUpdateSettings(
                                        standardFee = standardFeeText.toIntOrNull() ?: platformSettings.standardAdFeeDzd,
                                        featuredFee = featuredFeeText.toIntOrNull() ?: platformSettings.featuredAdFeeDzd,
                                        urgentFee = urgentFeeText.toIntOrNull() ?: platformSettings.urgentAdFeeDzd,
                                        durationDays = durationDaysText.toIntOrNull() ?: platformSettings.adDurationDays,
                                        autoPublish = autoPublishState
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("حفظ وتحديث الأسعار", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // TAB 4: USERS MANAGEMENT
            if (selectedTab == 4) {
                item {
                    OutlinedTextField(
                        value = userSearchQuery,
                        onValueChange = { userSearchQuery = it },
                        placeholder = { Text("بحث عن مستخدم بالاسم أو الهاتف...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                val filteredUsers = allUsers.filter {
                    userSearchQuery.isBlank() ||
                    it.name.contains(userSearchQuery, ignoreCase = true) ||
                    it.phone.contains(userSearchQuery)
                }

                items(filteredUsers) { user ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(user.name, fontWeight = FontWeight.Bold)
                                    if (user.isVerified) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.Verified, contentDescription = null, tint = VerifiedBlue, modifier = Modifier.size(16.dp))
                                    }
                                    if (user.isBanned) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(color = UrgentRed, shape = RoundedCornerShape(4.dp)) {
                                            Text("محظور", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                    }
                                }

                                Text(user.role, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Text("${user.phone} • ${user.wilaya}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.adminToggleVerification(user.id, user.isVerified) },
                                    modifier = Modifier.weight(1f).height(34.dp)
                                ) {
                                    Text(if (user.isVerified) "إلغاء التوثيق" else "توثيق الحساب", fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.adminToggleUserBan(user.id, user.isBanned) },
                                    modifier = Modifier.weight(1f).height(34.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (user.isBanned) EmeraldPrimary else UrgentRed)
                                ) {
                                    Text(if (user.isBanned) "فك الحظر" else "تعطيل الحساب", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // TAB 5: PAYMENTS LOG
            if (selectedTab == 5) {
                item {
                    val totalRevenue = allPayments.sumOf { it.amount }
                    Surface(
                        color = EmeraldDark,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("إجمالي إيرادات النشر:", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            Text("$totalRevenue دج", color = GoldLight, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                            Text("إجمالي المعاملات الناجحة: ${allPayments.size}", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                items(allPayments) { pay ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(pay.transactionReference, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("بواسطة ${pay.provider} • ${formatTimeAgo(pay.createdAt)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("+ ${pay.amount} دج", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }

            // TAB 6: ANALYTICS & FRAUD REPORTS
            if (selectedTab == 6) {
                item {
                    val publishedCount = adminListings.count { it.status == "PUBLISHED" }
                    val soldCount = adminListings.count { it.status == "SOLD" }
                    val conversionRate = if (adminListings.isNotEmpty()) (publishedCount * 100 / adminListings.size) else 0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("إجمالي الإعلانات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${adminListings.size}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = EmeraldPrimary)
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("المنشورة للعامة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$publishedCount", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = EmeraldPrimary)
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("تم بيعها", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$soldCount", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GoldSecondary)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("بلاغات الاحتيال والمخالفات (${allReports.size}):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }

                if (allReports.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Text("لا توجد بلاغات نشطة من المستخدمين ✓", modifier = Modifier.padding(14.dp))
                        }
                    }
                } else {
                    items(allReports) { rep ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("نوع البلاغ: ${rep.reason}", fontWeight = FontWeight.Bold, color = UrgentRed, fontSize = 13.sp)
                                    Text(formatTimeAgo(rep.timestamp), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("تفاصيل: ${rep.comment}", fontSize = 12.sp)
                                Text("رقم الإعلان: ${rep.reportedListingId}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // TAB 7: AUDIT LOGS & PLATFORM SECURITY
            if (selectedTab == 7) {
                item {
                    Text(
                        text = "سجل التدقيق الأمني والعمليات (${adminAuditLogs.size}):",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "سجل غير قابل للتعديل يوثق جميع قرارات الإشراف وتغييرات النظام مع التوقيت الدقيق.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (adminAuditLogs.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Text("لا توجد سجلات أمنية مسجلة حالياً.", modifier = Modifier.padding(14.dp))
                        }
                    }
                } else {
                    items(adminAuditLogs) { log ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (log.action.contains("حظر") || log.action.contains("مشبوهة") || log.action.contains("رفض")) UrgentRed.copy(alpha = 0.12f) else EmeraldPrimary.copy(alpha = 0.12f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = null,
                                            tint = if (log.action.contains("حظر") || log.action.contains("مشبوهة") || log.action.contains("رفض")) UrgentRed else EmeraldPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = log.action,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = formatTimeAgo(log.timestamp),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = log.details,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
