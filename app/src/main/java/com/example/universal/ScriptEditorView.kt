package com.example.universal

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

class ScriptEditorView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : AppCompatEditText(ctx, attrs) {
    private val keywords = setOf("function","var","let","const","if","else","for","while","return","true","false","null","undefined","new","this","try","catch","finally","throw")
    private val apis = setOf("speakText","delay","schedule","clearSchedule","magicClicker","magicScraper","launchApp","launchYouTube","launchTikTok","launchInstagram","launchTwitter","launchWhatsApp","launchTelegram","openApp","getLatestOtp","waitForOtp","sendAgentEmail","toggleWiFi","toggleBluetooth","toggleFlashlight","setVolume","setBrightness","takePhoto","openSettings","safeInt","undoLastAction")
    private var busy = false

    init {
        typeface = Typeface.MONOSPACE; textSize = 12f
        setBackgroundColor(Color.parseColor("#1e1e2e"))
        setTextColor(Color.parseColor("#cdd6f4"))
        setHintTextColor(Color.parseColor("#6c7086"))
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) { if (!busy && s != null) { busy = true; highlight(s); busy = false } }
        })
    }

    private fun highlight(s: Editable) {
        s.getSpans(0, s.length, ForegroundColorSpan::class.java).forEach { s.removeSpan(it) }
        val t = s.toString()
        fun span(r: MatchResult, color: String) = s.setSpan(ForegroundColorSpan(Color.parseColor(color)), r.range.first, r.range.last + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        Regex("""//[^\n]*""").findAll(t).forEach { span(it, "#6c7086") }
        Regex("""["'][^"'\n]*["']""").findAll(t).forEach { span(it, "#a6e3a1") }
        Regex("""\b\d+\b""").findAll(t).forEach { span(it, "#fab387") }
        apis.forEach { api -> Regex("""\b${Regex.escape(api)}\b""").findAll(t).forEach { span(it, "#89b4fa") } }
        keywords.forEach { kw -> Regex("""\b${Regex.escape(kw)}\b""").findAll(t).forEach { span(it, "#cba6f7") } }
    }
}
