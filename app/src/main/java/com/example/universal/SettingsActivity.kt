package com.example.universal

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import com.google.android.material.button.MaterialButton
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.applyTheme(this)
        findViewById<MaterialButton>(R.id.openLLMSettings)?.setOnClickListener {
            startActivity(Intent(this, LLMSettingsActivity::class.java))
        }
        setContentView(R.layout.activity_settings)
        prefs = getSharedPreferences("phoneclaw_config", MODE_PRIVATE)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"
        toolbar.setNavigationOnClickListener { finish() }

        setupThemePicker()
        setupToggles()
    }

    private fun setupThemePicker() {
        val themeSpinner = findViewById<Spinner>(R.id.themeSpinner)
        val themes = arrayOf("Dark (Catppuccin)", "Light", "AMOLED Black", "System Default")
        themeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, themes)
        val current = ThemeManager.getCurrentTheme(this)
        themeSpinner.setSelection(current)
        themeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, pos: Int, id: Long) {
                if (pos != ThemeManager.getCurrentTheme(this@SettingsActivity)) {
                    ThemeManager.setTheme(this@SettingsActivity, pos)
                    recreate()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupToggles() {
        val sandboxSwitch = findViewById<SwitchMaterial>(R.id.sandboxSwitch)
        sandboxSwitch.isChecked = prefs.getBoolean("sandbox_enabled", true)
        sandboxSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("sandbox_enabled", checked).apply()
        }

        val agentLoopSwitch = findViewById<SwitchMaterial>(R.id.agentLoopSwitch)
        agentLoopSwitch.isChecked = prefs.getBoolean("use_agent_loop", true)
        agentLoopSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("use_agent_loop", checked).apply()
        }

        val ollamaSwitch = findViewById<SwitchMaterial>(R.id.ollamaSwitch)
        ollamaSwitch.isChecked = prefs.getBoolean("ollama_enabled", false)
        ollamaSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("ollama_enabled", checked).apply()
        }
    }
}
