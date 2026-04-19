package com.example.universal

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log

object OtpReader {
    private const val TAG = "OtpReader"
    private val OTP_REGEX = Regex("""(?<!\d)(\d{4,8})(?!\d)""")

    fun getLatestOtp(ctx: Context, withinMs: Long = 10 * 60 * 1000L): String? {
        return try {
            val uri = Uri.parse("content://sms/inbox")
            val since = System.currentTimeMillis() - withinMs
            val cursor: Cursor? = ctx.contentResolver.query(
                uri, arrayOf("body", "date"), "date > ?", arrayOf(since.toString()), "date DESC"
            )
            cursor?.use { c ->
                while (c.moveToNext()) {
                    val body = c.getString(0) ?: continue
                    val match = OTP_REGEX.find(body)
                    if (match != null) { Log.d(TAG, "Found OTP: ${match.value}"); return match.value }
                }
            }
            null
        } catch (e: Exception) { Log.e(TAG, "Error: ${e.message}"); null }
    }

    suspend fun waitForOtp(ctx: Context, timeoutMs: Long = 60_000L, pollIntervalMs: Long = 3_000L): String? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val otp = getLatestOtp(ctx, withinMs = timeoutMs)
            if (otp != null) return otp
            kotlinx.coroutines.delay(pollIntervalMs)
        }
        return null
    }
}
