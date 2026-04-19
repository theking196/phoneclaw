package com.example.universal

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * LocalStorage — On-device storage for PhoneClaw.
 *
 * All data stays on this device. Nothing is sent to external servers.
 *
 * Storage locations (all private to app):
 *   - SharedPreferences  → simple key/value config
 *   - filesDir/action_log.json       → magicClicker/magicScraper history
 *   - filesDir/agent_emails.json     → sendAgentEmail queue
 *   - filesDir/debug_log.txt         → text debug log
 *   - filesDir/cron_tasks.json       → scheduled tasks
 *   - filesDir/generation_history.json → AI generation history
 */
object LocalStorage {

    private const val TAG = "LocalStorage"
    private const val PREFS_NAME = "phoneclaw_local_storage"
    private const val MAX_ACTION_LOG_ENTRIES = 500
    private const val MAX_DEBUG_LOG_BYTES = 1_000_000L   // 1 MB
    private const val MAX_HISTORY_ENTRIES = 200

    // ── Key-value store ──────────────────────────────────────────────────────

    fun put(ctx: Context, key: String, value: String) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(key, value).apply()
    }

    fun get(ctx: Context, key: String, default: String = ""): String =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(key, default) ?: default

    fun putLong(ctx: Context, key: String, value: Long) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(key, value).apply()
    }

    fun getLong(ctx: Context, key: String, default: Long = 0L): Long =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(key, default)

    fun putBoolean(ctx: Context, key: String, value: Boolean) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(key, value).apply()
    }

    fun getBoolean(ctx: Context, key: String, default: Boolean = false): Boolean =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(key, default)

    fun putInt(ctx: Context, key: String, value: Int) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(key, value).apply()
    }

    fun getInt(ctx: Context, key: String, default: Int = 0): Int =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(key, default)

    fun remove(ctx: Context, key: String) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(key).apply()
    }

    // ── Action log (replaces Firebase unit_tests / trackMagicRun) ────────────
    //
    // Every magicClicker / magicScraper action is logged here locally so you
    // can review what the AI did on your device — without sending anything out.

    fun logAction(ctx: Context, mode: String, input: String, output: String) {
        try {
            val file = File(ctx.filesDir, "action_log.json")
            val arr = if (file.exists()) JSONArray(file.readText()) else JSONArray()

            arr.put(JSONObject().apply {
                put("mode", mode)
                put("input", input)
                put("output", output)
                put("timestamp", System.currentTimeMillis())
            })

            // Keep only last MAX_ACTION_LOG_ENTRIES
            val trimmed = if (arr.length() > MAX_ACTION_LOG_ENTRIES) {
                val newArr = JSONArray()
                for (i in (arr.length() - MAX_ACTION_LOG_ENTRIES) until arr.length()) {
                    newArr.put(arr.get(i))
                }
                newArr
            } else arr

            file.writeText(trimmed.toString())
            Log.d(TAG, "Logged action: $mode | $input")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log action: ${e.message}")
        }
    }

    fun getActionLog(ctx: Context): JSONArray {
        return try {
            val file = File(ctx.filesDir, "action_log.json")
            if (file.exists()) JSONArray(file.readText()) else JSONArray()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read action log: ${e.message}")
            JSONArray()
        }
    }

    fun clearActionLog(ctx: Context) {
        File(ctx.filesDir, "action_log.json").delete()
        Log.d(TAG, "Action log cleared")
    }

    // ── Agent email log (replaces sendAgentEmail server call) ────────────────
    //
    // Emails queued via ClawScript are stored locally. You can implement your
    // own delivery mechanism (e.g., intent to Gmail) without a third-party server.

    fun logAgentEmail(ctx: Context, to: String, subject: String, message: String) {
        try {
            val file = File(ctx.filesDir, "agent_emails.json")
            val arr = if (file.exists()) JSONArray(file.readText()) else JSONArray()

            arr.put(JSONObject().apply {
                put("to", to)
                put("subject", subject)
                put("message", message)
                put("timestamp", System.currentTimeMillis())
                put("sent", false)
            })
            file.writeText(arr.toString())
            Log.d(TAG, "Agent email logged locally: to=$to subject=$subject")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log agent email: ${e.message}")
        }
    }

    fun getPendingAgentEmails(ctx: Context): JSONArray {
        return try {
            val file = File(ctx.filesDir, "agent_emails.json")
            if (file.exists()) JSONArray(file.readText()) else JSONArray()
        } catch (e: Exception) {
            JSONArray()
        }
    }

    fun clearAgentEmails(ctx: Context) {
        File(ctx.filesDir, "agent_emails.json").delete()
    }

    // ── Debug log ─────────────────────────────────────────────────────────────
    //
    // Replaces periodic Firebase debug screenshot uploads. Log text events
    // locally and view them in the Debug tab.

    fun appendDebugLog(ctx: Context, message: String) {
        try {
            val file = File(ctx.filesDir, "debug_log.txt")
            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            file.appendText("[$ts] $message\n")
            // Trim to stay under size limit
            if (file.length() > MAX_DEBUG_LOG_BYTES) {
                val lines = file.readLines()
                file.writeText(lines.takeLast(500).joinToString("\n") + "\n")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to append debug log: ${e.message}")
        }
    }

    fun getDebugLog(ctx: Context): String {
        return try {
            File(ctx.filesDir, "debug_log.txt").readText()
        } catch (e: Exception) {
            ""
        }
    }

    fun clearDebugLog(ctx: Context) {
        File(ctx.filesDir, "debug_log.txt").delete()
        Log.d(TAG, "Debug log cleared")
    }

    // ── Cron tasks persistence ─────────────────────────────────────────────────

    fun saveCronTasks(ctx: Context, tasks: Map<String, CronTask>) {
        try {
            val arr = JSONArray()
            tasks.values.forEach { task ->
                arr.put(JSONObject().apply {
                    put("id", task.id)
                    put("taskDescription", task.taskDescription)
                    put("cronExpression", task.cronExpression)
                    put("createdAt", task.createdAt)
                    put("lastExecuted", task.lastExecuted)
                    put("isActive", task.isActive)
                })
            }
            File(ctx.filesDir, "cron_tasks.json").writeText(arr.toString())
            Log.d(TAG, "Saved ${tasks.size} cron tasks")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save cron tasks: ${e.message}")
        }
    }

    fun loadCronTasks(ctx: Context): Map<String, CronTask> {
        return try {
            val file = File(ctx.filesDir, "cron_tasks.json")
            if (!file.exists()) return emptyMap()
            val arr = JSONArray(file.readText())
            val result = mutableMapOf<String, CronTask>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val task = CronTask(
                    id = obj.getString("id"),
                    taskDescription = obj.getString("taskDescription"),
                    cronExpression = obj.getString("cronExpression"),
                    createdAt = obj.getLong("createdAt"),
                    lastExecuted = obj.getLong("lastExecuted"),
                    isActive = obj.getBoolean("isActive")
                )
                result[task.id] = task
            }
            Log.d(TAG, "Loaded ${result.size} cron tasks")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load cron tasks: ${e.message}")
            emptyMap()
        }
    }

    // ── Generation history persistence ─────────────────────────────────────────

    fun saveGenerationHistory(ctx: Context, history: List<GenerationHistory>) {
        try {
            val arr = JSONArray()
            history.takeLast(MAX_HISTORY_ENTRIES).forEach { item ->
                arr.put(JSONObject().apply {
                    put("id", item.id)
                    put("userCommand", item.userCommand)
                    put("generatedCode", item.generatedCode)
                    put("timestamp", item.timestamp)
                })
            }
            File(ctx.filesDir, "generation_history.json").writeText(arr.toString())
            Log.d(TAG, "Saved ${arr.length()} generation history entries")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save generation history: ${e.message}")
        }
    }

    fun loadGenerationHistory(ctx: Context): List<GenerationHistory> {
        return try {
            val file = File(ctx.filesDir, "generation_history.json")
            if (!file.exists()) return emptyList()
            val arr = JSONArray(file.readText())
            val result = mutableListOf<GenerationHistory>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(
                    GenerationHistory(
                        id = obj.getString("id"),
                        userCommand = obj.getString("userCommand"),
                        generatedCode = obj.getString("generatedCode"),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
            Log.d(TAG, "Loaded ${result.size} history entries")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load generation history: ${e.message}")
            emptyList()
        }
    }

    // ── Storage info ──────────────────────────────────────────────────────────

    /** Returns a human-readable summary of local storage usage. */
    fun getStorageInfo(ctx: Context): String {
        val files = mapOf(
            "action_log.json" to "Action Log",
            "agent_emails.json" to "Agent Emails",
            "debug_log.txt" to "Debug Log",
            "cron_tasks.json" to "Cron Tasks",
            "generation_history.json" to "Generation History"
        )
        val sb = StringBuilder("=== Local Storage ===\n")
        var total = 0L
        for ((name, label) in files) {
            val f = File(ctx.filesDir, name)
            val size = if (f.exists()) f.length() else 0L
            total += size
            sb.appendLine("$label: ${formatBytes(size)}")
        }
        sb.appendLine("Total: ${formatBytes(total)}")
        return sb.toString()
    }

    fun clearAll(ctx: Context) {
        listOf("action_log.json", "agent_emails.json", "debug_log.txt",
               "cron_tasks.json", "generation_history.json").forEach {
            File(ctx.filesDir, it).delete()
        }
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
        Log.d(TAG, "All local storage cleared")
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes < 1024 -> "${bytes} B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}
