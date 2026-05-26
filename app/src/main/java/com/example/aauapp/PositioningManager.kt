package com.example.aauapp

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import com.example.aauapp.data.remote.PositioningRepository
import com.example.aauapp.data.remote.WifiFingerprintResponse
import com.example.aauapp.data.remote.WifiLocateResponse
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class PositioningManager(
    context: Context,
    private val repo: PositioningRepository = PositioningRepository()
) {
    private val appCtx = context.applicationContext
    private val wifiManager =
        appCtx.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val ranger = WifiRttRanger(appCtx)

    private val recentRssi = ArrayDeque<Map<String, Float>>(3)

    private val minRssiDbm = -85f

    @SuppressLint("MissingPermission")
    private suspend fun freshScanResults(): List<ScanResult> {
        val fresh = withTimeoutOrNull(3_000L) {
            suspendCancellableCoroutine { cont ->
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context, intent: Intent) {
                        try { appCtx.unregisterReceiver(this) } catch (_: Exception) {}
                        if (cont.isActive) cont.resume(
                            @Suppress("DEPRECATION") wifiManager.scanResults ?: emptyList()
                        )
                    }
                }
                appCtx.registerReceiver(
                    receiver,
                    IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                )
                @Suppress("DEPRECATION")
                val started = wifiManager.startScan()
                if (!started) {
                    // Throttled by the OS — unregister and return cached results now.
                    try { appCtx.unregisterReceiver(receiver) } catch (_: Exception) {}
                    if (cont.isActive) cont.resume(
                        @Suppress("DEPRECATION") wifiManager.scanResults ?: emptyList()
                    )
                }
                cont.invokeOnCancellation {
                    try { appCtx.unregisterReceiver(receiver) } catch (_: Exception) {}
                }
            }
        }
        // If the coroutine timed out (no broadcast), fall back to cached list.
        return fresh ?: @Suppress("DEPRECATION") (wifiManager.scanResults ?: emptyList())
    }

    private fun rssiMap(results: List<ScanResult>): Map<String, Float> =
        results
            .filter { !it.BSSID.isNullOrBlank() && it.level.toFloat() >= minRssiDbm }
            .associate { it.BSSID to it.level.toFloat() }

    private fun mergedRssi(): Map<String, Float> {
        val merged = mutableMapOf<String, Float>()
        for (scan in recentRssi) {
            for ((bssid, rssi) in scan) {
                val prev = merged[bssid]
                if (prev == null || rssi > prev) merged[bssid] = rssi
            }
        }
        return merged
    }

    private fun ftmResponders(results: List<ScanResult>): List<ScanResult> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            results.filter { runCatching { it.is80211mcResponder }.getOrDefault(false) }
        } else {
            emptyList()
        }

    private suspend fun rttFor(results: List<ScanResult>): Map<String, Float> =
        if (ranger.isAvailable()) ranger.range(ftmResponders(results)) else emptyMap()

    suspend fun locate(floorId: String): WifiLocateResponse? {
        val results = freshScanResults()
        if (results.isEmpty()) return null
        val rssiNow = rssiMap(results)
        if (rssiNow.isEmpty()) return null

        // Update rolling buffer (capacity 3).
        recentRssi.addLast(rssiNow)
        while (recentRssi.size > 3) recentRssi.removeFirst()

        val rtt = rttFor(results)
        return runCatching { repo.locate(floorId, mergedRssi(), rtt) }.getOrNull()
    }

    suspend fun collectFingerprint(spaceId: String, floorId: String?): WifiFingerprintResponse? {
        val results = freshScanResults()
        val rssi = rssiMap(results)
        if (rssi.isEmpty()) return null
        val rtt = rttFor(results)
        return runCatching { repo.saveFingerprint(spaceId, floorId, rssi, rtt) }.getOrNull()
    }
}
