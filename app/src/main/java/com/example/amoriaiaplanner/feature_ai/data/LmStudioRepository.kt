package com.example.amoriaiaplanner.feature_ai.data

import android.util.Log
import com.example.amoriaiaplanner.core.config.AiConfig
import com.example.amoriaiaplanner.feature_ai.model.NearbyPlace
import com.example.amoriaiaplanner.feature_ai.model.PlaceSuggestion
import com.example.amoriaiaplanner.feature_friends_room.model.FriendsQuizAnswer
import com.example.amoriaiaplanner.feature_profile.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class LmStudioRepository {

    suspend fun suggestForSingle(
        profile: UserProfile,
        extraPreference: String,
        nearbyPlaces: List<NearbyPlace> = emptyList()
    ): List<PlaceSuggestion> {
        val prompt = buildSinglePrompt(profile, extraPreference, nearbyPlaces)
        return callLmStudio(prompt)
    }

    suspend fun suggestForFriendsRoom(
        answers: List<FriendsQuizAnswer>,
        groupSize: Int,
        extraPreference: String = "",
        nearbyPlaces: List<NearbyPlace> = emptyList()
    ): List<PlaceSuggestion> {
        val prompt = buildFriendsRoomPrompt(answers, groupSize, extraPreference, nearbyPlaces)
        return callLmStudio(prompt)
    }

    private suspend fun callLmStudio(prompt: String): List<PlaceSuggestion> =
        withContext(Dispatchers.IO) {
            val url = URL(AiConfig.BASE_URL)

            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 180_000
                readTimeout = 180_000
            }

            val body = JSONObject().apply {
                put("model", AiConfig.MODEL)
                put("temperature", 0.35)
                put("max_tokens", 900)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put(
                            "content",
                            "You are a strict Tunisia outing recommendation engine. " +
                                    "You must prefer real nearby places when provided. " +
                                    "Do not invent fake place names. " +
                                    "Do not combine unrelated activities. " +
                                    "Return only valid JSON."
                        )
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }

            Log.d("LM_DEBUG", "REQUEST_BODY = $body")

            OutputStreamWriter(connection.outputStream).use {
                it.write(body.toString())
                it.flush()
            }

            val responseCode = connection.responseCode
            val rawResponse = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use(BufferedReader::readText)
            } else {
                connection.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                    ?: "HTTP error $responseCode"
            }

            if (responseCode !in 200..299) {
                throw IllegalStateException("LM Studio error ($responseCode): $rawResponse")
            }

            val content = JSONObject(rawResponse)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")

            Log.d("LM_DEBUG", "CONTENT = $content")

            parseSuggestions(content)
        }

    private fun parseSuggestions(content: String): List<PlaceSuggestion> {
        val clean = content
            .replace("```json", "")
            .replace("```", "")
            .trim()

        return try {
            val arr = if (clean.startsWith("[")) {
                JSONArray(clean)
            } else {
                JSONObject(clean).getJSONArray("suggestions")
            }

            buildSuggestions(arr)
        } catch (_: Exception) {
            val start = clean.indexOf("{")
            val end = clean.lastIndexOf("}")

            if (start >= 0 && end > start) {
                try {
                    val extracted = clean.substring(start, end + 1)
                    val arr = JSONObject(extracted).getJSONArray("suggestions")
                    buildSuggestions(arr)
                } catch (_: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }

    private fun buildSuggestions(jsonArray: JSONArray): List<PlaceSuggestion> {
        val result = mutableListOf<PlaceSuggestion>()

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)

            val title = item.optString("title").trim()
            val vibe = item.optString("vibe").trim()
            val reason = item.optString("reason").trim()

            if (title.isBlank()) continue

            val badMixedTitle =
                title.contains("+", ignoreCase = true) ||
                        title.contains(" then ", ignoreCase = true) ||
                        title.contains(" and then ", ignoreCase = true) ||
                        title.contains("museum coffee", ignoreCase = true) ||
                        title.contains("beach bar walk", ignoreCase = true)

            if (badMixedTitle) continue

            result.add(
                PlaceSuggestion(
                    title = title,
                    vibe = vibe.ifBlank { "Local / suitable" },
                    reason = reason.ifBlank { "This suggestion matches the nearby context and user preferences." }
                )
            )
        }

        return result.take(5)
    }

    private fun nearbyText(nearbyPlaces: List<NearbyPlace>): String {
        if (nearbyPlaces.isEmpty()) return "No nearby real place data provided."

        return nearbyPlaces
            .take(25)
            .joinToString("\n") {
                "- ${it.name} (${it.type})"
            }
    }

    private fun buildSinglePrompt(
        profile: UserProfile,
        extraPreference: String,
        nearbyPlaces: List<NearbyPlace>
    ): String {
        val hasNearby = nearbyPlaces.isNotEmpty()

        return """
Generate exactly 5 outing suggestions for ONE user in Tunisia.

IMPORTANT PRIORITY ORDER:
1. Extra preference from the user
2. Nearby real places list
3. Budget and transportation
4. Quiz/profile information

User profile:
- Budget: ${profile.budget}
- Lifestyle: ${profile.lifestyle}
- Transportation: ${profile.transportation}
- Availability: ${profile.availability}
- Outing energy: ${profile.outingEnergy}
- Date preferences: ${profile.datePreferences.joinToString()}
- Indoor/outdoor preference: ${profile.indoorOutdoorPreference}

Extra preference from user:
${extraPreference.ifBlank { "None" }}

The extra preference has HIGH PRIORITY and overrides weak profile assumptions.

Nearby real places from OpenStreetMap within 40km:
${nearbyText(nearbyPlaces)}

Rules:
- Return EXACTLY 5 suggestions.
- Return ONLY valid JSON.
- Do NOT invent fake place names.
- Do NOT combine unrelated activities.
- Do NOT suggest long-distance trips outside the 40km radius.
- Do NOT suggest generic mixed ideas like "museum + coffee + beach".
- Each suggestion must be ONE clear place or ONE clear activity.
- If nearby real places are provided, every title should reference ONE nearby real place from the list.
- If nearby real places are not enough, suggest realistic local Tunisia-style activity types, not fake business names.
- Respect the extra preference first.
- Respect budget, transportation, availability, indoor/outdoor preference, and energy.
- Prefer realistic categories: café, restaurant, tea salon, beach, park, mall, cinema, attraction, viewpoint, food place.

Nearby places available: $hasNearby

JSON format:
{
  "suggestions": [
    {
      "title": "Real nearby place name or clear local activity",
      "vibe": "Calm / cozy",
      "reason": "Short realistic reason why it fits."
    }
  ]
}
""".trimIndent()
    }

    private fun buildFriendsRoomPrompt(
        answers: List<FriendsQuizAnswer>,
        groupSize: Int,
        extraPreference: String,
        nearbyPlaces: List<NearbyPlace>
    ): String {
        val hasNearby = nearbyPlaces.isNotEmpty()

        val answersText = answers.mapIndexed { index, answer ->
            """
User ${index + 1}:
- Group mood: ${answer.groupMood}
- Budget: ${answer.budget}
- Outing type: ${answer.outingType}
- Food preference: ${answer.foodPreference}
- Energy level: ${answer.energyLevel}
- Indoor/outdoor: ${answer.indoorOutdoor}
- Distance preference: ${answer.distancePreference}
- Time preference: ${answer.timePreference}
- Activity preferences: ${answer.activityPreferences.joinToString()}
- Deal breaker: ${answer.dealBreaker}
            """.trimIndent()
        }.joinToString("\n\n")

        return """
Generate exactly 5 outing suggestions for a GROUP OF FRIENDS in Tunisia.

IMPORTANT PRIORITY ORDER:
1. Extra preference from host
2. Nearby real places list
3. Shared/repeated group preferences
4. Lowest budget and deal breakers
5. Individual quiz answers

Group size: $groupSize
Answered members: ${answers.size}

Extra preference from host:
${extraPreference.ifBlank { "None" }}

The extra preference has HIGH PRIORITY and overrides weak profile assumptions.

Nearby real places from OpenStreetMap within 40km:
${nearbyText(nearbyPlaces)}

Group quiz answers:
$answersText

Rules:
- Return EXACTLY 5 suggestions.
- Return ONLY valid JSON.
- Do NOT invent fake place names.
- Do NOT combine unrelated activities.
- Do NOT suggest long-distance trips outside the 40km radius.
- Do NOT suggest generic mixed ideas like "museum + coffee + beach".
- Each suggestion must be ONE clear place or ONE clear activity.
- If nearby real places are provided, every title should reference ONE nearby real place from the list.
- If nearby real places are not enough, suggest realistic local Tunisia-style activity types, not fake business names.
- Respect the host extra preference first.
- Respect the lowest budget.
- Respect deal breakers.
- Balance the group preferences.
- Prefer realistic categories: café, restaurant, tea salon, beach, park, mall, cinema, attraction, viewpoint, food place.

Nearby places available: $hasNearby

JSON format:
{
  "suggestions": [
    {
      "title": "Real nearby place name or clear local activity",
      "vibe": "Fun / social",
      "reason": "Short realistic reason why it fits the group."
    }
  ]
}
""".trimIndent()
    }
}