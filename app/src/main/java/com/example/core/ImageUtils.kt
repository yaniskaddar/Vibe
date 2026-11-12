package com.example.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import android.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

enum class FilterPreset(val label: String) {
    ORIGINAL("Original"),
    WARM_FILM("Warm Film"),
    OBSIDIAN_MONO("Obsidian Mono"),
    GOLDEN_HOUR("Golden Hour"),
    CYBER_GLOW("Cyber Glow");

    fun toColorFilter(): ColorFilter? = when (this) {
        ORIGINAL -> null
        WARM_FILM -> ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    1.08f, 0.04f, 0f, 0f, 8f,
                    0f, 1.02f, 0f, 0f, 4f,
                    0f, 0f, 0.88f, 0f, -6f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
        OBSIDIAN_MONO -> ColorFilter.colorMatrix(
            ColorMatrix().apply { setToSaturation(0f) }
        )
        GOLDEN_HOUR -> ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    1.15f, 0.05f, 0f, 0f, 12f,
                    0f, 1.04f, 0f, 0f, 6f,
                    0f, 0f, 0.80f, 0f, -10f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
        CYBER_GLOW -> ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    1.05f, 0f, 0.12f, 0f, 6f,
                    0f, 0.94f, 0.08f, 0f, 0f,
                    0.08f, 0.05f, 1.22f, 0f, 14f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    }

    companion object {
        fun fromName(name: String?): FilterPreset =
            entries.firstOrNull { it.name == name } ?: ORIGINAL
    }
}

object ImageUtils {
    private const val DATA_URI_PREFIX = "data:image/jpeg;base64,"

    suspend fun decodeUriToBitmap(context: Context, uri: Uri, maxDim: Int = 960): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundsOpts)
                }
                val sampleSize = calculateInSampleSize(boundsOpts, maxDim, maxDim)
                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOpts)
                } ?: return@withContext null

                val rotation = try {
                    context.contentResolver.openInputStream(uri)?.use { exifStream ->
                        when (ExifInterface(exifStream).getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )) {
                            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                            else -> 0f
                        }
                    } ?: 0f
                } catch (_: Exception) {
                    0f
                }

                val scaled = scaleBitmap(rawBitmap, maxDim)
                if (rotation != 0f) {
                    val matrix = Matrix().apply { postRotate(rotation) }
                    Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, matrix, true)
                } else {
                    scaled
                }
            } catch (_: Exception) {
                null
            }
        }

    fun compressBitmapToDataUri(
        bitmap: Bitmap,
        maxDimension: Int = 720,
        maxBytes: Int = 175_000
    ): String {
        val scaled = scaleBitmap(bitmap, maxDimension)
        var quality = 82
        var bytes: ByteArray
        do {
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            bytes = out.toByteArray()
            quality -= 10
        } while (bytes.size > maxBytes && quality >= 32)
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "$DATA_URI_PREFIX$encoded"
    }

    fun decodeDataUriToBitmap(payload: String): Bitmap? {
        if (payload.isBlank()) return null
        return try {
            val cleanBase64 = if (payload.startsWith("data:")) {
                payload.substringAfter("base64,", "")
            } else {
                payload
            }
            if (cleanBase64.isBlank()) return null
            val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun cacheVideoAndExtractThumbnail(
        context: Context,
        videoUri: Uri
    ): Pair<String, String?>? = withContext(Dispatchers.IO) {
        try {
            val reelsDir = File(context.filesDir, "reels").apply { mkdirs() }
            val outFile = File(reelsDir, "reel_${UUID.randomUUID()}.mp4")
            context.contentResolver.openInputStream(videoUri)?.use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            val retriever = MediaMetadataRetriever()
            var thumbDataUri: String? = null
            try {
                retriever.setDataSource(outFile.absolutePath)
                val frame = retriever.getFrameAtTime(0)
                if (frame != null) {
                    thumbDataUri = compressBitmapToDataUri(frame, maxDimension = 640, maxBytes = 140_000)
                }
            } catch (_: Exception) {
                // Fallback if frame extraction unavailable
            } finally {
                retriever.release()
            }
            outFile.absolutePath to thumbDataUri
        } catch (_: Exception) {
            null
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDim && height <= maxDim) return bitmap
        val ratio = width.toFloat() / height.toFloat()
        val targetW: Int
        val targetH: Int
        if (width >= height) {
            targetW = maxDim
            targetH = (maxDim / ratio).toInt().coerceAtLeast(1)
        } else {
            targetH = maxDim
            targetW = (maxDim * ratio).toInt().coerceAtLeast(1)
        }
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
