package com.flipglyph.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.flipglyph.ui.screens.AboutScreen
import com.flipglyph.ui.screens.HomeScreen
import com.flipglyph.ui.screens.SettingsScreen
import com.flipglyph.ui.theme.NothingDarkColorScheme
import com.flipglyph.ui.theme.NothingTypography

private enum class Screen { HOME, SETTINGS, ABOUT }

@Composable
fun FlipGlyphApp(viewModel: FlipGlyphViewModel) {
    // Always dark — this app is a companion to a black-background LED matrix, not something
    // that should follow the system's light/dark setting.
    MaterialTheme(colorScheme = NothingDarkColorScheme, typography = NothingTypography) {
        Surface(modifier = Modifier, color = MaterialTheme.colorScheme.background) {
            var screen by remember { mutableStateOf(Screen.HOME) }

            when (screen) {
                Screen.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onOpenSettings = { screen = Screen.SETTINGS },
                )
                Screen.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    onOpenAbout = { screen = Screen.ABOUT },
                    onBack = { screen = Screen.HOME },
                )
                Screen.ABOUT -> AboutScreen(
                    viewModel = viewModel,
                    onBack = { screen = Screen.SETTINGS },
                )
            }
        }
    }
}
