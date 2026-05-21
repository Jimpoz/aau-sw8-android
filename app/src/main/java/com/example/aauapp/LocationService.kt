package com.example.aauapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class UserLocationUiState(
    val currentLabel: String = "Unknown location",
    val latitude: Double? = null,
    val longitude: Double? = null
)

enum class LocalizationMode {
    GPS,
    GPS_WIFI;

    val label: String
        get() = when (this) {
            GPS -> "GPS"
            GPS_WIFI -> "GPS + Wi-Fi"
        }
}

data class LocationFix(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val isOnWifi: Boolean = false,
    val mode: LocalizationMode = LocalizationMode.GPS
)

class LocationService(private val context: Context) {

    private val fused = LocationServices.getFusedLocationProviderClient(context)
    private val connectivity =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _fix = MutableStateFlow(LocationFix())
    val fix: StateFlow<LocationFix> = _fix.asStateFlow()

    private var isOnWifi = false
    // Max tolerance — past this a sample is too coarse to be useful indoors.
    private var maxAcceptableAccuracy = 65f

    private var callback: LocationCallback? = null
    private var netCallback: ConnectivityManager.NetworkCallback? = null

    private fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun startUpdates() {
        if (!hasPermission() || callback != null) return
        startWifiMonitoring()
        val cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val best = result.locations
                    .filter { it.hasAccuracy() && it.accuracy in 0f..maxAcceptableAccuracy }
                    .minByOrNull { it.accuracy }
                    ?: result.lastLocation
                    ?: return
                publish(best.latitude, best.longitude, if (best.hasAccuracy()) best.accuracy else null)
            }
        }
        callback = cb
        fused.requestLocationUpdates(buildRequest(), cb, Looper.getMainLooper())
    }

    fun stopUpdates() {
        callback?.let { fused.removeLocationUpdates(it) }
        callback = null
        netCallback?.let {
            runCatching { connectivity.unregisterNetworkCallback(it) }
        }
        netCallback = null
    }

    private fun buildRequest(): LocationRequest {
        val interval = if (isOnWifi) 1000L else 2000L
        return LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
            .setMinUpdateIntervalMillis(interval / 2)
            .setMinUpdateDistanceMeters(if (isOnWifi) 0f else 2f)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun restartUpdates() {
        val cb = callback ?: return
        fused.removeLocationUpdates(cb)
        fused.requestLocationUpdates(buildRequest(), cb, Looper.getMainLooper())
    }

    private fun startWifiMonitoring() {
        if (netCallback != null) return
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                applyWifi(caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
            }

            override fun onLost(network: Network) {
                applyWifi(false)
            }
        }
        netCallback = cb
        val req = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivity.registerNetworkCallback(req, cb)
    }

    private fun applyWifi(onWifi: Boolean) {
        if (onWifi == isOnWifi) return
        isOnWifi = onWifi
        maxAcceptableAccuracy = if (onWifi) 25f else 65f
        _fix.value = _fix.value.copy(
            isOnWifi = onWifi,
            mode = if (onWifi) LocalizationMode.GPS_WIFI else LocalizationMode.GPS
        )
        if (callback != null) restartUpdates()
    }

    private fun publish(lat: Double, lng: Double, acc: Float?) {
        _fix.value = LocationFix(
            latitude = lat,
            longitude = lng,
            accuracyMeters = acc,
            isOnWifi = isOnWifi,
            mode = if (isOnWifi) LocalizationMode.GPS_WIFI else LocalizationMode.GPS
        )
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): UserLocationUiState {
        if (!hasPermission()) {
            return UserLocationUiState(currentLabel = "Location permission needed")
        }
        return suspendCancellableCoroutine { continuation ->
            fused.lastLocation
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
                        continuation.resume(UserLocationUiState(currentLabel = "Location unavailable"))
                    }
                }
                .addOnFailureListener {
                    continuation.resume(UserLocationUiState(currentLabel = "Could not get location"))
                }
        }
    }
}
