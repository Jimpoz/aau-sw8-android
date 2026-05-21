package com.example.aauapp

import com.example.aauapp.data.remote.ActivityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt


class ActivityTracker(
    private val locationService: LocationService,
    private val repo: ActivityRepository = ActivityRepository()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _todayDistanceM = MutableStateFlow(0.0)
    val todayDistanceM: StateFlow<Double> = _todayDistanceM.asStateFlow()

    private val _todaySteps = MutableStateFlow(0)
    val todaySteps: StateFlow<Int> = _todaySteps.asStateFlow()

    private var started = false
    private var lastLat: Double? = null
    private var lastLng: Double? = null
    private var pendingMeters = 0.0
    private var currentDay = localDate()

    // ~average adult stride; same constant used on iOS.
    private val strideMeters = 0.762
    private val minStepMeters = 3.0
    private val maxStepMeters = 200.0
    private val accuracyCeilingMeters = 30.0
    private val flushThresholdMeters = 20.0

    fun start() {
        if (started) return
        started = true
        locationService.startUpdates()

        scope.launch {
            val day = localDate()
            runCatching { repo.getToday(day) }.getOrNull()?.let {
                _todayDistanceM.value = it.distance_m
                _todaySteps.value = it.steps
            }
        }

        scope.launch {
            locationService.fix.collect { fix -> ingest(fix) }
        }
    }

    private fun ingest(fix: LocationFix) {
        // Roll over to a new local day: reset live counters (the backend resets
        // implicitly because deltas are keyed by date).
        val today = localDate()
        if (today != currentDay) {
            currentDay = today
            _todayDistanceM.value = 0.0
            _todaySteps.value = 0
            pendingMeters = 0.0
            lastLat = null
            lastLng = null
        }

        val lat = fix.latitude ?: return
        val lng = fix.longitude ?: return
        val acc = fix.accuracyMeters
        if (acc != null && acc > accuracyCeilingMeters) {
            // Too coarse to trust for step distance; keep it as the anchor only.
            lastLat = lat
            lastLng = lng
            return
        }

        val pLat = lastLat
        val pLng = lastLng
        lastLat = lat
        lastLng = lng
        if (pLat == null || pLng == null) return

        val d = haversineMeters(pLat, pLng, lat, lng)
        if (d < minStepMeters || d > maxStepMeters) return

        pendingMeters += d
        _todayDistanceM.value += d
        _todaySteps.value = (_todayDistanceM.value / strideMeters).roundToInt()

        if (pendingMeters >= flushThresholdMeters) flush()
    }

    private fun flush() {
        val delta = pendingMeters
        pendingMeters = 0.0
        if (delta <= 0.0) return
        val day = currentDay
        val steps = (delta / strideMeters).roundToInt()
        scope.launch {
            runCatching { repo.addToday(day, delta, steps) }
        }
    }

    private fun localDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
