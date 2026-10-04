package com.example.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.UrgentRed
import com.example.ui.viewmodel.MarketplaceViewModel

@Composable
fun ProfileTopUpDialog(
    viewModel: MarketplaceViewModel,
    onDismiss: () -> Unit,
    onLogin: () -> Unit
) {
    var topUpAmountText by remember { mutableStateOf("500") }
    var topUpProvider by remember { mutableStateOf("BARIDIMOB") }
    var topUpReferenceText by remember { mutableStateOf("") }
    var receiptImageUri by remember { mutableStateOf("") }
    var isAccountCopied by remember { mutableStateOf(false) }
    var isSubmittingTopUp by remember { mutableStateOf(false) }
    var topUpErrorText by remember { mutableStateOf<String?>(null) }
    val clipboardManager = LocalClipboardManager.current

    val receiptPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { receiptImageUri = it.toString() }
    }

    val platformSettings by viewModel.platformSettings.collectAsState()
    val officialRip = platformSettings.officialRip
    val officialKey = platformSettings.officialKey
    val officialProvider = platformSettings.officialProviderName
    val officialHolder = platformSettings.officialAccountHolder
    val officialInstructions = platformSettings.officialInstructions

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("شحن رصيد المحفظة", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            val currentUid by viewModel.currentUserId.collectAsState()
            val isUserLoggedIn = currentUid.isNotBlank() && currentUid != "deleted"
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    if (!isUserLoggedIn) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            colors = CardDefaults.cardColors(containerColor = UrgentRed.copy(alpha = 0.12f)),
                            border = BorderStroke(1.dp, UrgentRed.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = UrgentRed, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تنبيه: الجلسة غير مفعلة", fontWeight = FontWeight.Bold, color = UrgentRed, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "يجب تسجيل الدخول بحسابك أولاً حتى يتمكن المشرف من التعرف عليك وشحن الرصيد في محفظتك تلقائياً.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 15.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onLogin()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.fillMaxWidth().height(34.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("تسجيل الدخول الآن", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Official Account Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = EmeraldPrimary.copy(alpha = 0.08f)
                        ),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "حساب الشحن الرسمي",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = EmeraldPrimary
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = GoldSecondary
                                ) {
                                    Text(
                                        text = officialProvider,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (officialHolder.isNotBlank()) {
                                Text(
                                    text = officialHolder,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("رقم الحساب (RIP):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "$officialRip Clé $officialKey",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(officialRip))
                                        isAccountCopied = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isAccountCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = "نسخ RIP",
                                        tint = if (isAccountCopied) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (officialInstructions.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = officialInstructions,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("المبلغ المراد شحنه (دج):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("200", "500", "1000", "2000").forEach { preset ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (topUpAmountText == preset) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(enabled = !isSubmittingTopUp) { topUpAmountText = preset }
                            ) {
                                Text(
                                    text = "$preset دج",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (topUpAmountText == preset) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = topUpAmountText,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) topUpAmountText = it },
                        label = { Text("أو أدخل مبلغاً آخر") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSubmittingTopUp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = topUpReferenceText,
                        onValueChange = { topUpReferenceText = it },
                        label = { Text("رقم مرجع التحويل أو اسم المرسل") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSubmittingTopUp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Receipt Upload Section
                    Text("إثبات التحويل (وصل بريدي موب / CCP):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))

                    if (receiptImageUri.isBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isSubmittingTopUp) {
                                    receiptPickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = EmeraldPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إرفاق صورة الوصل من الهاتف", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = receiptImageUri,
                                        contentDescription = "وصل العملية",
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("تم إرفاق صورة الوصل ✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                        Text("انقر للاستبدال أو الحذف", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                IconButton(onClick = { receiptImageUri = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "حذف الصورة", tint = UrgentRed)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("وسيلة التحويل:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))

                    listOf(
                        "BARIDIMOB" to "تطبيق بريدي موب (BaridiMob)",
                        "CCP" to "حوالة بريد الجزائر CCP",
                        "EDAHABIA" to "البطاقة الذهبية بريد الجزائر",
                        "CIB" to "بطاقة بنكية CIB"
                    ).forEach { (code, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isSubmittingTopUp) { topUpProvider = code }
                                .padding(vertical = 3.dp)
                        ) {
                            RadioButton(selected = topUpProvider == code, onClick = { topUpProvider = code }, enabled = !isSubmittingTopUp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(label, fontSize = 12.sp)
                        }
                    }

                    if (topUpErrorText != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = UrgentRed.copy(alpha = 0.12f)),
                            border = BorderStroke(1.dp, UrgentRed.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = topUpErrorText!!,
                                color = UrgentRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val currentUid = viewModel.currentUserId.value
                    val isUserLoggedIn = currentUid.isNotBlank() && currentUid != "deleted"
                    if (!isUserLoggedIn) {
                        topUpErrorText = "يرجى تسجيل الدخول أولاً بحسابك لإرسال طلب الشحن والوصل إلى المشرف."
                        return@Button
                    }
                    val amount = topUpAmountText.toIntOrNull() ?: 0
                    if (amount < 200) {
                        topUpErrorText = "الحد الأدنى لشحن الرصيد هو 200 دج"
                        return@Button
                    }
                    if (receiptImageUri.isBlank() && topUpReferenceText.isBlank()) {
                        topUpErrorText = "يرجى إرفاق صورة وصل التحويل أو إدخال رقم مرجع العملية على الأقل"
                        return@Button
                    }

                    val reqAmount = amount
                    val reqProvider = topUpProvider
                    val reqRef = topUpReferenceText
                    val reqReceipt = receiptImageUri

                    // إغلاق النافذة فوراً عند إرسال طلب الشحن
                    onDismiss()
                    topUpReferenceText = ""
                    receiptImageUri = ""
                    topUpErrorText = null

                    viewModel.submitTopUpRequest(
                        amount = reqAmount,
                        provider = reqProvider,
                        reference = reqRef,
                        receiptImageUri = reqReceipt
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إرسال طلب الشحن للمراجعة")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    isSubmittingTopUp = false
                    topUpErrorText = null
                }
            ) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun ProfileDeleteAccountDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("حذف الحساب نهائياً", fontWeight = FontWeight.Bold, color = UrgentRed) },
        text = {
            Text("هل أنت متأكد من رغبتك في حذف الحساب؟ سيتم مسح بياناتك وإعلاناتك وفق سياسة الخصوصية وحماية البيانات.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = UrgentRed)
            ) {
                Text("نعم، حذف الحساب")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
