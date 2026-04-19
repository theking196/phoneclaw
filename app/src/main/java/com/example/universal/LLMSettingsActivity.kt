package com.example.universal

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class LLMSettingsActivity : AppCompatActivity() {
    
    private lateinit var providerDropdown: AutoCompleteTextView
    private lateinit var modelInput: TextInputEditText
    private lateinit var maxTokensInput: TextInputEditText
    private lateinit var temperatureSlider: com.google.android.material.slider.Slider
    private lateinit var topPSlider: com.google.android.material.slider.Slider
    private lateinit var saveButton: MaterialButton
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_llm_settings)
        
        // Initialize views
        providerDropdown = findViewById(R.id.providerDropdown)
        modelInput = findViewById(R.id.modelInput)
        maxTokensInput = findViewById(R.id.maxTokensInput)
        temperatureSlider = findViewById(R.id.temperatureSlider)
        topPSlider = findViewById(R.id.topPSlider)
        saveButton = findViewById(R.id.saveLlmSettings)
        
        // Load current settings
        loadSettings()
        
        // Setup provider dropdown
        providerDropdown.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, LLMConfig.PROVIDERS))
        
        providerDropdown.setOnItemClickListener { _, _, position, _ ->
            val provider = LLMConfig.PROVIDERS[position]
            val models = LLMConfig.getModelsForProvider(provider)
            modelInput.setText(models.first())
        }
        
        // Save button
        saveButton.setOnClickListener { saveSettings() }
    }
    
    private fun loadSettings() {
        providerDropdown.setText(LLMConfig.getProvider(this), false)
        modelInput.setText(LLMConfig.getModel(this))
        maxTokensInput.setText(LLMConfig.getMaxTokens(this).toString())
        temperatureSlider.value = LLMConfig.getTemperature(this)
        topPSlider.value = LLMConfig.getTopP(this)
    }
    
    private fun saveSettings() {
        LLMConfig.setProvider(this, providerDropdown.text.toString())
        LLMConfig.setModel(this, modelInput.text.toString())
        LLMConfig.setMaxTokens(this, maxTokensInput.text.toString().toIntOrNull() ?: 2048)
        LLMConfig.setTemperature(this, temperatureSlider.value)
        LLMConfig.setTopP(this, topPSlider.value)
        Toast.makeText(this, "LLM settings saved!", Toast.LENGTH_SHORT).show()
        finish()
    }
}