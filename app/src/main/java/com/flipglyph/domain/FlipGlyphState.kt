package com.flipglyph.domain

import com.flipglyph.sensors.DeviceOrientation

enum class GlyphState {
    OFF,
    ACTIVE,
    /** Matrix is off, same as OFF, but reached via timeout rather than a face-up flip or disable. */
    TIMED_OUT,
}

enum class ActivationMode {
    FLIP_TO_ACTIVATE,
    STAY_ACTIVE_WHILE_FLIPPED,
}

data class FlipGlyphState(
    val orientation: DeviceOrientation = DeviceOrientation.UNKNOWN,
    val glyphState: GlyphState = GlyphState.OFF,
    val activatedAt: Long? = null,
    val timeoutAt: Long? = null,
)
