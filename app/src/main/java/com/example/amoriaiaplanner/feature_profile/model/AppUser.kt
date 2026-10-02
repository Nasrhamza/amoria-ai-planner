package com.example.amoriaiaplanner.feature_profile.model

data class AppUser(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val provider: String = "",
    val role: String = UserRole.USER,
    val isBlocked: Boolean = false,
    val mustChangePassword: Boolean = false,
    val emailVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val profileCompleted: Boolean = false,
    val onboardingCompleted: Boolean = false
)

object UserRole {
    const val USER = "user"
    const val ADMIN = "admin"
}
