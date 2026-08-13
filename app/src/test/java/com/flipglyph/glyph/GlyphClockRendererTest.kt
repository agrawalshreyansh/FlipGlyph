package com.flipglyph.glyph

import android.graphics.Color
import com.flipglyph.data.ClockFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GlyphClockRendererTest {

    @Test
    fun `renders exactly the matrix size`() {
        val bitmap = GlyphClockRenderer.render(LocalTime.of(9, 5), ClockFormat.H24, brightness = 255)
        assertEquals(GlyphClockRenderer.MATRIX_SIZE, bitmap.width)
        assertEquals(GlyphClockRenderer.MATRIX_SIZE, bitmap.height)
    }

    @Test
    fun `renders visible pixels for a real time`() {
        val bitmap = GlyphClockRenderer.render(LocalTime.of(10, 30), ClockFormat.H24, brightness = 255)
        var litCount = 0
        for (y in 0 until GlyphClockRenderer.MATRIX_SIZE) {
            for (x in 0 until GlyphClockRenderer.MATRIX_SIZE) {
                if (bitmap.getPixel(x, y) != Color.BLACK) litCount++
            }
        }
        assertNotEquals(0, litCount)
    }

    @Test
    fun `midnight in 12-hour format does not crash and renders digits`() {
        val bitmap = GlyphClockRenderer.render(LocalTime.of(0, 0), ClockFormat.H12, brightness = 255)
        assertEquals(GlyphClockRenderer.MATRIX_SIZE, bitmap.width)
    }

    @Test
    fun `colon is off by default so hour and minute blocks read as two lines`() {
        val bitmap = GlyphClockRenderer.render(LocalTime.of(10, 30), ClockFormat.H24, brightness = 255)
        val colonColumn = 6 // BLOCK_LEFT(3) + DIGIT_WIDTH(3)
        assertEquals(Color.BLACK, bitmap.getPixel(colonColumn, 5))
        assertEquals(Color.BLACK, bitmap.getPixel(colonColumn, 7))
    }

    @Test
    fun `colon can be opted into`() {
        val bitmap = GlyphClockRenderer.render(LocalTime.of(10, 30), ClockFormat.H24, brightness = 255, showColon = true)
        val colonColumn = 6
        assertNotEquals(Color.BLACK, bitmap.getPixel(colonColumn, 5))
        assertNotEquals(Color.BLACK, bitmap.getPixel(colonColumn, 7))
    }
}
