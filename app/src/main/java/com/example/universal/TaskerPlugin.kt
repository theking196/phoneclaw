package com.example.universal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * Tasker Plugin for PhoneClaw
 * 
 * PhoneClaw broadcasts events that Tasker can listen to
 * and Tasker can send actions to PhoneClaw
 * 
 * Setup in Tasker:
 * - Receive Broadcast: com.example.universal.PHONECLAW_ACTION
 * - Send Broadcast: com.example.universal.PHONECLAW_RUN
 */
class TaskerPlugin : BroadcastReceiver() {

    companion object {
        // Broadcast when PhoneClaw completes an action
        const val ACTION_COMPLETE = "com.example.universal.PHONECLAW_ACTION"
        // Broadcast to run a command
        const val ACTION_RUN = "com.example.universal.PHONECLAW_RUN"
        // Category for events
        const val CATEGORY = "PhoneClaw"
        
        // Extra fields
        const val EXTRA_COMMAND = "command"
        const val EXTRA_RESULT = "result"
        const val EXTRA_TIMESTAMP = "timestamp"
    }

    // === PHONECLAW SENDS TO TASKER ===
    
    /**
     * Call from MainActivity when action completes
     */
    fun broadcastActionComplete(context: Context, command: String, result: String) {
        val intent = Intent(ACTION_COMPLETE).apply {
            putExtra(EXTRA_COMMAND, command)
            putExtra(EXTRA_RESULT, result)
            putExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
            addCategory(CATEGORY)
        }
        context.sendBroadcast(intent)
    }

    // === PHONECLAW RECEIVES FROM TASKER ===
    
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_RUN -> {
                val command = intent.getStringExtra(EXTRA_COMMAND)
                if (!command.isNullOrBlank()) {
                    // Execute command from Tasker
                    // This would integrate with MainActivity's command processor
                    TaskerReceiver.onCommandReceived(command, context)
                }
            }
        }
    }
}

/**
 * Handle commands received from Tasker
 */
object TaskerReceiver {
    fun onCommandReceived(command: String, context: Context) {
        // Route to MainActivity for execution
        // This is a placeholder - actual integration would call MainActivity's execute method
        android.util.Log.d("TaskerPlugin", "Received command: $command")
    }
}

/**
 * Quick reference for Tasker Project setup
 */
object TaskerHelp {
    // Tasker Project: Listen for PhoneClaw actions
    val receiveBroadcast = """
        Event: PhoneClaw Action
        Action: com.example.universal.PHONECLAW_ACTION
        Cat: PhoneClaw
    """.trimIndent()
    
    // Tasker Project: Send command to PhoneClaw
    val sendBroadcast = """
        Action: Send Broadcast
        Action: com.example.universal.PHONECLAW_RUN
        Extra: command = (your command)
        Cat: PhoneClaw
    """.trimIndent()
}