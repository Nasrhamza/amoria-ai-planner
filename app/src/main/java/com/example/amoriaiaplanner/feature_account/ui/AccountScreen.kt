@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.amoriaiaplanner.feature_account.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amoriaiaplanner.core.ui.AmoriaDangerButton
import com.example.amoriaiaplanner.core.ui.AmoriaPrimaryButton
import com.example.amoriaiaplanner.feature_account.vm.AccountViewModel

@Composable
fun AccountScreen(
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit,
    vm: AccountViewModel = viewModel()
) {
    val ui by vm.ui.collectAsState()
    var confirmDelete by remember { mutableStateOf(false) }
    val background = Brush.linearGradient(
        listOf(Color(0xFFB3202E), Color(0xFF4B1E6D), Color(0xFF0B2B6B))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Manage account", color = Color.White, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF3A1639)
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AccountSummaryCard(
                    displayName = ui.account.displayName,
                    email = ui.account.email,
                    provider = ui.provider
                )

                ui.message?.let {
                    MessageCard(message = it, isError = ui.isError)
                }

                if (ui.provider == "password") {
                    PasswordCard(
                        currentPassword = ui.currentPassword,
                        newPassword = ui.newPassword,
                        confirmPassword = ui.confirmPassword,
                        loading = ui.loading,
                        onCurrentPasswordChange = vm::onCurrentPasswordChange,
                        onNewPasswordChange = vm::onNewPasswordChange,
                        onConfirmPasswordChange = vm::onConfirmPasswordChange,
                        onUpdatePassword = vm::updatePassword
                    )
                } else {
                    MessageCard(
                        message = "Password is managed by ${ui.provider.ifBlank { "your sign-in provider" }}.",
                        isError = false
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFB3202E).copy(alpha = 0.24f)
                    ),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Delete account",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "This permanently removes your application profile and Firebase account.",
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 13.sp
                        )
                        AmoriaDangerButton(
                            text = "Delete my account",
                            onClick = { confirmDelete = true },
                            enabled = !ui.loading,
                            icon = {
                                Icon(
                                    Icons.Default.DeleteForever,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Color(0xFF24192D),
            title = {
                Text("Delete account?", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This action cannot be undone.",
                    color = Color.White.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        vm.deleteAccount(onAccountDeleted)
                    }
                ) {
                    Text("Delete permanently", color = Color(0xFFFF7A8A))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun AccountSummaryCard(
    displayName: String,
    email: String,
    provider: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.10f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                displayName.ifBlank { "Amoria user" },
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(email, color = Color.White.copy(alpha = 0.82f))
            Text(
                "Sign-in provider: $provider",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PasswordCard(
    currentPassword: String,
    newPassword: String,
    confirmPassword: String,
    loading: Boolean,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onUpdatePassword: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.10f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Update password",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            PasswordField(
                value = currentPassword,
                onValueChange = onCurrentPasswordChange,
                label = "Current password"
            )
            PasswordField(
                value = newPassword,
                onValueChange = onNewPasswordChange,
                label = "New password"
            )
            PasswordField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = "Confirm new password"
            )

            Spacer(Modifier.height(2.dp))
            AmoriaPrimaryButton(
                text = if (loading) "Updating..." else "Update password",
                onClick = onUpdatePassword,
                enabled = !loading &&
                    currentPassword.isNotBlank() &&
                    newPassword.length >= 6 &&
                    confirmPassword.isNotBlank()
            )
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, color = Color.White.copy(alpha = 0.75f)) },
        leadingIcon = {
            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White)
        },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.White.copy(alpha = 0.55f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color.White
        ),
        singleLine = true
    )
}

@Composable
private fun MessageCard(message: String, isError: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                Color(0xFFB3202E).copy(alpha = 0.45f)
            } else {
                Color(0xFF2E7D32).copy(alpha = 0.35f)
            }
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            text = message,
            color = Color.White,
            modifier = Modifier.padding(14.dp)
        )
    }
}
