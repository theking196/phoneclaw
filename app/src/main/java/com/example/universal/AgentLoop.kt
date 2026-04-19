package com.example.universal

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * AgentLoop — Multi-step LLM agent that thinks, acts, observes and retries.
 *
 * Instead of generating one big script, this breaks tasks into steps:
 *   1. LLM plans the next action
 *   2. Execute that action
 *   3. Observe the result (screen text via magicScraper)
 *   4. Feed observation back to LLM
 *   5. Repeat until done or max steps reached
 *
 * All intelligence stays in the LLM — no hardcoded logic.
 */
class AgentLoop(
    private val ctx: Context,
    private val byok: BYOKClient,
    private val executor: suspend (String) -> String,  // executes JS, returns observation
    private val onStatus: (String) -> Unit = {}
) {
    private val TAG = "AgentLoop"
    private val MAX_STEPS = 8
    private val conversationHistory = mutableListOf<Map<String, String>>()

    companion object {
        val AGENT_SYSTEM_PROMPT = """
You are an Android automation agent. You control an Android phone via JavaScript.
You work in a loop: you receive the user's goal, generate ONE step of JavaScript to execute,
observe the result, then decide what to do next.

RESPONSE FORMAT — you MUST follow this exactly:
THOUGHT: (your reasoning about the current state and what to do next)
ACTION: ```javascript
(one focused step of JavaScript code)
```
DONE: true/false  (true = the task is complete, false = more steps needed)

RULES:
- Generate ONE small step at a time, not the entire flow
- After each step you will receive an observation of what happened
- Use the observation to plan your next step
- If something failed, try a different approach
- Max 8 steps total
- When the task is complete, set DONE: true

AVAILABLE FUNCTIONS:
- openApp("package.name") — open app by package
- openAppByName("Display Name") — open app by name (e.g. "Audiomack", "WhatsApp Business")
- launchYouTube() / searchYouTube("query") — YouTube
- launchWhatsApp() — WhatsApp (auto-detects Business vs regular)
- magicClicker("description") — tap element described in plain English
- magicScraper("question") — read text/info from screen
- typeInField("text") / pressEnter() — type and submit
- delay(ms) — wait
- speakText("text") — Voice feedback to user (use to confirm actions, inform user)
- isTextPresentOnScreen("text") — check if text visible
- scrollDown() / scrollUp() — scroll
- clickByText("text") — click element by text
- searchYouTube("query") — search YouTube directly

EXAMPLE:
User: "open youtube and search for phoneclaw"
THOUGHT: I need to open YouTube first, then use searchYouTube to search.
ACTION: ```javascript
searchYouTube("phoneclaw");
```
DONE: true

EXAMPLE with observation:
User: "open audiomack and play something"
THOUGHT: I'll open Audiomack by name since I don't know its package.
ACTION: ```javascript
openAppByName("Audiomack");
delay(3000);
```
DONE: false

Observation: App opened. Screen shows Audiomack home with trending songs.
THOUGHT: Audiomack is open. Now I'll click on a trending song.
ACTION: ```javascript
magicClicker("first trending song or track");
```
DONE: true
""".trimIndent()
    }

    suspend fun run(userGoal: String): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting agent loop for: $userGoal")
        conversationHistory.clear()

        conversationHistory.add(mapOf("role" to "system", "content" to AGENT_SYSTEM_PROMPT))
        conversationHistory.add(mapOf("role" to "user", "content" to "Goal: $userGoal"))

        var stepCount = 0
        var finalObservation = ""

        while (stepCount < MAX_STEPS) {
            stepCount++
            Log.d(TAG, "Agent step $stepCount")
            onStatus("🤔 Planning step $stepCount...")

            // Ask LLM for next action
            val llmResponse = try {
                byok.chatSync(conversationHistory).getOrElse { err ->
                    Log.e(TAG, "LLM failed at step $stepCount: ${err.message}")
                    onStatus("❌ LLM error at step $stepCount: ${err.message?.take(80)}")
                    return@withContext "Agent failed: ${err.message}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception at step $stepCount: ${e.message}")
                return@withContext "Agent failed: ${e.message}"
            }

            Log.d(TAG, "LLM response step $stepCount: ${llmResponse.take(300)}")

            // Parse the response
            val thought = extractSection(llmResponse, "THOUGHT:")
            val action  = extractCodeBlock(llmResponse)
            val done    = llmResponse.contains("DONE: true", ignoreCase = true)

            Log.d(TAG, "Thought: $thought")
            Log.d(TAG, "Action: $action")
            Log.d(TAG, "Done: $done")

            onStatus("💭 $thought")

            if (action.isBlank()) {
                // LLM didn't produce an action — maybe it's done or confused
                if (done || stepCount >= 2) break
                // Add a nudge
                conversationHistory.add(mapOf("role" to "assistant", "content" to llmResponse))
                conversationHistory.add(mapOf("role" to "user", "content" to "No ACTION code found. Please provide the next JavaScript action."))
                continue
            }

            // Execute the action
            onStatus("⚡ Executing step $stepCount...")
            val observation = try {
                executor(action)
            } catch (e: Exception) {
                "Error executing step: ${e.message}"
            }

            finalObservation = observation
            Log.d(TAG, "Observation: $observation")
            LocalStorage.appendDebugLog(ctx, "AgentLoop step $stepCount: $action\nObservation: $observation")

            if (done) {
                onStatus("✅ Task complete in $stepCount steps")
                break
            }

            // Feed observation back to LLM
            conversationHistory.add(mapOf("role" to "assistant", "content" to llmResponse))
            conversationHistory.add(mapOf("role" to "user",
                "content" to "Observation after step $stepCount: $observation\n\nContinue with the next step, or set DONE: true if finished."))

            delay(500) // small pause between steps
        }

        if (stepCount >= MAX_STEPS) {
            onStatus("⚠️ Reached max steps ($MAX_STEPS)")
        }

        finalObservation
    }

    private fun extractSection(text: String, header: String): String {
        val idx = text.indexOf(header, ignoreCase = true)
        if (idx == -1) return ""
        val start = idx + header.length
        val end = text.indexOfFirst(start) { text[it] == '\n' && text.substring(it).trimStart().let { s -> s.startsWith("ACTION:") || s.startsWith("DONE:") } }
        return if (end == -1) text.substring(start).trim() else text.substring(start, end).trim()
    }

    private fun extractCodeBlock(text: String): String {
        // Look for ```javascript ... ``` or ``` ... ```
        val patterns = listOf(
            Regex("""```javascript\s*(.*?)```""", RegexOption.DOT_MATCHES_ALL),
            Regex("""```\s*(.*?)```""", RegexOption.DOT_MATCHES_ALL),
        )
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) return match.groupValues[1].trim()
        }
        // Fallback: anything after ACTION: on its own line
        val actionIdx = text.indexOf("ACTION:", ignoreCase = true)
        if (actionIdx != -1) {
            val after = text.substring(actionIdx + 7).trim()
            val doneIdx = after.indexOf("DONE:", ignoreCase = true)
            return if (doneIdx != -1) after.substring(0, doneIdx).trim() else after.trim()
        }
        return ""
    }

    private fun String.indexOfFirst(from: Int, predicate: (Int) -> Boolean): Int {
        for (i in from until length) if (predicate(i)) return i
        return -1
    }
}
