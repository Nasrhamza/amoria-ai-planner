@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.amoriaiaplanner.feature_admin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Room
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amoriaiaplanner.core.ui.AmoriaGlassButton
import com.example.amoriaiaplanner.feature_admin.model.AdminRoomItem
import com.example.amoriaiaplanner.feature_admin.model.AdminUiState
import com.example.amoriaiaplanner.feature_friends_room.model.RoomStatus
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun AdminRoomsScreen(
    ui: AdminUiState,
    onSearchChange: (String) -> Unit,
    onSelectRoom: (AdminRoomItem?) -> Unit,
    onUpdateRoomStatus: (String, String) -> Unit,
    onDeleteRoom: (String) -> Unit,
    onRefresh: () -> Unit
) {
    var pendingDelete by remember { mutableStateOf<AdminRoomItem?>(null) }
    val query = ui.roomSearchQuery.trim()
    val rooms = ui.rooms.filter { room ->
        query.isBlank() ||
            room.code.contains(query, ignoreCase = true) ||
            room.hostEmail.contains(query, ignoreCase = true) ||
            room.hostUid.contains(query, ignoreCase = true) ||
            room.status.contains(query, ignoreCase = true)
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Friends Rooms",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${ui.rooms.size} rooms | ${ui.rooms.count { it.status in setOf(RoomStatus.WAITING, RoomStatus.STARTED) }} active",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp
                )
            }
            TextButton(onClick = onRefresh, enabled = !ui.isBusy) {
                Text("Refresh", color = Color.White)
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = ui.roomSearchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.White.copy(alpha = 0.75f)
                )
            },
            placeholder = {
                Text("Search by code, host or status", color = Color.White.copy(alpha = 0.7f))
            },
            singleLine = true,
            colors = adminRoomFieldColors(),
            shape = RoundedCornerShape(22.dp)
        )

        Spacer(Modifier.height(12.dp))

        if (rooms.isEmpty()) {
            EmptyRoomsState(hasSearch = query.isNotBlank(), onRefresh = onRefresh, enabled = !ui.isBusy)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rooms.forEach { room ->
                    androidx.compose.runtime.key(room.roomId) {
                        AdminRoomCard(
                            room = room,
                            onClick = { onSelectRoom(room) }
                        )
                    }
                }
            }
        }
    }

    ui.selectedRoom?.let { room ->
        AdminRoomDetailsDialog(
            room = room,
            enabled = !ui.isBusy,
            onDismiss = { onSelectRoom(null) },
            onStatusChange = { status -> onUpdateRoomStatus(room.roomId, status) },
            onRequestDelete = {
                onSelectRoom(null)
                pendingDelete = room
            }
        )
    }

    pendingDelete?.let { room ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = Color(0xFF24192D),
            title = {
                Text("Delete room ${room.code}?", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "The room, members, quiz answers and stored suggestions will be removed. This cannot be undone.",
                    color = Color.White.copy(alpha = 0.82f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onDeleteRoom(room.roomId)
                    }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF8A80))
                    Text(" Delete permanently", color = Color(0xFFFF8A80))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.8f))
                }
            }
        )
    }
}

@Composable
private fun AdminRoomCard(
    room: AdminRoomItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.10f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Room, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Room ${room.code.ifBlank { room.roomId.take(8) }}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = room.hostEmail.ifBlank { room.hostUid.ifBlank { "Unknown host" } },
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 13.sp
                    )
                }
                RoomStatusBadge(room.status)
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.72f)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "${room.memberCount}/${room.maxMembers} members",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = formatAdminDate(room.createdAt),
                    color = Color.White.copy(alpha = 0.68f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun RoomStatusBadge(status: String) {
    val color = when (status.lowercase()) {
        RoomStatus.STARTED -> Color(0xFF7C4DFF)
        RoomStatus.COMPLETED -> Color(0xFF2E7D32)
        RoomStatus.CANCELLED -> Color(0xFFB3202E)
        else -> Color.White.copy(alpha = 0.20f)
    }

    Box(
        modifier = Modifier
            .background(color, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            status.ifBlank { "unknown" }.replaceFirstChar { it.uppercase() },
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminRoomDetailsDialog(
    room: AdminRoomItem,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onStatusChange: (String) -> Unit,
    onRequestDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF24192D),
        title = {
            Column {
                Text("Room ${room.code}", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    "Manage status or remove obsolete data",
                    color = Color.White.copy(alpha = 0.68f),
                    fontSize = 12.sp
                )
            }
        },
        text = {
            Column {
                DetailLine("Host", room.hostEmail.ifBlank { room.hostUid })
                DetailLine("Members", "${room.memberCount}/${room.maxMembers}")
                DetailLine("Created", formatAdminDate(room.createdAt, includeTime = true))
                DetailLine("Suggestions", if (room.suggestionsGenerated) "Generated" else "Not generated")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Room status",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(5.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        RoomStatus.WAITING,
                        RoomStatus.STARTED,
                        RoomStatus.COMPLETED,
                        RoomStatus.CANCELLED
                    ).forEach { status ->
                        FilterChip(
                            selected = room.status == status,
                            onClick = { onStatusChange(status) },
                            enabled = enabled && room.status != status,
                            label = {
                                Text(status.replaceFirstChar { it.uppercase() })
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF7C4DFF),
                                selectedLabelColor = Color.White,
                                labelColor = Color.White
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onRequestDelete, enabled = enabled) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF8A80))
                Text(" Delete room", color = Color(0xFFFF8A80))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = enabled) {
                Text("Close", color = Color.White)
            }
        }
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
        Text(value.ifBlank { "Unknown" }, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EmptyRoomsState(
    hasSearch: Boolean,
    onRefresh: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                if (hasSearch) "No matching rooms" else "No rooms found",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (hasSearch) {
                    "Try another code, host or status."
                } else {
                    "New Friends Rooms will appear here from Firestore."
                },
                color = Color.White.copy(alpha = 0.80f),
                fontSize = 13.sp
            )
            Spacer(Modifier.height(12.dp))
            AmoriaGlassButton(
                text = "Refresh rooms",
                onClick = onRefresh,
                enabled = enabled,
                icon = {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                }
            )
        }
    }
}

private fun formatAdminDate(timestamp: Long, includeTime: Boolean = false): String {
    if (timestamp <= 0L) return "Unknown date"
    val formatter = if (includeTime) {
        SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM, SimpleDateFormat.SHORT)
    } else {
        SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM)
    }
    return formatter.format(Date(timestamp))
}

@Composable
private fun adminRoomFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color.White.copy(alpha = 0.55f),
    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
    cursorColor = Color.White,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = Color.White.copy(alpha = 0.10f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.10f)
)
