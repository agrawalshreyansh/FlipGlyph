package com.flipglyph.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.flipglyph.domain.ActivationMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsRepositoryTest {

    @Test
    fun `defaults match the PRD spec`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = SettingsRepository(context).settings.first()

        assertEquals(AppSettings(), settings)
    }

    @Test
    fun `writes persist and are readable back`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SettingsRepository(context)

        repository.setEnabled(true)
        repository.setTimeoutSeconds(30)
        repository.setClockFormat(ClockFormat.H12)
        repository.setBrightness(128)
        repository.setActivationMode(ActivationMode.STAY_ACTIVE_WHILE_FLIPPED)
        repository.setStartOnBoot(false)

        val settings = repository.settings.first()

        assertEquals(
            AppSettings(
                enabled = true,
                timeoutSeconds = 30,
                clockFormat = ClockFormat.H12,
                brightness = 128,
                activationMode = ActivationMode.STAY_ACTIVE_WHILE_FLIPPED,
                startOnBoot = false,
            ),
            settings,
        )
    }
}
