package com.example.amoriaiaplanner.feature_friends_room.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amoriaiaplanner.feature_friends_room.data.FriendsRoomRepository
import com.example.amoriaiaplanner.feature_friends_room.model.FriendRoom
import com.example.amoriaiaplanner.feature_friends_room.model.RoomMember
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.content.Context
import com.example.amoriaiaplanner.feature_ai.data.LocationRepository
import com.example.amoriaiaplanner.feature_ai.data.OsmPlacesRepository

data class FriendsRoomUiState(
    val myUid: String = "",
    val myEmail: String = "",
    val enteredRoomCode: String = "",
    val activeRoom: FriendRoom? = null,
    val members: List<RoomMember> = emptyList(),
    val loading: Boolean = false,
    val message: String? = null,
    val extraPreference: String = "",
    val useLocation: Boolean = false,
    val nearbyPlacesCount: Int = 0,
)

class FriendsRoomViewModel(
    private val repo: FriendsRoomRepository = FriendsRoomRepository()
) : ViewModel() {

    private val _ui = MutableStateFlow(FriendsRoomUiState())
    val ui: StateFlow<FriendsRoomUiState> = _ui

    private var activeRoomListener: ListenerRegistration? = null
    private var roomListener: ListenerRegistration? = null
    private var membersListener: ListenerRegistration? = null

    private val locationRepo = LocationRepository()
    private val placesRepo = OsmPlacesRepository()

    fun onExtraPreferenceChange(value: String) {
        _ui.value = _ui.value.copy(extraPreference = value, message = null)
    }

    fun setUseLocation(enabled: Boolean) {
        _ui.value = _ui.value.copy(
            useLocation = enabled,
            nearbyPlacesCount = if (!enabled) 0 else _ui.value.nearbyPlacesCount,
            message = if (enabled) "Location will improve group suggestions." else "Location disabled."
        )
    }

    fun load() {
        _ui.value = _ui.value.copy(
            myUid = repo.currentUid(),
            myEmail = repo.currentEmail()
        )

        startActiveRoomListener()
    }

    fun onRoomCodeChange(value: String) {
        val cleanValue = value.filter { it.isDigit() }.take(6)
        _ui.value = _ui.value.copy(
            enteredRoomCode = cleanValue,
            message = null
        )
    }

    fun createRoom() {
        viewModelScope.launch {
            try {
                _ui.value = _ui.value.copy(
                    loading = true,
                    message = null
                )

                val room = repo.createRoom()

                _ui.value = _ui.value.copy(
                    loading = false,
                    activeRoom = room,
                    enteredRoomCode = "",
                    message = "Room created"
                )

                startRoomListeners(room.roomId)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    message = e.message ?: "Failed to create room"
                )
            }
        }
    }

    fun joinRoom() {
        viewModelScope.launch {
            try {
                val code = _ui.value.enteredRoomCode

                _ui.value = _ui.value.copy(
                    loading = true,
                    message = null
                )

                val room = repo.joinRoomByCode(code)

                _ui.value = _ui.value.copy(
                    loading = false,
                    activeRoom = room,
                    enteredRoomCode = "",
                    message = "Joined room"
                )

                startRoomListeners(room.roomId)
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    message = e.message ?: "Failed to join room"
                )
            }
        }
    }

    fun startRoom() {
        viewModelScope.launch {
            try {
                val roomId = _ui.value.activeRoom?.roomId.orEmpty()

                _ui.value = _ui.value.copy(
                    loading = true,
                    message = null
                )

                repo.startRoom(roomId)

                _ui.value = _ui.value.copy(
                    loading = false,
                    message = "Room started"
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    message = e.message ?: "Failed to start room"
                )
            }
        }
    }

    fun leaveRoom() {
        viewModelScope.launch {
            try {
                val roomId = _ui.value.activeRoom?.roomId.orEmpty()

                _ui.value = _ui.value.copy(
                    loading = true,
                    message = null
                )

                repo.leaveRoom(roomId)

                clearRoomListeners()

                _ui.value = _ui.value.copy(
                    loading = false,
                    activeRoom = null,
                    members = emptyList(),
                    enteredRoomCode = "",
                    message = "Left room"
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    message = e.message ?: "Failed to leave room"
                )
            }
        }
    }

    fun isHost(): Boolean {
        val room = _ui.value.activeRoom ?: return false
        return room.hostUid == _ui.value.myUid
    }

    private fun startActiveRoomListener() {
        activeRoomListener?.remove()

        try {
            activeRoomListener = repo.observeCurrentUserActiveRoomId(
                onChanged = { roomId ->
                    if (roomId.isNullOrBlank()) {
                        clearRoomListeners()
                        _ui.value = _ui.value.copy(
                            activeRoom = null,
                            members = emptyList()
                        )
                    } else {
                        startRoomListeners(roomId)
                    }
                },
                onError = { e ->
                    _ui.value = _ui.value.copy(
                        message = e.message ?: "Failed to listen to active room"
                    )
                }
            )
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(
                message = e.message ?: "Failed to start room listener"
            )
        }
    }

    private fun startRoomListeners(roomId: String) {
        if (roomId.isBlank()) return

        roomListener?.remove()
        membersListener?.remove()

        roomListener = repo.observeRoom(
            roomId = roomId,
            onChanged = { room ->
                _ui.value = _ui.value.copy(activeRoom = room)
            },
            onError = { e ->
                _ui.value = _ui.value.copy(
                    message = e.message ?: "Failed to listen to room"
                )
            }
        )

        membersListener = repo.observeMembers(
            roomId = roomId,
            onChanged = { members ->
                _ui.value = _ui.value.copy(members = members)
            },
            onError = { e ->
                _ui.value = _ui.value.copy(
                    message = e.message ?: "Failed to listen to members"
                )
            }
        )
    }

    private fun clearRoomListeners() {
        roomListener?.remove()
        roomListener = null

        membersListener?.remove()
        membersListener = null
    }

    override fun onCleared() {
        activeRoomListener?.remove()
        clearRoomListeners()
        super.onCleared()
    }

    fun deleteRoomCompletely() {
        viewModelScope.launch {
            try {
                val roomId = _ui.value.activeRoom?.roomId.orEmpty()

                _ui.value = _ui.value.copy(
                    loading = true,
                    message = null
                )

                repo.deleteRoomCompletely(roomId)

                clearRoomListeners()

                _ui.value = _ui.value.copy(
                    loading = false,
                    activeRoom = null,
                    members = emptyList(),
                    enteredRoomCode = "",
                    message = "Room deleted"
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    message = e.message ?: "Failed to delete room"
                )
            }
        }
    }
    fun startGroupSuggestionGeneration(context: Context) {
        viewModelScope.launch {
            try {
                val roomId = _ui.value.activeRoom?.roomId.orEmpty()

                _ui.value = _ui.value.copy(
                    loading = true,
                    message = "Checking room answers..."
                )

                val canGenerate = repo.shouldGenerateSuggestions(roomId)

                if (!canGenerate) {
                    _ui.value = _ui.value.copy(
                        loading = false,
                        message = "Wait until everyone submits or the 3-minute timer ends."
                    )
                    return@launch
                }

                val nearbyPlaces = if (_ui.value.useLocation) {
                    val location = locationRepo.getCurrentLocation(context)
                    if (location == null) {
                        emptyList()
                    } else {
                        try {
                            placesRepo.getNearbyPlaces(
                                location = location,
                                extraPreference = _ui.value.extraPreference
                            )
                        } catch (_: Exception) {
                            emptyList()
                        }
                    }
                } else {
                    emptyList()
                }

                _ui.value = _ui.value.copy(
                    message = "Generating group suggestions..."
                )

                repo.generateAndSaveGroupSuggestions(
                    roomId = roomId,
                    extraPreference = _ui.value.extraPreference,
                    nearbyPlaces = nearbyPlaces
                )

                _ui.value = _ui.value.copy(
                    loading = false,
                    nearbyPlacesCount = nearbyPlaces.size,
                    message = "Group suggestions generated"
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    message = e.message ?: "Failed to generate group suggestions"
                )
            }
        }
    }
}