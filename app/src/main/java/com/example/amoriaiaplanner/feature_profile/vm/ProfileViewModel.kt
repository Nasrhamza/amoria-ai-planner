package com.example.amoriaiaplanner.feature_profile.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amoriaiaplanner.feature_profile.data.ProfileRepository
import com.example.amoriaiaplanner.feature_profile.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val loading: Boolean = false,
    val profile: UserProfile = UserProfile(),
    val message: String? = null
)

class ProfileViewModel(
    private val repo: ProfileRepository = ProfileRepository()
) : ViewModel() {

    private val _ui = MutableStateFlow(ProfileUiState())
    val ui: StateFlow<ProfileUiState> = _ui

    fun ensureUserSetup() {
        viewModelScope.launch {
            try {
                _ui.value = _ui.value.copy(loading = true, message = null)
                repo.ensureUserDocumentsExist()
                val profile = repo.getCurrentUserProfile() ?: UserProfile()
                _ui.value = _ui.value.copy(loading = false, profile = profile)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    message = e.message ?: "Failed to load profile"
                )
            }
        }
    }

    fun updateRelationshipStatus(value: String) {
        _ui.value = _ui.value.copy(
            profile = _ui.value.profile.copy(relationshipStatus = value)
        )
    }

    fun updateRelationshipDuration(value: String) {
        _ui.value = _ui.value.copy(
            profile = _ui.value.profile.copy(relationshipDuration = value)
        )
    }

    fun updateDateFrequency(value: String) {
        _ui.value = _ui.value.copy(
            profile = _ui.value.profile.copy(dateFrequency = value)
        )
    }

    fun updateBudget(value: String) {
        _ui.value = _ui.value.copy(
            profile = _ui.value.profile.copy(budget = value)
        )
    }

    fun updateLifestyle(value: String) {
        _ui.value = _ui.value.copy(
            profile = _ui.value.profile.copy(lifestyle = value)
        )
    }

    fun updateTransportation(value: String) {
        _ui.value = _ui.value.copy(
            profile = _ui.value.profile.copy(transportation = value)
        )
    }

    fun updateAvailability(value: String) {
        _ui.value = _ui.value.copy(
            profile = _ui.value.profile.copy(availability = value)
        )
    }

    fun updateOutingEnergy(value: String) {
        _ui.value = _ui.value.copy(
            profile = _ui.value.profile.copy(outingEnergy = value)
        )
    }

    fun updateDatePreferences(value: List<String>) {
        _ui.value = _ui.value.copy(
            profile = _ui.value.profile.copy(datePreferences = value)
        )
    }

    fun updateIndoorOutdoorPreference(value: String) {
        _ui.value = _ui.value.copy(
            profile = _ui.value.profile.copy(indoorOutdoorPreference = value)
        )
    }

    fun saveProfile(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                _ui.value = _ui.value.copy(loading = true, message = null)
                repo.saveUserProfile(_ui.value.profile)
                _ui.value = _ui.value.copy(
                    loading = false,
                    message = "Profile saved ✅"
                )
                onSuccess()
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    message = e.message ?: "Failed to save profile"
                )
            }
        }
    }
}