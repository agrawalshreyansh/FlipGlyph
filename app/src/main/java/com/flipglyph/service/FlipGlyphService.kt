package com.flipglyph.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.flipglyph.FlipGlyphApplication
import com.flipglyph.R
import kotlinx.coroutines.launch

private const val NOTIFICATION_CHANNEL_ID = "flipglyph_monitoring"
private const val NOTIFICATION_ID = 1

/**
 * Keeps orientation monitoring alive while the screen is locked and the app isn't visible.
 * Only running while FlipGlyph is enabled — the notification is the visible, user-facing
 * explanation of why that's necessary, per Android's background execution limits.
 */
class FlipGlyphService : Service() {

    private lateinit var app: FlipGlyphApplication

    override fun onCreate() {
        super.onCreate()
        app = application as FlipGlyphApplication
        startForeground(NOTIFICATION_ID, buildNotification())
        app.applicationScope.launch { app.glyphController.initialize() }
        app.orientationDetector.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        app.orientationDetector.stop()
        app.applicationScope.launch { app.glyphController.shutdown() }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_MIN,
                )
            )
        }
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }
}
