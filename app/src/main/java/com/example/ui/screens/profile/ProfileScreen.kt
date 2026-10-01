package com.example.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatPriceDZD
import com.example.ui.model.UserProfile
import com.example.ui.theme.OcaAmberTertiary
import com.example.ui.theme.OcaGreenPrimary
import com.example.ui.theme.OcaNavySecondary

@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToMyListings: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToOffers: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToLegal: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // User Header Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(name = userProfile.name, size = 64.dp)

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = userProfile.name,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                if (userProfile.isVerified) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "موثق",
                                        tint = OcaGreenPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Text(
                                text = "${userProfile.phone} • ولاية ${userProfile.wilayaName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "عضو منذ ${userProfile.memberSince}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }

                        IconButton(
                            onClick = onNavigateToEditProfile,
                            modifier = Modifier.testTag("edit_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "تعديل الملف",
                                tint = OcaGreenPrimary
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Stats Quick Row (Listings, Balance, Rating)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItem(
                            title = "إعلاناتي",
                            value = "${userProfile.listingsCount}",
                            onClick = onNavigateToMyListings
                        )
                        StatItem(
                            title = "الرصيد",
                            value = formatPriceDZD(userProfile.balance),
                            onClick = onNavigateToWallet
                        )
                        StatItem(
                            title = "التقييم",
                            value = "⭐ ${userProfile.rating}",
                            onClick = {}
                        )
                    }
                }
            }

            // Menu Section: Activity
            MenuSectionCard(title = "نشاطاتي في OcaVenteDz") {
                ProfileMenuItem(
                    icon = Icons.Default.Inventory2,
                    title = "إعلاناتي المعروضة",
                    subtitle = "إدارة وحذف وتعديل الإعلانات",
                    onClick = onNavigateToMyListings,
                    testTag = "menu_my_listings"
                )
                ProfileMenuItem(
                    icon = Icons.Default.Favorite,
                    title = "المفضلة",
                    subtitle = "الإعلانات المحفوظة للرجوع إليها",
                    onClick = onNavigateToFavorites,
                    testTag = "menu_favorites"
                )
                ProfileMenuItem(
                    icon = Icons.Default.LocalOffer,
                    title = "العروض والتفاوض",
                    subtitle = "عروض الأسعار المقترحة والمستلمة",
                    onClick = onNavigateToOffers,
                    testTag = "menu_offers"
                )
                ProfileMenuItem(
                    icon = Icons.Default.LocalShipping,
                    title = "طلبات التوصيل",
                    subtitle = "تتبع الشحنات والطرود المتفق عليها",
                    onClick = onNavigateToOrders,
                    testTag = "menu_orders"
                )
                ProfileMenuItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "محفظتي",
                    subtitle = "الرصيد وعمليات الشحن ببريدي موب وCCP",
                    onClick = onNavigateToWallet,
                    testTag = "menu_wallet"
                )
                ProfileMenuItem(
                    icon = Icons.Default.Notifications,
                    title = "الإشعارات",
                    subtitle = "تنبيهات الرسائل والعروض",
                    onClick = onNavigateToNotifications,
                    testTag = "menu_notifications"
                )
            }

            // Menu Section: Account & Security
            MenuSectionCard(title = "الحساب والأمان") {
                ProfileMenuItem(
                    icon = Icons.Default.Security,
                    title = "مركز الأمان",
                    subtitle = "كلمة المرور وحماية الحساب والجلسات",
                    onClick = onNavigateToSecurity,
                    testTag = "menu_security"
                )
                ProfileMenuItem(
                    icon = Icons.Default.Settings,
                    title = "الإعدادات العامة",
                    subtitle = "المظهر الداكن واللغة والتفضيلات",
                    onClick = onNavigateToSettings,
                    testTag = "menu_settings"
                )
                ProfileMenuItem(
                    icon = Icons.Default.Gavel,
                    title = "المعلومات القانونية والشروط",
                    subtitle = "شروط الاستخدام وسياسة الخصوصية",
                    onClick = onNavigateToLegal,
                    testTag = "menu_legal"
                )
            }

            // Special Admin Switcher Section
            MenuSectionCard(title = "إدارة المنصة") {
                ProfileMenuItem(
                    icon = Icons.Default.AdminPanelSettings,
                    title = "لوحة تحكم الإدارة (Admin Panel)",
                    subtitle = "إحصائيات المنصة، إدارة الإعلانات ومراجعة الشحن",
                    iconTint = OcaGreenPrimary,
                    onClick = onNavigateToAdmin,
                    testTag = "menu_admin"
                )
            }

            // Logout Button
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("logout_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Text("تسجيل الخروج", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        ConfirmDialog(
            show = showLogoutDialog,
            title = "تسجيل الخروج",
            message = "هل أنت متأكد من رغبتك في تسجيل الخروج من حسابك؟",
            confirmText = "تسجيل الخروج",
            isDestructive = true,
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
}

@Composable
private fun StatItem(title: String, value: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = OcaGreenPrimary
            )
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MenuSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = OcaNavySecondary,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronLeft,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline
        )
    }
}
