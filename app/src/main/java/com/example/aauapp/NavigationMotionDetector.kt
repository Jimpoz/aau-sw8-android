package com.example.aauapp

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt


class NavigationMotionDetector(context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val sensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    val isAvailable: Boolean get() = sensor != null

    private val motionTimeoutMs = 1_500L

    private val accelerationThreshold = 0.25f

    @Volatile private var lastMotionAt = 0L
    private var running = false

    val isMoving: Boolean
        get() = lastMotionAt > 0L &&
                System.currentTimeMillis() - lastMotionAt <= motionTimeoutMs

    fun start() {
        if (!isAvailable || running) return
        running = true
        lastMotionAt = 0L
        // SENSOR_DELAY_GAME ≈ 20 ms / 50 Hz — fast enough for step detection
        // without draining the battery for every frame.
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        if (!running) return
        running = false
        sensorManager.unregisterListener(this)
        lastMotionAt = 0L
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val mag = sqrt(x * x + y * y + z * z)
        if (mag > accelerationThreshold) {
            lastMotionAt = System.currentTimeMillis()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* ignored */ }
}
