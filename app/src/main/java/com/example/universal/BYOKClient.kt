package com.example.universal

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * PhoneClaw BYOK - Multi-Provider AI Client
 * Supports: OpenRouter, OpenAI, Groq, Scitely, Anthropic, Custom
 */
class BYOKClient(private val context: Context) {

    private val prefs = context.getSharedPreferences("phoneclaw_config", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val JSON = "application/json".toMediaType()

    fun chat(messages: List<Map<String, String>>, callback: (String?, String?) -> Unit) {
        val provider = prefs.getString("ai_provider", "OpenRouter") ?: "OpenRouter"
        val apiKey = prefs.getString("api_key", "") ?: ""
        val model = prefs.getString("model", getDefaultModel(provider)) ?: getDefaultModel(provider)

        Log.d("BYOKClient", "Provider=$provider model=$model keyLen=${apiKey.length}")

        if (apiKey.isEmpty()) {
            callback(null, "No API key configured for $provider")
            return
        }

        val result = when (provider) {
            "OpenRouter" -> callOpenRouter(apiKey, model, messages)
            "OpenAI"     -> callOpenAI(apiKey, model, messages)
            "Groq"       -> callGroq(apiKey, model, messages)
            "Scitely"    -> callScitely(apiKey, model, messages)
            "Anthropic"  -> callAnthropic(apiKey, model, messages)
            "Custom"     -> {
                val customUrl = prefs.getString("custom_api_url", "") ?: ""
                if (customUrl.isEmpty()) {
                    callback(null, "Custom provider: no URL configured")
                    return
                }
                callOpenAICompatible(apiKey, model, messages, customUrl)
            }
            else -> callOpenRouter(apiKey, model, messages)
        }

        result.onSuccess { callback(it, null) }
              .onFailure { callback(null, it.message ?: "Unknown error") }
    }

    /** Synchronous chat — use only from background threads / coroutines */
    fun chatSync(messages: List<Map<String, String>>): Result<String> {
        val provider = prefs.getString("ai_provider", "OpenRouter") ?: "OpenRouter"
        val apiKey   = prefs.getString("api_key", "") ?: ""
        val model    = prefs.getString("model", getDefaultModel(provider)) ?: getDefaultModel(provider)
        if (apiKey.isEmpty()) return Result.failure(Exception("No API key configured for $provider"))
        return when (provider) {
            "OpenRouter" -> callOpenRouter(apiKey, model, messages)
            "OpenAI"     -> callOpenAI(apiKey, model, messages)
            "Groq"       -> callGroq(apiKey, model, messages)
            "Scitely"    -> callScitely(apiKey, model, messages)
            "Anthropic"  -> callAnthropic(apiKey, model, messages)
            "Custom"     -> {
                val url = prefs.getString("custom_api_url", "") ?: ""
                if (url.isEmpty()) Result.failure(Exception("Custom provider: no URL configured"))
                else callOpenAICompatible(apiKey, model, messages, url)
            }
            else -> callOpenRouter(apiKey, model, messages)
        }
    }

    private fun callOpenRouter(key: String, model: String, msgs: List<Map<String, String>>) =
        makeOpenAIRequest("https://openrouter.ai/api/v1/chat/completions", "Bearer $key", model, msgs,
            mapOf("HTTP-Referer" to "https://phoneclaw.app", "X-Title" to "PhoneClaw"))

    private fun callOpenAI(key: String, model: String, msgs: List<Map<String, String>>) =
        makeOpenAIRequest("https://api.openai.com/v1/chat/completions", "Bearer $key", model, msgs)

    private fun callGroq(key: String, model: String, msgs: List<Map<String, String>>) =
        makeOpenAIRequest("https://api.groq.com/openai/v1/chat/completions", "Bearer $key", model, msgs)

    private fun callScitely(key: String, model: String, msgs: List<Map<String, String>>) =
        makeOpenAIRequest("https://api.scitely.com/v1/chat/completions", "Bearer $key", model, msgs)

    private fun callOpenAICompatible(key: String, model: String, msgs: List<Map<String, String>>, url: String) =
        makeOpenAIRequest(url, "Bearer $key", model, msgs)

    private fun callAnthropic(key: String, model: String, msgs: List<Map<String, String>>): Result<String> {
        return try {
            // Anthropic uses a different format — separate system prompt
            val systemMsg = msgs.firstOrNull { it["role"] == "system" }?.get("content") ?: ""
            val userMsgs  = msgs.filter { it["role"] != "system" }

            val messagesArr = JsonArray().also { arr ->
                userMsgs.forEach { m ->
                    arr.add(JsonObject().apply {
                        addProperty("role", m["role"])
                        addProperty("content", m["content"] ?: "")
                    })
                }
            }

            val body = JsonObject().apply {
                addProperty("model", model)
                addProperty("max_tokens", 1024)
                if (systemMsg.isNotEmpty()) addProperty("system", systemMsg)
                add("messages", messagesArr)
            }

            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .addHeader("Content-Type", "application/json")
                .addHeader("x-api-key", key)
                .addHeader("anthropic-version", "2023-06-01")
                .post(gson.toJson(body).toRequestBody(JSON))
                .build()

            val response = client.newCall(request).execute()
            val rb = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val json = gson.fromJson(rb, JsonObject::class.java)
                val content = json.getAsJsonArray("content")?.get(0)?.asJsonObject?.get("text")?.asString ?: ""
                Result.success(content)
            } else {
                Log.e("BYOKClient", "Anthropic error ${response.code}: $rb")
                Result.failure(Exception("Anthropic ${response.code}: $rb"))
            }
        } catch (e: Exception) {
            Log.e("BYOKClient", "Anthropic exception: ${e.message}")
            Result.failure(e)
        }
    }

    private fun makeOpenAIRequest(
        url: String, auth: String, model: String,
        msgs: List<Map<String, String>>,
        extraHeaders: Map<String, String> = emptyMap()
    ): Result<String> {
        return try {
            val messagesArr = JsonArray().also { arr ->
                msgs.forEach { m ->
                    arr.add(JsonObject().apply {
                        addProperty("role", m["role"] ?: "user")
                        addProperty("content", m["content"] ?: "")
                    })
                }
            }
            val body = JsonObject().apply {
                addProperty("model", model)
                add("messages", messagesArr)
            }

            val reqBuilder = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", auth)
            extraHeaders.forEach { (k, v) -> reqBuilder.addHeader(k, v) }
            reqBuilder.post(gson.toJson(body).toRequestBody(JSON))

            val response = client.newCall(reqBuilder.build()).execute()
            val rb = response.body?.string() ?: ""

            Log.d("BYOKClient", "Response ${response.code} from $url: ${rb.take(300)}")

            if (response.isSuccessful) {
                val json = gson.fromJson(rb, JsonObject::class.java)
                val text = extractMessage(json)
                if (text.isEmpty()) {
                    Log.w("BYOKClient", "Empty message extracted from: $rb")
                    Result.failure(Exception("Empty response from $url"))
                } else {
                    Result.success(text)
                }
            } else {
                Log.e("BYOKClient", "HTTP ${response.code} from $url: $rb")
                Result.failure(Exception("HTTP ${response.code}: $rb"))
            }
        } catch (e: Exception) {
            Log.e("BYOKClient", "Request to $url failed: ${e.message}")
            Result.failure(e)
        }
    }

    private fun extractMessage(json: JsonObject): String {
        return try {
            json.getAsJsonArray("choices")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("message")
                ?.get("content")?.asString ?: ""
        } catch (e: Exception) {
            Log.e("BYOKClient", "extractMessage error: ${e.message}")
            ""
        }
    }

    fun getDefaultModel(p: String): String = when (p) {
        "OpenRouter" -> "openai/gpt-4o-mini"
        "OpenAI"     -> "gpt-4o-mini"
        "Groq"       -> "llama-3.1-8b-instant"
        "Scitely"    -> "deepseek-chat"
        "Anthropic"  -> "claude-3-haiku-20240307"
        else         -> "openai/gpt-4o-mini"
    }

    companion object {
        fun isConfigured(ctx: Context): Boolean {
            val p = ctx.getSharedPreferences("phoneclaw_config", Context.MODE_PRIVATE)
            return p.getBoolean("use_custom_config", false) &&
                   p.getString("api_key", "")?.isNotEmpty() == true
        }
    }
}
