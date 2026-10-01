package com.example.ui.screens.wallet

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.*
import com.example.ui.model.RechargeRequest
import com.example.ui.model.WalletTransaction
import com.example.ui.theme.OcaAmberTertiary
import com.example.ui.theme.OcaGreenDark
import com.example.ui.theme.OcaGreenPrimary
import com.example.ui.theme.OcaNavySecondary

const val OFFICIAL_CCP_ACCOUNT = "007999990008761821"
const val OFFICIAL_CCP_KEY = "94"
const val OFFICIAL_BENEFICIARY = "راهم محمد لمين (إدارة OcaVenteDz)"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    balance: Double,
    transactions: List<WalletTransaction>,
    rechargeRequests: List<RechargeRequest>,
    onRequestRecharge: (amount: Double, method: String, txRef: String, receiptUri: String) -> Unit,
    onBackClick: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var showRechargeModal by remember { mutableStateOf(false) }
    var rechargeAmountText by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("بريدي موب BaridiMob") }
    var txRefText by remember { mutableStateOf("") }
    var receiptImageUri by remember { mutableStateOf<Uri?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Photo picker for mandatory payment receipt
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            receiptImageUri = uri
            Toast.makeText(context, "تم إرفاق صورة وصل الدفع بنجاح!", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            OcaSubTopBar(
                title = "محفظتي في OcaVenteDz",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Balance Card (Credit-Card styled in Algerian Green and Navy)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(OcaNavySecondary, Color(0xFF0F766E), OcaGreenDark)
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "رصيد المحفظة المتاح",
                                    color = Color.White.copy(alpha = 0.85f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "🇩🇿 DZD",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(
                                text = formatPriceDZD(balance),
                                color = Color.White,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 32.sp
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "يستخدم لترقية وتمييز الإعلانات",
                                    color = Color.White.copy(alpha = 0.75f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Button(
                                    onClick = { showRechargeModal = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = OcaGreenPrimary,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("recharge_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("شحن الرصيد", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Official Destination Account Box (CCP / RIP)
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OcaGreenPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = OcaGreenPrimary)
                            Text(
                                text = "حساب الشحن المعتمد (البريد الجزائري)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Text(
                            text = "لشحن رصيدك، قم بالتحويل عبر بريدي موب أو أي مركز بريد للحساب التالي:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // CCP Number & Key Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("رقم الحساب (CCP / RIP):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = OFFICIAL_CCP_ACCOUNT,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 1.sp
                                            ),
                                            color = OcaNavySecondary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(OFFICIAL_CCP_ACCOUNT))
                                            Toast.makeText(context, "تم نسخ رقم الحساب بنجاح!", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ رقم الحساب", tint = OcaGreenPrimary)
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("المفتاح (Clé):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = OFFICIAL_CCP_KEY,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Black
                                            ),
                                            color = OcaGreenPrimary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(OFFICIAL_CCP_KEY))
                                            Toast.makeText(context, "تم نسخ المفتاح (94)!", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ المفتاح", tint = OcaGreenPrimary)
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                Text(
                                    text = "المستفيد: $OFFICIAL_BENEFICIARY",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Pending Recharge Requests with Manual Verification Notice
            if (rechargeRequests.isNotEmpty()) {
                item {
                    Text(
                        text = "طلبات الشحن السابقة (${rechargeRequests.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                items(rechargeRequests, key = { it.id }) { req ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = req.method,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "+${formatPriceDZD(req.amount)}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = OcaGreenPrimary
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "مرجع العملية: ${req.transactionRef} • ${req.date}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (req.status.contains("مقبول")) OcaGreenPrimary.copy(alpha = 0.15f)
                                            else OcaAmberTertiary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = req.status,
                                        color = if (req.status.contains("مقبول")) OcaGreenPrimary else OcaAmberTertiary,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Show attached receipt note
                            if (req.receiptImageUrl.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Receipt, contentDescription = null, tint = OcaGreenPrimary, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "وصل الدفع مرفق (قيد المراجعة اليدوية من راهم محمد لمين)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = OcaGreenPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Transactions History
            item {
                Text(
                    text = "سجل العمليات",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (transactions.isEmpty()) {
                item {
                    EmptyState(
                        title = "لا توجد معاملات بعد",
                        message = "ستظهر هنا جميع عمليات شحن المحفظة ودفع رسوم تمييز الإعلانات.",
                        icon = Icons.Default.ReceiptLong
                    )
                }
            } else {
                items(transactions, key = { it.id }) { tx ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (tx.isCredit) OcaGreenPrimary.copy(alpha = 0.12f)
                                            else MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (tx.isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = if (tx.isCredit) OcaGreenPrimary else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = tx.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "${tx.date} • مرجع: ${tx.reference}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                text = "${if (tx.isCredit) "+" else "-"}${formatPriceDZD(tx.amount)}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (tx.isCredit) OcaGreenPrimary else MaterialTheme.colorScheme.error
                                )
                            )
                        }
                    }
                }
            }
        }

        // Recharge BottomSheet with Mandatory Receipt Upload
        if (showRechargeModal) {
            ModalBottomSheet(
                onDismissRequest = { showRechargeModal = false },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "طلب شحن رصيد المحفظة",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    // CCP Account reminder
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = OcaGreenPrimary.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OcaGreenPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "حساب التحويل: $OFFICIAL_CCP_ACCOUNT مفتاح $OFFICIAL_CCP_KEY",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = OcaNavySecondary
                            )
                            Text(
                                text = "المستفيد: $OFFICIAL_BENEFICIARY",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Payment Method selector
                    Text(
                        text = "طريقة التحويل:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    val methods = listOf("بريدي موب BaridiMob", "الحساب البريدي الجاري CCP")
                    methods.forEach { method ->
                        Surface(
                            onClick = { selectedMethod = method },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedMethod == method) OcaGreenPrimary.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = if (selectedMethod == method) androidx.compose.foundation.BorderStroke(1.5.dp, OcaGreenPrimary) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RadioButton(
                                    selected = selectedMethod == method,
                                    onClick = { selectedMethod = method },
                                    colors = RadioButtonDefaults.colors(selectedColor = OcaGreenPrimary)
                                )
                                Text(
                                    text = method,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (selectedMethod == method) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }

                    // Amount input
                    OutlinedTextField(
                        value = rechargeAmountText,
                        onValueChange = { rechargeAmountText = it },
                        label = { Text("المبلغ المراد شحنه (دج) *") },
                        placeholder = { Text("مثال: 1000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("recharge_amount_input")
                    )

                    // Transaction Ref input
                    OutlinedTextField(
                        value = txRefText,
                        onValueChange = { txRefText = it },
                        label = { Text("رقم العملية / مرجع الوصل (Ref) *") },
                        placeholder = { Text("رقم المعاملة في تطبيق بريدي موب أو وصل البريد") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("recharge_ref_input")
                    )

                    // MANDATORY PAYMENT RECEIPT UPLOAD SECTION
                    Text(
                        text = "وصل الدفع (إجباري) *",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (receiptImageUri == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )

                    if (receiptImageUri == null) {
                        Surface(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("upload_receipt_btn")
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "اضغط لرفع صورة وصل الدفع (إجباري)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "صورة واضحة من بريدي موب أو الوصل الورقي",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // Preview uploaded receipt
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AsyncImage(
                                        model = receiptImageUri,
                                        contentDescription = "وصل الدفع",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, OcaGreenPrimary, RoundedCornerShape(8.dp))
                                    )
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OcaGreenPrimary, modifier = Modifier.size(16.dp))
                                            Text(
                                                text = "تم إرفاق الوصل بنجاح",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = OcaGreenPrimary
                                            )
                                        }
                                        Text(
                                            text = "جاهز للإرسال للتحقق اليدوي",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                ) {
                                    Text("تغيير", color = OcaGreenPrimary)
                                }
                            }
                        }
                    }

                    // Notice on manual verification
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                            Text(
                                text = "ملاحظة أمان: سيتم التحقق من الوصل يدوياً من طرف المسؤول (راهم محمد لمين) والتأكد من مطابقة المبلغ قبل إضافة الرصيد إلى محفظتك.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF92400E)
                            )
                        }
                    }

                    val isFormValid = rechargeAmountText.isNotBlank() &&
                                     txRefText.isNotBlank() &&
                                     receiptImageUri != null

                    PrimaryButton(
                        text = if (receiptImageUri == null) "يرجى إرفاق وصل الدفع أولاً" else "إرسال طلب الشحن للتحقق اليدوي",
                        enabled = isFormValid,
                        onClick = {
                            val amount = rechargeAmountText.toDoubleOrNull() ?: 1000.0
                            onRequestRecharge(
                                amount,
                                selectedMethod,
                                txRefText,
                                receiptImageUri.toString()
                            )
                            showRechargeModal = false
                            showSuccessDialog = true
                        },
                        testTag = "submit_recharge_btn"
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        InfoDialog(
            show = showSuccessDialog,
            title = "تم إرسال طلب الشحن بنجاح",
            message = "تم تسجيل طلبك وإرفاق وصل الدفع، وسيتم التحقق منه يدوياً من طرف الإدارة (راهم محمد لمين) وتفعيل رصيدك في أقرب وقت.",
            onDismiss = { showSuccessDialog = false }
        )
    }
}
