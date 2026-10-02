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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amoriaiaplanner.core.ui.AmoriaGlassButton
import com.example.amoriaiaplanner.core.ui.AmoriaPrimaryButton
import com.example.amoriaiaplanner.feature_admin.model.AdminUiState
import com.example.amoriaiaplanner.feature_admin.model.AdminUserItem
import com.example.amoriaiaplanner.feature_profile.model.UserRole

private data class PendingAccessChange(
    val user: AdminUserItem,
    val block: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    ui: AdminUiState,
    onSearchChange: (String) -> Unit,
    onBlockUser: (String) -> Unit,
    onUnblockUser: (String) -> Unit,
    onRoleChange: (String, String) -> Unit,
    onCreateUser: (String, String, String, String) -> Unit,
    onRefresh: () -> Unit
) {
    var showCreateUser by remember { mutableStateOf(false) }
    var pendingAccessChange by remember { mutableStateOf<PendingAccessChange?>(null) }
    var pendingRoleChange by remember { mutableStateOf<AdminUserItem?>(null) }
    val query = ui.searchQuery.trim()
    val filteredUsers = ui.users.filter { user ->
        query.isBlank() ||
            user.email.contains(query, ignoreCase = true) ||
            user.displayName.contains(query, ignoreCase = true)
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "User management",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${ui.users.size} accounts | ${ui.users.count { !it.isBlocked }} active",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp
                )
            }
            TextButton(onClick = onRefresh, enabled = !ui.isBusy) {
                Text("Refresh", color = Color.White)
            }
        }

        Spacer(Modifier.height(12.dp))

        AmoriaPrimaryButton(
            text = "Create user",
            onClick = { showCreateUser = true },
            enabled = !ui.isBusy,
            icon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = ui.searchQuery,
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
                Text("Search by name or email", color = Color.White.copy(alpha = 0.7f))
            },
            singleLine = true,
            colors = adminFieldColors(),
            shape = RoundedCornerShape(22.dp)
        )

        Spacer(Modifier.height(12.dp))

        if (filteredUsers.isEmpty()) {
            EmptyUsersState(hasSearch = query.isNotBlank())
            Spacer(Modifier.height(12.dp))
            AmoriaGlassButton(
                text = "Refresh users",
                onClick = onRefresh,
                enabled = !ui.isBusy
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredUsers.forEach { user ->
                    androidx.compose.runtime.key(user.uid) {
                        AdminUserCard(
                            user = user,
                            isCurrentUser = user.uid == ui.currentUserUid,
                            enabled = !ui.isBusy,
                            onAccessChange = { block ->
                                pendingAccessChange = PendingAccessChange(user, block)
                            },
                            onRoleChange = { pendingRoleChange = user }
                        )
                    }
                }
            }
        }
    }

    if (showCreateUser) {
        CreateUserDialog(
            loading = ui.isBusy,
            onDismiss = { showCreateUser = false },
            onCreate = { email, password, displayName, role ->
                showCreateUser = false
                onCreateUser(email, password, displayName, role)
            }
        )
    }

    pendingAccessChange?.let { pending ->
        AlertDialog(
            onDismissRequest = { pendingAccessChange = null },
            containerColor = Color(0xFF24192D),
            title = {
                Text(
                    if (pending.block) "Block user access?" else "Restore user access?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (pending.block) {
                        "${pending.user.displayName.ifBlank { pending.user.email }} will be signed out of the application on the next account check."
                    } else {
                        "${pending.user.displayName.ifBlank { pending.user.email }} will be allowed to use the application again."
                    },
                    color = Color.White.copy(alpha = 0.82f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingAccessChange = null
                        if (pending.block) onBlockUser(pending.user.uid) else onUnblockUser(pending.user.uid)
                    }
                ) {
                    Text(if (pending.block) "Block access" else "Restore access", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAccessChange = null }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.78f))
                }
            }
        )
    }

    pendingRoleChange?.let { user ->
        val newRole = if (user.role == UserRole.ADMIN) UserRole.USER else UserRole.ADMIN
        AlertDialog(
            onDismissRequest = { pendingRoleChange = null },
            containerColor = Color(0xFF24192D),
            title = {
                Text(
                    if (newRole == UserRole.ADMIN) "Promote to administrator?" else "Remove administrator role?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (newRole == UserRole.ADMIN) {
                        "This account will receive access to all administrator operations."
                    } else {
                        "This account will return to the normal user application."
                    },
                    color = Color.White.copy(alpha = 0.82f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRoleChange = null
                        onRoleChange(user.uid, newRole)
                    }
                ) {
                    Text("Confirm", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRoleChange = null }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.78f))
                }
            }
        )
    }
}

@Composable
private fun CreateUserDialog(
    loading: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String) -> Unit
) {
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.USER) }
    val canCreate = email.trim().isNotBlank() && password.length >= 8 && !loading

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF24192D),
        title = {
            Text("Create user", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (role == UserRole.ADMIN) {
                        "The administrator must replace this temporary password at first login."
                    } else {
                        "The account will open the normal user application."
                    },
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp
                )
                Text(
                    "Account type",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        UserRole.USER to "User",
                        UserRole.ADMIN to "Admin"
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = role == value,
                            onClick = { role = value },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF7C4DFF),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Display name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Temporary password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    supportingText = { Text("Minimum 8 characters") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(email.trim(), password, displayName.trim(), role) },
                enabled = canCreate
            ) {
                Text("Create", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) {
                Text("Cancel", color = Color.White.copy(alpha = 0.8f))
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminUserCard(
    user: AdminUserItem,
    isCurrentUser: Boolean,
    enabled: Boolean,
    onAccessChange: (Boolean) -> Unit,
    onRoleChange: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.10f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.displayName.ifBlank { "Unnamed user" },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = user.email.ifBlank { "No email" },
                        color = Color.White.copy(alpha = 0.80f),
                        fontSize = 13.sp
                    )
                }

                AssistChip(
                    onClick = {},
                    label = { Text(user.role.replaceFirstChar { it.uppercase() }, color = Color.White) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (user.role == UserRole.ADMIN) {
                                Icons.Default.VerifiedUser
                            } else {
                                Icons.Default.Person
                            },
                            contentDescription = null,
                            tint = Color.White
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (user.role == UserRole.ADMIN) {
                            Color(0xFF7C4DFF).copy(alpha = 0.42f)
                        } else {
                            Color.White.copy(alpha = 0.14f)
                        }
                    )
                )
            }

            Spacer(Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                StatusChip(
                    text = if (user.isBlocked) "Blocked" else "Active",
                    color = if (user.isBlocked) Color(0xFFB3202E) else Color(0xFF2E7D32)
                )
                StatusChip(
                    text = if (user.emailVerified) "Email verified" else "Email pending",
                    color = Color.White.copy(alpha = 0.18f)
                )
                if (user.mustChangePassword) {
                    StatusChip(
                        text = "Password change required",
                        color = Color(0xFFB26A00)
                    )
                }
                StatusChip(
                    text = if (user.profileCompleted) "Profile ready" else "Profile pending",
                    color = Color.White.copy(alpha = 0.18f)
                )
                StatusChip(
                    text = if (user.onboardingCompleted) "Onboarding done" else "Onboarding pending",
                    color = Color.White.copy(alpha = 0.18f)
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = "Provider: ${user.provider.ifBlank { "unknown" }}",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 12.sp
            )

            if (!isCurrentUser) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextButton(
                        onClick = { onAccessChange(!user.isBlocked) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (user.isBlocked) "Unblock" else "Block",
                            color = Color.White
                        )
                    }
                    TextButton(
                        onClick = onRoleChange,
                        enabled = enabled,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Text(
                            if (user.role == UserRole.ADMIN) " Make user" else " Make admin",
                            color = Color.White
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Current admin account | self-blocking and self-demotion are protected.",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EmptyUsersState(hasSearch: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                if (hasSearch) "No matching users" else "No users found",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (hasSearch) {
                    "Try a different name or email."
                } else {
                    "Refresh the list after checking the Firestore users collection."
                },
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun adminFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color.White.copy(alpha = 0.55f),
    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
    cursorColor = Color.White,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = Color.White.copy(alpha = 0.10f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.10f)
)
