package com.example.universal

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build

/**
 * Android Auto Integration
 * 
 * Enables basic voice control through Android Auto
 * 
 * To use:
 * 1. Connect phone to car
 * 2. Grant notification permission
 * 3. Use "Hey Google, ask PhoneClaw to..."
 */
class AndroidAutoHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "phoneclaw_auto"
        private const val CHANNEL_NAME = "PhoneClaw Voice"
    }

    /**
     * Create notification channel (Android 8+)
     */
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "PhoneClaw voice commands"
                setShowBadge(true)
            }
            
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Show voice command feedback as notification
     * This appears on Android Auto / car display
     */
    fun showVoiceFeedback(text: String, action: String? = null) {
        val intent = android.content.Intent(context, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            if (action != null) {
                putExtra("command", action)
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("PhoneClaw")
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(Notification.PRIORITY_HIGH)

        // Actions for quick replies (if supported)
        if (action != null) {
            builder.addAction(
                android.R.drawable.ic_media_play,
                "Run",
                pendingIntent
            )
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager?.notify(999, builder.build())
    }

    /**
     * Parse voice command from Android Auto
     */
    fun parseVoiceInput(voiceText: String): VoiceCommand {
        // Simple parsing - in production would be more sophisticated
        val lower = voiceText.lowercase()
        
        return when {
            lower.contains("open") && lower.contains("youtube") -> 
                VoiceCommand("open app", "com.google.android.youtube")
            lower.contains("play music") ->
                VoiceCommand("open app", "com.spotify.music")
            lower.contains("navigate") ->
                VoiceCommand("navigate", extractLocation(voiceText))
            lower.contains("call") ->
                VoiceCommand("call", extractContact(voiceText))
            lower.contains("message") || lower.contains("text") ->
                VoiceCommand("message", extractContact(voiceText))
            else -> VoiceCommand(voiceText, "")
        }
    }

    private fun extractLocation(text: String): String {
        // Simple extraction - after "to" or "navigate to"
        return text.replace(Regex(".*to\\s+", ""), "").trim()
    }

    private fun extractContact(text: String): String {
        // Simple extraction - after "call" or "text"
        return text.replace(Regex(".*(call|text)\\s+", ""), "").trim()
    }
}

/**
 * Parsed voice command
 */
data class VoiceCommand(
    val action: String,
    val target: String
)

/**
 * Android Auto reference for setup
 */
object AndroidAutoHelp {
    // What's supported
    val supportedCommands = """
        Voice commands that work with Android Auto:
        
        - "Open YouTube" → Opens app
        - "Play music" → Opens music app  
        - "Navigate to [location]" → Opens maps
        - "Call [contact]" → Initiates call
        - "Message [contact]" → Opens messages
        
        Notes:
        - Works best with Bluetooth connected
        - Say "Hey Google, ask PhoneClaw to..."
        - Make sure phone is unlocked for some actions
    """.trimIndent()
    
    // Setup instructions
    val setupInstructions = """
        To enable Android Auto:
        
        1. Open Android Auto on phone
        2. Go to Settings → About
        3. Enable "Untrusted notifications" (if needed)
        4. Connect to car Bluetooth
        
        PhoneClaw will show voice feedback as notifications
    """.trimIndent()
}