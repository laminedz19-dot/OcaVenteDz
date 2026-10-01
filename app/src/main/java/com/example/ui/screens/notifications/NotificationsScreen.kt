package com.example.ui.screens.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.components.EmptyState
import com.example.ui.components.NotificationItemRow
import com.example.ui.components.OcaSubTopBar
import com.example.ui.model.AppNotification
import com.example.ui.theme.OcaGreenPrimary

@Composable
fun NotificationsScreen(
    notifications: List<AppNotification>,
    onNotificationClick: (AppNotification) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            OcaSubTopBar(
                title = "الإشعارات (${notifications.size})",
                onBackClick = onBackClick,
                actions = {
                    if (notifications.any { !it.isRead }) {
                        IconButton(onClick = onMarkAllAsRead) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "تحديد الكل كمقروء",
                                tint = OcaGreenPrimary
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (notifications.isEmpty()) {
            EmptyState(
                title = "لا توجد إشعارات جديدة",
                message = "ستصلك هنا إشعارات حول العروض، الرسائل الجديدة، وتحديثات حسابك في OcaVenteDz.",
                icon = Icons.Default.NotificationsNone,
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(notifications, key = { it.id }) { notif ->
                    NotificationItemRow(
                        notification = notif,
                        onClick = { onNotificationClick(notif) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
            }
        }
    }
}
