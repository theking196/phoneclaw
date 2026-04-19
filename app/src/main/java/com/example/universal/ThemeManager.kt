package com.example.universal

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val PREFS = "phoneclaw_config"
    private const val KEY = "app_theme"

    const val THEME_DARK   = 0
    const val THEME_LIGHT  = 1
    const val THEME_AMOLED = 2
    const val THEME_SYSTEM = 3

    fun getCurrentTheme(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY, THEME_DARK)

    fun setTheme(ctx: Context, theme: Int) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY, theme).apply()
        // Apply night mode immediately
        applyNightMode(theme)
    }

    fun applyNightMode(theme: Int = -1) {
        val mode = when (theme) {
            THEME_LIGHT  -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else         -> AppCompatDelegate.MODE_NIGHT_YES  // Dark + AMOLED both use night mode
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    /**
     * Call this at the start of each Activity's onCreate, BEFORE setContentView.
     * Sets the correct theme variant based on saved preference.
     */
    fun applyTheme(activity: AppCompatActivity) {
        when (getCurrentTheme(activity)) {
            THEME_LIGHT  -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                activity.setTheme(R.style.Theme_AutoPhone_Light)
            }
            THEME_AMOLED -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                activity.setTheme(R.style.Theme_AutoPhone_Amoled)
            }
            THEME_SYSTEM -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                // Use default dark theme — system will switch automatically
            }
            else -> { // THEME_DARK (default)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                // Theme.AutoPhone is already the manifest default — no setTheme needed
            }
        }
    }
}
