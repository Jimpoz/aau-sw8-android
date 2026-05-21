package com.example.aauapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.rtt.RangingRequest
import android.net.wifi.rtt.RangingResult
import android.net.wifi.rtt.RangingResultCallback
import android.net.wifi.rtt.WifiRttManager
import android.os.Build
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executors
import kotlin.coroutines.resume

class WifiRttRanger(private val context: Context) {

    private val rttManager: WifiRttManager? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.getSystemService(Context.WIFI_RTT_RANGING_SERVICE) as? WifiRttManager
        } else null

    fun isAvailable(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_RTT) &&
            rttManager?.isAvailable == true

    @SuppressLint("MissingPermission")
    suspend fun range(responders: List<ScanResult>): Map<String, Float> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return emptyMap()
        val mgr = rttManager ?: return emptyMap()
        if (!isAvailable() || responders.isEmpty()) return emptyMap()

        val capped = responders.take(RangingRequest.getMaxPeers())
        val request = RangingRequest.Builder()
            .addAccessPoints(capped)
            .build()

        return suspendCancellableCoroutine { cont ->
            try {
                mgr.startRanging(
                    request,
                    Executors.newSingleThreadExecutor(),
                    object : RangingResultCallback() {
                        override fun onRangingFailure(code: Int) {
                            if (cont.isActive) cont.resume(emptyMap())
                        }

                        override fun onRangingResults(results: List<RangingResult>) {
                            val map = HashMap<String, Float>()
                            for (r in results) {
                                if (r.status == RangingResult.STATUS_SUCCESS) {
                                    val bssid = r.macAddress?.toString() ?: continue
                                    map[bssid] = r.distanceMm.toFloat()
                                }
                            }
                            if (cont.isActive) cont.resume(map)
                        }
                    }
                )
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(emptyMap())
            }
        }
    }
}
