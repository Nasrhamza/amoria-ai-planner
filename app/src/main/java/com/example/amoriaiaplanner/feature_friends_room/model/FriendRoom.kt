package com.example.amoriaiaplanner.feature_friends_room.model

data class FriendRoom(
    val roomId: String = "",
    val code: String = "",
    val hostUid: String = "",
    val hostEmail: String = "",
    val status: String = RoomStatus.WAITING,
    val maxMembers: Int = 5,
    val memberCount: Int = 1,
    val quizStartedAt: Long = 0L,
    val quizEndsAt: Long = 0L,
    val suggestionsGenerated: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

object RoomStatus {
    const val WAITING = "waiting"
    const val STARTED = "started"
    const val COMPLETED = "completed"
    const val CANCELLED = "cancelled"
}