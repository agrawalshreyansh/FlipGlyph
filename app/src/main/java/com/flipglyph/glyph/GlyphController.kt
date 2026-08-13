package com.flipglyph.glyph

import kotlinx.coroutines.flow.StateFlow
import java.time.LocalTime

enum class GlyphAvailability {
    READY,
    SDK_UNAVAILABLE,
    UNSUPPORTED_DEVICE,
    REGISTRATION_FAILED,
    SERVICE_DISCONNECTED,
}

interface GlyphController {
    val availability: StateFlow<GlyphAvailability>
    suspend fun initialize()
    suspend fun showClock(time: LocalTime)
    suspend fun clear()
    suspend fun shutdown()
}
