package com.example.amoriaiaplanner.feature_friends_room.vm

import androidx.lifecycle.ViewModel
import com.example.amoriaiaplanner.feature_ai.model.PlaceSuggestion
import com.example.amoriaiaplanner.feature_friends_room.data.FriendsRoomRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class FriendsRoomSuggestionsUiState(
    val loading: Boolean = true,
    val suggestions: List<PlaceSuggestion> = emptyList(),
    val message: String? = null
)

class FriendsRoomSuggestionsViewModel(
    private val repo: FriendsRoomRepository = FriendsRoomRepository()
) : ViewModel() {

    private val _ui = MutableStateFlow(FriendsRoomSuggestionsUiState())
    val ui: StateFlow<FriendsRoomSuggestionsUiState> = _ui

    private var suggestionsListener: ListenerRegistration? = null

    fun observeSuggestions(roomId: String) {
        if (roomId.isBlank()) {
            _ui.value = FriendsRoomSuggestionsUiState(
                loading = false,
                message = "Room not found"
            )
            return
        }

        suggestionsListener?.remove()

        suggestionsListener = repo.observeSavedSuggestions(
            roomId = roomId,
            onChanged = { suggestions ->
                _ui.value = FriendsRoomSuggestionsUiState(
                    loading = false,
                    suggestions = suggestions,
                    message = if (suggestions.isEmpty()) "No group suggestions found yet" else null
                )
            },
            onError = { e ->
                _ui.value = FriendsRoomSuggestionsUiState(
                    loading = false,
                    message = e.message ?: "Failed to load group suggestions"
                )
            }
        )
    }

    override fun onCleared() {
        suggestionsListener?.remove()
        super.onCleared()
    }
}