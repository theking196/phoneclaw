package com.example.universal

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Launch this Activity once to get user permission for screen capture.
 * On success, starts ScreenCaptureService with valid resultCode + data.
 */
class ScreenshotActivity : AppCompatActivity() {

    private val projectionManager by lazy {
        getSystemService(MediaProjectionManager::class.java)
    }

    private val screenshotPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // If user tapped "Start now," we get (RESULT_OK, data)
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            // Pass these extras to ScreenCaptureService
            val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                putExtra("resultCode", result.resultCode)
                putExtra("data", result.data)
            }
            ContextCompat.startForegroundService(this, serviceIntent)
        }
        // Whether granted or not, we’re done here
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestCapturePermissionOnce()
    }

    private fun requestCapturePermissionOnce() {
        val intent = projectionManager.createScreenCaptureIntent()
        screenshotPermissionLauncher.launch(intent)
    }
}
