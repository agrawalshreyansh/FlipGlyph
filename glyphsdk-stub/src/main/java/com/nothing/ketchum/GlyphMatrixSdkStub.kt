package com.nothing.ketchum

// DEV-ONLY COMPILE STAND-IN for the real Nothing Glyph Matrix Developer Kit AAR.
// Not the vendor SDK. Method/class shapes below follow the public surface described in
// Nothing's Glyph Matrix documentation but are NOT independently verified against the
// real AAR (which is distributed directly to registered developers, not fetchable here).
// Replace this whole module with the real AAR before shipping — see app/build.gradle.kts.

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap

object Glyph {
    const val DEVICE_25111p: String = "25111p"
}

class GlyphMatrixManager private constructor() {

    interface Callback {
        fun onServiceConnected(componentName: ComponentName)
        fun onServiceDisconnected(componentName: ComponentName)
    }

    fun init(callback: Callback) {
        callback.onServiceConnected(ComponentName("com.nothing.glyph", "GlyphMatrixService"))
    }

    fun unInit() {}

    fun register(device: String): Boolean = device == Glyph.DEVICE_25111p

    fun setAppMatrixFrame(frame: IntArray) {}

    fun closeAppMatrix() {}

    companion object {
        @Volatile private var instance: GlyphMatrixManager? = null

        fun getInstance(context: Context): GlyphMatrixManager =
            instance ?: synchronized(this) {
                instance ?: GlyphMatrixManager().also { instance = it }
            }
    }
}

class GlyphMatrixObject private constructor(
    val bitmap: Bitmap,
    val positionX: Int,
    val positionY: Int,
    val scale: Int,
    val brightness: Int,
) {
    class Builder {
        private var bitmap: Bitmap? = null
        private var positionX = 0
        private var positionY = 0
        private var scale = 100
        private var brightness = 255

        fun setImageSource(bitmap: Bitmap) = apply { this.bitmap = bitmap }
        fun setPosition(x: Int, y: Int) = apply { positionX = x; positionY = y }
        fun setScale(scale: Int) = apply { this.scale = scale }
        fun setBrightness(brightness: Int) = apply { this.brightness = brightness }

        fun build(): GlyphMatrixObject =
            GlyphMatrixObject(requireNotNull(bitmap) { "image source required" }, positionX, positionY, scale, brightness)
    }
}

class GlyphMatrixFrame private constructor(private val objects: List<GlyphMatrixObject>) {

    fun render(): IntArray {
        val size = 13
        val out = IntArray(size * size)
        val obj = objects.lastOrNull() ?: return out
        val bmp = obj.bitmap
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (x < bmp.width && y < bmp.height) {
                    val pixel = bmp.getPixel(x, y)
                    val gray = (pixel shr 16 and 0xFF)
                    out[y * size + x] = (gray * obj.brightness) / 255
                }
            }
        }
        return out
    }

    class Builder(private val context: Context) {
        private val objects = mutableListOf<GlyphMatrixObject>()

        fun addTop(obj: GlyphMatrixObject) = apply { objects.add(obj) }
        fun addMid(obj: GlyphMatrixObject) = apply { objects.add(obj) }
        fun addLow(obj: GlyphMatrixObject) = apply { objects.add(obj) }

        fun build(): GlyphMatrixFrame = GlyphMatrixFrame(objects.toList())
    }
}
