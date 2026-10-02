package com.example.amoriaiaplanner.feature_onboarding.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amoriaiaplanner.feature_onboarding.model.OnboardingUiState
import com.example.amoriaiaplanner.feature_profile.data.ProfileRepository
import com.example.amoriaiaplanner.feature_profile.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val repo: ProfileRepository = ProfileRepository()
) : ViewModel() {

    private val _ui = MutableStateFlow(OnboardingUiState())
    val ui: StateFlow<OnboardingUiState> = _ui

    fun loadExistingProfile() {
        viewModelScope.launch {
            try {
                _ui.value = _ui.value.copy(loading = true, message = null)

                val profile = repo.getCurrentUserProfile()

                if (profile != null) {
                    _ui.value = OnboardingUiState(
                        relationshipStatus = profile.relationshipStatus,
                        relationshipDuration = profile.relationshipDuration,
                        dateFrequency = profile.dateFrequency,
                        budget = profile.budget,
                        lifestyle = profile.lifestyle,
                        transportation = profile.transportation,
                        availability = profile.availability,
                        outingEnergy = profile.outingEnergy,
                        datePreferences = profile.datePreferences,
                        indoorOutdoorPreference = profile.indoorOutdoorPreference,
                        loading = false,
                        message = null
                    )
                } else {
                    _ui.value = _ui.value.copy(loading = false)
                }
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    message = e.message ?: "Failed to load saved profile"
                )
            }
        }
    }

    fun setRelationshipStatus(value: String) {
        _ui.value = _ui.value.copy(relationshipStatus = value, message = null)
    }

    fun setRelationshipDuration(value: String) {
        _ui.value = _ui.value.copy(relationshipDuration = value, message = null)
    }

    fun setDateFrequency(value: String) {
        _ui.value = _ui.value.copy(dateFrequency = value, message = null)
    }

    fun setBudget(value: String) {
        _ui.value = _ui.value.copy(budget = value, message = null)
    }

    fun setLifestyle(value: String) {
        _ui.value = _ui.value.copy(lifestyle = value, message = null)
    }

    fun setTransportation(value: String) {
        _ui.value = _ui.value.copy(transportation = value, message = null)
    }

    fun setAvailability(value: String) {
        _ui.value = _ui.value.copy(availability = value, message = null)
    }

    fun setOutingEnergy(value: String) {
        _ui.value = _ui.value.copy(outingEnergy = value, message = null)
    }

    fun toggleDatePreference(value: String) {
        val current = _ui.value.datePreferences.toMutableList()
        if (current.contains(value)) {
            current.remove(value)
        } else {
            if (current.size < 5) current.add(value)
        }
        _ui.value = _ui.value.copy(datePreferences = current, message = null)
    }

    fun setIndoorOutdoorPreference(value: String) {
        _ui.value = _ui.value.copy(indoorOutdoorPreference = value, message = null)
    }

    fun save(onSuccess: () -> Unit) {
        val state = _ui.value

        if (state.relationshipStatus.isBlank()) {
            _ui.value = state.copy(message = "Please select your situation.")
            return
        }

        if (state.relationshipDuration.isBlank()) {
            _ui.value = state.copy(message = "Please select your relationship duration.")
            return
        }

        if (state.dateFrequency.isBlank()) {
            _ui.value = state.copy(message = "Please select how often you go out together.")
            return
        }

        if (state.budget.isBlank()) {
            _ui.value = state.copy(message = "Please select your usual budget.")
            return
        }

        if (state.lifestyle.isBlank()) {
            _ui.value = state.copy(message = "Please select your lifestyle.")
            return
        }

        if (state.transportation.isBlank()) {
            _ui.value = state.copy(message = "Please select your transportation.")
            return
        }

        if (state.availability.isBlank()) {
            _ui.value = state.copy(message = "Please select your usual availability.")
            return
        }

        if (state.outingEnergy.isBlank()) {
            _ui.value = state.copy(message = "Please select your outing style.")
            return
        }

        if (state.datePreferences.size < 2) {
            _ui.value = state.copy(message = "Please choose at least 2 date preferences.")
            return
        }

        if (state.indoorOutdoorPreference.isBlank()) {
            _ui.value = state.copy(message = "Please select indoor or outdoor preference.")
            return
        }

        viewModelScope.launch {
            try {
                _ui.value = state.copy(loading = true, message = null)

                repo.saveUserProfile(
                    UserProfile(
                        relationshipStatus = state.relationshipStatus,
                        relationshipDuration = state.relationshipDuration,
                        dateFrequency = state.dateFrequency,
                        budget = state.budget,
                        lifestyle = state.lifestyle,
                        transportation = state.transportation,
                        availability = state.availability,
                        outingEnergy = state.outingEnergy,
                        datePreferences = state.datePreferences,
                        indoorOutdoorPreference = state.indoorOutdoorPreference
                    )
                )

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