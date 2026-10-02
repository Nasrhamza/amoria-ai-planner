package com.example.amoriaiaplanner.feature_friends_room.model

data class RoomMember(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val isHost: Boolean = false,
    val joinedAt: Long = System.currentTimeMillis(),
    val hasFinishedQuiz: Boolean = false
)