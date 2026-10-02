package com.example.amoriaiaplanner.feature_admin.vm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amoriaiaplanner.feature_admin.data.AdminRepository
import com.example.amoriaiaplanner.feature_admin.model.AdminFeatureSettings
import com.example.amoriaiaplanner.feature_admin.model.AdminOperation
import com.example.amoriaiaplanner.feature_admin.model.AdminRoomItem
import com.example.amoriaiaplanner.feature_admin.model.AdminTab
import com.example.amoriaiaplanner.feature_admin.model.AdminUiState
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class AdminViewModel(
    private val repo: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _ui = MutableStateFlow(AdminUiState(loading = true))
    val ui: StateFlow<AdminUiState> = _ui

    init {
        viewModelScope.launch { loadAll(showFullLoader = true) }
    }

    fun setTab(tab: AdminTab) {
        _ui.value = _ui.value.copy(selectedTab = tab, error = null, message = null)
    }

    fun setSearchQuery(query: String) {
        _ui.value = _ui.value.copy(searchQuery = query)
    }

    fun setRoomSearchQuery(query: String) {
        _ui.value = _ui.value.copy(roomSearchQuery = query)
    }

    fun selectRoom(room: AdminRoomItem?) {
        _ui.value = _ui.value.copy(selectedRoom = room)
    }

    fun refresh() {
        if (_ui.value.isBusy) return
        viewModelScope.launch {
            loadAll(showFullLoader = _ui.value.currentUserUid.isBlank())
        }
    }

    fun blockUser(userId: String) {
        runMutation(AdminOperation.UPDATE_USER, "User access blocked.") {
            repo.toggleUserBlocked(userId, true)
        }
    }

    fun unblockUser(userId: String) {
        runMutation(AdminOperation.UPDATE_USER, "User access restored.") {
            repo.toggleUserBlocked(userId, false)
        }
    }

    fun updateUserRole(userId: String, role: String) {
        runMutation(AdminOperation.UPDATE_USER, "User role updated.") {
            repo.updateUserRole(userId, role)
        }
    }

    fun createUser(
        context: Context,
        email: String,
        password: String,
        displayName: String,
        role: String
    ) {
        val successMessage = if (role == com.example.amoriaiaplanner.feature_profile.model.UserRole.ADMIN) {
            "Admin account created. The temporary password must be changed at first login."
        } else {
            "User created. A verification email was sent."
        }
        runMutation(AdminOperation.CREATE_USER, successMessage) {
            repo.createUser(
                context = context,
                email = email,
                password = password,
                displayName = displayName,
                role = role
            )
        }
    }

    fun setFeatureSettings(settings: AdminFeatureSettings) {
        if (_ui.value.operation != null) return
        _ui.value = _ui.value.copy(featureSettings = settings, message = null, error = null)
    }

    fun saveFeatureSettings() {
        val settings = _ui.value.featureSettings
        if (!_ui.value.hasUnsavedFeatureChanges) return
        runMutation(AdminOperation.SAVE_FEATURES, "Application functionalities updated.") {
            repo.updateFeatureSettings(settings)
        }
    }

    fun updateRoomStatus(roomId: String, status: String) {
        runMutation(AdminOperation.UPDATE_ROOM, "Room status updated.") {
            repo.updateRoomStatus(roomId, status)
        }
    }

    fun deleteRoom(roomId: String) {
        runMutation(AdminOperation.DELETE_ROOM, "Room and related data deleted.") {
            repo.deleteRoomCompletely(roomId)
        }
    }

    private fun runMutation(
        operation: AdminOperation,
        successMessage: String,
        action: suspend () -> Unit
    ) {
        if (_ui.value.isBusy) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(
                operation = operation,
                error = null,
                message = null,
                dataWarnings = emptyList()
            )

            try {
                action()
                loadAll(showFullLoader = false, successMessage = successMessage)
            } catch (error: Exception) {
                _ui.value = _ui.value.copy(
                    operation = null,
                    error = friendlyError(error)
                )
            }
        }
    }

    private suspend fun loadAll(
        showFullLoader: Boolean,
        successMessage: String? = null
    ) {
        _ui.value = _ui.value.copy(
            loading = showFullLoader,
            refreshing = !showFullLoader && _ui.value.operation == null,
            error = null,
            message = null,
            dataWarnings = emptyList()
        )

        val adminResult = runCatching { repo.loadCurrentAdminUser() }
        val admin = adminResult.getOrElse { error ->
            _ui.value = _ui.value.copy(
                loading = false,
                refreshing = false,
                operation = null,
                error = friendlyError(error)
            )
            return
        }

        supervisorScope {
            val statsDeferred = async { runCatching { repo.loadDashboardStats() } }
            val usersDeferred = async { runCatching { repo.loadUsers() } }
            val roomsDeferred = async { runCatching { repo.loadRooms() } }
            val featuresDeferred = async { runCatching { repo.loadFeatureSettings() } }

            val statsResult = statsDeferred.await()
            val usersResult = usersDeferred.await()
            val roomsResult = roomsDeferred.await()
            val featuresResult = featuresDeferred.await()
            val warnings = buildList {
                statsResult.exceptionOrNull()?.let { add("Statistics: ${friendlyError(it)}") }
                usersResult.exceptionOrNull()?.let { add("Users: ${friendlyError(it)}") }
                roomsResult.exceptionOrNull()?.let { add("Rooms: ${friendlyError(it)}") }
                featuresResult.exceptionOrNull()?.let { add("Features: ${friendlyError(it)}") }
            }
            val loadedFeatures = featuresResult.getOrNull()

            _ui.value = _ui.value.copy(
                loading = false,
                refreshing = false,
                operation = null,
                currentUserRole = admin.role,
                currentUserUid = admin.uid,
                stats = statsResult.getOrElse { _ui.value.stats },
                users = usersResult.getOrElse { _ui.value.users },
                rooms = roomsResult.getOrElse { _ui.value.rooms },
                selectedRoom = _ui.value.selectedRoom?.let { selected ->
                    roomsResult.getOrNull()?.firstOrNull { it.roomId == selected.roomId }
                },
                featureSettings = loadedFeatures ?: _ui.value.featureSettings,
                savedFeatureSettings = loadedFeatures ?: _ui.value.savedFeatureSettings,
                message = successMessage,
                error = warnings.takeIf { it.isNotEmpty() }
                    ?.joinToString(prefix = "Some data could not be loaded. ", separator = " | "),
                dataWarnings = warnings
            )
        }
    }

    private fun friendlyError(error: Throwable): String {
        val message = error.message?.takeIf { it.isNotBlank() }
        return when {
            error is SecurityException -> message ?: "Administrator access is required."
            message?.contains("PERMISSION_DENIED", ignoreCase = true) == true ->
                "Firestore denied this operation. Deploy the updated security rules."
            else -> message ?: "Admin operation failed."
        }
    }
}
