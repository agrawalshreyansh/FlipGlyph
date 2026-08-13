package com.flipglyph.glyph

import android.graphics.Bitmap
import android.graphics.Color
import com.flipglyph.data.ClockFormat
import java.time.LocalTime

/**
 * Renders HH:MM into a 13x13 bitmap for the Glyph Matrix. The matrix is closer to square
 * than wide, so digits are stacked (hours on top, minutes below) rather than laid out in
 * one horizontal row — a 4-digit horizontal clock does not fit legibly at 13px wide.
 */
object GlyphClockRenderer {

    const val MATRIX_SIZE = 13
    private const val DIGIT_WIDTH = 3
    private const val DIGIT_HEIGHT = 5
    private const val DIGIT_GAP = 1
    private val BLOCK_LEFT = (MATRIX_SIZE - (DIGIT_WIDTH * 2 + DIGIT_GAP)) / 2 // = 3

    // 3x5 pixel digits, one string per row, '1' = lit.
    private val DIGITS: Map<Char, List<String>> = mapOf(
        '0' to listOf("111", "101", "101", "101", "111"),
        '1' to listOf("010", "110", "010", "010", "111"),
        '2' to listOf("111", "001", "111", "100", "111"),
        '3' to listOf("111", "001", "111", "001", "111"),
        '4' to listOf("101", "101", "111", "001", "001"),
        '5' to listOf("111", "100", "111", "001", "111"),
        '6' to listOf("111", "100", "111", "101", "111"),
        '7' to listOf("111", "001", "001", "001", "001"),
        '8' to listOf("111", "101", "111", "101", "111"),
        '9' to listOf("111", "101", "111", "001", "111"),
        ' ' to listOf("000", "000", "000", "000", "000"),
    )

    // Default off: on a 13-row matrix the colon dots read as a visible third line between
    // the hour and minute blocks, breaking the intended 2-line stacked layout.
    fun render(time: LocalTime, clockFormat: ClockFormat, brightness: Int, showColon: Boolean = false): Bitmap {
        val grid = Array(MATRIX_SIZE) { BooleanArray(MATRIX_SIZE) }

        val (h1, h2) = hourDigits(time, clockFormat)
        val minute = time.minute
        val m1 = '0' + (minute / 10)
        val m2 = '0' + (minute % 10)

        drawDigit(grid, h1, top = 0, left = BLOCK_LEFT)
        drawDigit(grid, h2, top = 0, left = BLOCK_LEFT + DIGIT_WIDTH + DIGIT_GAP)
        if (showColon) drawColon(grid)
        drawDigit(grid, m1, top = MATRIX_SIZE - DIGIT_HEIGHT, left = BLOCK_LEFT)
        drawDigit(grid, m2, top = MATRIX_SIZE - DIGIT_HEIGHT, left = BLOCK_LEFT + DIGIT_WIDTH + DIGIT_GAP)

        return toBitmap(grid, brightness.coerceIn(0, 255))
    }

    private fun hourDigits(time: LocalTime, clockFormat: ClockFormat): Pair<Char, Char> {
        val hour = when (clockFormat) {
            ClockFormat.H24 -> time.hour
            ClockFormat.H12 -> {
                val h = time.hour % 12
                if (h == 0) 12 else h
            }
        }
        val tens = if (hour < 10) ' ' else '0' + (hour / 10)
        val ones = '0' + (hour % 10)
        return tens to ones
    }

    private fun drawDigit(grid: Array<BooleanArray>, digit: Char, top: Int, left: Int) {
        val pattern = DIGITS[digit] ?: return
        for (row in pattern.indices) {
            for (col in pattern[row].indices) {
                if (pattern[row][col] == '1') {
                    val y = top + row
                    val x = left + col
                    if (y in 0 until MATRIX_SIZE && x in 0 until MATRIX_SIZE) grid[y][x] = true
                }
            }
        }
    }

    private fun drawColon(grid: Array<BooleanArray>) {
        val col = BLOCK_LEFT + DIGIT_WIDTH // gap column between the two hour/minute digits
        val midRow = MATRIX_SIZE / 2
        grid[midRow - 1][col] = true
        grid[midRow + 1][col] = true
    }

    private fun toBitmap(grid: Array<BooleanArray>, brightness: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(MATRIX_SIZE, MATRIX_SIZE, Bitmap.Config.ARGB_8888)
        val lit = Color.rgb(brightness, brightness, brightness)
        for (y in 0 until MATRIX_SIZE) {
            for (x in 0 until MATRIX_SIZE) {
                bitmap.setPixel(x, y, if (grid[y][x]) lit else Color.BLACK)
            }
        }
        return bitmap
    }
}
