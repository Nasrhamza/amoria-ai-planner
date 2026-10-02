@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.amoriaiaplanner.feature_admin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Room
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amoriaiaplanner.core.ui.AmoriaPrimaryButton
import com.example.amoriaiaplanner.core.ui.AmoriaTextButton
import com.example.amoriaiaplanner.feature_admin.model.AdminTab
import com.example.amoriaiaplanner.feature_admin.model.AdminOperation
import com.example.amoriaiaplanner.feature_admin.model.AdminUiState
import com.example.amoriaiaplanner.feature_admin.vm.AdminViewModel
import com.example.amoriaiaplanner.feature_profile.model.UserRole

@Composable
fun AdminDashboardScreen(
    onLogout: () -> Unit,
    vm: AdminViewModel = viewModel()
) {
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current
    val bg = Brush.linearGradient(
        listOf(Color(0xFFB3202E), Color(0xFF4B1E6D), Color(0xFF0B2B6B))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Column {
                        Text("Admin Dashboard", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            "Amoria control center",
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 12.sp
                        )
                    }
                },
                actions = {
                    AmoriaTextButton(
                        text = "Logout",
                        onClick = onLogout,
                        modifier = Modifier.padding(end = 12.dp),
                        enabled = !ui.isBusy
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.20f),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                AdminHeaderCard(ui)

                Spacer(Modifier.height(14.dp))

                AdminTabStrip(
                    selectedTab = ui.selectedTab,
                    onTabSelected = vm::setTab
                )

                Spacer(Modifier.height(14.dp))

                AdminBanner(ui = ui)

                Spacer(Modifier.height(14.dp))

                when (ui.selectedTab) {
                    AdminTab.OVERVIEW -> AdminOverviewSection(ui = ui, onRefresh = vm::refresh)
                    AdminTab.USERS -> AdminUsersScreen(
                        ui = ui,
                        onSearchChange = vm::setSearchQuery,
                        onBlockUser = vm::blockUser,
                        onUnblockUser = vm::unblockUser,
                        onRoleChange = vm::updateUserRole,
                        onCreateUser = { email, password, displayName, role ->
                            vm.createUser(
                                context = context,
                                email = email,
                                password = password,
                                displayName = displayName,
                                role = role
                            )
                        },
                        onRefresh = vm::refresh
                    )
                    AdminTab.ROOMS -> AdminRoomsScreen(
                        ui = ui,
                        onSearchChange = vm::setRoomSearchQuery,
                        onSelectRoom = vm::selectRoom,
                        onUpdateRoomStatus = vm::updateRoomStatus,
                        onDeleteRoom = vm::deleteRoom,
                        onRefresh = vm::refresh
                    )
                    AdminTab.FEATURES -> AdminFeaturesScreen(
                        ui = ui,
                        onSettingsChange = vm::setFeatureSettings,
                        onSave = vm::saveFeatureSettings
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminHeaderCard(ui: AdminUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.10f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Dashboard,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.size(10.dp))
                Column {
                    Text(
                        text = "Administrator",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Role: ${ui.currentUserRole}",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 13.sp
                    )
                }
            }

            if (ui.currentUserRole == UserRole.ADMIN) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Manage users, rooms, statistics, and application functionalities.",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun AdminTabStrip(
    selectedTab: AdminTab,
    onTabSelected: (AdminTab) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        AdminTabChip(
            text = "Overview",
            icon = Icons.Default.Dashboard,
            selected = selectedTab == AdminTab.OVERVIEW,
            onClick = { onTabSelected(AdminTab.OVERVIEW) },
            modifier = Modifier.width(138.dp)
        )
        AdminTabChip(
            text = "Users",
            icon = Icons.Default.People,
            selected = selectedTab == AdminTab.USERS,
            onClick = { onTabSelected(AdminTab.USERS) },
            modifier = Modifier.width(128.dp)
        )
        AdminTabChip(
            text = "Rooms",
            icon = Icons.Default.Room,
            selected = selectedTab == AdminTab.ROOMS,
            onClick = { onTabSelected(AdminTab.ROOMS) },
            modifier = Modifier.width(128.dp)
        )
        AdminTabChip(
            text = "Features",
            icon = Icons.Default.Settings,
            selected = selectedTab == AdminTab.FEATURES,
            onClick = { onTabSelected(AdminTab.FEATURES) },
            modifier = Modifier.width(142.dp)
        )
    }
}

@Composable
private fun AdminTabChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brush = if (selected) {
        Brush.linearGradient(listOf(Color(0xFFE14D72), Color(0xFF7C4DFF)))
    } else {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.12f)))
    }

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(brush)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(text = text, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AdminBanner(ui: AdminUiState) {
    when {
        ui.operation != null -> AdminInfoBanner(
            title = operationTitle(ui.operation),
            subtitle = "Applying the change securely and refreshing Firestore data..."
        )
        ui.loading -> AdminInfoBanner(
            title = "Loading admin data",
            subtitle = "Fetching users, rooms, and statistics from Firestore..."
        )
        ui.refreshing -> AdminInfoBanner(
            title = "Refreshing data",
            subtitle = "Keeping the current dashboard visible while new data is loaded..."
        )
        ui.error != null -> AdminInfoBanner(
            title = "Admin error",
            subtitle = ui.error,
            accent = Color(0xFFB3202E)
        )
        ui.message != null -> AdminInfoBanner(
            title = "Success",
            subtitle = ui.message,
            accent = Color(0xFF2E7D32)
        )
    }
}

private fun operationTitle(operation: AdminOperation): String {
    return when (operation) {
        AdminOperation.CREATE_USER -> "Creating user"
        AdminOperation.UPDATE_USER -> "Updating user"
        AdminOperation.UPDATE_ROOM -> "Updating room"
        AdminOperation.DELETE_ROOM -> "Deleting room data"
        AdminOperation.SAVE_FEATURES -> "Saving application controls"
    }
}

@Composable
private fun AdminInfoBanner(
    title: String,
    subtitle: String,
    accent: Color = Color.White.copy(alpha = 0.16f)
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.30f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun AdminOverviewSection(
    ui: AdminUiState,
    onRefresh: () -> Unit
) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AdminStatCard(
                title = "Users",
                value = ui.stats.totalUsers.toString(),
                icon = Icons.Default.People,
                modifier = Modifier.weight(1f)
            )
            AdminStatCard(
                title = "Rooms",
                value = ui.stats.totalRooms.toString(),
                icon = Icons.Default.Room,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AdminStatCard(
                title = "Room members",
                value = ui.stats.totalRoomMembers.toString(),
                icon = Icons.Default.People,
                modifier = Modifier.weight(1f)
            )
            AdminStatCard(
                title = "Average / room",
                value = String.format(java.util.Locale.US, "%.1f", ui.stats.averageMembersPerRoom),
                icon = Icons.Default.Group,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            AdminStatCard(
                title = "Active rooms",
                value = ui.stats.activeRooms.toString(),
                icon = Icons.Default.Group,
                modifier = Modifier.weight(1f)
            )
            AdminStatCard(
                title = "Suggestions",
                value = ui.stats.totalSuggestions?.toString() ?: "N/A",
                icon = Icons.Default.Dashboard,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(14.dp))

        AmoriaPrimaryButton(
            text = if (ui.isBusy) "Refreshing..." else "Refresh data",
            onClick = onRefresh,
            enabled = !ui.isBusy
        )
    }
}

@Composable
private fun AdminStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.10f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White.copy(alpha = 0.9f))
                Spacer(Modifier.size(8.dp))
                Text(title, color = Color.White.copy(alpha = 0.82f), fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text(value, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
}
