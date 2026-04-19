package com.example.universal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputEditText

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnNext: MaterialButton
    private lateinit var btnSkip: TextView
    private lateinit var dots: TabLayout
    private val totalPages = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)

        // Skip if already onboarded
        val prefs = getSharedPreferences("phoneclaw_config", MODE_PRIVATE)
        if (prefs.getBoolean("onboarded", false)) {
            goToMain()
            return
        }

        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.onboardingPager)
        dots      = findViewById(R.id.onboardingDots)
        btnNext   = findViewById(R.id.btnNext)
        btnSkip   = findViewById(R.id.btnSkip)

        viewPager.adapter = OnboardingPagerAdapter(this)
        viewPager.isUserInputEnabled = true

        TabLayoutMediator(dots, viewPager) { _, _ -> }.attach()

        btnNext.setOnClickListener {
            if (viewPager.currentItem < totalPages - 1) {
                viewPager.currentItem += 1
            } else {
                finishOnboarding()
            }
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                btnNext.text = if (position == totalPages - 1) "Get Started →" else "Next →"
            }
        })

        btnSkip.setOnClickListener { finishOnboarding() }
    }

    private fun finishOnboarding() {
        getSharedPreferences("phoneclaw_config", MODE_PRIVATE)
            .edit().putBoolean("onboarded", true).apply()
        goToMain()
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    inner class OnboardingPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount() = totalPages
        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> WelcomePageFragment()
            1 -> PermissionsPageFragment()
            2 -> AIKeyPageFragment()
            3 -> VisionKeyPageFragment()
            4 -> ReadyPageFragment()
            else -> WelcomePageFragment()
        }
    }

    // ── Page 1: Welcome ───────────────────────────────────────────────

    class WelcomePageFragment : Fragment() {
        override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
            i.inflate(R.layout.fragment_onboarding_page, c, false)
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            view.findViewById<TextView>(R.id.pageEmoji).text = "🤖"
            view.findViewById<TextView>(R.id.pageTitle).text = "Welcome to AutoPhone"
            view.findViewById<TextView>(R.id.pageDesc).text = "AI-powered Android automation. Speak or type any command and AutoPhone does it for you — no root required."
            view.findViewById<TextView>(R.id.pageExample).text =
                "\"Open YouTube and search for AI tutorials\"\n" +
                "\"Turn on flashlight every morning at 7am\"\n" +
                "\"Send a WhatsApp message to John\"\n" +
                "\"Play Afrobeats on Audiomack\""
        }
    }

    // ── Page 2: Permissions ────────────────────────────────────────────

    inner class PermissionsPageFragment : Fragment() {
        override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
            i.inflate(R.layout.fragment_onboarding_permissions, c, false)

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            val btnAccessibility = view.findViewById<MaterialButton>(R.id.btnGrantAccessibility)
            val btnMic = view.findViewById<MaterialButton>(R.id.btnGrantMic)
            val btnNotification = view.findViewById<MaterialButton>(R.id.btnGrantNotification)
            val btnSms = view.findViewById<MaterialButton>(R.id.btnGrantSms)

            updatePermissionButtons(view)

            btnAccessibility.setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }

            btnMic.setOnClickListener {
                ActivityCompat.requestPermissions(requireActivity(),
                    arrayOf(Manifest.permission.RECORD_AUDIO), 1001)
            }

            btnNotification.setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.requestPermissions(requireActivity(),
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1002)
                } else {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${requireContext().packageName}")))
                }
            }

            btnSms.setOnClickListener {
                ActivityCompat.requestPermissions(requireActivity(),
                    arrayOf(Manifest.permission.READ_SMS), 1003)
            }
        }

        private fun updatePermissionButtons(view: View) {
            val ctx = requireContext()
            val micGranted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            val smsGranted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

            view.findViewById<MaterialButton>(R.id.btnGrantMic)?.text =
                if (micGranted) "✅ Microphone granted" else "🎤 Grant Microphone"
            view.findViewById<MaterialButton>(R.id.btnGrantSms)?.text =
                if (smsGranted) "✅ SMS granted" else "💬 Grant SMS (for 2FA)"
        }

        override fun onResume() {
            super.onResume()
            view?.let { updatePermissionButtons(it) }
        }
    }

    // ── Page 3: AI Key ─────────────────────────────────────────────────

    class AIKeyPageFragment : Fragment() {
        override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
            i.inflate(R.layout.fragment_onboarding_aikey, c, false)

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            val prefs = requireContext().getSharedPreferences("phoneclaw_config", android.content.Context.MODE_PRIVATE)
            val providerSpinner = view.findViewById<Spinner>(R.id.providerSpinner)
            val apiKeyInput = view.findViewById<TextInputEditText>(R.id.apiKeyInput)
            val modelInput = view.findViewById<TextInputEditText>(R.id.modelInput)
            val btnSave = view.findViewById<MaterialButton>(R.id.btnSaveAI)
            val btnGetKey = view.findViewById<MaterialButton>(R.id.btnGetKey)

            val providers = arrayOf("OpenRouter", "OpenAI", "Groq", "Scitely", "Anthropic", "Custom")
            providerSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, providers)

            val savedProvider = prefs.getString("ai_provider", "OpenRouter") ?: "OpenRouter"
            providerSpinner.setSelection(providers.indexOf(savedProvider).coerceAtLeast(0))

            apiKeyInput.setText(prefs.getString("api_key", ""))
            modelInput.setText(prefs.getString("model", ""))

            providerSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    val defaultModel = BYOKClient(requireContext()).getDefaultModel(providers[pos])
                    if (modelInput.text.isNullOrEmpty()) modelInput.setText(defaultModel)
                    val url = when (providers[pos]) {
                        "OpenRouter" -> "https://openrouter.ai/keys"
                        "OpenAI" -> "https://platform.openai.com/api-keys"
                        "Groq" -> "https://console.groq.com/keys"
                        "Anthropic" -> "https://console.anthropic.com/settings/keys"
                        "Scitely" -> "https://scitely.com"
                        else -> ""
                    }
                    btnGetKey.visibility = if (url.isNotEmpty()) View.VISIBLE else View.GONE
                    btnGetKey.setOnClickListener {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }

            btnSave.setOnClickListener {
                val key = apiKeyInput.text.toString().trim()
                val model = modelInput.text.toString().trim()
                val provider = providers[providerSpinner.selectedItemPosition]
                if (key.isNotEmpty()) {
                    prefs.edit()
                        .putBoolean("use_custom_config", true)
                        .putString("ai_provider", provider)
                        .putString("api_key", key)
                        .putString("model", model)
                        .apply()
                    btnSave.text = "✅ Saved!"
                    btnSave.isEnabled = false
                }
            }

            // Show saved state
            if (prefs.getBoolean("use_custom_config", false) && !prefs.getString("api_key","").isNullOrEmpty()) {
                btnSave.text = "✅ API Key configured"
            }
        }
    }

    // ── Page 4: Vision Key (Moondream) ────────────────────────────────

    class VisionKeyPageFragment : Fragment() {
        override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
            i.inflate(R.layout.fragment_onboarding_vision, c, false)

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            val prefs = requireContext().getSharedPreferences("phoneclaw_config", android.content.Context.MODE_PRIVATE)
            val keyInput = view.findViewById<TextInputEditText>(R.id.visionKeyInput)
            val btnSave = view.findViewById<MaterialButton>(R.id.btnSaveVision)
            val btnGetKey = view.findViewById<MaterialButton>(R.id.btnGetVisionKey)

            // Load existing key from prefs
            val existingKey = prefs.getString("moondream_key", "")
            if (!existingKey.isNullOrEmpty()) {
                keyInput.setText(existingKey)
                btnSave.text = "✅ Vision key configured"
            }

            btnGetKey.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://moondream.ai")))
            }

            btnSave.setOnClickListener {
                val key = keyInput.text.toString().trim()
                if (key.isNotEmpty()) {
                    prefs.edit().putString("moondream_key", key).apply()
                    btnSave.text = "✅ Saved!"
                    btnSave.isEnabled = false
                }
            }
        }
    }

    // ── Page 5: Ready ──────────────────────────────────────────────────

    class ReadyPageFragment : Fragment() {
        override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
            i.inflate(R.layout.fragment_onboarding_page, c, false)
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            view.findViewById<TextView>(R.id.pageEmoji).text = "🚀"
            view.findViewById<TextView>(R.id.pageTitle).text = "You're all set!"
            view.findViewById<TextView>(R.id.pageDesc).text = "AutoPhone is ready. Tap Get Started and try your first command."
            view.findViewById<TextView>(R.id.pageExample).text =
                "💡 Start with something simple:\n\n" +
                "\"Open YouTube and search for AI news\"\n\n" +
                "You can always access Settings from the bottom nav to update your AI key or change themes."
        }
    }
}
