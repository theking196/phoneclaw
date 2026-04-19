package com.example.universal

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class ScreenCaptureService : Service() {

    companion object {
        private const val TAG = "ScreenCaptureService"
        private const val CHANNEL_ID = "ScreenCaptureChannel"
        private const val NOTIFICATION_ID = 999

        // Capture interval in milliseconds (adjust this to control frequency)
        private const val CAPTURE_INTERVAL_MS = 500L // 1 second between captures

        @Volatile
        private var _lastCapturedPng: ByteArray? = null

        val lastCapturedPng: ByteArray?
            get() = _lastCapturedPng

        @Volatile
        private var _isReady: Boolean = false

        val isReady: Boolean
            get() = _isReady

        var instance: ScreenCaptureService? = null
            private set
    }

    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Track last capture time for rate limiting
    @Volatile
    private var lastCaptureTime: Long = 0L

    // Track if we're currently processing a screenshot
    @Volatile
    private var isProcessing: Boolean = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Preparing screen capture..."))
        Log.i(TAG, "Service created and in foreground.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED
        val data = intent?.getParcelableExtra<Intent>("data")

        if (resultCode == Activity.RESULT_OK && data != null) {
            val pm = getSystemService(MediaProjectionManager::class.java)
            mediaProjection = pm?.getMediaProjection(resultCode, data)

            if (mediaProjection != null) {
                Log.i(TAG, "MediaProjection acquired. Starting capture.")
                startCapture()
            } else {
                Log.e(TAG, "MediaProjection is null.")
                stopSelf()
            }
        } else if (mediaProjection == null) {
            Log.e(TAG, "No valid MediaProjection permission.")
            stopSelf()
        } else {
            Log.w(TAG, "Service restarted with no new permission. Continuing existing capture.")
        }

        return START_STICKY
    }

    private fun startCapture() {
        try {
            // Get actual display metrics from WindowManager
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val displayMetrics = DisplayMetrics()

            // Get the default display from WindowManager
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay
            display.getRealMetrics(displayMetrics)

            val width = displayMetrics.widthPixels
            val height = displayMetrics.heightPixels
            val density = displayMetrics.densityDpi

            Log.i(TAG, "Display metrics: ${width}x${height} @ ${density}dpi")
            Log.i(TAG, "Capture interval: ${CAPTURE_INTERVAL_MS}ms")

            // Create ImageReader
            imageReader = ImageReader.newInstance(
                width,
                height,
                PixelFormat.RGBA_8888,
                2
            )

            // Create VirtualDisplay
            val virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenCaptureDisplay",
                width,
                height,
                density,
                0,
                imageReader?.surface,
                null,
                null
            )

            if (virtualDisplay == null) {
                Log.e(TAG, "Failed to create VirtualDisplay.")
                stopSelf()
                return
            }

            // Set up image listener with rate limiting
            imageReader?.setOnImageAvailableListener({ reader ->
                // Check if enough time has passed since last capture
                val currentTime = System.currentTimeMillis()
                val timeSinceLastCapture = currentTime - lastCaptureTime

                // Skip if we're still processing or haven't waited long enough
                if (isProcessing || timeSinceLastCapture < CAPTURE_INTERVAL_MS) {
                    // Just acquire and close the image to prevent buffer overflow
                    reader.acquireLatestImage()?.close()
                    return@setOnImageAvailableListener
                }

                var image: android.media.Image? = null
                try {
                    image = reader.acquireLatestImage()
                    if (image == null) {
                        return@setOnImageAvailableListener
                    }

                    // Mark as processing
                    isProcessing = true
                    lastCaptureTime = currentTime

                    val planes = image.planes
                    val buffer: ByteBuffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * width

                    // Create bitmap from buffer
                    val bitmapWidth = width + rowPadding / pixelStride
                    val bitmap = Bitmap.createBitmap(
                        bitmapWidth,
                        height,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.copyPixelsFromBuffer(buffer)

                    // Crop to actual screen size (remove padding)
                    val croppedBitmap = if (rowPadding != 0) {
                        Bitmap.createBitmap(bitmap, 0, 0, width, height)
                    } else {
                        bitmap
                    }

                    // Convert to PNG in background
                    serviceScope.launch(Dispatchers.IO) {
                        try {
                            val baos = ByteArrayOutputStream()
                            croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                            _lastCapturedPng = baos.toByteArray()
                            _isReady = true

                            Log.i(TAG, "Screenshot captured: ${_lastCapturedPng?.size} bytes (interval: ${timeSinceLastCapture}ms)")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error compressing bitmap", e)
                        } finally {
                            croppedBitmap.recycle()
                            if (rowPadding != 0) {
                                bitmap.recycle()
                            }
                            // Mark as done processing
                            isProcessing = false
                        }
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error capturing image", e)
                    isProcessing = false
                } finally {
                    image?.close()
                }
            }, null)

            // Update notification
            val notification = buildNotification("Capturing screen every ${CAPTURE_INTERVAL_MS}ms...")
            val nm = getSystemService(NotificationManager::class.java)
            nm?.notify(NOTIFICATION_ID, notification)

        } catch (e: Exception) {
            Log.e(TAG, "Error starting capture", e)
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Service destroying...")

        instance = null
        _isReady = false
        _lastCapturedPng = null
        lastCaptureTime = 0L
        isProcessing = false

        imageReader?.close()
        imageReader = null

        mediaProjection?.stop()
        mediaProjection = null

        serviceScope.cancel()

        Log.i(TAG, "Service destroyed.")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(contentText: String): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Capture Active")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Capture Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing screen capture notification"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
}