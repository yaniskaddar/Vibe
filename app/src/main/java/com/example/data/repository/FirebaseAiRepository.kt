package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class AiCaptionResult(
    val captions: List<String>,
    val hashtags: List<String>,
    val vibeSummary: String
)

class FirebaseAiRepository {
    private val captionSchema = Schema.obj(
        properties = mapOf(
            "captions" to Schema.array(
                items = Schema.string(),
                description = "Three engaging social media caption options (witty, poetic, minimal)"
            ),
            "hashtags" to Schema.array(
                items = Schema.string(),
                description = "Up to 6 relevant hashtags starting with #"
            ),
            "vibeSummary" to Schema.string(
                description = "A concise 2-4 word aesthetic vibe tag for this post"
            )
        )
    )

    private val captionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = captionSchema
            }
        )
    }

    private val imageModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    suspend fun generateSmartCaptions(
        bitmap: Bitmap?,
        userHint: String
    ): Result<AiCaptionResult> {
        return try {
            val response = withTimeout(60_000L) {
                if (bitmap != null) {
                    captionModel.generateContent(
                        content {
                            image(bitmap)
                            text(
                                "Analyze this image and optional creator context: '${userHint.ifBlank { "everyday creative moment" }}'. " +
                                    "Generate 3 distinct Instagram-ready captions (one witty, one poetic, one clean minimal), " +
                                    "up to 6 trending hashtags starting with #, and a 2-4 word vibeSummary."
                            )
                        }
                    )
                } else {
                    captionModel.generateContent(
                        "Create 3 distinct social media captions (witty, poetic, minimal), up to 6 hashtags starting with #, " +
                            "and a 2-4 word vibeSummary for a post about: '${userHint.ifBlank { "creative studio life" }}'."
                    )
                }
            }
            val jsonText = response.text
                ?: return Result.failure(IllegalStateException("Gemini returned an empty response."))
            val json = JSONObject(jsonText)
            val captionsArr = json.getJSONArray("captions")
            val captions = (0 until captionsArr.length()).map { captionsArr.getString(it) }
            val tagsArr = json.getJSONArray("hashtags")
            val hashtags = (0 until tagsArr.length()).map { tag ->
                val clean = tagsArr.getString(tag).trim()
                if (clean.startsWith("#")) clean else "#$clean"
            }
            val vibeSummary = json.optString("vibeSummary", "Creative Vibe")
            Result.success(AiCaptionResult(captions, hashtags, vibeSummary))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Caption generation timed out", e)
            Result.failure(IllegalStateException("Caption generation timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Caption generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun generateOrRemixImage(
        prompt: String,
        sourceBitmap: Bitmap? = null
    ): Result<Bitmap> {
        return try {
            val response = withTimeout(60_000L) {
                if (sourceBitmap != null) {
                    imageModel.generateContent(
                        content {
                            image(sourceBitmap)
                            text(prompt)
                        }
                    )
                } else {
                    imageModel.generateContent(prompt)
                }
            }
            val bitmap = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()?.image
            if (bitmap != null) {
                Result.success(bitmap)
            } else {
                Result.failure(IllegalStateException("No image was returned by the model."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image generation timed out", e)
            Result.failure(IllegalStateException("Image generation timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "FirebaseAiRepo"
    }
}
