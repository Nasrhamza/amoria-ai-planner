package com.example.amoriaiaplanner.feature_friends_room.model

data class FriendsQuizAnswer(
    val uid: String = "",
    val email: String = "",

    val groupMood: String = "",
    val budget: String = "",
    val outingType: String = "",
    val foodPreference: String = "",
    val energyLevel: String = "",
    val indoorOutdoor: String = "",
    val distancePreference: String = "",
    val timePreference: String = "",
    val activityPreferences: List<String> = emptyList(),
    val dealBreaker: String = "",

    val finishedAt: Long = System.currentTimeMillis()
)