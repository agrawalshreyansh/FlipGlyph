package com.flipglyph.domain

import com.flipglyph.data.AppSettings
import com.flipglyph.glyph.GlyphController
import com.flipglyph.sensors.DeviceOrientation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * Reacts to already-debounced orientation changes and drives the Glyph Matrix through
 * [glyphController]. Owns no sensor or SDK details — those live in the sensors and glyph
 * packages respectively.
 */
class FlipGlyphEngine(
    private val scope: CoroutineScope,
    private val glyphController: GlyphController,
    private val settings: () -> AppSettings,
    private val nowMs: () -> Long = System::currentTimeMillis,
    private val clockNow: () -> LocalTime = LocalTime::now,
) {
    private val _state = MutableStateFlow(FlipGlyphState())
    val state: StateFlow<FlipGlyphState> = _state.asStateFlow()

    private var timeoutJob: Job? = null
    private var clockTickJob: Job? = null

    fun onOrientationChanged(orientation: DeviceOrientation) {
        val previous = _state.value.orientation
        _state.value = _state.value.copy(orientation = orientation)
        if (orientation == previous) return

        when (orientation) {
            DeviceOrientation.FACE_DOWN -> activate()
            DeviceOrientation.FACE_UP -> deactivate()
            DeviceOrientation.UNKNOWN -> Unit
        }
    }

    /** Shows the current clock immediately, bypassing orientation/enabled checks, for the Test Glyph button. */
    fun testGlyph() {
        scope.launch { glyphController.showClock(clockNow()) }
    }

    private fun activate() {
        if (!settings().enabled) return
        cancelJobs()
        val now = nowMs()
        _state.value = _state.value.copy(glyphState = GlyphState.ACTIVE, activatedAt = now, timeoutAt = null)
        startClockTicking()
        scheduleTimeout()
    }

    private fun deactivate() {
        cancelJobs()
        val wasOff = _state.value.glyphState == GlyphState.OFF
        _state.value = _state.value.copy(glyphState = GlyphState.OFF, activatedAt = null, timeoutAt = null)
        if (!wasOff) scope.launch { glyphController.clear() }
    }

    private fun startClockTicking() {
        clockTickJob = scope.launch {
            while (isActive) {
                glyphController.showClock(clockNow())
                delay(millisUntilNextMinuteBoundary())
            }
        }
    }

    private fun scheduleTimeout() {
        if (settings().activationMode == ActivationMode.STAY_ACTIVE_WHILE_FLIPPED) return
        val timeoutSeconds = settings().timeoutSeconds
        if (timeoutSeconds <= 0) return // "Never"

        _state.value = _state.value.copy(timeoutAt = nowMs() + timeoutSeconds * 1000L)
        timeoutJob = scope.launch {
            delay(timeoutSeconds * 1000L)
            onTimeout()
        }
    }

    private fun onTimeout() {
        clockTickJob?.cancel()
        clockTickJob = null
        _state.value = _state.value.copy(glyphState = GlyphState.TIMED_OUT, timeoutAt = null)
        scope.launch { glyphController.clear() }
    }

    private fun cancelJobs() {
        timeoutJob?.cancel()
        timeoutJob = null
        clockTickJob?.cancel()
        clockTickJob = null
    }

    private fun millisUntilNextMinuteBoundary(): Long {
        val now = clockNow()
        val secondsRemaining = 60 - now.second
        return secondsRemaining * 1000L - (now.nano / 1_000_000L)
    }
}
