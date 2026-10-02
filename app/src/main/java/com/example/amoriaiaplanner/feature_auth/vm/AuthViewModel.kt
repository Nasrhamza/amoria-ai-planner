package com.example.amoriaiaplanner.feature_auth.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amoriaiaplanner.feature_auth.data.AuthRepository
import com.example.amoriaiaplanner.feature_profile.data.ProfileRepository
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
    val showResendVerification: Boolean = false
)

class AuthViewModel(
    private val repo: AuthRepository = AuthRepository(),
    private val profileRepo: ProfileRepository = ProfileRepository() // ✅ NEW
) : ViewModel() {

    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui

    fun onEmailChange(v: String) {
        _ui.value = _ui.value.copy(email = v, message = null, showResendVerification = false)
    }

    fun onPasswordChange(v: String) {
        _ui.value = _ui.value.copy(password = v, message = null, showResendVerification = false)
    }

    fun refresh() {
        _ui.value = _ui.value.copy(
            password = "",
            message = null,
            isError = false,
            showResendVerification = false,
            loading = false
        )
    }

    fun login(onSuccess: () -> Unit) {
        val email = _ui.value.email.trim()
        val pass = _ui.value.password

        if (email.isBlank() || pass.isBlank()) {
            _ui.value = _ui.value.copy(message = "Email and password are required.", isError = true)
            return
        }

        viewModelScope.launch {
            try {
                _ui.value = _ui.value.copy(loading = true, message = null, showResendVerification = false, isError = false)

                repo.loginAndRequireVerified(email, pass)

                // 🔥 NEW: create Firestore user/profile
                profileRepo.ensureUserDocumentsExist()

                _ui.value = _ui.value.copy(loading = false, isError = false)
                onSuccess()

            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    message = repo.friendlyError(e),
                    showResendVerification = e is com.google.firebase.auth.FirebaseAuthException,
                    isError = true
                )
            }
        }
    }

    fun resendVerification() {
        val email = _ui.value.email.trim()
        val pass = _ui.value.password

        if (email.isBlank() || pass.isBlank()) {
            _ui.value = _ui.value.copy(message = "Enter email + password to resend verification.", isError = true)
            return
        }

        viewModelScope.launch {
            try {
                _ui.value = _ui.value.copy(loading = true, message = null, isError = false)
                repo.resendVerificationWithCredentials(email, pass)
                _ui.value = _ui.value.copy(
                    loading = false,
                    showResendVerification = false,
                    message = "Verification email sent again ✅",
                    isError = false
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    message = repo.friendlyError(e),
                    isError = true
                )
            }
        }
    }

    fun resetPassword() {
        val email = _ui.value.email.trim()
        if (email.isBlank()) {
            _ui.value = _ui.value.copy(message = "Enter your email first.", isError = true)
            return
        }

        viewModelScope.launch {
            try {
                _ui.value = _ui.value.copy(loading = true, message = null, isError = false)
                repo.sendPasswordReset(email)
                _ui.value = _ui.value.copy(
                    loading = false,
                    message = "Reset email sent ✅",
                    isError = false
                )
            } catch (e: Exception) {
                val errorMsg = when (e) {
                    is FirebaseAuthInvalidUserException -> "No account found with this email."
                    else -> repo.friendlyError(e)
                }

                _ui.value = _ui.value.copy(
                    loading = false,
                    message = errorMsg,
                    isError = true
                )
            }
        }
    }

    fun loginWithGoogle(idToken: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                _ui.value = _ui.value.copy(loading = true, message = null, isError = false)

                repo.loginWithGoogleIdToken(idToken)

                // 🔥 NEW
                profileRepo.ensureUserDocumentsExist()

                _ui.value = _ui.value.copy(loading = false, isError = false)
                onSuccess()

            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    message = repo.friendlyError(e),
                    isError = true
                )
            }
        }
    }

    fun logout() {
        repo.logout()
    }
}
