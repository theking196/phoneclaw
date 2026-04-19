package com.example.universal

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * MemoryManager — Persistent context/memory for the agent.
 *
 * Stores:
 * - Short-term memory: recent conversation turns (last N interactions)
 * - Long-term facts: things the user explicitly asked the agent to remember
 * - Usage patterns: which apps/commands used most
 * - Device context: installed apps cache for fast resolution
 */
object MemoryManager {
    private const val TAG = "MemoryManager"
    private const val MAX_SHORT_TERM = 20
    private const val MAX_FACTS = 100

    // ── Short-term memory ────────────────────────────────────────────────────

    fun addInteraction(ctx: Context, userInput: String, agentOutput: String) {
        try {
            val file = File(ctx.filesDir, "memory_short.json")
            val arr = if (file.exists()) JSONArray(file.readText()) else JSONArray()
            arr.put(JSONObject().apply {
                put("user", userInput)
                put("agent", agentOutput.take(300))
                put("ts", System.currentTimeMillis())
            })
            // Keep only last MAX_SHORT_TERM
            val trimmed = if (arr.length() > MAX_SHORT_TERM) {
                JSONArray((0 until arr.length()).map { arr.get(it) }.takeLast(MAX_SHORT_TERM))
            } else arr
            file.writeText(trimmed.toString())
        } catch (e: Exception) { Log.e(TAG, "addInteraction: ${e.message}") }
    }

    fun getRecentInteractions(ctx: Context, count: Int = 5): List<Pair<String, String>> {
        return try {
            val file = File(ctx.filesDir, "memory_short.json")
            if (!file.exists()) return emptyList()
            val arr = JSONArray(file.readText())
            val result = mutableListOf<Pair<String, String>>()
            val start = maxOf(0, arr.length() - count)
            for (i in start until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(obj.optString("user") to obj.optString("agent"))
            }
            result
        } catch (e: Exception) { emptyList() }
    }

    fun clearShortTerm(ctx: Context) {
        File(ctx.filesDir, "memory_short.json").delete()
    }

    // ── Long-term facts ───────────────────────────────────────────────────────

    fun rememberFact(ctx: Context, key: String, value: String) {
        try {
            val file = File(ctx.filesDir, "memory_facts.json")
            val obj = if (file.exists()) JSONObject(file.readText()) else JSONObject()
            obj.put(key.lowercase().trim(), JSONObject().apply {
                put("value", value)
                put("ts", System.currentTimeMillis())
            })
            file.writeText(obj.toString(2))
            Log.d(TAG, "Remembered: $key = $value")
        } catch (e: Exception) { Log.e(TAG, "rememberFact: ${e.message}") }
    }

    fun recallFact(ctx: Context, key: String): String? {
        return try {
            val file = File(ctx.filesDir, "memory_facts.json")
            if (!file.exists()) return null
            JSONObject(file.readText()).optJSONObject(key.lowercase().trim())?.optString("value")
        } catch (e: Exception) { null }
    }

    fun getAllFacts(ctx: Context): Map<String, String> {
        return try {
            val file = File(ctx.filesDir, "memory_facts.json")
            if (!file.exists()) return emptyMap()
            val obj = JSONObject(file.readText())
            val result = mutableMapOf<String, String>()
            obj.keys().forEach { k ->
                result[k] = obj.getJSONObject(k).optString("value", "")
            }
            result
        } catch (e: Exception) { emptyMap() }
    }

    fun forgetFact(ctx: Context, key: String) {
        try {
            val file = File(ctx.filesDir, "memory_facts.json")
            if (!file.exists()) return
            val obj = JSONObject(file.readText())
            obj.remove(key.lowercase().trim())
            file.writeText(obj.toString(2))
        } catch (e: Exception) { }
    }

    fun clearAllFacts(ctx: Context) {
        File(ctx.filesDir, "memory_facts.json").delete()
    }

    // ── Context summary for LLM ───────────────────────────────────────────────

    fun buildContextSummary(ctx: Context): String {
        val facts = getAllFacts(ctx)
        val recent = getRecentInteractions(ctx, 3)
        val sb = StringBuilder()

        if (facts.isNotEmpty()) {
            sb.appendLine("## Known facts about user:")
            facts.forEach { (k, v) -> sb.appendLine("- $k: $v") }
        }

        if (recent.isNotEmpty()) {
            sb.appendLine("\n## Recent interactions:")
            recent.forEach { (user, agent) ->
                sb.appendLine("User: $user")
                sb.appendLine("Agent: ${agent.take(100)}")
            }
        }

        return sb.toString().trim()
    }

    // ── App usage tracking ────────────────────────────────────────────────────

    fun trackAppUsage(ctx: Context, appName: String) {
        try {
            val file = File(ctx.filesDir, "memory_usage.json")
            val obj = if (file.exists()) JSONObject(file.readText()) else JSONObject()
            val count = obj.optInt(appName, 0) + 1
            obj.put(appName, count)
            file.writeText(obj.toString())
        } catch (e: Exception) { }
    }

    fun getMostUsedApps(ctx: Context, limit: Int = 5): List<String> {
        return try {
            val file = File(ctx.filesDir, "memory_usage.json")
            if (!file.exists()) return emptyList()
            val obj = JSONObject(file.readText())
            obj.keys().asSequence()
                .map { it to obj.getInt(it) }
                .sortedByDescending { it.second }
                .take(limit)
                .map { it.first }
                .toList()
        } catch (e: Exception) { emptyList() }
    }
}
