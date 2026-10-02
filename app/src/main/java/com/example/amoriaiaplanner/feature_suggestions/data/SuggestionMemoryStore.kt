package com.example.amoriaiaplanner.feature_suggestions.data

import com.example.amoriaiaplanner.feature_ai.model.PlaceSuggestion

object SuggestionMemoryStore {
    var latestSuggestions: List<PlaceSuggestion> = emptyList()
    var latestModeLabel: String = ""
    var latestExtraPreference: String = ""
}