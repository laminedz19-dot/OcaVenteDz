package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.ApiClient
import com.example.ui.components.ListingCard
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onOpenAuthDialog: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allListings by viewModel.allListings.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    var selectedSection by remember { mutableStateOf(0) } // 0: My Ads, 1: Favorites, 2: Notifications, 3: Settings
    var showServerConfigDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الملف الشخصي والحساب", fontWeight = FontWeight.Bold) }
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
            // Profile Card Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (currentUser != null) {
                            Text(
                                text = currentUser!!.username,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentUser!!.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currentUser!!.wilaya,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "الرتبة: ${currentUser!!.role}",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            Text("أنت غير مسجل الدخول", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = onOpenAuthDialog) {
                                Text("تسجيل الدخول / إنشاء حساب")
                            }
                        }
                    }
                }
            }

            // Navigation Sections Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("إعلاناتي", "المفضلة", "الإشعارات", "الإعدادات").forEachIndexed { idx, label ->
                        FilterChip(
                            selected = selectedSection == idx,
                            onClick = { selectedSection = idx },
                            label = { Text(label, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Section 0: My Listings
            if (selectedSection == 0) {
                val myListings = allListings.filter { it.userId == (currentUser?.id ?: "demo-user-1") }
                if (myListings.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("ليس لديك إعلانات منشورة بعد", color = Color.Gray)
                        }
                    }
                } else {
                    items(myListings) { item ->
                        ListingCard(
                            listing = item,
                            isFavorite = favoriteIds.contains(item.id),
                            onToggleFavorite = { viewModel.toggleFavorite(item.id) },
                            onClick = { viewModel.openListingDetail(item) }
                        )
                    }
                }
            }

            // Section 1: Favorites
            else if (selectedSection == 1) {
                val favListings = allListings.filter { favoriteIds.contains(it.id) }
                if (favListings.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("لا توجد إعلانات في المفضلة", color = Color.Gray)
                        }
                    }
                } else {
                    items(favListings) { item ->
                        ListingCard(
                            listing = item,
                            isFavorite = true,
                            onToggleFavorite = { viewModel.toggleFavorite(item.id) },
                            onClick = { viewModel.openListingDetail(item) }
                        )
                    }
                }
            }

            // Section 2: Notifications
            else if (selectedSection == 2) {
                if (notifications.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("لا توجد إشعارات حالياً", color = Color.Gray)
                        }
                    }
                } else {
                    items(notifications) { notif ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.markNotificationRead(notif.id) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (notif.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Notifications,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = notif.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = notif.message, fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Settings & Backend URL
            else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("إعدادات النظام والاتصال", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                            OutlinedButton(
                                onClick = { showServerConfigDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Dns, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("عنوان خادم الـ Backend (API URL)")
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("الخادم الحالي:", fontSize = 11.sp, color = Color.Gray)
                                    Text(ApiClient.getBaseUrl(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("100% خادم Node.js + Express + PostgreSQL مستقل (Zero Firebase)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            if (currentUser != null) {
                                Button(
                                    onClick = { viewModel.logout() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Logout, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("تسجيل الخروج")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showServerConfigDialog) {
        var serverInput by remember { mutableStateOf(ApiClient.getBaseUrl()) }
        AlertDialog(
            onDismissRequest = { showServerConfigDialog = false },
            title = { Text("تكوين خادم Backend") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل عنوان الـ API للاتصال بالخادم المحلي أو السحابي:")
                    OutlinedTextField(
                        value = serverInput,
                        onValueChange = { serverInput = it },
                        label = { Text("API Base URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("افتراضي للمحاكي: http://10.0.2.2:4000/api/", fontSize = 11.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(onClick = {
                    ApiClient.setBaseUrl(serverInput)
                    showServerConfigDialog = false
                }) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerConfigDialog = false }) { Text("إلغاء") }
            }
        )
    }
}
