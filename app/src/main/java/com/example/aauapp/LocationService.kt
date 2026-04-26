package com.example.aauapp

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class UserLocationUiState(
    val currentLabel: String = "Unknown location",
    val latitude: Double? = null,
    val longitude: Double? = null
)

class LocationService(context: Context) {
    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): UserLocationUiState {
        return suspendCancellableCoroutine { continuation ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        continuation.resume(
                            UserLocationUiState(
                                currentLabel = "Lat ${location.latitude}, Lng ${location.longitude}",
                                latitude = location.latitude,
                                longitude = location.longitude
                            )
                        )
                    } else {
                        continuation.resume(
                            UserLocationUiState(
                                currentLabel = "Location unavailable"
                            )
                        )
                    }
                }
                .addOnFailureListener {
                    continuation.resume(
                        UserLocationUiState(
                            currentLabel = "Could not get location"
                        )
                    )
                }
        }
    }
}