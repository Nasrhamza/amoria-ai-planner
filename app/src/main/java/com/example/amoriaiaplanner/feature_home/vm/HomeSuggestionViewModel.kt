package com.example.amoriaiaplanner.feature_home.vm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.amoriaiaplanner.feature_ai.data.LmStudioRepository
import com.example.amoriaiaplanner.feature_ai.data.LocationRepository
import com.example.amoriaiaplanner.feature_ai.data.OsmPlacesRepository
import com.example.amoriaiaplanner.feature_ai.model.NearbyPlace
import com.example.amoriaiaplanner.feature_ai.model.PlaceSuggestion
import com.example.amoriaiaplanner.feature_profile.data.ProfileRepository
import com.example.amoriaiaplanner.feature_suggestions.data.SuggestionMemoryStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeSuggestionUiState(
    val extraPreference: String = "",
    val useLocation: Boolean = false,
    val showEnableLocationDialog: Boolean = false,
    val nearbyPlacesCount: Int = 0,
    val suggestions: List<PlaceSuggestion> = emptyList(),
    val randomIdeas: List<PlaceSuggestion> = listOf(
        PlaceSuggestion(title = "Sunset beach walk", vibe = "Relaxed", reason = "A simple low-cost idea for a calm evening."),
        PlaceSuggestion(title = "Coffee and dessert outing", vibe = "Cozy", reason = "Perfect for a quiet and easy meet-up."),
        PlaceSuggestion(title = "Bookstore and café combo", vibe = "Soft / intellectual", reason = "Great if you want a calm outing with conversation."),
        PlaceSuggestion(title = "Museum or medina stroll", vibe = "Cultural", reason = "A nice option for discovering something together.")
    ),
    val loading: Boolean = false,
    val message: String? = null
)

class HomeSuggestionViewModel(
    private val aiRepo: LmStudioRepository = LmStudioRepository(),
    private val profileRepo: ProfileRepository = ProfileRepository(),
    private val locationRepo: LocationRepository = LocationRepository(),
    private val placesRepo: OsmPlacesRepository = OsmPlacesRepository()
) : ViewModel() {

    private val _ui = MutableStateFlow(HomeSuggestionUiState())
    val ui: StateFlow<HomeSuggestionUiState> = _ui

    // Internal flag to track if the user *intended* to use location
    private var userWantsLocation: Boolean = false

    fun onExtraPreferenceChange(value: String) {
        _ui.value = _ui.value.copy(extraPreference = value, message = null)
    }

    /**
     * Called when the user clicks the location icon.
     */
    fun toggleLocation(context: Context, onPermissionRequired: () -> Unit) {
        if (_ui.value.useLocation) {
            userWantsLocation = false
            disableLocation()
        } else {
            userWantsLocation = true
            val hasPermission = locationRepo.hasLocationPermission(context)
            if (hasPermission) {
                requestEnableLocation(context)
            } else {
                onPermissionRequired()
            }
        }
    }

    /**
     * Checks GPS status and permissions. Turns icon green if everything is ready.
     */
    fun requestEnableLocation(context: Context) {
        val hasPermission = locationRepo.hasLocationPermission(context)
        val serviceEnabled = locationRepo.isLocationServiceEnabled(context)

        when {
            !hasPermission -> {
                _ui.value = _ui.value.copy(
                    useLocation = false,
                    message = "Location permission is required."
                )
            }
            !serviceEnabled -> {
                _ui.value = _ui.value.copy(
                    useLocation = false,
                    showEnableLocationDialog = true, // Shows the "Open Settings" popup
                    message = "Phone location is turned off."
                )
            }
            else -> {
                _ui.value = _ui.value.copy(
                    useLocation = true,
                    showEnableLocationDialog = false,
                    message = "Location synced! (40km radius) ✅"
                )
            }
        }
    }

    fun onLocationPermissionResult(context: Context, granted: Boolean) {
        if (granted) {
            userWantsLocation = true
            requestEnableLocation(context)
        } else {
            userWantsLocation = false
            _ui.value = _ui.value.copy(
                useLocation = false,
                message = "Permission denied. Location disabled."
            )
        }
    }

    fun dismissLocationDialog() {
        _ui.value = _ui.value.copy(showEnableLocationDialog = false)
    }

    fun disableLocation() {
        userWantsLocation = false
        _ui.value = _ui.value.copy(
            useLocation = false,
            showEnableLocationDialog = false,
            nearbyPlacesCount = 0,
            message = "Location disabled."
        )
    }

    /**
     * ✅ Automatic Sync: This is called by the UI's lifecycle observer.
     * It turns the icon green if the user previously wanted location and now GPS is ON.
     */
    fun refreshLocationAvailability(context: Context) {
        val hasPermission = locationRepo.hasLocationPermission(context)
        val serviceEnabled = locationRepo.isLocationServiceEnabled(context)
        val isHardwareReady = hasPermission && serviceEnabled

        // If user wants location and hardware just became ready, turn it green!
        if (userWantsLocation && isHardwareReady && !_ui.value.useLocation) {
            _ui.value = _ui.value.copy(
                useLocation = true,
                showEnableLocationDialog = false,
                message = "Location synchronized! ✅"
            )
        } 
        // If hardware is lost, turn it grey
        else if (!isHardwareReady && _ui.value.useLocation) {
            _ui.value = _ui.value.copy(useLocation = false)
        }
    }

    fun suggestSingle(context: Context) {
        viewModelScope.launch {
            try {
                _ui.value = _ui.value.copy(loading = true, message = null)

                val profile = profileRepo.getCurrentUserProfile()
                    ?: throw IllegalStateException("Profile not found")

                val nearbyPlaces = loadNearbyPlacesIfEnabled(context)

                val suggestions = aiRepo.suggestForSingle(
                    profile = profile,
                    extraPreference = _ui.value.extraPreference,
                    nearbyPlaces = nearbyPlaces
                )

                SuggestionMemoryStore.latestSuggestions = suggestions
                SuggestionMemoryStore.latestModeLabel = "Default mode"
                SuggestionMemoryStore.latestExtraPreference = _ui.value.extraPreference

                _ui.value = _ui.value.copy(
                    loading = false,
                    suggestions = suggestions,
                    nearbyPlacesCount = nearbyPlaces.size,
                    message = if (suggestions.isEmpty()) "No suggestions found" else null
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    loading = false,
                    message = e.message ?: "Failed to generate suggestions"
                )
            }
        }
    }

    private suspend fun loadNearbyPlacesIfEnabled(context: Context): List<NearbyPlace> {
        if (!_ui.value.useLocation) return emptyList()

        val location = locationRepo.getCurrentLocation(context)
        if (location == null) {
            return emptyList()
        }

        return try {
            placesRepo.getNearbyPlaces(
                location = location,
                extraPreference = _ui.value.extraPreference
            )
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clearSuggestions() {
        _ui.value = _ui.value.copy(suggestions = emptyList(), message = null)
    }
}
