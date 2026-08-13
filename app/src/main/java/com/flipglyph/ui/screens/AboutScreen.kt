@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.flipglyph.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.flipglyph.BuildConfig
import com.flipglyph.glyph.GlyphAvailability
import com.flipglyph.ui.FlipGlyphViewModel

private const val DEVELOPER_NAME = "Shreyansh Agrawal"
private const val DEVELOPER_EMAIL = "contactshrage@gmail.com"
private const val REPO_URL = "https://github.com/agrawalshreyansh/FlipGlyph"

@Composable
fun AboutScreen(viewModel: FlipGlyphViewModel, onBack: () -> Unit) {
    val availability by viewModel.glyphAvailability.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ABOUT") },
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("DEVICE SUPPORT", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                if (viewModel.isDeviceSupported) {
                    "Nothing Phone (4a) Pro detected."
                } else {
                    "FlipGlyph currently supports Nothing Phone (4a) Pro only."
                },
            )

            Text("SDK STATUS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(describeAvailability(availability))

            Text("VERSION", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")

            Text("DEVELOPER", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(DEVELOPER_NAME)
            Text(
                DEVELOPER_EMAIL,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                ),
                modifier = Modifier.clickable {
                    context.startActivity(
                        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$DEVELOPER_EMAIL"))
                    )
                },
            )
            Text(
                "Source & contributions",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                ),
                modifier = Modifier.clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL)))
                },
            )
        }
    }
}

private fun describeAvailability(availability: GlyphAvailability): String = when (availability) {
    GlyphAvailability.READY -> "Glyph Matrix ready"
    GlyphAvailability.SDK_UNAVAILABLE -> "Glyph Matrix unavailable"
    GlyphAvailability.UNSUPPORTED_DEVICE -> "Unsupported device"
    GlyphAvailability.REGISTRATION_FAILED -> "Glyph registration failed"
    GlyphAvailability.SERVICE_DISCONNECTED -> "Glyph service disconnected"
}
