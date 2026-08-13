package com.flipglyph.sensors

import org.junit.Assert.assertEquals
import org.junit.Test

class OrientationStabilizerTest {

    @Test
    fun `orientation reported only after holding for the stable duration`() {
        val stabilizer = OrientationStabilizer(stableDurationMs = 700)
        assertEquals(DeviceOrientation.UNKNOWN, stabilizer.onRawSample(DeviceOrientation.FACE_DOWN, 0))
        assertEquals(DeviceOrientation.UNKNOWN, stabilizer.onRawSample(DeviceOrientation.FACE_DOWN, 500))
        assertEquals(DeviceOrientation.FACE_DOWN, stabilizer.onRawSample(DeviceOrientation.FACE_DOWN, 700))
    }

    @Test
    fun `rapid flapping never accumulates enough continuous time to report a change`() {
        val stabilizer = OrientationStabilizer(stableDurationMs = 700)
        var result = DeviceOrientation.UNKNOWN
        var t = 0L
        repeat(10) {
            result = stabilizer.onRawSample(DeviceOrientation.FACE_DOWN, t)
            t += 100
            result = stabilizer.onRawSample(DeviceOrientation.FACE_UP, t)
            t += 100
        }
        assertEquals(DeviceOrientation.UNKNOWN, result)
    }

    @Test
    fun `hysteresis keeps the last stable value until a new orientation holds`() {
        val stabilizer = OrientationStabilizer(stableDurationMs = 700)
        stabilizer.onRawSample(DeviceOrientation.FACE_DOWN, 0)
        assertEquals(DeviceOrientation.FACE_DOWN, stabilizer.onRawSample(DeviceOrientation.FACE_DOWN, 700))

        // A blip to FACE_UP that doesn't hold long enough must not move the stable value.
        assertEquals(DeviceOrientation.FACE_DOWN, stabilizer.onRawSample(DeviceOrientation.FACE_UP, 750))
        assertEquals(DeviceOrientation.FACE_DOWN, stabilizer.onRawSample(DeviceOrientation.FACE_DOWN, 800))
    }

    @Test
    fun `reset clears stable and candidate state`() {
        val stabilizer = OrientationStabilizer(stableDurationMs = 700)
        stabilizer.onRawSample(DeviceOrientation.FACE_DOWN, 0)
        stabilizer.onRawSample(DeviceOrientation.FACE_DOWN, 700)

        stabilizer.reset()

        assertEquals(DeviceOrientation.UNKNOWN, stabilizer.onRawSample(DeviceOrientation.FACE_DOWN, 701))
    }
}
