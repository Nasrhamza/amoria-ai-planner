package com.example.amoriaiaplanner.feature_onboarding.model

data class OnboardingUiState(
    val relationshipStatus: String = "",
    val relationshipDuration: String = "",
    val dateFrequency: String = "",
    val budget: String = "",
    val lifestyle: String = "",
    val transportation: String = "",
    val availability: String = "",
    val outingEnergy: String = "",
    val datePreferences: List<String> = emptyList(),
    val indoorOutdoorPreference: String = "",
    val loading: Boolean = false,
    val message: String? = null
)