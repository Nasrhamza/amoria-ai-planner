package com.example.amoriaiaplanner.feature_couple.model

data class InviteCode(
    val ownerUid: String = "",
    val code: String = "",
    val status: String = "active",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)