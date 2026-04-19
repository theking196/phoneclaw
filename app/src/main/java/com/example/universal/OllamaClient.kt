package com.example.universal

import android.content.Context
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OllamaClient(private val ctx: Context) {
    private val prefs = ctx.getSharedPreferences("phoneclaw_config", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS).build()

    fun isEnabled(): Boolean = prefs.getBoolean("ollama_enabled", false)
    fun getEndpoint(): String = prefs.getString("ollama_endpoint", "http://localhost:11434") ?: "http://localhost:11434"
    fun getModel(): String = prefs.getString("ollama_model", "llama3.2") ?: "llama3.2"

    fun isReachable(): Boolean = try {
        client.newCall(Request.Builder().url("${getEndpoint()}/api/tags").get().build()).execute().isSuccessful
    } catch (e: Exception) { false }

    fun listModels(): List<String> {
        return try {
            val r = client.newCall(Request.Builder().url("${getEndpoint()}/api/tags").get().build()).execute()
            val body = r.body?.string() ?: return emptyList()
            val j = JSONObject(body)
            val models = j.getJSONArray("models")
            (0 until models.length()).map { models.getJSONObject(it).getString("name") }
        } catch (e: Exception) { emptyList() }
    }

    fun chat(messages: List<Map<String, String>>): Result<String> {
        return try {
            val arr = JSONArray().also { a -> messages.forEach { m -> a.put(JSONObject().apply { put("role", m["role"]); put("content", m["content"]) }) } }
            val body = JSONObject().apply { put("model", getModel()); put("messages", arr); put("stream", false) }
            val req = Request.Builder().url("${getEndpoint()}/api/chat").post(body.toString().toRequestBody("application/json".toMediaType())).build()
            val resp = client.newCall(req).execute()
            val rb = resp.body?.string() ?: ""
            if (resp.isSuccessful) Result.success(JSONObject(rb).getJSONObject("message").getString("content"))
            else Result.failure(Exception("Ollama ${resp.code}: $rb"))
        } catch (e: Exception) { Log.e("OllamaClient", e.message ?: ""); Result.failure(e) }
    }
}
