package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BalanceTransaction
import com.example.model.RechargeRequest
import com.example.viewmodel.MainViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val rechargeRequests by viewModel.rechargeRequests.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    var showRechargeDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Recharges, 1: Transactions

    val formatter = NumberFormat.getNumberInstance(Locale.FRANCE)
    val balanceStr = formatter.format(currentUser?.balance ?: 0.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("المحفظة والرصيد", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Balance Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF008751),
                                        Color(0xFF0F172A)
                                    )
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "رصيدك الحالي",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 14.sp
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "دينار جزائري DZD",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "$balanceStr د.ج",
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // Recharge Action Button
                            Button(
                                onClick = { showRechargeDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF59E0B),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.AddCard, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("طلب شحن الرصيد الآن", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }

            // Tabs for Recharges / Transactions
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("طلبات الشحن (${rechargeRequests.size})", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("سجل العمليات", fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            // Tab 0: Recharge Requests
            if (selectedTab == 0) {
                if (rechargeRequests.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("لا توجد طلبات شحن سابقة", color = Color.Gray)
                        }
                    }
                } else {
                    items(rechargeRequests) { req ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${formatter.format(req.amount)} د.ج",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    // Status Badge
                                    val (badgeBg, badgeText, badgeFg) = when (req.status) {
                                        "APPROVED" -> Triple(Color(0xFFE8F5E9), "مقبول ومضاف للرصيد", Color(0xFF008751))
                                        "REJECTED" -> Triple(Color(0xFFFFEBEE), "مرفوض", Color(0xFFD32F2F))
                                        else -> Triple(Color(0xFFFFF8E1), "قيد المراجعة بالادارة", Color(0xFFD97706))
                                    }
                                    Surface(
                                        color = badgeBg,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            color = badgeFg,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "طريقة الدفع: ${req.paymentMethod} • رقم الوصل: ${req.receiptNumber}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )

                                req.adminNotes?.let { note ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "ملاحظة الإدارة: $note",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Tab 1: Balance Transactions
                if (transactions.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("لا توجد حركات رصيد بعد", color = Color.Gray)
                        }
                    }
                } else {
                    items(transactions) { tx ->
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
                                    Text(
                                        text = tx.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = tx.createdAt,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                                val isCredit = tx.amount > 0
                                Text(
                                    text = (if (isCredit) "+" else "") + "${formatter.format(tx.amount)} د.ج",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = if (isCredit) Color(0xFF008751) else Color(0xFFD32F2F)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Submit Recharge Dialog
    if (showRechargeDialog) {
        var amountText by remember { mutableStateOf("2000") }
        var selectedMethod by remember { mutableStateOf("BARIDIMOB") }
        var receiptNumber by remember { mutableStateOf("") }
        var receiptUrl by remember { mutableStateOf("https://example.com/receipt.jpg") }

        AlertDialog(
            onDismissRequest = { showRechargeDialog = false },
            title = { Text("طلب شحن الرصيد", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val clipboardManager = LocalClipboardManager.current
                    val context = LocalContext.current

                    // Payment Method Guide
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "بيانات الحساب البريدي المعتمد للشحن:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // BaridiMob RIP Card
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("بريدي موب BaridiMob (RIP):", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Text("00799999000876182194", fontSize = 13.sp, fontWeight = FontWeight.Black)
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString("00799999000876182194"))
                                            Toast.makeText(context, "تم نسخ رقم RIP بنجاح", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ RIP", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }

                            // CCP Card
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("الحساب البريدي الجاري CCP:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Text("0008761821 مفتاح 94", fontSize = 13.sp, fontWeight = FontWeight.Black)
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString("0008761821 94"))
                                            Toast.makeText(context, "تم نسخ رقم CCP والمفتاح بنجاح", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ CCP", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }

                    // Select Method
                    Text("اختر وسيلة الدفع:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("BARIDIMOB" to "بريدي موب", "CCP" to "CCP").forEach { (methodKey, label) ->
                            FilterChip(
                                selected = selectedMethod == methodKey,
                                onClick = { selectedMethod = methodKey },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Amount
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("المبلغ (د.ج) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Receipt Number
                    OutlinedTextField(
                        value = receiptNumber,
                        onValueChange = { receiptNumber = it },
                        label = { Text("رقم العملية / وصل التحويل *") },
                        placeholder = { Text("مثال: 00489281") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        if (amount > 0 && receiptNumber.isNotBlank()) {
                            viewModel.submitRecharge(
                                amount = amount,
                                method = selectedMethod,
                                receiptNumber = receiptNumber,
                                receiptUrl = receiptUrl
                            )
                            showRechargeDialog = false
                        }
                    },
                    enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0 && receiptNumber.isNotBlank()
                ) {
                    Text("إرسال للمراجعة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRechargeDialog = false }) { Text("إلغاء") }
            }
        )
    }
}
