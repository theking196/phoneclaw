package com.example.universal

import android.content.Context
import android.content.SharedPreferences

/**
 * Execution Safety Configuration
 */
object SafetyConfig {
    private const val PREFS = "phoneclaw_safety"
    
    // Confirm before execute
    fun isConfirmEnabled(ctx: Context): Boolean = 
        getPrefs(ctx).getBoolean("confirm_before_run", false)
    
    fun setConfirmEnabled(ctx: Context, enabled: Boolean) {
        getPrefs(ctx).edit().putBoolean("confirm_before_run", enabled).apply()
    }
    
    // Sandbox mode (future use)
    fun isSandboxEnabled(ctx: Context): Boolean = 
        getPrefs(ctx).getBoolean("sandbox_mode", false)
    
    fun setSandboxEnabled(ctx: Context, enabled: Boolean) {
        getPrefs(ctx).edit().putBoolean("sandbox_mode", enabled).apply()
    }
    
    // Timeout (seconds)
    fun getTimeout(ctx: Context): Int = 
        getPrefs(ctx).getInt("script_timeout", 60)
    
    fun setTimeout(ctx: Context, seconds: Int) {
        getPrefs(ctx).edit().putInt("script_timeout", seconds).apply()
    }
    
    private fun getPrefs(ctx: Context): SharedPreferences = 
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}