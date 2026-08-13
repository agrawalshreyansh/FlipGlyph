package com.flipglyph.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.flipglyph.domain.ActivationMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "flipglyph_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val TIMEOUT_SECONDS = intPreferencesKey("timeout_seconds")
        val CLOCK_FORMAT = stringPreferencesKey("clock_format")
        val BRIGHTNESS = intPreferencesKey("brightness")
        val ACTIVATION_MODE = stringPreferencesKey("activation_mode")
        val START_ON_BOOT = booleanPreferencesKey("start_on_boot")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val defaults = AppSettings()
        AppSettings(
            enabled = prefs[Keys.ENABLED] ?: defaults.enabled,
            timeoutSeconds = prefs[Keys.TIMEOUT_SECONDS] ?: defaults.timeoutSeconds,
            clockFormat = prefs[Keys.CLOCK_FORMAT]?.let { runCatching { ClockFormat.valueOf(it) }.getOrNull() }
                ?: defaults.clockFormat,
            brightness = prefs[Keys.BRIGHTNESS] ?: defaults.brightness,
            activationMode = prefs[Keys.ACTIVATION_MODE]?.let { runCatching { ActivationMode.valueOf(it) }.getOrNull() }
                ?: defaults.activationMode,
            startOnBoot = prefs[Keys.START_ON_BOOT] ?: defaults.startOnBoot,
        )
    }

    suspend fun setEnabled(value: Boolean) = context.dataStore.edit { it[Keys.ENABLED] = value }
    suspend fun setTimeoutSeconds(value: Int) = context.dataStore.edit { it[Keys.TIMEOUT_SECONDS] = value }
    suspend fun setClockFormat(value: ClockFormat) = context.dataStore.edit { it[Keys.CLOCK_FORMAT] = value.name }
    suspend fun setBrightness(value: Int) = context.dataStore.edit { it[Keys.BRIGHTNESS] = value }
    suspend fun setActivationMode(value: ActivationMode) = context.dataStore.edit { it[Keys.ACTIVATION_MODE] = value.name }
    suspend fun setStartOnBoot(value: Boolean) = context.dataStore.edit { it[Keys.START_ON_BOOT] = value }
}
