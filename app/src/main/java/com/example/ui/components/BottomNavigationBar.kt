package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OcaGreenPrimary

enum class MainTab(val titleAr: String) {
    HOME("الرئيسية"),
    SEARCH("البحث"),
    CREATE("أضف إعلان"),
    CHAT("المحادثات"),
    PROFILE("حسابي")
}

@Composable
fun OcaBottomNavigationBar(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    unreadMessagesCount: Int = 1,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home Tab
            BottomNavItem(
                title = MainTab.HOME.titleAr,
                iconSelected = Icons.Default.Home,
                iconUnselected = Icons.Outlined.Home,
                isSelected = currentTab == MainTab.HOME,
                onClick = { onTabSelected(MainTab.HOME) },
                testTag = "nav_tab_home"
            )

            // Search Tab
            BottomNavItem(
                title = MainTab.SEARCH.titleAr,
                iconSelected = Icons.Default.Search,
                iconUnselected = Icons.Outlined.Search,
                isSelected = currentTab == MainTab.SEARCH,
                onClick = { onTabSelected(MainTab.SEARCH) },
                testTag = "nav_tab_search"
            )

            // Add Listing (Special prominent center action)
            Box(
                modifier = Modifier
                    .offset(y = (-6).dp)
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(OcaGreenPrimary)
                    .clickable { onTabSelected(MainTab.CREATE) }
                    .testTag("nav_tab_create"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "أضف إعلان",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }

            // Chat Tab
            BottomNavItem(
                title = MainTab.CHAT.titleAr,
                iconSelected = Icons.Default.ChatBubble,
                iconUnselected = Icons.Outlined.ChatBubbleOutline,
                isSelected = currentTab == MainTab.CHAT,
                badgeCount = unreadMessagesCount,
                onClick = { onTabSelected(MainTab.CHAT) },
                testTag = "nav_tab_chat"
            )

            // Profile Tab
            BottomNavItem(
                title = MainTab.PROFILE.titleAr,
                iconSelected = Icons.Default.Person,
                iconUnselected = Icons.Outlined.Person,
                isSelected = currentTab == MainTab.PROFILE,
                onClick = { onTabSelected(MainTab.PROFILE) },
                testTag = "nav_tab_profile"
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    title: String,
    iconSelected: ImageVector,
    iconUnselected: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    badgeCount: Int = 0,
    testTag: String
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        BadgedBox(
            badge = {
                if (badgeCount > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    ) {
                        Text(badgeCount.toString())
                    }
                }
            }
        ) {
            Icon(
                imageVector = if (isSelected) iconSelected else iconUnselected,
                contentDescription = title,
                tint = if (isSelected) OcaGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isSelected) OcaGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
