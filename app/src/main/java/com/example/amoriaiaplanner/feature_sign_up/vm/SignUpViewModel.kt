package com.example.amoriaiaplanner.feature_sign_up.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amoriaiaplanner.feature_auth.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val loading: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
    val showResend: Boolean = false
)

class SignUpViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _ui = MutableStateFlow(SignUpUiState())
    val ui: StateFlow<SignUpUiState> = _ui

    fun onEmailChange(v: String) { _ui.value = _ui.value.copy(email = v, message = null, showResend = false) }
    fun onPasswordChange(v: String) { _ui.value = _ui.value.copy(password = v, message = null, showResend = false) }
    fun onConfirmChange(v: String) { _ui.value = _ui.value.copy(confirmPassword = v, message = null, showResend = false) }

    fun refresh() {
        _ui.value = _ui.value.copy(
            password = "",
            confirmPassword = "",
            message = null,
            isError = false,
            showResend = false,
            loading = false
        )
    }

    fun signUp(onSuccessBackToLogin: (email: String) -> Unit) {
        val email = _ui.value.email.trim()
        val pass = _ui.value.password
        val confirm = _ui.value.confirmPassword

        if (email.isBlank()) { _ui.value = _ui.value.copy(message = "Enter a valid email.", isError = true); return }
        if (pass.length < 6) { _ui.value = _ui.value.copy(message = "Password must be 6+ characters.", isError = true); return }
        if (pass != confirm) { _ui.value = _ui.value.copy(message = "Passwords do not match.", isError = true); return }

        viewModelScope.launch {
            try {
                _ui.value = _ui.value.copy(loading = true, message = null, isError = false, showResend = false)

                repo.signupAndSendVerification(email, pass)

                _ui.value = _ui.value.copy(
                    loading = false,
                    message = "Account created! Verification email sent ✅ Please verify, then login.",
                    isError = false,
                    showResend = true
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(loading = false, message = repo.friendlyError(e), isError = true)
            }
        }
    }

    fun resendVerification() {
        val email = _ui.value.email.trim()
        val pass = _ui.value.password

        if (email.isBlank() || pass.isBlank()) {
            _ui.value = _ui.value.copy(message = "Enter email and password to resend.", isError = true)
            return
        }

        viewModelScope.launch {
            try {
                _ui.value = _ui.value.copy(loading = true, message = null, isError = false)
                repo.resendVerificationWithCredentials(email, pass)
                _ui.value = _ui.value.copy(
                    loading = false,
                    message = "Verification email sent again ✅ Check your inbox.",
                    isError = false
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(loading = false, message = repo.friendlyError(e), isError = true)
            }
        }
    }
}
