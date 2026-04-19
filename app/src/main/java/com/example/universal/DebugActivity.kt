package com.example.universal

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class DebugActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.applyTheme(this)
        setContentView(R.layout.activity_debug)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Debug Log"
        toolbar.setNavigationOnClickListener { finish() }

        val logView = findViewById<TextView>(R.id.debugLogText)
        val debugLog = LocalStorage.getDebugLog(this)
        logView.text = if (debugLog.isNotEmpty()) debugLog else "No debug logs yet."
    }
}
