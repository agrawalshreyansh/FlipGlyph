package com.flipglyph.domain

import com.flipglyph.data.AppSettings
import com.flipglyph.glyph.GlyphAvailability
import com.flipglyph.glyph.GlyphController
import com.flipglyph.sensors.DeviceOrientation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class FlipGlyphEngineTest {

    private class FakeGlyphController : GlyphController {
        override val availability = MutableStateFlow(GlyphAvailability.READY)
        var showClockCalls = 0
        var clearCalls = 0
        override suspend fun initialize() {}
        override suspend fun showClock(time: LocalTime) { showClockCalls++ }
        override suspend fun clear() { clearCalls++ }
        override suspend fun shutdown() {}
    }

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)
    private lateinit var controller: FakeGlyphController
    private lateinit var settings: AppSettings
    private lateinit var engine: FlipGlyphEngine

    @Before
    fun setUp() {
        controller = FakeGlyphController()
        settings = AppSettings(enabled = true)
        engine = FlipGlyphEngine(
            scope = scope,
            glyphController = controller,
            settings = { settings },
            nowMs = { dispatcher.scheduler.currentTime },
            clockNow = { LocalTime.NOON },
        )
    }

    private fun advance(ms: Long) {
        dispatcher.scheduler.advanceTimeBy(ms)
        dispatcher.scheduler.runCurrent()
    }

    @Test
    fun `face up then face down activates the glyph`() {
        engine.onOrientationChanged(DeviceOrientation.FACE_UP)
        engine.onOrientationChanged(DeviceOrientation.FACE_DOWN)

        assertEquals(GlyphState.ACTIVE, engine.state.value.glyphState)
    }

    @Test
    fun `timeout turns the glyph off`() {
        settings = settings.copy(timeoutSeconds = 10)
        engine.onOrientationChanged(DeviceOrientation.FACE_DOWN)
        advance(10_000)

        assertEquals(GlyphState.TIMED_OUT, engine.state.value.glyphState)
        assertTrue(controller.clearCalls >= 1)
    }

    @Test
    fun `face up turns the glyph off immediately`() {
        engine.onOrientationChanged(DeviceOrientation.FACE_DOWN)
        engine.onOrientationChanged(DeviceOrientation.FACE_UP)
        advance(0)

        assertEquals(GlyphState.OFF, engine.state.value.glyphState)
        assertEquals(1, controller.clearCalls)
    }

    @Test
    fun `face down after face up starts a fresh timeout`() {
        settings = settings.copy(timeoutSeconds = 10)
        engine.onOrientationChanged(DeviceOrientation.FACE_DOWN)
        engine.onOrientationChanged(DeviceOrientation.FACE_UP)
        engine.onOrientationChanged(DeviceOrientation.FACE_DOWN)

        assertEquals(GlyphState.ACTIVE, engine.state.value.glyphState)
        assertTrue(engine.state.value.timeoutAt != null)
    }

    @Test
    fun `flipping face up cancels a pending timeout`() {
        settings = settings.copy(timeoutSeconds = 10)
        engine.onOrientationChanged(DeviceOrientation.FACE_DOWN)
        advance(3_000)
        engine.onOrientationChanged(DeviceOrientation.FACE_UP)
        advance(10_000)

        assertEquals(GlyphState.OFF, engine.state.value.glyphState)
        assertEquals(1, controller.clearCalls) // not called again by the (cancelled) timeout
    }

    @Test
    fun `rapid orientation flapping settles on the latest reading`() {
        // FlipGlyphEngine reacts to whatever the sensor stabilizer already decided is stable;
        // debounce/hysteresis itself is tested in OrientationStabilizerTest.
        repeat(5) {
            engine.onOrientationChanged(DeviceOrientation.FACE_UP)
            engine.onOrientationChanged(DeviceOrientation.FACE_DOWN)
        }
        assertEquals(GlyphState.ACTIVE, engine.state.value.glyphState)
    }

    @Test
    fun `disabled settings suppress activation`() {
        settings = settings.copy(enabled = false)
        engine.onOrientationChanged(DeviceOrientation.FACE_DOWN)

        assertEquals(GlyphState.OFF, engine.state.value.glyphState)
        assertEquals(0, controller.showClockCalls)
    }

    @Test
    fun `stay active while flipped mode never times out`() {
        settings = settings.copy(activationMode = ActivationMode.STAY_ACTIVE_WHILE_FLIPPED, timeoutSeconds = 5)
        engine.onOrientationChanged(DeviceOrientation.FACE_DOWN)
        advance(60_000)

        assertEquals(GlyphState.ACTIVE, engine.state.value.glyphState)
    }

    @Test
    fun `never timeout keeps glyph active`() {
        settings = settings.copy(timeoutSeconds = 0)
        engine.onOrientationChanged(DeviceOrientation.FACE_DOWN)
        advance(120_000)

        assertEquals(GlyphState.ACTIVE, engine.state.value.glyphState)
    }

    @Test
    fun `press to peek mode ignores the flip transition itself`() {
        settings = settings.copy(activationMode = ActivationMode.PRESS_TO_PEEK)
        engine.onOrientationChanged(DeviceOrientation.FACE_DOWN)

        assertEquals(GlyphState.OFF, engine.state.value.glyphState)
        assertEquals(0, controller.showClockCalls)
    }

    @Test
    fun `press to peek mode activates on peek request while face down`() {
        settings = settings.copy(activationMode = ActivationMode.PRESS_TO_PEEK, timeoutSeconds = 10)
        engine.onOrientationChanged(DeviceOrientation.FACE_DOWN)
        engine.onPeekRequested()

        assertEquals(GlyphState.ACTIVE, engine.state.value.glyphState)
        assertTrue(engine.state.value.timeoutAt != null)
    }

    @Test
    fun `press to peek request while face up does nothing`() {
        settings = settings.copy(activationMode = ActivationMode.PRESS_TO_PEEK)
        engine.onOrientationChanged(DeviceOrientation.FACE_UP)
        engine.onPeekRequested()

        assertEquals(GlyphState.OFF, engine.state.value.glyphState)
    }

    @Test
    fun `press to peek still turns off immediately on face up`() {
        settings = settings.copy(activationMode = ActivationMode.PRESS_TO_PEEK, timeoutSeconds = 10)
        engine.onOrientationChanged(DeviceOrientation.FACE_DOWN)
        engine.onPeekRequested()
        engine.onOrientationChanged(DeviceOrientation.FACE_UP)
        advance(0)

        assertEquals(GlyphState.OFF, engine.state.value.glyphState)
        assertEquals(1, controller.clearCalls)
    }

    @Test
    fun `peek request ignored outside press to peek mode`() {
        engine.onOrientationChanged(DeviceOrientation.FACE_DOWN)
        advance(0)
        val callsAfterFlip = controller.showClockCalls

        engine.onPeekRequested()
        advance(0)

        assertEquals(callsAfterFlip, controller.showClockCalls)
    }

    @Test
    fun `test glyph shows the clock without requiring face down`() {
        engine.testGlyph()
        advance(0)

        assertEquals(1, controller.showClockCalls)
        assertEquals(GlyphState.OFF, engine.state.value.glyphState)
    }
}
