package com.example.aauapp

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

data class BarometerFloor(val floorIndex: Int, val elevationMeters: Double?)

class BarometerService(context: Context) : SensorEventListener {

    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val pressureSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

    private val _currentFloorIndex = MutableStateFlow<Int?>(null)
    val currentFloorIndex: StateFlow<Int?> = _currentFloorIndex.asStateFlow()

    private val _relativeAltitudeMeters = MutableStateFlow<Double?>(null)
    val relativeAltitudeMeters: StateFlow<Double?> = _relativeAltitudeMeters.asStateFlow()

    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    private var floors: List<BarometerFloor> = emptyList()
    private var baselinePressureHpa: Float? = null
    private var baselineFloorElevation: Double? = null
    private var lastPressureHpa: Float? = null
    private var started = false

    private val hysteresisMeters = 1.5
    private val defaultFloorHeightMeters = 3.3

    fun start(floors: List<BarometerFloor>, baselineFloorIndex: Int = 0) {
        if (pressureSensor == null) {
            _isAvailable.value = false
            return
        }
        this.floors = floors
        val anchor = floors.firstOrNull { it.floorIndex == baselineFloorIndex }
            ?: floors.minByOrNull { it.floorIndex }
        baselineFloorElevation = anchor?.let { elevationOf(it) }
        baselinePressureHpa = null // captured on the first sample
        _currentFloorIndex.value = anchor?.floorIndex
        _isAvailable.value = true
        if (!started) {
            sensorManager.registerListener(
                this, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL
            )
            started = true
        }
    }

    fun stop() {
        if (started) {
            sensorManager.unregisterListener(this)
            started = false
        }
        baselinePressureHpa = null
        baselineFloorElevation = null
        _currentFloorIndex.value = null
        _relativeAltitudeMeters.value = null
    }

    fun recalibrate(toFloorIndex: Int) {
        if (!_isAvailable.value) return
        val anchor = floors.firstOrNull { it.floorIndex == toFloorIndex } ?: return
        val p = lastPressureHpa ?: return
        baselinePressureHpa = p
        baselineFloorElevation = elevationOf(anchor)
        _currentFloorIndex.value = toFloorIndex
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_PRESSURE) return
        val pressure = event.values.firstOrNull() ?: return
        lastPressureHpa = pressure

        val base = baselinePressureHpa
        if (base == null) {
            baselinePressureHpa = pressure
            _relativeAltitudeMeters.value = 0.0
            return
        }
        val relative = SensorManager.getAltitude(base, pressure).toDouble()
        _relativeAltitudeMeters.value = relative
        recomputeFloorIndex(relative)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun recomputeFloorIndex(relativeAltitude: Double) {
        val e0 = baselineFloorElevation ?: return
        if (floors.isEmpty()) return
        val userElevation = e0 + relativeAltitude

        val best = floors.minByOrNull { abs(elevationOf(it) - userElevation) } ?: return
        val bestDelta = abs(elevationOf(best) - userElevation)

        val cur = _currentFloorIndex.value
        if (cur != null) {
            val curFloor = floors.firstOrNull { it.floorIndex == cur }
            if (curFloor != null) {
                val curDelta = abs(elevationOf(curFloor) - userElevation)
                // Stay put unless the best candidate is clearly closer.
                if (curDelta - bestDelta < hysteresisMeters) return
            }
        }
        if (best.floorIndex != cur) {
            _currentFloorIndex.value = best.floorIndex
        }
    }

    private fun elevationOf(f: BarometerFloor): Double =
        f.elevationMeters ?: (f.floorIndex * defaultFloorHeightMeters)
}
