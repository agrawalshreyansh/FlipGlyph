package com.flipglyph.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.flipglyph.FlipGlyphApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Restores monitoring after reboot. Only starts the service — the engine itself waits for
 * the next real FACE_DOWN reading, it never activates the Glyph Matrix on its own.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val app = context.applicationContext as FlipGlyphApplication
        val pendingResult = goAsync()
        app.applicationScope.launch {
            try {
                val settings = app.settingsRepository.settings.first()
                if (settings.enabled && settings.startOnBoot) {
                    ContextCompat.startForegroundService(context, Intent(context, FlipGlyphService::class.java))
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
