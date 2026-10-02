package com.example.amoriaiaplanner.feature_profile.model

data class UserProfile(
    val uid: String = "",
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
    val bio: String = "",
    val photosCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)