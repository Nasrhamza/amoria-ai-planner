package com.example.amoriaiaplanner.feature_auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amoriaiaplanner.R
import com.example.amoriaiaplanner.core.ui.AmoriaGlassButton
import com.example.amoriaiaplanner.core.ui.AmoriaPrimaryButton
import com.example.amoriaiaplanner.feature_auth.vm.ForcePasswordChangeViewModel

@Composable
fun ForcePasswordChangeScreen(
    onPasswordChanged: () -> Unit,
    onLogout: () -> Unit,
    vm: ForcePasswordChangeViewModel = viewModel()
) {
    val ui by vm.ui.collectAsState()
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmation by remember { mutableStateOf(false) }
    val background = Brush.linearGradient(
        listOf(Color(0xFFB3202E), Color(0xFF4B1E6D), Color(0xFF0B2B6B))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(46.dp))
            Image(
                painter = painterResource(R.drawable.logo_1),
                contentDescription = "Amoria",
                modifier = Modifier.size(88.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Secure your admin account",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "This is your first login. Replace the temporary password before opening the Admin Dashboard.",
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 14.sp
            )
            Spacer(Modifier.height(22.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.10f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ui.error?.let { message ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color(0xFFB3202E).copy(alpha = 0.72f),
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(message, color = Color.White, fontSize = 13.sp)
                        }
                    }

                    AdminPasswordField(
                        value = ui.newPassword,
                        onValueChange = vm::setNewPassword,
                        label = "New password",
                        visible = showNewPassword,
                        onToggleVisibility = { showNewPassword = !showNewPassword },
                        enabled = !ui.loading && !ui.passwordUpdated
                    )
                    AdminPasswordField(
                        value = ui.confirmPassword,
                        onValueChange = vm::setConfirmPassword,
                        label = "Confirm new password",
                        visible = showConfirmation,
                        onToggleVisibility = { showConfirmation = !showConfirmation },
                        enabled = !ui.loading && !ui.passwordUpdated
                    )
                    Text(
                        "Use at least 8 characters. You will use this password for future admin logins.",
                        color = Color.White.copy(alpha = 0.70f),
                        fontSize = 12.sp
                    )

                    AmoriaPrimaryButton(
                        text = if (ui.loading) "Securing account..." else "Change password and continue",
                        onClick = { vm.submit(onPasswordChanged) },
                        enabled = !ui.loading
                    )
                    AmoriaGlassButton(
                        text = "Sign out",
                        onClick = onLogout,
                        enabled = !ui.loading
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AdminPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
    enabled: Boolean
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        leadingIcon = {
            Icon(Icons.Default.Lock, contentDescription = null)
        },
        trailingIcon = {
            IconButton(onClick = onToggleVisibility, enabled = enabled) {
                Icon(
                    if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Hide password" else "Show password"
                )
            }
        },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.White.copy(alpha = 0.60f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.28f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedLabelColor = Color.White,
            unfocusedLabelColor = Color.White.copy(alpha = 0.72f),
            focusedLeadingIconColor = Color.White,
            unfocusedLeadingIconColor = Color.White.copy(alpha = 0.72f),
            focusedTrailingIconColor = Color.White,
            unfocusedTrailingIconColor = Color.White.copy(alpha = 0.72f)
        )
    )
}
