package com.example.amoriaiaplanner.feature_ai.data

import com.example.amoriaiaplanner.feature_ai.model.PlaceIntent

object PlaceIntentDetector {

    fun detect(text: String): PlaceIntent {
        val t = text.lowercase()

        return when {
            listOf("party", "club", "nightclub", "night club", "alcohol", "drink", "drinks", "music", "bar", "pub", "dance").any { it in t } ->
                PlaceIntent.NIGHTLIFE

            listOf("restaurant", "food", "dinner", "lunch", "eat", "pizza", "burger", "sushi").any { it in t } ->
                PlaceIntent.FOOD

            listOf("coffee", "café", "cafe", "tea", "calm", "quiet").any { it in t } ->
                PlaceIntent.CAFE

            listOf("beach", "sea", "corniche", "coast").any { it in t } ->
                PlaceIntent.BEACH

            listOf("mall", "shopping", "shop", "stores").any { it in t } ->
                PlaceIntent.SHOPPING

            listOf("museum", "culture", "gallery", "art", "history").any { it in t } ->
                PlaceIntent.CULTURE

            listOf("park", "walk", "outdoor", "nature", "garden").any { it in t } ->
                PlaceIntent.OUTDOOR

            else -> PlaceIntent.GENERAL
        }
    }
}