package com.example.amoriaiaplanner.feature_ai.data

import com.example.amoriaiaplanner.feature_ai.model.NearbyPlace
import com.example.amoriaiaplanner.feature_ai.model.PlaceIntent
import com.example.amoriaiaplanner.feature_ai.model.UserLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

class OsmPlacesRepository {

    suspend fun getNearbyPlaces(
        location: UserLocation,
        extraPreference: String = ""
    ): List<NearbyPlace> = withContext(Dispatchers.IO) {
        val radiusMeters = location.radiusKm * 1000
        val intent = PlaceIntentDetector.detect(extraPreference)

        val queryBody = when (intent) {
            PlaceIntent.NIGHTLIFE -> """
              nwr["amenity"~"bar|pub|nightclub|restaurant"](around:$radiusMeters,${location.latitude},${location.longitude});
              nwr["leisure"~"dance|adult_gaming_centre"](around:$radiusMeters,${location.latitude},${location.longitude});
            """

            PlaceIntent.FOOD -> """
              nwr["amenity"~"restaurant|fast_food|food_court"](around:$radiusMeters,${location.latitude},${location.longitude});
            """

            PlaceIntent.CAFE -> """
              nwr["amenity"~"cafe|ice_cream|restaurant"](around:$radiusMeters,${location.latitude},${location.longitude});
            """

            PlaceIntent.BEACH -> """
              nwr["natural"="beach"](around:$radiusMeters,${location.latitude},${location.longitude});
              nwr["tourism"~"viewpoint|attraction"](around:$radiusMeters,${location.latitude},${location.longitude});
              nwr["amenity"~"cafe|restaurant"](around:$radiusMeters,${location.latitude},${location.longitude});
            """

            PlaceIntent.SHOPPING -> """
              nwr["shop"~"mall|department_store|clothes|shoes|supermarket"](around:$radiusMeters,${location.latitude},${location.longitude});
            """

            PlaceIntent.CULTURE -> """
              nwr["tourism"~"museum|gallery|attraction"](around:$radiusMeters,${location.latitude},${location.longitude});
            """

            PlaceIntent.OUTDOOR -> """
              nwr["leisure"~"park|garden|sports_centre|fitness_centre"](around:$radiusMeters,${location.latitude},${location.longitude});
              nwr["tourism"~"viewpoint|attraction"](around:$radiusMeters,${location.latitude},${location.longitude});
            """

            PlaceIntent.GENERAL -> """
              nwr["amenity"~"cafe|restaurant|fast_food|ice_cream|cinema|theatre|bar|pub"](around:$radiusMeters,${location.latitude},${location.longitude});
              nwr["tourism"~"hotel|attraction|museum|viewpoint|gallery"](around:$radiusMeters,${location.latitude},${location.longitude});
              nwr["leisure"~"park|garden|sports_centre|bowling_alley|fitness_centre"](around:$radiusMeters,${location.latitude},${location.longitude});
              nwr["shop"~"mall|department_store|books"](around:$radiusMeters,${location.latitude},${location.longitude});
              nwr["natural"="beach"](around:$radiusMeters,${location.latitude},${location.longitude});
            """
        }

        val query = """
[out:json][timeout:40];
(
$queryBody
);
out center 120;
""".trimIndent()

        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = URL("https://overpass-api.de/api/interpreter?data=$encoded")

        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", "AmoriaAIPlanner/1.0")
            connectTimeout = 30_000
            readTimeout = 45_000
        }

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val elements = JSONObject(response).getJSONArray("elements")

        val places = mutableListOf<NearbyPlace>()

        for (i in 0 until elements.length()) {
            val item = elements.getJSONObject(i)
            val tags = item.optJSONObject("tags") ?: continue

            val name = tags.optString("name").trim()
            if (name.isBlank()) continue

            val type = when {
                tags.has("amenity") -> tags.optString("amenity")
                tags.has("tourism") -> tags.optString("tourism")
                tags.has("leisure") -> tags.optString("leisure")
                tags.has("shop") -> tags.optString("shop")
                tags.has("natural") -> tags.optString("natural")
                else -> "place"
            }

            val center = item.optJSONObject("center")

            val lat = when {
                item.has("lat") -> item.optDouble("lat", 0.0)
                center != null -> center.optDouble("lat", 0.0)
                else -> 0.0
            }

            val lon = when {
                item.has("lon") -> item.optDouble("lon", 0.0)
                center != null -> center.optDouble("lon", 0.0)
                else -> 0.0
            }

            if (lat == 0.0 && lon == 0.0) continue

            places.add(
                NearbyPlace(
                    name = name,
                    type = type,
                    latitude = lat,
                    longitude = lon
                )
            )
        }

        places
            .distinctBy { "${it.name.lowercase()}_${it.type.lowercase()}" }
            .take(40)
    }
}