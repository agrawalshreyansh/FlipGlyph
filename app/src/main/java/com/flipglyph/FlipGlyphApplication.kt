package com.flipglyph

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import com.flipglyph.data.AppSettings
import com.flipglyph.data.SettingsRepository
import com.flipglyph.domain.FlipGlyphEngine
import com.flipglyph.glyph.NothingGlyphController
import com.flipglyph.sensors.OrientationDetector
import com.flipglyph.service.FlipGlyphService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Minimal manual DI: a handful of process-scoped singletons, no framework needed. */
class FlipGlyphApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val glyphController: NothingGlyphController by lazy { NothingGlyphController(this) }
    val orientationDetector: OrientationDetector by lazy { OrientationDetector(this) }

    @Volatile var currentSettings: AppSettings = AppSettings()
        private set

    val engine: FlipGlyphEngine by lazy {
        FlipGlyphEngine(applicationScope, glyphController, settings = { currentSettings })
    }

    override fun onCreate() {
        super.onCreate()

        var wasEnabled = false
        applicationScope.launch {
            settingsRepository.settings.collect { settings ->
                currentSettings = settings
                glyphController.updateRenderSettings(settings.clockFormat, settings.brightness)

                if (settings.enabled != wasEnabled) {
                    wasEnabled = settings.enabled
                    val intent = Intent(this@FlipGlyphApplication, FlipGlyphService::class.java)
                    if (settings.enabled) {
                        ContextCompat.startForegroundService(this@FlipGlyphApplication, intent)
                    } else {
                        stopService(intent)
                    }
                }
            }
        }

        // NOTE: proximityProtectionEnabled is intentionally not wired to anything here.
        // It used to force FACE_DOWN to UNKNOWN whenever the proximity sensor read "near",
        // meant to catch pocket insertion. But a phone resting face-down on a table reads
        // "near" too — the screen-side sensor is pressed against the surface — so that gate
        // was indistinguishable from the app's core use case and silently broke it. Proximity
        // alone can't tell "table" from "pocket"; needs a better signal before this is worth
        // reintroducing.
        applicationScope.launch {
            orientationDetector.orientation.collect { orientation -> engine.onOrientationChanged(orientation) }
        }
    }
}
