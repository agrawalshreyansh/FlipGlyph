package com.flipglyph

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.flipglyph.ui.FlipGlyphApp
import com.flipglyph.ui.FlipGlyphViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FlipGlyphViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlipGlyphApp(viewModel)
        }
    }
}
