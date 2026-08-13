@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.flipglyph.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flipglyph.domain.GlyphState
import com.flipglyph.sensors.DeviceOrientation
import com.flipglyph.ui.FlipGlyphViewModel

@Composable
fun HomeScreen(viewModel: FlipGlyphViewModel, onOpenSettings: () -> Unit) {
    val settings by viewModel.settings.collectAsState()
    val engineState by viewModel.engineState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FlipGlyph") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (!viewModel.isDeviceSupported) {
                Text(
                    "FlipGlyph currently supports Nothing Phone (4a) Pro only.",
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Text(
                if (settings.enabled) "Enabled" else "Disabled",
                style = MaterialTheme.typography.titleMedium,
            )

            OrientationGlyph(orientation = engineState.orientation, active = engineState.glyphState == GlyphState.ACTIVE)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Enable FlipGlyph", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = settings.enabled,
                    onCheckedChange = viewModel::setEnabled,
                    enabled = viewModel.isDeviceSupported,
                )
            }

            Button(
                onClick = viewModel::testGlyph,
                modifier = Modifier.fillMaxWidth(),
                enabled = viewModel.isDeviceSupported,
            ) {
                Text("Test Glyph")
            }
        }
    }
}

@Composable
private fun OrientationGlyph(orientation: DeviceOrientation, active: Boolean) {
    val label = when (orientation) {
        DeviceOrientation.FACE_DOWN -> if (active) "Face-down · glyph active" else "Face-down"
        DeviceOrientation.FACE_UP -> "Face-up"
        DeviceOrientation.UNKNOWN -> "—"
    }
    Text(label, style = MaterialTheme.typography.displaySmall)
}
