package com.example.amoriaiaplanner.feature_auth.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amoriaiaplanner.feature_auth.data.AuthRepository
import com.example.amoriaiaplanner.feature_profile.data.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ForcePasswordChangeUiState(
    val newPassword: String = "",
    val confirmPassword: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val passwordUpdated: Boolean = false
)

class ForcePasswordChangeViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val profileRepository: ProfileRepository = ProfileRepository()
) : ViewModel() {

    private val _ui = MutableStateFlow(ForcePasswordChangeUiState())
    val ui: StateFlow<ForcePasswordChangeUiState> = _ui

    fun setNewPassword(value: String) {
        _ui.value = _ui.value.copy(newPassword = value, error = null)
    }

    fun setConfirmPassword(value: String) {
        _ui.value = _ui.value.copy(confirmPassword = value, error = null)
    }

    fun submit(onSuccess: () -> Unit) {
        val state = _ui.value
        when {
            state.newPassword.length < MIN_PASSWORD_LENGTH -> {
                showError("Use at least $MIN_PASSWORD_LENGTH characters.")
                return
            }
            state.newPassword != state.confirmPassword -> {
                showError("Passwords do not match.")
                return
            }
            state.loading -> return
        }

        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null)
            try {
                if (!_ui.value.passwordUpdated) {
                    authRepository.updateCurrentUserPassword(_ui.value.newPassword)
                    _ui.value = _ui.value.copy(passwordUpdated = true)
                }
                profileRepository.completeForcedPasswordChange()
                _ui.value = _ui.value.copy(loading = false)
                onSuccess()
            } catch (error: Exception) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    error = if (_ui.value.passwordUpdated) {
                        "The password was updated, but account activation could not finish. Check the connection and try again."
                    } else {
                        authRepository.friendlyError(error)
                    }
                )
            }
        }
    }

    private fun showError(message: String) {
        _ui.value = _ui.value.copy(error = message)
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 8
    }
}
