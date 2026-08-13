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
import com.flipglyph.domain.ActivationMode
import com.flipglyph.domain.GlyphState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val NOTIFICATION_CHANNEL_ID = "flipglyph_monitoring"
private const val NOTIFICATION_ID = 1
private const val WAKE_LOCK_TAG = "FlipGlyph:orientationMonitoring"
private const val PEEK_WAKE_LOCK_TAG = "FlipGlyph:peekSample"
private const val PEEK_WAKE_LOCK_TIMEOUT_MS = 2_000L

/**
 * Keeps orientation monitoring alive while the screen is locked and the app isn't visible.
 * Only running while FlipGlyph is enabled — the notification is the visible, user-facing
 * explanation of why that's necessary, per Android's background execution limits.
 *
 * Continuous accelerometer monitoring needs a partial wake lock: it's a non-wakeup sensor, so
 * once the CPU deep-sleeps its events stop being delivered even with this foreground service
 * alive. That's a real, deliberate battery cost for FLIP_TO_ACTIVATE/STAY_ACTIVE_WHILE_FLIPPED,
 * since the sensor IS the trigger for those modes — no way around always-on sensing there.
 *
 * PRESS_TO_PEEK doesn't have that constraint: its trigger is a power-button press (observed as
 * a screen on/off broadcast), not a sensor transition. So it runs continuous monitoring + the
 * wake lock only while the Matrix is actually ACTIVE (to still catch an immediate face-up
 * pickup during the peek window) and takes a brief one-shot sample — its own short wake lock,
 * no continuous registration — at the moment of the button press otherwise. The CPU can deep
 * sleep the rest of the time, which is most of the time.
 */
class FlipGlyphService : Service() {

    private lateinit var app: FlipGlyphApplication
    private var wakeLock: PowerManager.WakeLock? = null
    private var monitoringJob: Job? = null

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (app.currentSettings.activationMode != ActivationMode.PRESS_TO_PEEK) return
            app.applicationScope.launch { sampleAndPeek() }
        }
    }

    override fun onCreate() {
        super.onCreate()
        app = application as FlipGlyphApplication
        startForeground(NOTIFICATION_ID, buildNotification())
        app.applicationScope.launch { app.glyphController.initialize() }

        ContextCompat.registerReceiver(
            this,
            screenStateReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        monitoringJob = app.applicationScope.launch {
            combine(app.settingsRepository.settings, app.engine.state) { settings, state ->
                settings.activationMode != ActivationMode.PRESS_TO_PEEK || state.glyphState == GlyphState.ACTIVE
            }.distinctUntilChanged().collect { needsContinuousMonitoring ->
                if (needsContinuousMonitoring) startContinuousMonitoring() else stopContinuousMonitoring()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        unregisterReceiver(screenStateReceiver)
        monitoringJob?.cancel()
        stopContinuousMonitoring()
        app.applicationScope.launch { app.glyphController.shutdown() }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun sampleAndPeek() {
        val powerManager = getSystemService(PowerManager::class.java)
        val peekLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, PEEK_WAKE_LOCK_TAG)
        peekLock.acquire(PEEK_WAKE_LOCK_TIMEOUT_MS) // self-releasing safety net
        try {
            val orientation = app.orientationDetector.sampleOnce()
            app.engine.onPeekRequested(orientation)
        } finally {
            if (peekLock.isHeld) peekLock.release()
        }
    }

    private fun startContinuousMonitoring() {
        if (wakeLock?.isHeld == true) return
        app.orientationDetector.start()
        val powerManager = getSystemService(PowerManager::class.java)
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun stopContinuousMonitoring() {
        app.orientationDetector.stop()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

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
