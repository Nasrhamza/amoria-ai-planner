package com.example.amoriaiaplanner.feature_admin.model

import com.example.amoriaiaplanner.core.config.AppFeatureSettings
import com.example.amoriaiaplanner.feature_profile.model.UserRole

enum class AdminTab {
    OVERVIEW,
    USERS,
    ROOMS,
    FEATURES
}

enum class AdminOperation {
    CREATE_USER,
    UPDATE_USER,
    UPDATE_ROOM,
    DELETE_ROOM,
    SAVE_FEATURES
}

data class AdminDashboardStats(
    val totalUsers: Int = 0,
    val totalRooms: Int = 0,
    val activeRooms: Int = 0,
    val totalSuggestions: Int? = null,
    val totalRoomMembers: Int = 0,
    val averageMembersPerRoom: Double = 0.0
)

typealias AdminFeatureSettings = AppFeatureSettings

data class AdminUserItem(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = UserRole.USER,
    val isBlocked: Boolean = false,
    val mustChangePassword: Boolean = false,
    val emailVerified: Boolean = false,
    val profileCompleted: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val provider: String = "",
    val createdAt: Long = 0L
)

data class AdminRoomItem(
    val roomId: String = "",
    val code: String = "",
    val hostUid: String = "",
    val hostEmail: String = "",
    val status: String = "",
    val memberCount: Int = 0,
    val maxMembers: Int = 0,
    val suggestionsGenerated: Boolean = false,
    val createdAt: Long = 0L
)

data class AdminUiState(
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val operation: AdminOperation? = null,
    val error: String? = null,
    val message: String? = null,
    val dataWarnings: List<String> = emptyList(),
    val selectedTab: AdminTab = AdminTab.OVERVIEW,
    val searchQuery: String = "",
    val roomSearchQuery: String = "",
    val stats: AdminDashboardStats = AdminDashboardStats(),
    val users: List<AdminUserItem> = emptyList(),
    val rooms: List<AdminRoomItem> = emptyList(),
    val selectedRoom: AdminRoomItem? = null,
    val currentUserRole: String = UserRole.USER,
    val currentUserUid: String = "",
    val featureSettings: AdminFeatureSettings = AdminFeatureSettings(),
    val savedFeatureSettings: AdminFeatureSettings = AdminFeatureSettings()
) {
    val isBusy: Boolean
        get() = loading || refreshing || operation != null

    val hasUnsavedFeatureChanges: Boolean
        get() = featureSettings != savedFeatureSettings
}
