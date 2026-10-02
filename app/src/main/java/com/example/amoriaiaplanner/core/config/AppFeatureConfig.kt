package com.example.amoriaiaplanner.core.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AppFeatureSettings(
    val defaultSuggestionsEnabled: Boolean = true,
    val friendsRoomsEnabled: Boolean = true,
    val locationSuggestionsEnabled: Boolean = true
)

class AppFeatureConfigRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun load(): AppFeatureSettings {
        val document = db.collection(APP_CONFIG_COLLECTION)
            .document(FEATURES_DOCUMENT)
            .get()
            .await()

        return document.toFeatureSettings()
    }

    fun observe(
        onChanged: (AppFeatureSettings) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection(APP_CONFIG_COLLECTION)
            .document(FEATURES_DOCUMENT)
            .addSnapshotListener { document, error ->
                when {
                    error != null -> onError(error)
                    document != null -> onChanged(document.toFeatureSettings())
                }
            }
    }

    suspend fun save(settings: AppFeatureSettings, updatedBy: String) {
        db.collection(APP_CONFIG_COLLECTION)
            .document(FEATURES_DOCUMENT)
            .set(
                mapOf(
                    "defaultSuggestionsEnabled" to settings.defaultSuggestionsEnabled,
                    "friendsRoomsEnabled" to settings.friendsRoomsEnabled,
                    "locationSuggestionsEnabled" to settings.locationSuggestionsEnabled,
                    "updatedAt" to System.currentTimeMillis(),
                    "updatedBy" to updatedBy
                ),
                SetOptions.merge()
            )
            .await()
    }

    private companion object {
        const val APP_CONFIG_COLLECTION = "app_config"
        const val FEATURES_DOCUMENT = "features"
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toFeatureSettings(): AppFeatureSettings {
        if (!exists()) return AppFeatureSettings()
        return AppFeatureSettings(
            defaultSuggestionsEnabled = getBoolean("defaultSuggestionsEnabled") ?: true,
            friendsRoomsEnabled = getBoolean("friendsRoomsEnabled") ?: true,
            locationSuggestionsEnabled = getBoolean("locationSuggestionsEnabled") ?: true
        )
    }
}

data class AppFeatureConfigUiState(
    val loading: Boolean = true,
    val settings: AppFeatureSettings = AppFeatureSettings(),
    val error: String? = null
)

class AppFeatureConfigViewModel(
    private val repository: AppFeatureConfigRepository = AppFeatureConfigRepository()
) : ViewModel() {
    private val _ui = MutableStateFlow(AppFeatureConfigUiState())
    val ui: StateFlow<AppFeatureConfigUiState> = _ui
    private var listener: ListenerRegistration? = null

    init {
        observeSettings()
    }

    private fun observeSettings() {
        listener?.remove()
        _ui.value = _ui.value.copy(loading = true, error = null)
        listener = repository.observe(
            onChanged = { settings ->
                _ui.value = AppFeatureConfigUiState(
                    loading = false,
                    settings = settings
                )
            },
            onError = { error ->
                _ui.value = _ui.value.copy(
                    loading = false,
                    error = error.message ?: "Unable to load application settings."
                )
            }
        )
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null)
            try {
                _ui.value = AppFeatureConfigUiState(
                    loading = false,
                    settings = repository.load()
                )
            } catch (error: Exception) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    error = error.message ?: "Unable to load application settings."
                )
            }
        }
    }


    override fun onCleared() {
        listener?.remove()
        listener = null
        super.onCleared()
    }
}
