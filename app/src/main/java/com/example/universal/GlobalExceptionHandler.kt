package com.example.universal

import android.app.Application
import android.content.Context
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Global Uncaught Exception Handler
 *
 * Prevents app crashes and catches all unhandled exceptions.
 * Logs everything and continues execution instead of crashing.
 */
object GlobalExceptionHandler {

    private val TAG = "GlobalExceptionHandler"

    fun install(ctx: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "UNCAUGHT EXCEPTION in thread ${thread.name}", throwable)

            // Log to debug log
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                LocalStorage.appendDebugLog(ctx, "CRASH: ${throwable.message}\n${sw}")
            } catch (e: Exception) {
                // Ignore
            }

            // Don't crash the app
            Log.e(TAG, "Prevented app crash. Continuing execution.")

            // If this is the main thread, reset the looper
            if (thread == ctx.mainLooper.thread) {
                while (true) {
                    try {
                        android.os.Looper.loop()
                    } catch (e: Throwable) {
                        Log.e(TAG, "Exception in main looper", e)
                    }
                }
            }
        }

        Log.d(TAG, "Global exception handler installed successfully")
    }

    fun runSafe(ctx: Context, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            Log.e(TAG, "Caught exception", t)
            try {
                LocalStorage.appendDebugLog(ctx, "Exception: ${t.message}")
            } catch (e: Exception) {}
        }
    }

    suspend fun runSafeSuspend(ctx: Context, block: suspend () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            Log.e(TAG, "Caught suspend exception", t)
            try {
                LocalStorage.appendDebugLog(ctx, "Exception: ${t.message}")
            } catch (e: Exception) {}
        }
    }
}