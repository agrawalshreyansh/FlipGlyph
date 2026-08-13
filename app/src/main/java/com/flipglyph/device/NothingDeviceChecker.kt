package com.flipglyph.device

import android.os.Build

/**
 * Cheap pre-filter before touching the Glyph SDK at all: only "is this a Nothing phone".
 * The authoritative device check is GlyphMatrixManager.register(Glyph.DEVICE_25111p)
 * succeeding at runtime — Build.MODEL for Phone (4a) Pro specifically was not available
 * to verify against real hardware, so this deliberately doesn't guess at it.
 */
object NothingDeviceChecker {

    fun isSupportedDeviceHint(): Boolean = Build.MANUFACTURER.equals("Nothing", ignoreCase = true)
}
