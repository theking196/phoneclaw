package com.example.universal

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * VisionClient
 *
 * Moondream vision API integration. Implements magicClicker and magicScraper functionality.
 */
class VisionClient(private val ctx: Context) {

    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    suspend fun magicScraper(question: String, screenshot: Bitmap): String {
        val apiKey = LocalStorage.get(ctx.applicationContext, "moondream_api_key", "")
        if (apiKey.isBlank()) {
            return "Error: No Moondream API key configured"
        }

        val base64 = bitmapToBase64(screenshot)

        val payload = JSONObject().apply {
            put("model", "moondream:latest")
            put("prompt", question)
            put("image", "data:image/jpeg;base64,$base64")
            put("stream", false)
        }

        val request = Request.Builder()
            .url("https://api.moondream.ai/v1/chat")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(payload.toString().toRequestBody(JSON))
            .build()

        return try {
            val response = client.newCall(request).execute()
            response.body?.string() ?: "No response"
        } catch (e: IOException) {
            "Vision error: ${e.message}"
        }
    }

    suspend fun magicClicker(description: String, screenshot: Bitmap): Point? {
        val result = magicScraper(
            "Return only the x,y pixel coordinates as numbers separated by comma to click on: $description",
            screenshot
        )

        return try {
            val parts = result.split(",").map { it.trim().toInt() }
            Point(parts[0], parts[1])
        } catch (e: Exception) {
            null
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    fun isConfigured(): Boolean {
        return LocalStorage.get(ctx.applicationContext, "moondream_api_key", "").isNotBlank()
    }

    data class Point(val x: Int, val y: Int)

    companion object {
        @Volatile
        private var INSTANCE: VisionClient? = null

        fun getInstance(ctx: Context): VisionClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VisionClient(ctx).also { INSTANCE = it }
            }
        }
    }
}
