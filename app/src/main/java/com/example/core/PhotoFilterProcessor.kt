package com.example.core

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix as ComposeColorMatrix

/**
 * Basic non-destructive photo filters that can be previewed in Compose
 * and baked into captured Bitmaps by [PhotoFilterProcessor].
 */
enum class BasicImageFilter(val label: String) {
    ORIGINAL("Original"),
    GRAYSCALE("Grayscale"),
    SEPIA("Sepia"),
    WARM_FILM("Warm Film"),
    GOLDEN("Golden"),
    VIVID("Vivid"),
    CYBER("Cyber");

    internal fun toAndroidColorMatrix(): ColorMatrix? = when (this) {
        ORIGINAL -> null
        GRAYSCALE -> ColorMatrix().apply {
            setSaturation(0f)
        }
        SEPIA -> ColorMatrix(
            floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        WARM_FILM -> ColorMatrix(
            floatArrayOf(
                1.08f, 0.04f, 0f, 0f, 8f,
                0f, 1.02f, 0f, 0f, 4f,
                0f, 0f, 0.88f, 0f, -6f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        GOLDEN -> ColorMatrix(
            floatArrayOf(
                1.15f, 0.05f, 0f, 0f, 12f,
                0f, 1.04f, 0f, 0f, 6f,
                0f, 0f, 0.80f, 0f, -10f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        VIVID -> ColorMatrix().apply {
            setSaturation(1.38f)
            val contrast = ColorMatrix(
                floatArrayOf(
                    1.12f, 0f, 0f, 0f, -10f,
                    0f, 1.12f, 0f, 0f, -10f,
                    0f, 0f, 1.12f, 0f, -10f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            postConcat(contrast)
        }
        CYBER -> ColorMatrix(
            floatArrayOf(
                1.05f, 0f, 0.12f, 0f, 6f,
                0f, 0.94f, 0.08f, 0f, 0f,
                0.08f, 0.05f, 1.22f, 0f, 14f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    fun toComposeColorFilter(): ColorFilter? {
        val matrix = toAndroidColorMatrix() ?: return null
        return ColorFilter.colorMatrix(ComposeColorMatrix(matrix.array.copyOf()))
    }
}

/**
 * Applies basic color-matrix image filters (such as Grayscale, Sepia, Golden, and Vivid)
 * to captured camera [Bitmap] instances without mutating the original source bitmap.
 */
class PhotoFilterProcessor {

    fun applyFilter(source: Bitmap, filter: BasicImageFilter): Bitmap {
        val colorMatrix = filter.toAndroidColorMatrix() ?: return source
        val safeConfig = source.config ?: Bitmap.Config.ARGB_8888
        val targetConfig = if (safeConfig == Bitmap.Config.HARDWARE) Bitmap.Config.ARGB_8888 else safeConfig
        val output = Bitmap.createBitmap(source.width, source.height, targetConfig)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }
}
