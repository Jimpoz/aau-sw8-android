package com.example.aauapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat

class WifiScanner(private val context: Context) {

    @Suppress("DEPRECATION")
    fun scanWifi(): List<ScanResult> {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return if (fineGranted && coarseGranted) {
            wifiManager.scanResults
        } else {
            emptyList()
        }
    }
}