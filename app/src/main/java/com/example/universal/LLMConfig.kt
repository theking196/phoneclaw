package com.example.universal

import android.content.Context

/**
 * Unified LLM Configuration
 * All places (Settings, MainActivity, etc) should read from here
 */
object LLMConfig {
    private const val PREFS = "phoneclaw_llm"
    
    fun getProvider(ctx: Context): String = 
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("provider", "kilocode")
    
    fun getModel(ctx: Context): String = 
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("model", "gpt-4o-mini")
    
    fun getTemperature(ctx: Context): Float = 
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat("temperature", 0.7f)
    
    fun getMaxTokens(ctx: Context): Int = 
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("max_tokens", 2048)
    
    fun getTopP(ctx: Context): Float = 
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat("top_p", 0.9f)
    
    fun setProvider(ctx: Context, provider: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("provider", provider).apply()
    }
    
    fun setModel(ctx: Context, model: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("model", model).apply()
    }
    
    fun setTemperature(ctx: Context, temp: Float) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putFloat("temperature", temp).apply()
    }
    
    fun setMaxTokens(ctx: Context, tokens: Int) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("max_tokens", tokens).apply()
    }
    
    fun setTopP(ctx: Context, topP: Float) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putFloat("top_p", topP).apply()
    }
    
    // All available providers
    val PROVIDERS = listOf("kilocode", "ollama", "openrouter", "anthropic", "openai")
    
    // Get model for provider (could expand based on provider)
    fun getModelsForProvider(provider: String): List<String> = when(provider) {
        "kilocode" -> listOf("gpt-4o-mini", "gpt-4o", "claude-3.5")
        "ollama" -> listOf("llama3", "mistral", "codellama")
        "openrouter" -> listOf("gpt-4o-mini", "gpt-4o", "claude-3-opus")
        "openai" -> listOf("gpt-4o-mini", "gpt-4o", "gpt-3.5-turbo")
        "anthropic" -> listOf("claude-3.5-sonnet", "claude-3-opus")
        else -> listOf("gpt-4o-mini")
    }
}