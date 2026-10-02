package com.example.amoriaiaplanner.feature_ai.model

data class UserLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radiusKm: Int = 40
)