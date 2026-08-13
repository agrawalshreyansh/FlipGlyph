package com.flipglyph.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.flipglyph.FlipGlyphApplication
import com.flipglyph.R
import kotlinx.coroutines.launch

private const val NOTIFICATION_CHANNEL_ID = "flipglyph_monitoring"
private const val NOTIFICATION_ID = 1
private const val WAKE_LOCK_TAG = "FlipGlyph:orientationMonitoring"

/**
 * Keeps orientation monitoring alive while the screen is locked and the app isn't visible.
 * Only running while FlipGlyph is enabled — the notification is the visible, user-facing
 * explanation of why that's necessary, per Android's background execution limits.
 *
 * A partial wake lock is required here, not optional: the accelerometer is a non-wakeup
 * sensor, so once the CPU deep-sleeps (screen off for a bit) its events stop being delivered
 * even though this foreground service is still alive. The wake lock keeps the CPU awake —
 * screen stays off, battery cost is the tradeoff for face-down detection actually working
 * when the phone is locked, which is the whole point of the app.
 */
class FlipGlyphService : Service() {

    private lateinit var app: FlipGlyphApplication
    private var wakeLock: PowerManager.WakeLock? = null

    // Drives ActivationMode.PRESS_TO_PEEK: a power-button (or other wake-key) press is not
    // observable via SensorManager, so it's picked up as a screen on/off transition instead.
    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            app.engine.onPeekRequested()
        }
    }

    override fun onCreate() {
        super.onCreate()
        app = application as FlipGlyphApplication
        startForeground(NOTIFICATION_ID, buildNotification())
        app.applicationScope.launch { app.glyphController.initialize() }
        app.orientationDetector.start()

        val powerManager = getSystemService(PowerManager::class.java)
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            setReferenceCounted(false)
            acquire()
        }

        ContextCompat.registerReceiver(
            this,
            screenStateReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        unregisterReceiver(screenStateReceiver)
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
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
