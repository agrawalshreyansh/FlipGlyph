package com.flipglyph.sensors

enum class DeviceOrientation {
    FACE_UP,
    FACE_DOWN,
    UNKNOWN,
}

data class OrientationSample(
    val orientation: DeviceOrientation,
    val timestampMs: Long,
)

data class SensorDiagnostics(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val orientation: DeviceOrientation = DeviceOrientation.UNKNOWN,
    val accelerometerAvailable: Boolean = false,
    val gyroscopeAvailable: Boolean = false,
    val proximityAvailable: Boolean = false,
    val nearProximity: Boolean = false,
)
