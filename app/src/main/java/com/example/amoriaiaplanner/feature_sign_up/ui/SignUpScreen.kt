package com.example.amoriaiaplanner.feature_sign_up.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amoriaiaplanner.core.ui.AmoriaGlassButton
import com.example.amoriaiaplanner.core.ui.AmoriaPrimaryButton
import com.example.amoriaiaplanner.feature_sign_up.vm.SignUpViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onBackToLogin: () -> Unit,
    onSignUpSuccess: (email: String) -> Unit,
    vm: SignUpViewModel = viewModel()
) {
    val ui by vm.ui.collectAsState()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }

    val bg = Brush.linearGradient(
        listOf(Color(0xFFB3202E), Color(0xFF4B1E6D), Color(0xFF0B2B6B))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                scope.launch {
                    refreshing = true
                    vm.refresh() // Call the refresh logic in ViewModel
                    delay(1000)
                    refreshing = false
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(22.dp)
            ) {
                Spacer(Modifier.height(50.dp))

                Text("Sign Up", style = MaterialTheme.typography.displaySmall, color = Color.White)

                Spacer(Modifier.height(30.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(Modifier.padding(18.dp)) {

                        // Message Box (Success or Error)
                        AnimatedVisibility(visible = ui.message != null) {
                            val bgColor = if (ui.isError) Color(0xFFB3202E).copy(alpha = 0.8f) else Color(0xFF2E7D32).copy(alpha = 0.8f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .background(bgColor, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text(ui.message ?: "", color = Color.White, fontSize = 14.sp)
                            }
                        }

                        OutlinedTextField(
                            value = ui.email,
                            onValueChange = vm::onEmailChange,
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Email, null, tint = Color.White.copy(0.85f)) },
                            label = { Text("Email", color = Color.White.copy(0.8f)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = fieldColors()
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = ui.password,
                            onValueChange = vm::onPasswordChange,
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color.White.copy(0.85f)) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Hide Password" else "Show Password",
                                        tint = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            },
                            label = { Text("Password", color = Color.White.copy(0.8f)) },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = fieldColors()
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = ui.confirmPassword,
                            onValueChange = vm::onConfirmChange,
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color.White.copy(0.85f)) },
                            trailingIcon = {
                                IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                    Icon(
                                        imageVector = if (confirmVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (confirmVisible) "Hide Password" else "Show Password",
                                        tint = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            },
                            label = { Text("Confirm Password", color = Color.White.copy(0.8f)) },
                            singleLine = true,
                            visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = fieldColors()
                        )

                        Spacer(Modifier.height(24.dp))

                        AmoriaPrimaryButton(
                            text = if (ui.loading) "Creating..." else "Create account",
                            onClick = { vm.signUp(onSignUpSuccess) },
                            enabled = !ui.loading
                        )

                        if (ui.showResend) {
                            Spacer(Modifier.height(12.dp))
                            TextButton(
                                onClick = { vm.resendVerification() },
                                enabled = !ui.loading,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Resend verification email", color = Color.White)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        AmoriaGlassButton(
                            text = "Back to login",
                            onClick = onBackToLogin,
                            enabled = !ui.loading
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color.White.copy(0.55f),
    unfocusedBorderColor = Color.White.copy(0.25f),
    cursorColor = Color.White,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)
