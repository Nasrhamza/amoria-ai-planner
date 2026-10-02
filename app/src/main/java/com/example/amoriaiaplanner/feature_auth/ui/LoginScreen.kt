package com.example.amoriaiaplanner.feature_auth.ui

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amoriaiaplanner.R
import com.example.amoriaiaplanner.core.ui.AmoriaGlassButton
import com.example.amoriaiaplanner.core.ui.AmoriaOutlineButton
import com.example.amoriaiaplanner.core.ui.AmoriaPrimaryButton
import com.example.amoriaiaplanner.core.util.UserPrefs
import com.example.amoriaiaplanner.feature_auth.vm.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onGoToSignup: () -> Unit,
    vm: AuthViewModel = viewModel()
) {
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current
    val activity = context as Activity
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    var passwordVisible by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }

    // Prefill saved email
    LaunchedEffect(Unit) {
        val saved = UserPrefs.lastEmailFlow(context).first()
        if (saved.isNotBlank() && ui.email.isBlank()) vm.onEmailChange(saved)
    }

    // Google Sign-In
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(activity, gso) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                vm.loginWithGoogle(idToken) {
                    scope.launch { UserPrefs.saveLastEmail(context, account.email ?: "") }
                    onLoginSuccess()
                }
            }
        } catch (e: Exception) {
            Log.e("GOOGLE_AUTH", "Google sign-in failed", e)
        }
    }

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
                    vm.refresh() // Clear fields except email
                    delay(1000)
                    refreshing = false
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(50.dp))

                Image(
                    painter = painterResource(id = R.drawable.logo_1),
                    contentDescription = "Logo",
                    modifier = Modifier.size(100.dp)
                )

                Spacer(Modifier.height(8.dp))

                Text("Amoria", style = MaterialTheme.typography.displaySmall, color = Color.White)
                Text("AI Planner", style = MaterialTheme.typography.titleMedium, color = Color(0xFFD9B7FF))

                Spacer(Modifier.height(30.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        
                        // Displaying Message Box (Success or Error)
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

                        // Email Field with Clear Button
                        OutlinedTextField(
                            value = ui.email,
                            onValueChange = { vm.onEmailChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Email, null, tint = Color.White.copy(0.85f)) },
                            trailingIcon = {
                                if (ui.email.isNotEmpty()) {
                                    IconButton(onClick = { vm.onEmailChange("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear email",
                                            tint = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            },
                            label = { Text("Email", color = Color.White.copy(0.8f)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = fieldColors()
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = ui.password,
                            onValueChange = { vm.onPasswordChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color.White.copy(0.85f)) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle Password",
                                        tint = Color.White.copy(0.7f)
                                    )
                                }
                            },
                            label = { Text("Password", color = Color.White.copy(0.8f)) },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = fieldColors()
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { vm.resetPassword() }) {
                                Text("Forgot password?", color = Color.White.copy(0.85f))
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        AmoriaPrimaryButton(
                            text = if (ui.loading) "Logging in..." else "Log in",
                            onClick = {
                                vm.login {
                                    scope.launch { UserPrefs.saveLastEmail(context, ui.email.trim()) }
                                    onLoginSuccess()
                                }
                            },
                            enabled = !ui.loading
                        )

                        if (ui.showResendVerification) {
                            Spacer(Modifier.height(8.dp))
                            TextButton(
                                onClick = { vm.resendVerification() },
                                enabled = !ui.loading,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Resend verification email", color = Color.White)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Stylish Google Button
                        AmoriaOutlineButton(
                            text = "Sign in with Google",
                            onClick = {
                                googleSignInClient.signOut().addOnCompleteListener {
                                    launcher.launch(googleSignInClient.signInIntent)
                                }
                            },
                            enabled = !ui.loading,
                            icon = {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_google),
                                    contentDescription = "Google",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )

                        Spacer(Modifier.height(16.dp))

                        AmoriaGlassButton(
                            text = "Create account",
                            onClick = onGoToSignup,
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
    unfocusedTextColor = Color.White,
    focusedTrailingIconColor = Color.White,
    unfocusedTrailingIconColor = Color.White.copy(alpha = 0.7f)
)
