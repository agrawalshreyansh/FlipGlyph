package com.flipglyph.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flipglyph.FlipGlyphApplication
import com.flipglyph.data.AppSettings
import com.flipglyph.data.ClockFormat
import com.flipglyph.device.NothingDeviceChecker
import com.flipglyph.domain.ActivationMode
import com.flipglyph.domain.FlipGlyphState
import com.flipglyph.glyph.GlyphAvailability
import com.flipglyph.data.SettingsRepository
import com.flipglyph.sensors.SensorDiagnostics
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FlipGlyphViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FlipGlyphApplication

    val isDeviceSupported: Boolean = NothingDeviceChecker.isSupportedDeviceHint()

    val settings: StateFlow<AppSettings> = app.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val engineState: StateFlow<FlipGlyphState> = app.engine.state

    val glyphAvailability: StateFlow<GlyphAvailability> = app.glyphController.availability

    val diagnostics: StateFlow<SensorDiagnostics> = app.orientationDetector.diagnostics

    fun setEnabled(value: Boolean) = update { it.setEnabled(value) }
    fun setTimeoutSeconds(value: Int) = update { it.setTimeoutSeconds(value) }
    fun setClockFormat(value: ClockFormat) = update { it.setClockFormat(value) }
    fun setBrightness(value: Int) = update { it.setBrightness(value) }
    fun setActivationMode(value: ActivationMode) = update { it.setActivationMode(value) }
    fun setBatterySaverEnabled(value: Boolean) = update { it.setBatterySaverEnabled(value) }
    fun setStartOnBoot(value: Boolean) = update { it.setStartOnBoot(value) }
    fun setProximityProtectionEnabled(value: Boolean) = update { it.setProximityProtectionEnabled(value) }

    fun testGlyph() = app.engine.testGlyph()

    private inline fun update(crossinline block: suspend (SettingsRepository) -> Unit) {
        viewModelScope.launch { block(app.settingsRepository) }
    }
}
