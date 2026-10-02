package com.example.amoriaiaplanner.feature_couple.model

data class Couple(
    val coupleId: String = "",
    val user1Uid: String = "",
    val user2Uid: String = "",
    val user1Email: String = "",
    val user2Email: String = "",
    val status: String = "active",
    val linkedAt: Long = System.currentTimeMillis()
)