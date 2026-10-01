package com.example.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.admin.model.*
import com.example.admin.ui.OcaventeAdminTheme
import java.text.NumberFormat
import java.util.Locale

enum class AdminTab(val title: String, val icon: ImageVector) {
    DASHBOARD("الرئيسية", Icons.Filled.Dashboard),
    LISTINGS("مراجعة الإعلانات", Icons.Filled.FactCheck),
    RECHARGES("طلبات الشحن", Icons.Filled.CurrencyExchange),
    USERS("المستخدمين", Icons.Filled.People),
    AUDIT("سجل العمليات", Icons.Filled.HistoryEdu)
}

class AdminMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OcaventeAdminTheme {
                AdminApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminApp() {
    var selectedTab by remember { mutableStateOf(AdminTab.DASHBOARD) }

    // Admin state
    var listings by remember {
        mutableStateOf(
            listOf(
                AdminListing(
                    id = "adm-l-1",
                    title = "Peugeot 208 GT Line 2022 نقية ماشية 45 ألف",
                    description = "سيارة بيجو 208 جي تي لاين، سنة 2022، حالة ممتازة صبيغة نقية، مفحوصة سكنار كامل.",
                    price = 3200000.0,
                    categoryName = "مركبات وسيارات",
                    sellerUsername = "ahmed_auto",
                    sellerPhone = "0550112233",
                    wilaya = "16 - الجزائر (Alger)",
                    status = "PENDING",
                    imageUrl = "https://images.unsplash.com/photo-1541899481282-d53bffe3c35d?w=800"
                ),
                AdminListing(
                    id = "adm-l-2",
                    title = "شقة F3 مفروشة للكراء بالبليدة",
                    description = "شقة F3 في حي راقي قريبة من الترامواي ومحطة الحافلات، متوفر غاز وماء 24/24.",
                    price = 45000.0,
                    categoryName = "عقارات وأراضي",
                    sellerUsername = "samir_blida",
                    sellerPhone = "0661223344",
                    wilaya = "09 - البليدة (Blida)",
                    status = "PENDING",
                    imageUrl = "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=800"
                ),
                AdminListing(
                    id = "adm-l-3",
                    title = "MacBook Pro M2 16GB 512GB جديد",
                    description = "ماك بوك برو إم 2 بطارية 100% دورات شحن 12 فقط، معه الشاحن الأصلي والكرتونة.",
                    price = 210000.0,
                    categoryName = "هواتف وتكنولوجيا",
                    sellerUsername = "tech_dz",
                    sellerPhone = "0770998877",
                    wilaya = "31 - وهران (Oran)",
                    status = "PENDING",
                    imageUrl = "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800"
                )
            )
        )
    }

    var rechargeRequests by remember {
        mutableStateOf(
            listOf(
                AdminRechargeRequest(
                    id = "rec-adm-1",
                    userId = "user-101",
                    username = "karim_oran",
                    userPhone = "0770123456",
                    currentBalance = 1500.0,
                    amount = 5000.0,
                    paymentMethod = "BARIDIMOB",
                    receiptNumber = "BRD-9988771122",
                    status = "PENDING"
                ),
                AdminRechargeRequest(
                    id = "rec-adm-2",
                    userId = "user-102",
                    username = "sofiane_alger",
                    userPhone = "0555987654",
                    currentBalance = 0.0,
                    amount = 3000.0,
                    paymentMethod = "CCP",
                    receiptNumber = "CCP-77334455",
                    status = "PENDING"
                )
            )
        )
    }

    var users by remember {
        mutableStateOf(
            listOf(
                AdminUser("u-1", "karim@gmail.com", "karim_oran", "0770123456", "31 - وهران", "USER", 1500.0, false, "2026-09-01"),
                AdminUser("u-2", "sofiane@yahoo.fr", "sofiane_alger", "0555987654", "16 - الجزائر", "USER", 0.0, false, "2026-09-10"),
                AdminUser("u-3", "admin@ocavente.dz", "superadmin", "0550123456", "16 - الجزائر", "SUPER_ADMIN", 50000.0, false, "2026-08-01")
            )
        )
    }

    var auditLogs by remember {
        mutableStateOf(
            listOf(
                AdminAuditLog("log-1", "superadmin", "ADMIN_LOGIN", "AUTH", "تسجيل دخول المشرف العام بنجاح", "اليوم 08:30"),
                AdminAuditLog("log-2", "superadmin", "RECHARGE_APPROVED", "RECHARGE", "الموافقة على شحن 2000 د.ج للمستخدم karim_dz", "أمس 14:15")
            )
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            shadowElevation = 2.dp,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.app_logo),
                                contentDescription = "Logo",
                                modifier = Modifier.fillMaxSize().padding(2.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("OcaVenteDz", fontWeight = FontWeight.Black, fontSize = 17.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFF59E0B)) {
                                    Text("Admin", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text("لوحة إدارة المنصة والعمليات • مستقل تماماً", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF008751).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "مشرف نشط",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color(0xFF008751),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                AdminTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, fontSize = 10.sp) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                AdminTab.DASHBOARD -> AdminDashboardView(
                    stats = AdminDashboardStats(
                        totalUsers = users.size,
                        totalActiveListings = 142,
                        pendingListingsCount = listings.count { it.status == "PENDING" },
                        pendingRechargeCount = rechargeRequests.count { it.status == "PENDING" },
                        totalRechargeVolume = 485000.0
                    ),
                    onNavigateTo = { selectedTab = it }
                )
                AdminTab.LISTINGS -> AdminListingsView(
                    listings = listings,
                    onApprove = { listing ->
                        listings = listings.map { if (it.id == listing.id) it.copy(status = "ACTIVE") else it }
                        auditLogs = listOf(
                            AdminAuditLog("log-${System.currentTimeMillis()}", "superadmin", "LISTING_APPROVE", "LISTING", "قبول إعلان: ${listing.title}", "الآن")
                        ) + auditLogs
                        snackbarMessage = "تمت الموافقة على الإعلان بنجاح وتفعيله للمستخدمين"
                    },
                    onReject = { listing, reason ->
                        listings = listings.map { if (it.id == listing.id) it.copy(status = "REJECTED", rejectionReason = reason) else it }
                        auditLogs = listOf(
                            AdminAuditLog("log-${System.currentTimeMillis()}", "superadmin", "LISTING_REJECT", "LISTING", "رفض إعلان: ${listing.title} - السبب: $reason", "الآن")
                        ) + auditLogs
                        snackbarMessage = "تم رفض الإعلان وإشعار صاحبه بالسبب"
                    }
                )
                AdminTab.RECHARGES -> AdminRechargesView(
                    requests = rechargeRequests,
                    onApprove = { req ->
                        // Transactional simulation: update request, increment user balance
                        rechargeRequests = rechargeRequests.map { if (it.id == req.id) it.copy(status = "APPROVED") else it }
                        users = users.map { if (it.id == req.userId) it.copy(balance = it.balance + req.amount) else it }
                        auditLogs = listOf(
                            AdminAuditLog("log-${System.currentTimeMillis()}", "superadmin", "RECHARGE_APPROVED", "RECHARGE", "إضافة ${req.amount} د.ج إلى رصيد ${req.username} عبر ${req.paymentMethod}", "الآن")
                        ) + auditLogs
                        snackbarMessage = "تم التحقق وإضافة ${req.amount} د.ج إلى رصيد المستخدم بنجاح"
                    },
                    onReject = { req, reason ->
                        rechargeRequests = rechargeRequests.map { if (it.id == req.id) it.copy(status = "REJECTED", adminNotes = reason) else it }
                        auditLogs = listOf(
                            AdminAuditLog("log-${System.currentTimeMillis()}", "superadmin", "RECHARGE_REJECTED", "RECHARGE", "رفض طلب شحن ${req.amount} د.ج لـ ${req.username} - السبب: $reason", "الآن")
                        ) + auditLogs
                        snackbarMessage = "تم رفض طلب الشحن وتدوين الملاحظة"
                    }
                )
                AdminTab.USERS -> AdminUsersView(
                    users = users,
                    onToggleBlock = { user ->
                        users = users.map { if (it.id == user.id) it.copy(isBlocked = !it.isBlocked) else it }
                        val action = if (!user.isBlocked) "حظر" else "إلغاء حظر"
                        auditLogs = listOf(
                            AdminAuditLog("log-${System.currentTimeMillis()}", "superadmin", "USER_STATUS_CHANGE", "USER", "$action المستخدم: ${user.username}", "الآن")
                        ) + auditLogs
                        snackbarMessage = "تم $action المستخدم ${user.username}"
                    }
                )
                AdminTab.AUDIT -> AdminAuditLogsView(auditLogs = auditLogs)
            }
        }
    }
}

@Composable
fun AdminDashboardView(
    stats: AdminDashboardStats,
    onNavigateTo: (AdminTab) -> Unit
) {
    val formatter = NumberFormat.getNumberInstance(Locale.FRANCE)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("مؤشرات الأداء الحالية (Live KPI)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminStatCard(
                    title = "إعلانات قيد المراجعة",
                    value = "${stats.pendingListingsCount}",
                    color = Color(0xFFF59E0B),
                    icon = Icons.Default.PendingActions,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo(AdminTab.LISTINGS) }
                )
                AdminStatCard(
                    title = "طلبات شحن معلقة",
                    value = "${stats.pendingRechargeCount}",
                    color = Color(0xFFD97706),
                    icon = Icons.Default.CurrencyExchange,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo(AdminTab.RECHARGES) }
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminStatCard(
                    title = "إجمالي المستخدمين",
                    value = "${stats.totalUsers}",
                    color = Color(0xFF0284C7),
                    icon = Icons.Default.People,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo(AdminTab.USERS) }
                )
                AdminStatCard(
                    title = "إعلانات نشطة بالسوق",
                    value = "${stats.totalActiveListings}",
                    color = Color(0xFF008751),
                    icon = Icons.Default.ShoppingBag,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo(AdminTab.LISTINGS) }
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("حجم عمليات الشحن المعتمدة", color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "${formatter.format(stats.totalRechargeVolume)} د.ج",
                        fontWeight = FontWeight.Black,
                        fontSize = 26.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("جميع المعاملات تتم عبر معاملات قاعدة بيانات PostgreSQL الذرية لمنع التكرار.", fontSize = 12.sp, color = Color(0xFF008751))
                }
            }
        }
    }
}

@Composable
fun AdminStatCard(
    title: String,
    value: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(shape = CircleShape, color = color.copy(alpha = 0.15f), modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(2.dp))
            Text(title, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun AdminListingsView(
    listings: List<AdminListing>,
    onApprove: (AdminListing) -> Unit,
    onReject: (AdminListing, String) -> Unit
) {
    var rejectDialogItem by remember { mutableStateOf<AdminListing?>(null) }
    var rejectionReason by remember { mutableStateOf("مخالف لشروط النشر والتصنيف") }

    val formatter = NumberFormat.getNumberInstance(Locale.FRANCE)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("مراجعة وقبول الإعلانات (${listings.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }

        items(listings) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (item.status == "PENDING") Color(0xFFFEF3C7) else if (item.status == "ACTIVE") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        ) {
                            Text(
                                text = if (item.status == "PENDING") "قيد المراجعة" else if (item.status == "ACTIVE") "مقبول ونشط" else "مرفوض",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (item.status == "PENDING") Color(0xFFD97706) else if (item.status == "ACTIVE") Color(0xFF008751) else Color(0xFFD32F2F)
                            )
                        }
                        Text(item.categoryName, fontSize = 12.sp, color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${formatter.format(item.price)} د.ج", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.secondary)

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(item.description, fontSize = 13.sp, color = Color.Gray, maxLines = 2)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("صاحب الإعلان: ${item.sellerUsername} • ${item.sellerPhone} • ${item.wilaya}", fontSize = 12.sp, color = Color.DarkGray)

                    if (item.status == "PENDING") {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onApprove(item) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008751))
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("قبول ونشر")
                            }

                            OutlinedButton(
                                onClick = { rejectDialogItem = item },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("رفض الإعلان")
                            }
                        }
                    }
                }
            }
        }
    }

    if (rejectDialogItem != null) {
        AlertDialog(
            onDismissRequest = { rejectDialogItem = null },
            title = { Text("رفض الإعلان") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("حدد سبب الرفض لإشعار المستخدم:")
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReject(rejectDialogItem!!, rejectionReason)
                        rejectDialogItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("تأكيد الرفض")
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectDialogItem = null }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun AdminRechargesView(
    requests: List<AdminRechargeRequest>,
    onApprove: (AdminRechargeRequest) -> Unit,
    onReject: (AdminRechargeRequest, String) -> Unit
) {
    val formatter = NumberFormat.getNumberInstance(Locale.FRANCE)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("مراجعة طلبات شحن الرصيد (${requests.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("الحساب البريدي المعتمد للاستقبال:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        Text("• CCP: 0008761821 مفتاح 94", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("• BaridiMob RIP: 00799999000876182194", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        items(requests) { req ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${formatter.format(req.amount)} د.ج",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = Color(0xFF008751)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (req.status == "PENDING") Color(0xFFFEF3C7) else if (req.status == "APPROVED") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        ) {
                            Text(
                                text = req.status,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (req.status == "PENDING") Color(0xFFD97706) else if (req.status == "APPROVED") Color(0xFF008751) else Color(0xFFD32F2F)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("وسيلة التحويل: ${req.paymentMethod}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("رقم العملية / الوصل: ${req.receiptNumber}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("المستخدم: ${req.username} (${req.userPhone})", fontSize = 13.sp, color = Color.Gray)
                    Text("الرصيد الحالي للمستخدم: ${formatter.format(req.currentBalance)} د.ج", fontSize = 12.sp, color = Color.Gray)

                    if (req.status == "PENDING") {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onApprove(req) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008751))
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تأكيد وإضافة الرصيد")
                            }

                            OutlinedButton(
                                onClick = { onReject(req, "رقم الوصل غير صحيح أو غير متطابق") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("رفض الوصل")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminUsersView(
    users: List<AdminUser>,
    onToggleBlock: (AdminUser) -> Unit
) {
    val formatter = NumberFormat.getNumberInstance(Locale.FRANCE)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("إدارة المستخدمين (${users.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }

        items(users) { u ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(u.username, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = u.role,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Text(u.email, fontSize = 12.sp, color = Color.Gray)
                        Text("الولاية: ${u.wilaya} • الرصيد: ${formatter.format(u.balance)} د.ج", fontSize = 12.sp, color = Color.DarkGray)
                    }

                    if (u.role != "SUPER_ADMIN") {
                        Button(
                            onClick = { onToggleBlock(u) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (u.isBlocked) Color(0xFF008751) else Color(0xFFD32F2F)
                            )
                        ) {
                            Text(if (u.isBlocked) "إلغاء الحظر" else "حظر")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAuditLogsView(auditLogs: List<AdminAuditLog>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("سجل الرقابة والعمليات الإدارية (Audit Logs)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }

        items(auditLogs) { log ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text(log.action, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(log.timestamp, fontSize = 11.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(log.details, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("المشرف: ${log.adminUsername}", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}
