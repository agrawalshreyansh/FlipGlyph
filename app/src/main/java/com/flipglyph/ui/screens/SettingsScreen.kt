@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.flipglyph.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flipglyph.data.AppSettings
import com.flipglyph.data.ClockFormat
import com.flipglyph.domain.ActivationMode
import com.flipglyph.ui.FlipGlyphViewModel

@Composable
fun SettingsScreen(viewModel: FlipGlyphViewModel, onOpenAbout: () -> Unit, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader("Activation")
            ListItem(
                headlineContent = { Text("Enabled") },
                trailingContent = { Switch(checked = settings.enabled, onCheckedChange = viewModel::setEnabled) },
            )
            ActivationModeRow(settings, viewModel)
            TimeoutRow(settings, viewModel)

            SectionHeader("Clock")
            ClockFormatRow(settings, viewModel)
            BrightnessRow(settings, viewModel)

            SectionHeader("Behavior")
            ListItem(
                headlineContent = { Text("Start on boot") },
                trailingContent = {
                    Switch(checked = settings.startOnBoot, onCheckedChange = viewModel::setStartOnBoot)
                },
            )

            SectionHeader("Advanced")
            ListItem(
                headlineContent = { Text("Sensor diagnostics") },
                supportingContent = { Text("Live X/Y/Z and orientation readout") },
            )
            DiagnosticsPanel(viewModel)

            SectionHeader("About")
            ListItem(
                headlineContent = { Text("Device support, SDK status, version") },
                trailingContent = { TextButton(onClick = onOpenAbout) { Text("View") } },
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
    )
    HorizontalDivider()
}

@Composable
private fun ActivationModeRow(settings: AppSettings, viewModel: FlipGlyphViewModel) {
    var expanded by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text("Activation mode") },
        supportingContent = { Text(settings.activationMode.name) },
        modifier = Modifier.fillMaxWidth(),
        trailingContent = {
            Row {
                IconButton(onClick = { expanded = true }) { Text("▾") }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    ActivationMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.name) },
                            onClick = {
                                viewModel.setActivationMode(mode)
                                expanded = false
                            },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun TimeoutRow(settings: AppSettings, viewModel: FlipGlyphViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val label = if (settings.timeoutSeconds <= 0) "Never" else "${settings.timeoutSeconds}s"
    ListItem(
        headlineContent = { Text("Timeout") },
        supportingContent = { Text(label) },
        trailingContent = {
            Row {
                IconButton(onClick = { expanded = true }) { Text("▾") }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    AppSettings.TIMEOUT_OPTIONS_SECONDS.forEach { seconds ->
                        DropdownMenuItem(
                            text = { Text(if (seconds <= 0) "Never" else "${seconds}s") },
                            onClick = {
                                viewModel.setTimeoutSeconds(seconds)
                                expanded = false
                            },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun ClockFormatRow(settings: AppSettings, viewModel: FlipGlyphViewModel) {
    ListItem(
        headlineContent = { Text("24-hour") },
        trailingContent = {
            Switch(
                checked = settings.clockFormat == ClockFormat.H24,
                onCheckedChange = { viewModel.setClockFormat(if (it) ClockFormat.H24 else ClockFormat.H12) },
            )
        },
    )
}

@Composable
private fun BrightnessRow(settings: AppSettings, viewModel: FlipGlyphViewModel) {
    ListItem(
        headlineContent = { Text("Brightness") },
        supportingContent = {
            Slider(
                value = settings.brightness.toFloat(),
                onValueChange = { viewModel.setBrightness(it.toInt()) },
                valueRange = 1f..255f,
            )
        },
    )
}

@Composable
private fun DiagnosticsPanel(viewModel: FlipGlyphViewModel) {
    val diagnostics by viewModel.diagnostics.collectAsState()
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("X=%.2f  Y=%.2f  Z=%.2f".format(diagnostics.x, diagnostics.y, diagnostics.z))
        Text("Orientation: ${diagnostics.orientation}")
        Text("Accelerometer: ${diagnostics.accelerometerAvailable}  Gyroscope: ${diagnostics.gyroscopeAvailable}  Proximity: ${diagnostics.proximityAvailable}")
    }
}
