package com.example.ui.admin

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.ui.components.EmptyState
import com.example.ui.components.OcaSubTopBar
import com.example.ui.components.formatPriceDZD
import com.example.ui.model.RechargeRequest
import com.example.ui.screens.wallet.OFFICIAL_CCP_ACCOUNT
import com.example.ui.screens.wallet.OFFICIAL_CCP_KEY
import com.example.ui.theme.OcaAmberTertiary
import com.example.ui.theme.OcaGreenPrimary
import com.example.ui.theme.OcaNavySecondary

@Composable
fun AdminRechargeScreen(
    requests: List<RechargeRequest>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var previewReceiptUrl by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            OcaSubTopBar(
                title = "التحقق اليدوي من وصولات الدفع (${requests.size})",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        if (requests.isEmpty()) {
            EmptyState(
                title = "لا توجد طلبات شحن معلقة",
                message = "لم يتم تقديم أي طلبات شحن جديدة بحاجة لمراجعة وصولات الدفع.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    // Admin Info Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = OcaGreenPrimary.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OcaGreenPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = OcaGreenPrimary)
                            Column {
                                Text(
                                    text = "الحساب المعتمد: $OFFICIAL_CCP_ACCOUNT مفتاح $OFFICIAL_CCP_KEY",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = OcaNavySecondary
                                )
                                Text(
                                    text = "المسؤول عن التحقق اليدوي: راهم محمد لمين",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                items(requests, key = { it.id }) { req ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = req.method,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = formatPriceDZD(req.amount),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = OcaGreenPrimary
                                    )
                                )
                            }

                            Text(
                                text = "رقم المعاملة: ${req.transactionRef} • تاريخ الإرسال: ${req.date}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Attached Receipt Preview
                            if (req.receiptImageUrl.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { previewReceiptUrl = req.receiptImageUrl }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        AsyncImage(
                                            model = req.receiptImageUrl,
                                            contentDescription = "وصل الدفع",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, OcaGreenPrimary, RoundedCornerShape(8.dp))
                                        )
                                        Column {
                                            Text(
                                                text = "وصل الدفع مرفق (انقر للتكبير)",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = OcaGreenPrimary
                                            )
                                            Text(
                                                text = "تحقق من تطابق المبلغ ورقم العملية يدوياً",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Text(
                                        text = "تنبيه: لم يتم إرفاق صورة الوصل",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
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

                                if (req.status.contains("قيد") || req.status.contains("المراجعة")) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = { onReject(req.id) }) {
                                            Text("رفض الوصل", color = MaterialTheme.colorScheme.error)
                                        }
                                        Button(
                                            onClick = { onApprove(req.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = OcaGreenPrimary)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("تأكيد وشحن الرصيد")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Full Screen Receipt Preview Dialog
        if (previewReceiptUrl != null) {
            Dialog(onDismissRequest = { previewReceiptUrl = null }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "معاينة وصل الدفع",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        AsyncImage(
                            model = previewReceiptUrl,
                            contentDescription = "وصل الدفع بالحجم الكامل",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Button(
                            onClick = { previewReceiptUrl = null },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إغلاق")
                        }
                    }
                }
            }
        }
    }
}
