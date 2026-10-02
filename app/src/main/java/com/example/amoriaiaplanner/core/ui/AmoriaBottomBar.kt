package com.example.amoriaiaplanner.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

@Composable
fun AmoriaBottomBar(
    selected: BottomBarItem,
    onHomeClick: () -> Unit,
    onManageProfileClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 34.dp, vertical = 14.dp)
            .background(
                color = Color.Black.copy(alpha = 0.34f),
                shape = RoundedCornerShape(30.dp)
            )
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(260.dp)
        ) {
            BottomBarButton(
                item = BottomBarItem.PROFILE,
                selected = selected == BottomBarItem.PROFILE,
                onClick = onManageProfileClick
            )

            BottomBarButton(
                item = BottomBarItem.HOME,
                selected = selected == BottomBarItem.HOME,
                onClick = onHomeClick
            )

            BottomBarButton(
                item = BottomBarItem.LOGOUT,
                selected = selected == BottomBarItem.LOGOUT,
                onClick = onLogoutClick
            )
        }
    }
}

@Composable
private fun BottomBarButton(
    item: BottomBarItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val icon = when (item) {
        BottomBarItem.HOME -> Icons.Default.Home
        BottomBarItem.PROFILE -> Icons.Default.ManageAccounts
        BottomBarItem.LOGOUT -> Icons.AutoMirrored.Filled.ExitToApp
    }

    val label = when (item) {
        BottomBarItem.HOME -> "Home"
        BottomBarItem.PROFILE -> "Profile"
        BottomBarItem.LOGOUT -> "Logout"
    }

    androidx.compose.foundation.layout.Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(if (selected) 54.dp else 44.dp)
                .background(
                    brush = if (selected) {
                        Brush.linearGradient(
                            listOf(Color(0xFFE14D72), Color(0xFF7C4DFF))
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(Color.Transparent, Color.Transparent)
                        )
                    },
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(if (selected) 26.dp else 22.dp)
            )
        }

        Text(
            text = label,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.65f),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

enum class BottomBarItem {
    PROFILE,
    HOME,
    LOGOUT
}
