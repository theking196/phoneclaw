package com.example.universal

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsFragment : Fragment() {

    private lateinit var prefs: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.activity_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        prefs = requireContext().getSharedPreferences("phoneclaw_config", android.content.Context.MODE_PRIVATE)

        // Theme picker
        val themeSpinner = view.findViewById<Spinner>(R.id.themeSpinner)
        val themes = arrayOf("Dark (Catppuccin)", "Light", "AMOLED Black", "System Default")
        themeSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, themes)
        themeSpinner.setSelection(ThemeManager.getCurrentTheme(requireContext()))
        themeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (pos != ThemeManager.getCurrentTheme(requireContext())) {
                    ThemeManager.setTheme(requireContext(), pos)
                    activity?.recreate()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val sandboxSwitch = view.findViewById<SwitchMaterial>(R.id.sandboxSwitch)
        sandboxSwitch?.isChecked = prefs.getBoolean("sandbox_enabled", true)
        sandboxSwitch?.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("sandbox_enabled", checked).apply()
        }

        val agentLoopSwitch = view.findViewById<SwitchMaterial>(R.id.agentLoopSwitch)
        agentLoopSwitch?.isChecked = prefs.getBoolean("use_agent_loop", true)
        agentLoopSwitch?.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("use_agent_loop", checked).apply()
        }

        val ollamaSwitch = view.findViewById<SwitchMaterial>(R.id.ollamaSwitch)
        ollamaSwitch?.isChecked = prefs.getBoolean("ollama_enabled", false)
        ollamaSwitch?.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("ollama_enabled", checked).apply()
        }
    }
}
