package com.flipglyph.data

import com.flipglyph.domain.ActivationMode

enum class ClockFormat { H12, H24 }

/** 0 means "Never" (no automatic timeout); glyph stays active until FACE_UP. */
data class AppSettings(
    val enabled: Boolean = false,
    val timeoutSeconds: Int = 10,
    val clockFormat: ClockFormat = ClockFormat.H24,
    val brightness: Int = 255,
    val activationMode: ActivationMode = ActivationMode.FLIP_TO_ACTIVATE,
    val batterySaverEnabled: Boolean = true,
    val startOnBoot: Boolean = true,
    val proximityProtectionEnabled: Boolean = true,
) {
    companion object {
        val TIMEOUT_OPTIONS_SECONDS = listOf(5, 10, 30, 60, 0) // 0 = Never
    }
}
