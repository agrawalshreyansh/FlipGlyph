@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.flipglyph.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.flipglyph.domain.GlyphState
import com.flipglyph.sensors.DeviceOrientation
import com.flipglyph.ui.FlipGlyphViewModel
import com.flipglyph.ui.theme.NothingColors

@Composable
fun HomeScreen(viewModel: FlipGlyphViewModel, onOpenSettings: () -> Unit) {
    val settings by viewModel.settings.collectAsState()
    val engineState by viewModel.engineState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FLIPGLYPH") },
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
                    "FLIPGLYPH CURRENTLY SUPPORTS NOTHING PHONE (4A) PRO ONLY.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Text(
                if (settings.enabled) "ENABLED" else "DISABLED",
                style = MaterialTheme.typography.labelLarge,
                color = if (settings.enabled) NothingColors.Red else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Box(contentAlignment = Alignment.Center) {
                DotMatrixBackdrop(active = engineState.glyphState == GlyphState.ACTIVE)
                OrientationGlyph(orientation = engineState.orientation, active = engineState.glyphState == GlyphState.ACTIVE)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ENABLE FLIPGLYPH", style = MaterialTheme.typography.labelLarge)
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
                Text("TEST GLYPH")
            }
        }
    }
}

@Composable
private fun OrientationGlyph(orientation: DeviceOrientation, active: Boolean) {
    val label = when (orientation) {
        DeviceOrientation.FACE_DOWN -> if (active) "FACE-DOWN · ACTIVE" else "FACE-DOWN"
        DeviceOrientation.FACE_UP -> "FACE-UP"
        DeviceOrientation.UNKNOWN -> "— — —"
    }
    Text(
        label,
        style = MaterialTheme.typography.displaySmall,
        color = if (active) NothingColors.Red else MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(32.dp),
    )
}

/** A quiet nod to the physical Glyph Matrix this app drives — an original dot grid, not a copy of it. */
@Composable
private fun DotMatrixBackdrop(active: Boolean) {
    val dotColor = if (active) NothingColors.Red.copy(alpha = 0.35f) else NothingColors.Divider
    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        val columns = 13
        val rows = 5
        val cellWidth = size.width / columns
        val cellHeight = size.height / rows
        val dotRadius = minOf(cellWidth, cellHeight) * 0.12f
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                drawRoundRect(
                    color = dotColor,
                    topLeft = Offset(
                        col * cellWidth + cellWidth / 2 - dotRadius,
                        row * cellHeight + cellHeight / 2 - dotRadius,
                    ),
                    size = Size(dotRadius * 2, dotRadius * 2),
                    cornerRadius = CornerRadius(dotRadius),
                )
            }
        }
    }
}
