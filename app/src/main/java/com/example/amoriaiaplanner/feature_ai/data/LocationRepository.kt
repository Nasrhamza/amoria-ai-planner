package com.example.amoriaiaplanner.feature_ai.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.amoriaiaplanner.feature_ai.model.UserLocation
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import com.google.android.gms.tasks.CancellationTokenSource


class LocationRepository {

    fun hasLocationPermission(context: Context): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    fun isLocationServiceEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return try {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): UserLocation? {
        if (!hasLocationPermission(context)) {
            Log.d("LOCATION_SYNC", "Permissions not granted")
            return null
        }

        if (!isLocationServiceEnabled(context)) {
            Log.d("LOCATION_SYNC", "Phone location service is OFF")
            return null
        }

        return try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()

            // Try to get a fresh location fix with high accuracy (GPS)
            // We wrap this call in its own try-catch so that if it throws an exception
            // (common if GPS hardware is busy), we can still fallback to lastLocation.
            val location = try {
                client.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cts.token
                ).await()
            } catch (e: Exception) {
                Log.e("LOCATION_SYNC", "Fresh location failed, trying last known", e)
                null
            } ?: client.lastLocation.await()

            if (location != null) {
                Log.d("LOCATION_SYNC", "Synced! Lat: ${location.latitude}, Lon: ${location.longitude}")
                UserLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    radiusKm = 40
                )
            } else {
                Log.d("LOCATION_SYNC", "Location retrieved was null")
                null
            }
        } catch (e: Exception) {
            Log.e("LOCATION_SYNC", "Error syncing location", e)
            null
        }
    }
}
