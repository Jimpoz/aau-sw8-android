package com.example.aauapp

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import com.example.aauapp.data.remote.PositioningRepository
import com.example.aauapp.data.remote.WifiFingerprintResponse
import com.example.aauapp.data.remote.WifiLocateResponse

class PositioningManager(
    context: Context,
    private val repo: PositioningRepository = PositioningRepository()
) {
    private val appCtx = context.applicationContext
    private val wifiManager =
        appCtx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val ranger = WifiRttRanger(appCtx)

    @SuppressLint("MissingPermission")
    private fun scanResults(): List<ScanResult> = try {
        @Suppress("DEPRECATION")
        wifiManager.startScan()
        @Suppress("DEPRECATION")
        wifiManager.scanResults ?: emptyList()
    } catch (e: SecurityException) {
        emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    private fun rssiMap(results: List<ScanResult>): Map<String, Float> =
        results.filter { !it.BSSID.isNullOrBlank() }
            .associate { it.BSSID to it.level.toFloat() }

    private fun ftmResponders(results: List<ScanResult>): List<ScanResult> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            results.filter { runCatching { it.is80211mcResponder }.getOrDefault(false) }
        } else {
            emptyList()
        }

    private suspend fun rttFor(results: List<ScanResult>): Map<String, Float> =
        if (ranger.isAvailable()) ranger.range(ftmResponders(results)) else emptyMap()

    /** Resolve the current space from a live scan. Null if no Wi-Fi visible. */
    suspend fun locate(floorId: String): WifiLocateResponse? {
        val results = scanResults()
        if (results.isEmpty()) return null
        val rssi = rssiMap(results)
        if (rssi.isEmpty()) return null
        val rtt = rttFor(results)
        return runCatching { repo.locate(floorId, rssi, rtt) }.getOrNull()
    }

    /** Capture one fingerprint sample for a space (owner/editor calibration). */
    suspend fun collectFingerprint(spaceId: String, floorId: String?): WifiFingerprintResponse? {
        val results = scanResults()
        val rssi = rssiMap(results)
        if (rssi.isEmpty()) return null
        val rtt = rttFor(results)
        return runCatching { repo.saveFingerprint(spaceId, floorId, rssi, rtt) }.getOrNull()
    }
}
