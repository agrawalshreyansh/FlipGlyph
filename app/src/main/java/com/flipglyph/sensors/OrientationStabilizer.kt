package com.flipglyph.sensors

/**
 * Pure debounce/hysteresis: a raw classification only becomes the reported stable
 * orientation once it has held continuously for [stableDurationMs]. Oscillating raw
 * input (sensor noise, a mid-flip pass through FACE_UP) never accumulates enough
 * continuous time and is ignored.
 */
class OrientationStabilizer(private val stableDurationMs: Long = 700L) {

    private var candidate: DeviceOrientation = DeviceOrientation.UNKNOWN
    private var candidateSinceMs: Long = 0L
    private var stable: DeviceOrientation = DeviceOrientation.UNKNOWN

    fun onRawSample(orientation: DeviceOrientation, timestampMs: Long): DeviceOrientation {
        if (orientation != candidate) {
            candidate = orientation
            candidateSinceMs = timestampMs
        }
        if (orientation != DeviceOrientation.UNKNOWN &&
            orientation != stable &&
            timestampMs - candidateSinceMs >= stableDurationMs
        ) {
            stable = orientation
        }
        return stable
    }

    fun reset() {
        candidate = DeviceOrientation.UNKNOWN
        candidateSinceMs = 0L
        stable = DeviceOrientation.UNKNOWN
    }
}
