package com.flipglyph.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "FlipGlyph.Sensor"

/** Accelerometer gravity component (m/s^2) beyond which orientation is classified with confidence. */
private const val FACE_THRESHOLD = 7.0f

/** Low-pass filter weight; smaller = smoother/slower to react. */
private const val LOW_PASS_ALPHA = 0.15f

/** Below this proximity distance (cm), the sensor reports "near" (e.g. pocketed). */
private const val PROXIMITY_NEAR_CM = 3.0f

class OrientationDetector(
    context: Context,
    private val stableDurationMs: Long = 700L,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val proximity: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private val stabilizer = OrientationStabilizer(stableDurationMs)
    private val gravity = FloatArray(3)
    private var gravityInitialized = false

    private val _orientation = MutableStateFlow(DeviceOrientation.UNKNOWN)
    val orientation: StateFlow<DeviceOrientation> = _orientation.asStateFlow()

    private val _diagnostics = MutableStateFlow(
        SensorDiagnostics(
            accelerometerAvailable = accelerometer != null,
            gyroscopeAvailable = gyroscope != null,
            proximityAvailable = proximity != null,
        )
    )
    val diagnostics: StateFlow<SensorDiagnostics> = _diagnostics.asStateFlow()

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        } ?: Log.w(TAG, "No accelerometer available; face-down detection disabled")
        proximity?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        gravityInitialized = false
        stabilizer.reset()
        _orientation.value = DeviceOrientation.UNKNOWN
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> handleAccelerometer(event)
            Sensor.TYPE_PROXIMITY -> handleProximity(event)
        }
    }

    private fun handleAccelerometer(event: SensorEvent) {
        if (!gravityInitialized) {
            gravity[0] = event.values[0]
            gravity[1] = event.values[1]
            gravity[2] = event.values[2]
            gravityInitialized = true
        } else {
            gravity[0] += LOW_PASS_ALPHA * (event.values[0] - gravity[0])
            gravity[1] += LOW_PASS_ALPHA * (event.values[1] - gravity[1])
            gravity[2] += LOW_PASS_ALPHA * (event.values[2] - gravity[2])
        }

        val raw = classify(gravity[2])
        val stable = stabilizer.onRawSample(raw, event.timestamp / 1_000_000L)
        _orientation.value = stable

        _diagnostics.value = _diagnostics.value.copy(
            x = gravity[0],
            y = gravity[1],
            z = gravity[2],
            orientation = stable,
        )
    }

    private fun handleProximity(event: SensorEvent) {
        val near = event.values.isNotEmpty() && event.values[0] < PROXIMITY_NEAR_CM
        _diagnostics.value = _diagnostics.value.copy(nearProximity = near)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    /**
     * Classifies from the gravity vector's Z component: the display faces the ground when
     * Z points down. Threshold is a starting point only — PRD requires calibration against
     * the physical Phone (4a) Pro before shipping.
     */
    private fun classify(z: Float): DeviceOrientation = when {
        z <= -FACE_THRESHOLD -> DeviceOrientation.FACE_DOWN
        z >= FACE_THRESHOLD -> DeviceOrientation.FACE_UP
        else -> DeviceOrientation.UNKNOWN
    }
}
