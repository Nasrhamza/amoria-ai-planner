package com.example.amoriaiaplanner.feature_account.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amoriaiaplanner.feature_account.data.AccountRepository
import com.example.amoriaiaplanner.feature_profile.model.AppUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AccountUiState(
    val loading: Boolean = true,
    val account: AppUser = AppUser(),
    val provider: String = "password",
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val message: String? = null,
    val isError: Boolean = false
)

class AccountViewModel(
    private val repository: AccountRepository = AccountRepository()
) : ViewModel() {
    private val _ui = MutableStateFlow(AccountUiState())
    val ui: StateFlow<AccountUiState> = _ui

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, message = null, isError = false)
            try {
                _ui.value = _ui.value.copy(
                    loading = false,
                    account = repository.loadAccount(),
                    provider = repository.primaryProvider()
                )
            } catch (error: Exception) {
                showError(error)
            }
        }
    }

    fun onCurrentPasswordChange(value: String) {
        _ui.value = _ui.value.copy(currentPassword = value, message = null)
    }

    fun onNewPasswordChange(value: String) {
        _ui.value = _ui.value.copy(newPassword = value, message = null)
    }

    fun onConfirmPasswordChange(value: String) {
        _ui.value = _ui.value.copy(confirmPassword = value, message = null)
    }

    fun updatePassword() {
        val state = _ui.value
        if (state.newPassword != state.confirmPassword) {
            _ui.value = state.copy(
                message = "New passwords do not match.",
                isError = true
            )
            return
        }

        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, message = null, isError = false)
            try {
                repository.updatePassword(
                    currentPassword = state.currentPassword,
                    newPassword = state.newPassword
                )
                _ui.value = _ui.value.copy(
                    loading = false,
                    currentPassword = "",
                    newPassword = "",
                    confirmPassword = "",
                    message = "Password updated successfully.",
                    isError = false
                )
            } catch (error: Exception) {
                showError(error)
            }
        }
    }

    fun deleteAccount(onDeleted: () -> Unit) {
        val currentPassword = _ui.value.currentPassword
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, message = null, isError = false)
            try {
                repository.deleteAccount(currentPassword)
                _ui.value = _ui.value.copy(loading = false)
                onDeleted()
            } catch (error: Exception) {
                showError(error)
            }
        }
    }

    private fun showError(error: Exception) {
        _ui.value = _ui.value.copy(
            loading = false,
            message = error.message ?: "Account operation failed.",
            isError = true
        )
    }
}
