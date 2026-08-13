package com.flipglyph.glyph

import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.flipglyph.data.ClockFormat
import com.flipglyph.device.NothingDeviceChecker
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.LocalTime
import kotlin.coroutines.resume

private const val TAG = "FlipGlyph.Glyph"

/**
 * The only class that knows Nothing SDK implementation details. Every call is guarded —
 * a Glyph failure must never crash the app, and the Matrix can be silently reclaimed by
 * the system Glyph Toy carousel at any time.
 */
class NothingGlyphController(
    private val context: Context,
    private var clockFormat: ClockFormat = ClockFormat.H24,
    private var brightness: Int = 255,
) : GlyphController {

    private val _availability = MutableStateFlow(GlyphAvailability.SDK_UNAVAILABLE)
    override val availability: StateFlow<GlyphAvailability> = _availability.asStateFlow()

    private var manager: GlyphMatrixManager? = null
    private var registered = false

    fun updateRenderSettings(clockFormat: ClockFormat, brightness: Int) {
        this.clockFormat = clockFormat
        this.brightness = brightness
    }

    override suspend fun initialize() {
        if (!NothingDeviceChecker.isSupportedDeviceHint()) {
            _availability.value = GlyphAvailability.UNSUPPORTED_DEVICE
            return
        }
        if (manager != null && registered) return

        runCatching {
            val mgr = GlyphMatrixManager.getInstance(context)
            suspendCancellableCoroutine { cont ->
                mgr.init(object : GlyphMatrixManager.Callback {
                    override fun onServiceConnected(componentName: ComponentName) {
                        registered = mgr.register(Glyph.DEVICE_25111p)
                        _availability.value = if (registered) {
                            GlyphAvailability.READY
                        } else {
                            GlyphAvailability.REGISTRATION_FAILED
                        }
                        if (cont.isActive) cont.resume(Unit)
                    }

                    override fun onServiceDisconnected(componentName: ComponentName) {
                        registered = false
                        _availability.value = GlyphAvailability.SERVICE_DISCONNECTED
                    }
                })
            }
            manager = mgr
        }.onFailure { e ->
            Log.w(TAG, "Glyph SDK initialization failed: ${e.javaClass.simpleName}")
            _availability.value = GlyphAvailability.SDK_UNAVAILABLE
        }
    }

    override suspend fun showClock(time: LocalTime) {
        val mgr = manager ?: return
        if (_availability.value != GlyphAvailability.READY) return

        runCatching {
            val bitmap = GlyphClockRenderer.render(time, clockFormat, brightness)
            val obj = GlyphMatrixObject.Builder()
                .setImageSource(bitmap)
                .setPosition(0, 0)
                .setBrightness(brightness)
                .build()
            val frame = GlyphMatrixFrame.Builder(context)
                .addTop(obj)
                .build()
            mgr.setAppMatrixFrame(frame.render())
        }.onFailure { e ->
            Log.w(TAG, "setAppMatrixFrame failed: ${e.javaClass.simpleName}")
        }
    }

    override suspend fun clear() {
        val mgr = manager ?: return
        runCatching { mgr.closeAppMatrix() }
            .onFailure { e -> Log.w(TAG, "closeAppMatrix failed: ${e.javaClass.simpleName}") }
    }

    override suspend fun shutdown() {
        runCatching { manager?.unInit() }
        manager = null
        registered = false
        _availability.value = GlyphAvailability.SDK_UNAVAILABLE
    }
}
