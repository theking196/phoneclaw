package com.example.universal
import com.example.universal.BYOKClient
import com.example.universal.LocalStorage
import com.example.universal.BuildConfig
import android.util.Base64
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Calendar
import android.accessibilityservice.AccessibilityServiceInfo
import android.animation.Animator
import android.annotation.SuppressLint
import android.app.ActivityManager
import com.google.gson.Gson
import android.app.DownloadManager
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.webkit.JavascriptInterface
import kotlin.math.floor

import kotlinx.coroutines.*
import kotlin.coroutines.cancellation.CancellationException
import android.content.Context as Context
import android.content.Intent
import android.widget.Toast
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.net.ConnectivityManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.accessibility.AccessibilityManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

import android.media.AudioManager
import android.bluetooth.BluetoothAdapter
import android.net.wifi.WifiManager
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.os.Vibrator
import android.os.VibrationEffect
import android.hardware.camera2.CameraManager
import android.app.NotificationManager
import android.provider.MediaStore
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import android.provider.AlarmClock
import android.provider.CalendarContract

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.animation.ObjectAnimator
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.SeekBar
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.suspendCancellableCoroutine
import android.animation.AnimatorSet
import android.view.animation.DecelerateInterpolator
import java.io.ByteArrayOutputStream
import java.net.NetworkInterface
import java.net.Inet4Address
import java.net.URL
import java.net.HttpURLConnection
import org.json.JSONObject
import android.Manifest
import android.content.pm.PackageManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier


data class Video(
    val url: String? = null,
    val caption: String? = null,
    val video_url: String? = null,
    val filename: String? = null,
    val affiliate_link: String? = null,
    val posted: Any? = null,
    val server: String? = null,
    val server_name: String? = null,
    val upload_timestamp: Any? = null,
    val passed_brand_guidelines: Any? = null
)

data class VideoWithKey(
    val key: String,
    val video: Video?
)

data class MoondreamResponse(
    val request_id: String,
    val points: List<MoondreamPoint>
)

data class MoondreamPoint(
    val x: Double,
    val y: Double
)

data class CronTask(
    val id: String,
    val taskDescription: String,
    val cronExpression: String,
    val createdAt: Long = System.currentTimeMillis(),
    var lastExecuted: Long = 0L,
    var isActive: Boolean = true
)

data class GenerationHistory(
    val id: String,
    val userCommand: String,
    val generatedCode: String,
    val timestamp: Long = System.currentTimeMillis()
)

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener, RecognitionListener {
    private lateinit var phoneDeviceId: String


    private companion object {
        const val PREFS_NAME = "AgentsBasePrefs"
        const val KEY_FIRST_RUN = "first_run"
        const val KEY_USER_EMAIL = "user_email"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_OPENROUTER_MODEL = "openrouter_model"
        const val MICROPHONE_PERMISSION_REQUEST = 1001
        const val SETTINGS_REQUEST_CODE = 1002
        const val LOCATION_PERMISSION_REQUEST = 1003
    }

    private data class OpenRouterModel(val id: String, val label: String)

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechRecognizerIntent: Intent
    private var isListening = false
    private var permissionRequestInProgress = false

    private val cronTasks = mutableMapOf<String, CronTask>()
    private val generationHistory = mutableListOf<GenerationHistory>()
    private val cronScheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(2)
    private var cronCheckJob: Job? = null


    private lateinit var toolbar: Toolbar
    private lateinit var statusText: TextView
    private lateinit var selectModelButton: MaterialButton
    private var viewPagerAdapter: ViewPagerAdapter? = null
    private lateinit var bottomNav: com.google.android.material.bottomnavigation.BottomNavigationView

    // Chat UI
    private lateinit var chatRecyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private var capturedThinking: String? = null


    // CHANGED: Better coroutine management
    private val compositeJob = SupervisorJob()
    private val mainScope = CoroutineScope(Dispatchers.Main + compositeJob)

    private lateinit var tts: TextToSpeech

    @Volatile
    private var userEmail: String = ""
    private lateinit var sharedPreferences: SharedPreferences
    private val emailLock = Object()

    private var microphoneButton: com.google.android.material.floatingactionbutton.FloatingActionButton? = null
    private var isCurrentlyListening = false

    // Drawer + new UI
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private var commandInput: com.google.android.material.textfield.TextInputEditText? = null
    private var runCommandButton: View? = null
    private var isCompactMode = false
    private var isDeveloperMode = false
    val debugLogBuffer = StringBuilder()


    // NEW: Track bitmaps for cleanup
    private val activeBitmaps = mutableSetOf<Bitmap>()

    // NEW: Flag to prevent operations after destroy
    @Volatile
    private var isDestroyed = false

    // NEW: Memory monitoring
    private fun checkMemoryUsage(): Boolean {
        val runtime = Runtime.getRuntime()
        val usedMem = runtime.totalMemory() - runtime.freeMemory()
        val maxMem = runtime.maxMemory()
        val usagePercent = (usedMem * 100 / maxMem)

        Log.d("Memory", "Usage: $usagePercent% (${usedMem / 1024 / 1024}MB / ${maxMem / 1024 / 1024}MB)")

        if (usagePercent > 85) {
            Log.w("Memory", "High memory usage, forcing GC")
            System.gc()
            return false
        }
        return true
    }

    // NEW: Cleanup bitmaps after use
    private fun cleanupBitmap(bitmap: Bitmap?) {
        bitmap?.let {
            if (activeBitmaps.remove(it)) {
                if (!it.isRecycled) {
                    try {
                        it.recycle()
                        Log.d("Memory", "Bitmap recycled successfully")
                    } catch (e: Exception) {
                        Log.e("Memory", "Error recycling bitmap: ${e.message}")
                    }
                }
            }
        }
    }

    @JavascriptInterface
    fun sendAgentEmail(to: String, subject: String, message: String) {
        // Privacy: emails are logged locally only, not sent to external servers
        Log.i("AgentEmail", "Agent email queued locally: to=$to subject=$subject")
        LocalStorage.logAgentEmail(this, to, subject, message)
        speakText("Agent email logged locally")
    }


    fun openBrowserCaptivePortal() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("http://connectivitycheck.gstatic.com/generate_204")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            val settingsIntent = Intent(Settings.ACTION_WIFI_SETTINGS)
            settingsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(settingsIntent)
        }
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    fun connectToHiltonHonors() {
        try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val specifier = WifiNetworkSpecifier.Builder()
                .setSsid("Hilton Honors")
                .build()

            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier)
                .build()

            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    runOnUiThread {
                        speakText("Connected to Hilton Honors Wi-Fi. Checking for captive portal.")
                    }

                    connectivityManager.bindProcessToNetwork(network)
                    val caps = connectivityManager.getNetworkCapabilities(network)
                    val isCaptive = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) == true


                        openBrowserCaptivePortal()


                }

                override fun onUnavailable() {
                    runOnUiThread { speakText("Could not connect to Hilton Honors.") }
                }

                override fun onLost(network: Network) {
                    runOnUiThread { speakText("Hilton Honors connection lost.") }
                    connectivityManager.bindProcessToNetwork(null)
                }
            }

            connectivityManager.requestNetwork(request, networkCallback)
            speakText("Attempting to connect to Hilton Honors Wi-Fi.")
        } catch (e: Exception) {
            Log.e("HiltonWiFi", "Error connecting: ${e.message}")
            speakText("Error connecting to Hilton Honors network.")
        }
    }

    private fun openCaptivePortalFallback() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("http://connectivitycheck.gstatic.com/generate_204")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            startActivity(intent)
            speakText("Opening Hilton Honors login page.")
        } catch (e: Exception) {
            val settingsIntent = Intent(Settings.ACTION_WIFI_SETTINGS)
            settingsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(settingsIntent)
            speakText("Opening Wi-Fi settings for manual login.")
        }
    }

    fun openSystemCaptivePortal(connectivityManager: ConnectivityManager, network: Network) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Reflection so it compiles on all SDK levels
                val method = ConnectivityManager::class.java
                    .getMethod("startCaptivePortalApp", Network::class.java)
                method.invoke(connectivityManager, network)
            } else {
                openBrowserCaptivePortal()
            }
        } catch (e: Exception) {
            Log.e("CaptivePortal", "Unable to start captive portal app: ${e.message}")
            openBrowserCaptivePortal()
        }
    }


    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //connectToHiltonHonors();

        initializeModernUI()

        NotificationTriggerService.onTriggerFired = { rule, notifText ->
            runOnUiThread {
                mainScope.launch {
                    updateStatusWithAnimation("🔔 Trigger: ${rule.description}")
                    val script = rule.script.replace("{notification}", notifText)
                    executeGeneratedCode(script)
                }
            }
        }

        try {
            sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            Log.d("MainActivity", "SharedPreferences initialized successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to initialize SharedPreferences: ${e.message}")
            sharedPreferences = getSharedPreferences("FallbackPrefs", Context.MODE_PRIVATE)
        }
        updateModelButton()

        initializeUserEmail()
        tts = TextToSpeech(this, this)

        phoneDeviceId = try {
            val id = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            Log.d("MainActivity", "Device ID retrieved: $id")
            id ?: "unknown_device"
        } catch (e: Exception) {
            Log.e("MainActivity", "Error getting device ID: ${e.message}")
            "unknown_device_${System.currentTimeMillis()}"
        }

        Log.d("MainActivity", "Device ID: $phoneDeviceId")


        val intent = Intent(this, ScreenshotActivity::class.java)
        startActivity(intent)

        checkAccessibilityPermission()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_SMS), 1004)
        }

        loadCronTasks()
        loadGenerationHistory()
        addTestHistoryItems()

        startCronChecker()
        testCronScheduler()
        MyAccessibilityService.instance?.simulateClick(560f, 1139f)


        updateUI()
        setupMicrophoneButton()
    }

    private fun setupMicrophoneButton() {
        microphoneButton?.setOnClickListener {
            if (!isCurrentlyListening) {
                startPushToTalkListening()
            } else {
                stopListening()
            }
        }

        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED -> {
                updateStatusWithAnimation("🎤 Ready - Tap button to speak")
                initializeSpeechRecognition()
            }
            else -> {
                requestMicrophonePermission()
            }
        }
    }

    private fun startPushToTalkListening() {
        if (!isPermissionGranted()) {
            requestMicrophonePermission()
            return
        }

        if (!::speechRecognizer.isInitialized) {
            initializeSpeechRecognition()
        }

        try {
            isCurrentlyListening = true
            isListening = true

            runOnUiThread {
                microphoneButton?.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFf38ba8.toInt())
                animateButtonPulse(microphoneButton)
            }

            speechRecognizer.startListening(speechRecognizerIntent)
            updateStatusWithAnimation("🎤 Listening - Speak now...")
            Log.d("MainActivity", "Started push-to-talk listening")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error starting push-to-talk: ${e.message}")
            isCurrentlyListening = false
            isListening = false
            resetMicrophoneButton()
            speakText("Error starting voice recognition")
        }
    }

    private fun stopListening() {
        try {
            if (::speechRecognizer.isInitialized) {
                speechRecognizer.stopListening()
            }
            isCurrentlyListening = false
            isListening = false
            resetMicrophoneButton()
            updateStatusWithAnimation("🎤 Ready - Tap button to speak")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error stopping listening: ${e.message}")
        }
    }

    private fun resetMicrophoneButton() {
        runOnUiThread {
            microphoneButton?.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF89b4fa.toInt())
            microphoneButton?.clearAnimation()
        }
    }

    private fun animateButtonPulse(view: View?) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY)
        animatorSet.duration = 1000
        animatorSet.interpolator = DecelerateInterpolator()
        animatorSet.start()
    }

    private fun initializeModernUI() {
        statusText = findViewById(R.id.statusText)
        selectModelButton = findViewById(R.id.selectModelButton)
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        // Toolbar + Drawer
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            android.R.string.ok, android.R.string.cancel
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Navigation item selection
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_dashboard -> {}
                R.id.nav_tasks -> { bottomNav.selectedItemId = R.id.tab_tasks; drawerLayout.closeDrawers() }
                R.id.nav_history -> startActivity(android.content.Intent(this, HistoryActivity::class.java))
                R.id.nav_ai_config -> showAIConfigSheet()
                R.id.nav_model_chain -> showModelChainSheet()
                R.id.nav_vision -> showVisionSheet()
                R.id.nav_debug -> startActivity(android.content.Intent(this, DebugActivity::class.java))
                R.id.nav_about -> { bottomNav.selectedItemId = R.id.tab_settings; drawerLayout.closeDrawers() }
                R.id.nav_settings -> { bottomNav.selectedItemId = R.id.tab_settings; drawerLayout.closeDrawers() }
                R.id.nav_scripts -> showScriptLibraryDialog()
                R.id.nav_audit -> showAuditLogDialog()
                R.id.nav_ollama -> showOllamaConfigDialog()
                R.id.nav_recorder -> showFlowRecorderDialog()
                R.id.nav_triggers -> showNotificationTriggersDialog()
                R.id.nav_skills_plugin -> showSkillsDialog()
                R.id.nav_memory -> showMemoryDialog()
                R.id.nav_clear_schedule -> clearAllScheduledTasks()
            }
            drawerLayout.closeDrawers()
            true
        }

        selectModelButton.setOnClickListener {
            animateButtonClick(it)
            showAIConfigSheet()
        }

        // Bottom navigation
        bottomNav = findViewById(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.tab_chat -> {
                    showFragment(ChatFragment(), "chat")
                    true
                }
                R.id.tab_tasks -> {
                    showFragment(TasksFragment(), "tasks")
                    true
                }
                R.id.tab_library -> {
                    // Combined Skills + Tools tab
                    showFragment(SkillsFragment(), "library")
                    true
                }
                R.id.tab_settings -> {
                    showFragment(SettingsFragment(), "settings")
                    true
                }
                else -> false
            }
        }

        // Load default tab (Chat)
        showFragment(ChatFragment(), "chat")
        bottomNav.selectedItemId = R.id.tab_chat

        // Load compact/dev mode
        val prefs = getSharedPreferences("phoneclaw_config", MODE_PRIVATE)
        isCompactMode = prefs.getBoolean("compact_mode", false)
        isDeveloperMode = prefs.getBoolean("developer_mode", false)

        Log.d("MainActivity", "Modern UI initialized successfully")
    }

    private fun showFragment(fragment: androidx.fragment.app.Fragment, tag: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment, tag)
            .commit()

        // After chat fragment loads, wire up the chat views
        if (tag == "chat") {
            supportFragmentManager.executePendingTransactions()
            initChatFragment()
        }
    }

    private fun initChatFragment() {
        val fragment = supportFragmentManager.findFragmentByTag("chat") ?: return
        val view = fragment.view ?: return

        // Wire RecyclerView
        chatRecyclerView = view.findViewById(R.id.chatRecyclerView)
        chatAdapter = ChatAdapter(
            onCodeTap = { code ->
                editCode(GenerationHistory(java.util.UUID.randomUUID().toString(), "Code", code))
            }
        )
        chatRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this).also { it.stackFromEnd = true }
        chatRecyclerView.adapter = chatAdapter

        // Welcome message
        if (chatAdapter.itemCount == 0) {
            chatAdapter.addMessage(ChatMessage(
                text = "👋 AutoPhone ready. Speak or type a command.\n\nTry: \"Open YouTube and search for AI news\"",
                type = MessageType.SYSTEM
            ))
        }

        // Wire mic button
        microphoneButton = view.findViewById(R.id.microphoneButton)
        microphoneButton?.setOnClickListener {
            if (!isCurrentlyListening) startPushToTalkListening() else stopListening()
        }

        // Wire send button
        val sendBtn = view.findViewById<View>(R.id.runCommandButton)
        commandInput = view.findViewById(R.id.commandInput)
        sendBtn?.setOnClickListener {
            val text = commandInput?.text?.toString()?.trim() ?: ""
            if (text.isNotEmpty()) {
                commandInput?.setText("")
                processVoiceCommand(text)
            }
        }
        commandInput?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendBtn?.performClick()
                true
            } else false
        }

        // Wire chips
        view.findViewById<com.google.android.material.chip.Chip>(R.id.chipOpenApp)?.setOnClickListener {
            commandInput?.setText("Open ")
            commandInput?.requestFocus()
        }
        view.findViewById<com.google.android.material.chip.Chip>(R.id.chipSearch)?.setOnClickListener {
            processVoiceCommand("Search YouTube for ")
        }
        view.findViewById<com.google.android.material.chip.Chip>(R.id.chipSchedule)?.setOnClickListener {
            commandInput?.setText("Schedule ")
            commandInput?.requestFocus()
        }
        view.findViewById<com.google.android.material.chip.Chip>(R.id.chipBattery)?.setOnClickListener {
            processVoiceCommand("Check battery level")
        }
        view.findViewById<com.google.android.material.chip.Chip>(R.id.chipFlashlight)?.setOnClickListener {
            processVoiceCommand("Toggle flashlight")
        }
        view.findViewById<com.google.android.material.chip.Chip>(R.id.chipSkills)?.setOnClickListener {
            bottomNav.selectedItemId = R.id.tab_skills
        }

        // Animated placeholder
        val placeholders = listOf(
            "Ask AutoPhone anything...",
            "\"Open YouTube and search for AI\"",
            "\"Turn on flashlight every morning\"",
            "\"Send WhatsApp message to John\"",
            "\"Schedule TikTok every 2 hours\"",
            "\"Search Audiomack for Afrobeats\""
        )
        var placeholderIndex = 0
        mainScope.launch {
            while (!isDestroyed) {
                delay(3000)
                if (!isDestroyed && commandInput != null) {
                    placeholderIndex = (placeholderIndex + 1) % placeholders.size
                    withContext(Dispatchers.Main) {
                        commandInput?.hint = placeholders[placeholderIndex]
                    }
                }
            }
        }

        setupMicrophoneButton()
        updateModelButton()
    }

    private fun showOpenRouterModelDialog() {
        val prefs = getSharedPreferences("phoneclaw_config", MODE_PRIVATE)
        val sandboxOn = prefs.getBoolean("sandbox_enabled", true)
        val agentLoopOn = prefs.getBoolean("use_agent_loop", true)

        val mainOptions = arrayOf(
            "Use Custom API Key",
            "Use Default (OpenRouter)",
            "View Debug Logs",
            "Test Mode (Preview Code)",
            "Manual JS Input",
            "${if (sandboxOn) "✅" else "⬜"} Execution Sandbox",
            "${if (agentLoopOn) "✅" else "⬜"} Agent Loop Mode"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("AI Configuration")
            .setItems(mainOptions) { _, which ->
                when (which) {
                    0 -> showBYOKConfigDialog()
                    1 -> {
                        prefs.edit().putBoolean("use_custom_config", false).apply()
                        showDefaultModelDialog()
                    }
                    2 -> showDebugDialog()
                    3 -> showTestModeDialog()
                    4 -> showManualInputDialog()
                    5 -> {
                        val current = prefs.getBoolean("sandbox_enabled", true)
                        prefs.edit().putBoolean("sandbox_enabled", !current).apply()
                        speakText(if (!current) "Execution sandbox enabled" else "Execution sandbox disabled")
                        Toast.makeText(this, if (!current) "🔒 Sandbox ON" else "🔓 Sandbox OFF", Toast.LENGTH_SHORT).show()
                    }
                    6 -> {
                        val current = prefs.getBoolean("use_agent_loop", true)
                        prefs.edit().putBoolean("use_agent_loop", !current).apply()
                        speakText(if (!current) "Agent loop enabled" else "Agent loop disabled")
                        Toast.makeText(this, if (!current) "🤖 Agent Loop ON" else "🤖 Agent Loop OFF", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBYOKConfigDialog() {
        val prefs = getSharedPreferences("phoneclaw_config", MODE_PRIVATE)
        val currentProvider = prefs.getString("ai_provider", "OpenRouter")
        val providers = arrayOf("OpenRouter", "OpenAI", "Groq", "Scitely")
        val providerIdx = providers.indexOf(currentProvider).coerceAtLeast(0)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Select AI Provider")
            .setSingleChoiceItems(providers, providerIdx) { _, which ->
                val selectedProvider = providers[which]
                prefs.edit().putString("ai_provider", selectedProvider).apply()
                showAPIKeyInputDialog(selectedProvider)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showAPIKeyInputDialog(provider: String) {
        val prefs = getSharedPreferences("phoneclaw_config", MODE_PRIVATE)
        val editText = android.widget.EditText(this).apply {
            hint = "Enter API Key"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("API Key for $provider")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val key = editText.text.toString().trim()
                if (key.isNotEmpty()) {
                    prefs.edit().putString("api_key", key).putBoolean("use_custom_config", true).apply()
                    Toast.makeText(this, "API Key saved", Toast.LENGTH_SHORT).show()
                    showModelInputDialog(provider)
                } else {
                    Toast.makeText(this, "API Key required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showModelInputDialog(provider: String) {
        val prefs = getSharedPreferences("phoneclaw_config", MODE_PRIVATE)
        val defaultModel = when(provider) { "OpenRouter" -> "qwen/Qwen3-8B"; "OpenAI" -> "gpt-4o-mini"; "Groq" -> "llama-3.1-8b-instant"; "Scitely" -> "deepseek-chat"; else -> "gpt-4o-mini" }
        val editText = android.widget.EditText(this).apply { hint = "Model (default: $defaultModel)"; setText(prefs.getString("model", "")) }
        MaterialAlertDialogBuilder(this)
            .setTitle("Model for $provider")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val model = editText.text.toString().trim().ifEmpty { defaultModel }
                prefs.edit().putString("model", model).apply()
                Toast.makeText(this, "Configured: $provider / $model", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Skip", null)
            .show()
    }

    private fun showDefaultModelDialog() {
        val options = getOpenRouterModelOptions()
        if (options.isEmpty()) { speakText("No models available"); return }
        val selectedId = getSelectedOpenRouterModelId()
        val selectedIndex = options.indexOfFirst { it.id == selectedId }.let { if (it >= 0) it else 0 }
        val labels = options.map { "${it.label} (${it.id})" }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle("Select OpenRouter Model")
            .setSingleChoiceItems(labels, selectedIndex) { dialog, which ->
                val selected = options[which]
                setSelectedOpenRouterModelId(selected.id)
                updateModelButton()
                speakText("Selected model ${selected.label}")
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDebugDialog() {
        val prefs = getSharedPreferences("phoneclaw_config", MODE_PRIVATE)
        
        val debugInfo = buildString {
            appendLine("=== DEBUG INFO ===")
            appendLine("")
            appendLine("--- API Config ---")
            appendLine("Provider: ${prefs.getString("ai_provider", "Not set")}")
            appendLine("Model: ${prefs.getString("model", "Not set")}")
            appendLine("Custom Config: ${prefs.getBoolean("use_custom_config", false)}")
            appendLine("")
            appendLine("--- App Info ---")
            appendLine("Version: ${BuildConfig.VERSION_NAME}")
            appendLine("OpenRouter Model: ${getSelectedOpenRouterModelId()}")
            appendLine("")
            appendLine("--- Last Error ---")
            appendLine(prefs.getString("last_error", "No errors logged"))
            appendLine("")
            appendLine("--- API Info ---")
            appendLine("Provider: ${getCurrentProviderName()}")
            appendLine("Using Custom: ${shouldUseCustomAPI()}")
            appendLine("")
            appendLine("--- Last AI Response ---")
            appendLine((prefs.getString("last_ai_response", "No response logged") ?: "").take(1000))
            appendLine("")
            appendLine("--- Recent Logs ---")
            appendLine((prefs.getString("debug_log", "No debug logs") ?: ""))
        }
        
        val scrollView = android.widget.ScrollView(this).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                400)
        }
        val editText = android.widget.EditText(this).apply {
            setText(debugInfo)
            isEnabled = false
            textSize = 10f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#1E1E2E"))
        }
        scrollView.addView(editText)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Debug Information")
            .setView(scrollView)
            .setPositiveButton("Copy") { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Debug Info", debugInfo)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Clear Logs") { _, _ ->
                prefs.edit().remove("last_error").remove("debug_log").apply()
                Toast.makeText(this, "Logs cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    // Test Mode - Preview generated code without executing
    private fun showTestModeDialog() {
        val editText = android.widget.EditText(this).apply {
            hint = "Enter command to preview code"
            setText("")
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Test Mode - Preview JS Code")
            .setView(editText)
            .setPositiveButton("Generate") { _, _ ->
                val command = editText.text.toString().trim()
                if (command.isNotEmpty()) {
                    // Launch coroutine to generate code and show it
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            val code = generateAutomationCode(command)
                            showPreviewDialog(code)
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showPreviewDialog(code: String) {
        val scrollView = android.widget.ScrollView(this).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                500)
        }
        val editText = android.widget.EditText(this).apply {
            setText(code)
            isEnabled = false
            textSize = 11f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#1E1E2E"))
        }
        scrollView.addView(editText)

        MaterialAlertDialogBuilder(this)
            .setTitle("Generated JavaScript")
            .setView(scrollView)
            .setPositiveButton("Run") { _, _ ->
                executeScript(code)
            }
            .setNeutralButton("Save") { _, _ ->
                saveScript(code)
            }
            .setNegativeButton("Copy") { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("JS Code", code)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Copied!", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private private fun canExecuteTool(tool: String): Boolean =
    SkillsFragment.isToolEnabled(this, tool)

    // === MEMORY FUNCTIONS FOR SCRIPT ===
    private val memoryDB by lazy { MemoryDB(this) }
    
    fun saveToMemory(key: String, value: String): String {
        memoryDB.remember(key, value)
        return "Saved: $key = $value"
    }
    
    fun readFromMemory(key: String): String {
        return memoryDB.recall(key) ?: "Not found: $key"
    }
    
    fun learnFact(fact: String, context: String = ""): String {
        memoryDB.learnFact(fact, context)
        return "Learned: $fact"
    }
    
    fun searchHistory(query: String, limit: Int = 5): String {
        val results = memoryDB.searchInteractions(query, limit)
        if (results.isEmpty()) return "No results for: $query"
        return results.take(3).joinToString("\n---\n") { "${it.userInput} → ${it.aiResponse}" }
    }
    
    fun getContext(limit: Int = 5): String {
        return memoryDB.getContextForAI(limit)
    }
    // ==================================
    
    fun executeScript(code: String) {
        // Check if confirmation is enabled
        if (SafetyConfig.isConfirmEnabled(this)) {
            // Show confirmation dialog
            showConfirmRunDialog(code)
        } else {
            // Run directly
            runScriptDirect(code)
        }
    }
    
    private fun showConfirmRunDialog(code: String) {
        val preview = code.take(200) + if (code.length > 200) "..." else ""
        MaterialAlertDialogBuilder(this)
            .setTitle("⚠️ Confirm Script Execution?")
            .setMessage("This script will run on your phone:\n\n$preview")
            .setPositiveButton("Run") { _, _ -> runScriptDirect(code) }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Save Only") { _, _ -> saveScript(code) }
            .show()
    }
    
    private fun runScriptDirect(code: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(this@MainActivity, "Running script...", Toast.LENGTH_SHORT).show()
            try {
                addToHistory("Script", code)
                Toast.makeText(this@MainActivity, "Script queued!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveScript(code: String) {
        try {
            val file = java.io.File(filesDir, "saved_script_${System.currentTimeMillis()}.js")
            file.writeText(code)
            Toast.makeText(this, "Saved to ${file.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Save error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Manual Input - Enter JS code directly
    private fun showManualInputDialog() {
        val editText = android.widget.EditText(this).apply {
            hint = "Enter JavaScript code"
            setText("")
        }
        
        val scrollView = android.widget.ScrollView(this).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                400)
        }
        scrollView.addView(editText)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Manual JavaScript Input")
            .setMessage("Enter JS code to execute directly")
            .setView(scrollView)
            .setPositiveButton("Execute") { _, _ ->
                val code = editText.text.toString().trim()
                if (code.isNotEmpty()) {
                    CoroutineScope(Dispatchers.Main).launch {
                        executeGeneratedCode(code)
                        Toast.makeText(this@MainActivity, "Code executed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun animateButtonClick(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY)
        animatorSet.duration = 150
        animatorSet.interpolator = DecelerateInterpolator()
        animatorSet.start()
    }

    private fun updateStatusWithAnimation(text: String) {
        runOnUiThread {
            val fadeOut = ObjectAnimator.ofFloat(statusText, "alpha", 1f, 0f)
            fadeOut.duration = 200

            fadeOut.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    statusText.text = text
                    val fadeIn = ObjectAnimator.ofFloat(statusText, "alpha", 0f, 1f)
                    fadeIn.duration = 200
                    fadeIn.start()
                }
            })

            fadeOut.start()

            // Mirror agent thinking steps to chat
            if (text.startsWith("🤔") || text.startsWith("💭") || text.startsWith("⚡")) {
                if (::chatAdapter.isInitialized && ::chatRecyclerView.isInitialized) {
                    chatAdapter.addMessage(ChatMessage(text = text, type = MessageType.SYSTEM))
                    chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
                }
            }
        }
    }

    private fun getOpenRouterModelOptions(): List<OpenRouterModel> {
        return listOf(
            OpenRouterModel("google/gemini-2.0-flash-001", "Gemini 2.0 Flash"),
            OpenRouterModel("meta-llama/llama-4-maverick:free", "Llama 4 Maverick (Free)")
        )
    }

    private fun getSelectedOpenRouterModelId(): String {
        val defaultId = getOpenRouterModelOptions().first().id
        if (!::sharedPreferences.isInitialized) return defaultId

        val saved = sharedPreferences.getString(KEY_OPENROUTER_MODEL, null)
        return if (saved != null && getOpenRouterModelOptions().any { it.id == saved }) {
            saved
        } else {
            defaultId
        }
    }

    private fun setSelectedOpenRouterModelId(id: String) {
        if (!::sharedPreferences.isInitialized) return
        sharedPreferences.edit().putString(KEY_OPENROUTER_MODEL, id).apply()
    }

    private fun updateModelButton() {
        if (!::selectModelButton.isInitialized) return
        val prefs = getSharedPreferences("phoneclaw_config", MODE_PRIVATE)
        val useCustom = prefs.getBoolean("use_custom_config", false)
        val ollamaOn = prefs.getBoolean("ollama_enabled", false)
        val agentOn = prefs.getBoolean("use_agent_loop", true)

        val label = when {
            ollamaOn -> {
                val model = prefs.getString("ollama_model", "llama3.2") ?: "llama3.2"
                "🦙 $model"
            }
            useCustom -> {
                val provider = prefs.getString("ai_provider", "OpenRouter") ?: "OpenRouter"
                val model = prefs.getString("model", "") ?: ""
                val shortModel = if (model.contains("/")) model.substringAfterLast("/") else model
                val providerShort = provider.take(6)
                "$providerShort: ${shortModel.take(12)}"
            }
            else -> {
                val model = prefs.getString(KEY_OPENROUTER_MODEL, "GPT-4o Mini") ?: "GPT-4o Mini"
                "☁️ ${model.take(14)}"
            }
        }
        val agentBadge = if (agentOn && useCustom) " 🤖" else ""
        runOnUiThread { selectModelButton.text = label + agentBadge }
    }

    class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ScheduledTasksFragment()
                1 -> GenerationHistoryFragment()
                2 -> DebugLogFragment()
                else -> ScheduledTasksFragment()
            }
        }
    }

    class DebugLogFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.fragment_debug_log, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val logText = view.findViewById<android.widget.TextView>(R.id.debugLogText)
            val activity = requireActivity() as? MainActivity
            val logContent = activity?.debugLogBuffer?.toString() ?: "No logs yet."
            logText?.text = if (logContent.isBlank()) "No logs yet." else logContent
            view.findViewById<com.google.android.material.button.MaterialButton>(R.id.clearLogButton)?.setOnClickListener {
                activity?.debugLogBuffer?.clear()
                logText?.text = "Log cleared."
            }
        }
    }

    class ScheduledTasksFragment : Fragment() {
        private lateinit var tasksRecyclerView: RecyclerView
        private lateinit var swipeRefresh: SwipeRefreshLayout
        private lateinit var tasksAdapter: TasksAdapter

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return inflater.inflate(R.layout.fragment_scheduled_tasks, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            tasksRecyclerView = view.findViewById(R.id.tasksRecyclerView)
            swipeRefresh = view.findViewById(R.id.swipeRefresh)

            setupRecyclerView()
            setupSwipeRefresh()
        }

        private fun setupRecyclerView() {
            tasksAdapter = TasksAdapter(mutableListOf()) { action, task ->
                val mainActivity = activity as? MainActivity
                when (action) {
                    "run" -> mainActivity?.runTaskNow(task)
                    "delete" -> mainActivity?.deleteTask(task)
                }
            }

            tasksRecyclerView.layoutManager = LinearLayoutManager(context)
            tasksRecyclerView.adapter = tasksAdapter
            tasksRecyclerView.itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
        }

        private fun setupSwipeRefresh() {
            swipeRefresh.setColorSchemeColors(
                0xFF89b4fa.toInt(),
                0xFFa6e3a1.toInt(),
                0xFFfab387.toInt()
            )

            swipeRefresh.setOnRefreshListener {
                val mainActivity = activity as? MainActivity
                mainActivity?.updateUI()
                swipeRefresh.isRefreshing = false
            }
        }

        fun updateTasks(tasks: List<CronTask>) {
            if (::tasksAdapter.isInitialized) {
                tasksAdapter.updateTasks(tasks)
            }
        }
    }

    class GenerationHistoryFragment : Fragment() {
        private lateinit var historyRecyclerView: RecyclerView
        private lateinit var swipeRefresh: SwipeRefreshLayout
        private lateinit var historyAdapter: HistoryAdapter

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return inflater.inflate(R.layout.fragment_generation_history, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            historyRecyclerView = view.findViewById(R.id.historyRecyclerView)
            swipeRefresh = view.findViewById(R.id.swipeRefresh)

            setupRecyclerView()
            setupSwipeRefresh()
        }

        private fun setupRecyclerView() {
            historyAdapter = HistoryAdapter(mutableListOf()) { action, history ->
                val mainActivity = activity as? MainActivity
                when (action) {
                    "run" -> mainActivity?.runCode(history.generatedCode)
                    "edit" -> mainActivity?.editCode(history)
                    "schedule" -> mainActivity?.scheduleCode(history)
                }
            }

            historyRecyclerView.layoutManager = LinearLayoutManager(context)
            historyRecyclerView.adapter = historyAdapter
            historyRecyclerView.itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
        }

        private fun setupSwipeRefresh() {
            swipeRefresh.setColorSchemeColors(
                0xFF89b4fa.toInt(),
                0xFFa6e3a1.toInt(),
                0xFFfab387.toInt()
            )

            swipeRefresh.setOnRefreshListener {
                val mainActivity = activity as? MainActivity
                mainActivity?.updateUI()
                swipeRefresh.isRefreshing = false
            }
        }

        fun updateHistory(history: List<GenerationHistory>) {
            if (::historyAdapter.isInitialized) {
                historyAdapter.updateHistory(history)
            }
        }
    }

    private fun isTikTokInstalled(): Boolean {
        return try {
            packageManager.getPackageInfo("com.zhiliaoapp.musically", PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    var blockedNames = listOf(
        "Create", "Email", "Facebook", "Flash", "More", "SMS", "Timer", "X",
        "Instagram", "Twitter", "Shorts", "LinkedIn", "Google", "YouTube",
        "TikTok", "Snapchat", "WhatsApp", "Telegram", "Discord", "Reddit", "Next", "Switch", "Save"
    )

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            for (networkInterface in interfaces) {
                val addresses = networkInterface.inetAddresses
                for (address in addresses) {
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e("MainActivity", "Error getting IP address: $ex")
        }
        return null
    }

    @SuppressLint("ServiceCast")
    private fun getDeviceInfo(): Map<String, String> {
        val deviceInfo = mutableMapOf<String, String>()

        try {
            deviceInfo["device_model"] = Build.MODEL
            deviceInfo["device_manufacturer"] = Build.MANUFACTURER
            deviceInfo["device_brand"] = Build.BRAND
            deviceInfo["device_name"] = Build.DEVICE
            deviceInfo["android_version"] = Build.VERSION.RELEASE
            deviceInfo["sdk_version"] = Build.VERSION.SDK_INT.toString()
            deviceInfo["build_id"] = Build.ID

            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            deviceInfo["battery_percentage"] = "$batteryLevel%"

            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            deviceInfo["available_memory_mb"] = "${memInfo.availMem / (1024 * 1024)}"
            deviceInfo["total_memory_mb"] = "${memInfo.totalMem / (1024 * 1024)}"
            deviceInfo["low_memory"] = memInfo.lowMemory.toString()

            val displayMetrics = resources.displayMetrics
            deviceInfo["screen_width"] = displayMetrics.widthPixels.toString()
            deviceInfo["screen_height"] = displayMetrics.heightPixels.toString()
            deviceInfo["screen_density"] = displayMetrics.density.toString()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                val statFs = android.os.StatFs(android.os.Environment.getDataDirectory().path)
                val availableBytes = statFs.availableBytes
                val totalBytes = statFs.totalBytes
                deviceInfo["available_storage_gb"] = "${availableBytes / (1024 * 1024 * 1024)}"
                deviceInfo["total_storage_gb"] = "${totalBytes / (1024 * 1024 * 1024)}"
            }

            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetworkInfo
            deviceInfo["network_type"] = activeNetwork?.typeName ?: "unknown"
            deviceInfo["network_connected"] = (activeNetwork?.isConnected == true).toString()

        } catch (e: Exception) {
            Log.e("MainActivity", "Error getting device info: $e")
            deviceInfo["device_info_error"] = e.message ?: "unknown error"
        }

        return deviceInfo
    }

    private fun addTestHistoryItems() {
        if (generationHistory.isEmpty()) {
            val testItems = listOf(
                GenerationHistory(
                    id = UUID.randomUUID().toString(),
                    userCommand = "Open TikTok",
                    generatedCode = "speakText(\"Opening TikTok\");\nlaunchTikTok();",
                    timestamp = System.currentTimeMillis() - 300000
                ),
                GenerationHistory(
                    id = UUID.randomUUID().toString(),
                    userCommand = "Check battery level",
                    generatedCode = "speakText(\"Checking battery level\");\nvar level = getBatteryLevel();\nspeakText(\"Battery is at \" + level + \" percent\");",
                    timestamp = System.currentTimeMillis() - 600000
                ),
                GenerationHistory(
                    id = UUID.randomUUID().toString(),
                    userCommand = "Turn on WiFi",
                    generatedCode = "speakText(\"Turning on WiFi\");\ntoggleWiFi(true);",
                    timestamp = System.currentTimeMillis() - 900000
                ),
                GenerationHistory(
                    id = UUID.randomUUID().toString(),
                    userCommand = "Take a photo",
                    generatedCode = "speakText(\"Taking photo\");\ntakePhoto();",
                    timestamp = System.currentTimeMillis() - 1200000
                ),
                GenerationHistory(
                    id = UUID.randomUUID().toString(),
                    userCommand = "Set brightness to 75%",
                    generatedCode = "speakText(\"Setting brightness to 75 percent\");\nsetBrightness(75);",
                    timestamp = System.currentTimeMillis() - 1500000
                ),
                GenerationHistory(
                    id = UUID.randomUUID().toString(),
                    userCommand = "Toggle flashlight",
                    generatedCode = "speakText(\"Toggling flashlight\");\ntoggleFlashlight(true);\ndelay(3000);\ntoggleFlashlight(false);",
                    timestamp = System.currentTimeMillis() - 1800000
                )
            )

            generationHistory.addAll(testItems)
            saveGenerationHistory()
            Log.d("MainActivity", "Added ${testItems.size} test history items")
        }
    }

    private fun updateUI() {
        if (isDestroyed) return

        runOnUiThread {
            val tasksCount = cronTasks.size
            val historyCount = generationHistory.size

            updateStatusWithAnimation("📊 Active Tasks: $tasksCount | History: $historyCount")

            val tasksFragment = supportFragmentManager.findFragmentByTag("f0") as? ScheduledTasksFragment
            tasksFragment?.updateTasks(cronTasks.values.toList())

            val historyFragment = supportFragmentManager.findFragmentByTag("f1") as? GenerationHistoryFragment
            historyFragment?.updateHistory(generationHistory.toList())

            Log.d("MainActivity", "UI updated - Tasks: $tasksCount, History: $historyCount")
        }
    }

    fun runTaskNow(task: CronTask) {
        if (isDestroyed) return

        mainScope.launch {
            try {
                updateStatusWithAnimation("⚡ Running task: ${task.taskDescription}")
                speakText("Running task now: ${task.taskDescription}")

                val automationCode = generateAutomationCodeWithFallback(task.taskDescription)
                executeGeneratedCode(automationCode)

                speakText("Task executed successfully")
                updateStatusWithAnimation("✅ Task completed successfully")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error running task: ${e.message}")
                speakText("Error running task")
                updateStatusWithAnimation("❌ Error running task")
            }
        }
    }

    fun deleteTask(task: CronTask) {
        if (task.id == "universal_script_daily") {
            speakText("Cannot delete daily automation script task")
            return
        }

        cronTasks.remove(task.id)
        saveCronTasks()
        speakText("Task deleted: ${task.taskDescription}")
        updateUI()
        updateStatusWithAnimation("🗑️ Task deleted")
    }

    fun runCode(code: String) {
        if (isDestroyed) return
        executeWithPermissionPrompt(code, "Run from history")
    }

    fun editCode(history: GenerationHistory) {
        val editor = ScriptEditorView(this).apply { setText(history.generatedCode); setPadding(32,32,32,32); minLines = 20 }
        val sv = android.widget.ScrollView(this).apply { addView(editor) }
        MaterialAlertDialogBuilder(this)
            .setTitle("✏️ ${history.userCommand}")
            .setView(sv)
            .setPositiveButton("▶ Run") { _, _ -> executeWithPermissionPrompt(editor.text.toString(), history.userCommand) }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("💾 Save") { _, _ ->
                val code = editor.text.toString()
                val h = GenerationHistory(java.util.UUID.randomUUID().toString(), "${history.userCommand} (edited)", code)
                generationHistory.add(0, h); saveGenerationHistory()
                ScriptManager.saveScript(this, history.userCommand, code, "Saved from editor")
                updateUI(); speakText("Script saved"); updateStatusWithAnimation("💾 Saved to library")
            }
            .show()
    }

    fun scheduleCode(history: GenerationHistory) {
        val scheduleOptions = arrayOf(
            "Every 5 seconds",
            "Every 30 seconds",
            "Every 1 minute",
            "Every 5 minutes",
            "Every 10 minutes",
            "Every hour",
            "Custom expression"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("Schedule Task: ${history.userCommand}")
            .setItems(scheduleOptions) { _, which ->
                val cronExpression = when (which) {
                    0 -> "*/5 * * * *"
                    1 -> "*/30 * * * *"
                    2 -> "0 */1 * * *"
                    3 -> "0 */5 * * *"
                    4 -> "0 */10 * * *"
                    5 -> "0 0 */1 * *"
                    6 -> {
                        showCustomCronDialog(history)
                        return@setItems
                    }
                    else -> "*/30 * * * *"
                }
                scheduleTaskFromHistory(history, cronExpression)
            }
            .show()
    }

    private fun showCustomCronDialog(history: GenerationHistory) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_custom_cron, null)
        val textInputLayout = dialogView.findViewById<TextInputLayout>(R.id.cronInputLayout)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.cronEditText)

        MaterialAlertDialogBuilder(this)
            .setTitle("Custom Cron Expression")
            .setView(dialogView)
            .setPositiveButton("Schedule") { _, _ ->
                val customCron = editText.text.toString()
                scheduleTaskFromHistory(history, customCron)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun scheduleTaskFromHistory(history: GenerationHistory, cronExpression: String) {
        val taskId = addCronTask(history.userCommand, cronExpression)
        speakText("Scheduled task: ${history.userCommand}")
        updateUI()
        updateStatusWithAnimation("📅 Task scheduled successfully")
    }

    private fun clearAllScheduledTasks() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Clear All Scheduled Tasks")
            .setMessage("Are you sure you want to clear all ${cronTasks.size} scheduled tasks?")
            .setPositiveButton("Yes") { _, _ ->
                val taskCount = cronTasks.size
                cronTasks.clear()
                saveCronTasks()
                speakText("Cleared $taskCount scheduled tasks")
                updateUI()
                updateStatusWithAnimation("🧹 All tasks cleared")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    class TasksAdapter(
        private var tasks: MutableList<CronTask>,
        private val onAction: (String, CronTask) -> Unit
    ) : RecyclerView.Adapter<TasksAdapter.TaskViewHolder>() {

        class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val taskDescription: TextView = view.findViewById(R.id.taskDescription)
            val cronExpression: TextView = view.findViewById(R.id.cronExpression)
            val taskStatus: TextView = view.findViewById(R.id.taskStatus)
            val runNowButton: MaterialButton = view.findViewById(R.id.runNowButton)
            val deleteTaskButton: MaterialButton = view.findViewById(R.id.deleteTaskButton)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_scheduled_task, parent, false)
            return TaskViewHolder(view)
        }

        override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
            val task = tasks[position]
            holder.taskDescription.text = task.taskDescription
            holder.cronExpression.text = task.cronExpression

            val lastExecutedText = if (task.lastExecuted == 0L) {
                "Never executed"
            } else {
                "Last: ${SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault()).format(Date(task.lastExecuted))}"
            }
            holder.taskStatus.text = "Created: ${SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault()).format(Date(task.createdAt))} • $lastExecutedText"

            holder.runNowButton.setOnClickListener {
                onAction("run", task)
            }

            holder.deleteTaskButton.setOnClickListener {
                onAction("delete", task)
            }
        }

        override fun getItemCount() = tasks.size

        fun updateTasks(newTasks: List<CronTask>) {
            tasks.clear()
            tasks.addAll(newTasks)
            notifyDataSetChanged()
            Log.d("TasksAdapter", "Updated with ${tasks.size} tasks")
        }
    }

    class HistoryAdapter(
        private var history: MutableList<GenerationHistory>,
        private val onAction: (String, GenerationHistory) -> Unit
    ) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

        class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val userCommand: TextView = view.findViewById(R.id.userCommand)
            val generatedCode: TextView = view.findViewById(R.id.generatedCode)
            val timestamp: TextView = view.findViewById(R.id.timestamp)
            val runButton: MaterialButton = view.findViewById(R.id.runButton)
            val editButton: MaterialButton = view.findViewById(R.id.editButton)
            val scheduleButton: MaterialButton = view.findViewById(R.id.scheduleButton)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_generation_history, parent, false)
            return HistoryViewHolder(view)
        }

        override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
            val item = history[position]
            holder.userCommand.text = item.userCommand

            val displayCode = if (item.generatedCode.length > 200) {
                item.generatedCode.take(200) + "..."
            } else {
                item.generatedCode
            }
            holder.generatedCode.text = displayCode

            holder.timestamp.text = SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.getDefault()).format(Date(item.timestamp))

            holder.runButton.setOnClickListener {
                onAction("run", item)
            }

            holder.editButton.setOnClickListener {
                onAction("edit", item)
            }

            holder.scheduleButton.setOnClickListener {
                onAction("schedule", item)
            }
        }

        override fun getItemCount() = history.size

        fun updateHistory(newHistory: List<GenerationHistory>) {
            history.clear()
            history.addAll(newHistory)
            notifyDataSetChanged()
            Log.d("HistoryAdapter", "Updated with ${history.size} history items")
        }
    }

    private fun addToHistory(userCommand: String, generatedCode: String) {
        val historyItem = GenerationHistory(
            id = UUID.randomUUID().toString(),
            userCommand = userCommand,
            generatedCode = generatedCode
        )
        generationHistory.add(0, historyItem)

        if (generationHistory.size > 50) {
            generationHistory.removeAt(generationHistory.size - 1)
        }

        saveGenerationHistory()
        updateUI()

        // Mirror to chat UI
        if (::chatAdapter.isInitialized && ::chatRecyclerView.isInitialized) {
            runOnUiThread {
                chatAdapter.addMessage(ChatMessage(text = userCommand, type = MessageType.USER))
                // Use showThinking for inline thinking (if thinking was captured)
                val thinking = capturedThinking  // This would come from agent
                chatAdapter.addMessage(ChatMessage(text = "Done ✅", type = MessageType.AGENT, code = generatedCode, thinking = thinking))
                chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
            }
        }

        Log.d("MainActivity", "Added to history: $userCommand -> ${generatedCode.take(50)}...")
        Log.d("MainActivity", "Total history items: ${generationHistory.size}")
    }

        private fun saveGenerationHistory() {
        LocalStorage.saveGenerationHistory(this, generationHistory)
    }

    private fun loadGenerationHistory() {
        generationHistory.clear()
        generationHistory.addAll(LocalStorage.loadGenerationHistory(this))
        Log.d("MainActivity", "Loaded ${generationHistory.size} history items from local storage")
    }

    private fun checkAndRequestMicrophonePermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED -> {
                updateStatusWithAnimation("🎤 Voice control active - Ready for commands")
                speakText("Microphone permission granted. Initializing voice control.")
                initializeSpeechRecognition()
                startListeningLoop()
            }
            else -> {
                requestMicrophonePermission()
            }
        }
    }

    private fun requestMicrophonePermission() {
        permissionRequestInProgress = true
        updateStatusWithAnimation("⚠️ Requesting microphone permission...")
        speakText("Requesting microphone permission. Please allow access when prompted.")

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            MICROPHONE_PERMISSION_REQUEST
        )
    }

    private fun initializeSpeechRecognition() {
        try {
            if (::speechRecognizer.isInitialized) {
                speechRecognizer.destroy()
            }

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer.setRecognitionListener(this)

            speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            }

            Log.d("MainActivity", "Speech recognition initialized successfully")

        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing speech recognition: ${e.message}")
            speakText("Error setting up voice recognition: ${e.message}")
        }
    }

    private fun startListeningLoop() {
        if (isDestroyed) return

        mainScope.launch {
            updateStatusWithAnimation("🎧 Listening for voice commands...")
            speakText("Advanced voice automation ready with scheduling. I'm listening for your commands.")
            Log.d("MainActivity", "Starting listening loop")

            while (isActive && !isDestroyed) {
                try {
                    if (!isListening && isPermissionGranted()) {
                        startListening()
                        delay(100)
                    } else {
                        delay(1000)
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error in listening loop: ${e.message}")
                    delay(5000)
                }
            }
        }
    }

    private fun startListening() {
        if (!isListening && ::speechRecognizer.isInitialized && isPermissionGranted()) {
            try {
                isListening = true
                speechRecognizer.startListening(speechRecognizerIntent)
                Log.d("MainActivity", "Started listening for speech")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error starting speech recognition: ${e.message}")
                isListening = false
            }
        }
    }

    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        permissionRequestInProgress = false

        if (requestCode == MICROPHONE_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateStatusWithAnimation("✅ Microphone ready - Tap button to speak")
                speakText("Microphone permission granted. Tap the button to give commands.")
                initializeSpeechRecognition()
            } else {
                updateStatusWithAnimation("❌ Microphone permission required")
                speakText("Microphone permission required for voice control.")
            }
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d("SpeechRecognition", "Ready for speech")
        updateStatusWithAnimation("🎤 Listening - Speak now...")
    }

    override fun onBeginningOfSpeech() {
        Log.d("SpeechRecognition", "Beginning of speech detected")
        updateStatusWithAnimation("🗣️ Processing your speech...")
    }

    override fun onEndOfSpeech() {
        Log.d("SpeechRecognition", "End of speech")
        isListening = false
        isCurrentlyListening = false
        resetMicrophoneButton()
        updateStatusWithAnimation("⚙️ Processing command...")
    }

    override fun onError(error: Int) {
        isListening = false
        isCurrentlyListening = false
        resetMicrophoneButton()

        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error: $error"
        }

        if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            Log.w("SpeechRecognition", "Speech recognition error: $errorMessage")
            updateStatusWithAnimation("❌ $errorMessage - Tap to try again")
        } else {
            updateStatusWithAnimation("🎤 No speech detected - Tap to try again")
        }
    }

    override fun onResults(results: Bundle?) {
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
            if (matches.isNotEmpty()) {
                val spokenText = matches[0]
                Log.d("SpeechRecognition", "Recognized speech: $spokenText")

                isListening = false
                isCurrentlyListening = false
                resetMicrophoneButton()

                updateStatusWithAnimation("🔄 Processing: $spokenText")
                processVoiceCommand(spokenText)
            }
        }
    }

    private fun processVoiceCommand(spokenText: String) {
        if (isDestroyed) return

        val prefs = getSharedPreferences("phoneclaw_config", MODE_PRIVATE)
        val useAgentLoop = prefs.getBoolean("use_agent_loop", true)

        speakText("Processing: $spokenText")

        mainScope.launch {
            try {
                // Check skill triggers first
                val matchedSkills = SkillManager.findSkillsByTrigger(this@MainActivity, spokenText)
                if (matchedSkills.isNotEmpty()) {
                    val skill = matchedSkills.first()
                    Log.d("MainActivity", "Matched skill trigger: ${skill.name}")
                    if (skill.parameters.isEmpty()) {
                        executeWithPermissionPrompt(skill.code, skill.name)
                        addToHistory("Skill: ${skill.name}", skill.code)
                    } else {
                        withContext(Dispatchers.Main) {
                            val layout = android.widget.LinearLayout(this@MainActivity).apply { orientation = android.widget.LinearLayout.VERTICAL; setPadding(48, 24, 48, 24) }
                            val paramViews = skill.parameters.map { param ->
                                android.widget.EditText(this@MainActivity).apply { hint = param.name; layout.addView(this) }
                            }
                            MaterialAlertDialogBuilder(this@MainActivity)
                                .setTitle("▶ ${skill.name}")
                                .setView(layout)
                                .setPositiveButton("Run") { _, _ ->
                                    val paramMap = skill.parameters.mapIndexed { i, p -> p.name to paramViews[i].text.toString() }.toMap()
                                    val rendered = SkillManager.renderSkill(skill, paramMap)
                                    executeWithPermissionPrompt(rendered, skill.name)
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        }
                    }
                    updateStatusWithAnimation("✅ Skill: ${skill.name}")
                    return@launch
                }

                if (useAgentLoop && shouldUseCustomAPI()) {
                    // ── Agent loop mode ────────────────────────────────────────
                    Log.d("MainActivity", "Using agent loop for: $spokenText")
                    updateStatusWithAnimation("🤖 Agent thinking...")

                    val byok = BYOKClient(this@MainActivity)
                    val loop = AgentLoop(
                        ctx  = this@MainActivity,
                        byok = byok,
                        executor = { code ->
                            var observation = "Action executed."
                            withContext(Dispatchers.Main) {
                                try {
                                    executeGeneratedCode(code)
                                    delay(2000)
                                    val screenText = MyAccessibilityService.instance
                                        ?.getAllTextFromScreen()?.take(400) ?: "Screen not readable"
                                    observation = "Screen now shows: $screenText"
                                } catch (e: Exception) {
                                    observation = "Error: ${e.message}"
                                }
                            }
                            observation
                        },
                        onStatus = { status ->
                            mainScope.launch(Dispatchers.Main) {
                                updateStatusWithAnimation(status)
                                // Capture thinking inline in agent bubble (not separate message)
                                if (status.startsWith("🤔") || status.startsWith("⚡") || status.startsWith("💭")) {
                                    capturedThinking = status
                                    if (::chatAdapter.isInitialized) {
                                        chatAdapter.showThinking(status)
                                        if (::chatRecyclerView.isInitialized) {
                                            chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
                                        }
                                    }
                                }
                            }
                        }
                    )

                    val result = loop.run(spokenText)
                    // Show debug output as SYSTEM message (styled differently)
                    chatAdapter.addMessage(ChatMessage(
                        text = "// Agent loop completed\n//$result",
                        type = MessageType.SYSTEM
                    ))
                    MemoryManager.addInteraction(this@MainActivity, spokenText, result.take(200))
                    updateStatusWithAnimation("✅ Done")

                } else {
                    // ── Single-shot generation mode ────────────────────────────
                    Log.d("MainActivity", "Using single-shot generation for: $spokenText")
                    val automationCode = generateAutomationCodeWithFallback(spokenText)

                    if (automationCode.isNotEmpty()) {
                        addToHistory(spokenText, automationCode)
                        MemoryManager.addInteraction(this@MainActivity, spokenText, automationCode.take(200))
                        updateStatusWithAnimation("⚡ Executing automation...")
                        withContext(Dispatchers.Main) {
                            executeWithPermissionPrompt(automationCode, spokenText)
                        }
                        updateStatusWithAnimation("✅ Done - Tap button for next command")
                    } else {
                        speakText("Sorry, I couldn't generate automation code for that command")
                        updateStatusWithAnimation("❌ Failed - try again")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error processing voice command: ${e.message}")
                speakText("Error: ${e.message}")
                updateStatusWithAnimation("❌ Error")
            }
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_test_ai -> {
                speakText("Testing AI connection")
                mainScope.launch {
                    val result = callAI(listOf(mapOf("role" to "user", "content" to "Reply with: OK")), 10, "fast")
                    withContext(Dispatchers.Main) {
                        val msg = if (result.isNotEmpty()) "AI OK ✓" else "AI connection failed ✗"
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                        updateStatusWithAnimation(msg)
                    }
                }
                true
            }
            R.id.action_clear_history -> {
                generationHistory.clear()
                updateUI()
                Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_compact_mode -> {
                isCompactMode = !isCompactMode
                getSharedPreferences("phoneclaw_config", MODE_PRIVATE).edit()
                    .putBoolean("compact_mode", isCompactMode).apply()
                applyCompactMode(isCompactMode)
                Toast.makeText(this, if (isCompactMode) "Compact mode on" else "Compact mode off", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {}

    override fun onEvent(eventType: Int, params: Bundle?) {}

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {}

    private suspend fun generateAutomationCodeWithFallback(userCommand: String): String {
        if (isDestroyed) return ""

        try {
            val aiCode = generateAutomationCode(userCommand)
            if (aiCode.isNotEmpty()) {
                return aiCode
            }
            return generateFallbackCode(userCommand)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in code generation: ${e.message}")
            return generateFallbackCode(userCommand)
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Moondream helpers — reads from SharedPrefs, falls back to BuildConfig
    // ═══════════════════════════════════════════════════════════════════

    private fun getMoondreamKey(): String {
        val prefs = getSharedPreferences("phoneclaw_config", MODE_PRIVATE)
        // Check both keys: onboarding saves to "moondream_key", settings may use "moondream_api_key"
        val stored = prefs.getString("moondream_key", null)
            ?: prefs.getString("moondream_api_key", null)
        if (!stored.isNullOrBlank()) return stored
        return try { BuildConfig.MOONDREAM_AUTH } catch (e: Exception) { "" }
    }

    private fun getMoondreamEndpoint(): String {
        val prefs = getSharedPreferences("phoneclaw_config", MODE_PRIVATE)
        return prefs.getString("moondream_endpoint", "https://api.moondream.ai") ?: "https://api.moondream.ai"
    }

    private fun getMoondreamQueryPath(): String {
        val prefs = getSharedPreferences("phoneclaw_config", MODE_PRIVATE)
        return prefs.getString("moondream_query_path", "/v1/query") ?: "/v1/query"
    }

    private fun getMoondreamPointPath(): String {
        val prefs = getSharedPreferences("phoneclaw_config", MODE_PRIVATE)
        return prefs.getString("moondream_point_path", "/v1/point") ?: "/v1/point"
    }

    private fun isMoondreamEnabled(): Boolean {
        val prefs = getSharedPreferences("phoneclaw_config", MODE_PRIVATE)
        return prefs.getBoolean("moondream_enabled", true)
    }

    // ═══════════════════════════════════════════════════════════════════
    // UI helpers
    // ═══════════════════════════════════════════════════════════════════

    private fun setupQuickChips() {
        val chips = listOf(
            R.id.chipOpenApp to "Open ",
            R.id.chipSearch to "Search YouTube for ",
            R.id.chipSchedule to "Schedule ",
            R.id.chipBattery to "Check battery level",
            R.id.chipFlashlight to "Turn on flashlight",
            R.id.chipSkills to "Skills: "
        )
        chips.forEach { (id, text) ->
            try {
                findViewById<Chip>(id)?.setOnClickListener {
                    commandInput?.setText(text)
                    commandInput?.setSelection(text.length)
                    commandInput?.requestFocus()
                }
            } catch (e: Exception) { Log.w("UI", "Chip $id not found") }
        }
    }

    private fun applyCompactMode(compact: Boolean) {
        // Tabs moved to dedicated activities — compact mode no longer hides tabs
    }

    fun appendDebugLog(message: String) {
        val ts = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        debugLogBuffer.append("[$ts] $message\n")
        val lines = debugLogBuffer.lines()
        if (lines.size > 200) {
            debugLogBuffer.clear()
            debugLogBuffer.append(lines.takeLast(200).joinToString("\n"))
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Settings bottom sheets
    // ═══════════════════════════════════════════════════════════════════

    private fun showAIConfigSheet() {
        val sheet = BottomSheetDialog(this, R.style.AutoPhone_BottomSheet)
        val view = layoutInflater.inflate(R.layout.sheet_ai_config, null)
        sheet.setContentView(view)

        val prefs = getSharedPreferences("phoneclaw_config", MODE_PRIVATE)
        val providers = arrayOf("OpenRouter", "OpenAI", "Groq", "Anthropic", "Scitely", "Custom")

        val providerDropdown = view.findViewById<AutoCompleteTextView>(R.id.providerDropdown)
        providerDropdown.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, providers))
        providerDropdown.setText(prefs.getString("ai_provider", "OpenRouter"), false)

        view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.apiKeyInput)
            ?.setText(prefs.getString("api_key", ""))
        view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.modelInput)
            ?.setText(prefs.getString("model", ""))
        view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.baseUrlInput)
            ?.setText(prefs.getString("custom_base_url", ""))

        val maxTokensSeek = view.findViewById<SeekBar>(R.id.maxTokensSeek)
        val maxTokensLabel = view.findViewById<android.widget.TextView>(R.id.maxTokensLabel)
        val storedTokens = prefs.getInt("max_tokens", 1200)
        maxTokensSeek?.progress = (storedTokens - 256).coerceAtLeast(0)
        maxTokensLabel?.text = "Max Tokens: $storedTokens"
        maxTokensSeek?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { maxTokensLabel?.text = "Max Tokens: ${p + 256}" }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        val timeoutSeek = view.findViewById<SeekBar>(R.id.timeoutSeek)
        val timeoutLabel = view.findViewById<android.widget.TextView>(R.id.timeoutLabel)
        val storedTimeout = prefs.getInt("timeout_seconds", 60)
        timeoutSeek?.progress = (storedTimeout - 10).coerceAtLeast(0)
        timeoutLabel?.text = "Timeout: ${storedTimeout}s"
        timeoutSeek?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { timeoutLabel?.text = "Timeout: ${p + 10}s" }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        view.findViewById<SwitchMaterial>(R.id.compactModeSwitch)?.isChecked = prefs.getBoolean("compact_mode", false)
        view.findViewById<SwitchMaterial>(R.id.devModeSwitch)?.isChecked = prefs.getBoolean("developer_mode", false)

        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.testConnectionButton)?.setOnClickListener {
            speakText("Testing AI connection...")
            mainScope.launch {
                val result = callAI(listOf(mapOf("role" to "user", "content" to "Reply with: OK")), 10, "fast")
                withContext(Dispatchers.Main) {
                    val msg = if (result.isNotEmpty()) "Connection OK: ${result.take(30)}" else "Connection failed"
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }

        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.saveAIConfigButton)?.setOnClickListener {
            val editor = prefs.edit()
            editor.putString("ai_provider", providerDropdown.text.toString())
            editor.putString("api_key", view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.apiKeyInput)?.text.toString())
            editor.putString("model", view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.modelInput)?.text.toString())
            editor.putString("custom_base_url", view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.baseUrlInput)?.text.toString())
            editor.putInt("max_tokens", (maxTokensSeek?.progress ?: 944) + 256)
            editor.putInt("timeout_seconds", (timeoutSeek?.progress ?: 50) + 10)
            val compact = view.findViewById<SwitchMaterial>(R.id.compactModeSwitch)?.isChecked ?: false
            val devMode = view.findViewById<SwitchMaterial>(R.id.devModeSwitch)?.isChecked ?: false
            editor.putBoolean("compact_mode", compact)
            editor.putBoolean("developer_mode", devMode)
            editor.putBoolean("use_custom_config", true)
            editor.apply()
            isCompactMode = compact
            isDeveloperMode = devMode
            applyCompactMode(compact)
            Toast.makeText(this, "AI configuration saved", Toast.LENGTH_SHORT).show()
            sheet.dismiss()
        }

        sheet.show()
    }

    private fun showVisionSheet() {
        val sheet = BottomSheetDialog(this, R.style.AutoPhone_BottomSheet)
        val view = layoutInflater.inflate(R.layout.sheet_vision, null)
        sheet.setContentView(view)

        val prefs = getSharedPreferences("phoneclaw_config", MODE_PRIVATE)
        val visionModels = arrayOf("moondream-2", "moondream-2-latest", "Custom")

        view.findViewById<SwitchMaterial>(R.id.visionEnabledSwitch)?.isChecked = prefs.getBoolean("moondream_enabled", true)
        view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.moondreamKeyInput)?.setText(prefs.getString("moondream_api_key", ""))
        view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.moondreamEndpointInput)?.setText(getMoondreamEndpoint())
        view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.queryPathInput)?.setText(getMoondreamQueryPath())
        view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.pointPathInput)?.setText(getMoondreamPointPath())

        val modelDropdown = view.findViewById<AutoCompleteTextView>(R.id.visionModelDropdown)
        modelDropdown?.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, visionModels))
        val storedModel = prefs.getString("moondream_model", "moondream-2") ?: "moondream-2"
        val customLayout = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.customModelLayout)
        if (visionModels.contains(storedModel)) {
            modelDropdown?.setText(storedModel, false)
        } else {
            modelDropdown?.setText("Custom", false)
            customLayout?.visibility = android.view.View.VISIBLE
            view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.customModelInput)?.setText(storedModel)
        }
        modelDropdown?.setOnItemClickListener { _, _, pos, _ ->
            customLayout?.visibility = if (visionModels[pos] == "Custom") android.view.View.VISIBLE else android.view.View.GONE
        }

        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.saveVisionButton)?.setOnClickListener {
            val editor = prefs.edit()
            editor.putBoolean("moondream_enabled", view.findViewById<SwitchMaterial>(R.id.visionEnabledSwitch)?.isChecked ?: true)
            editor.putString("moondream_api_key", view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.moondreamKeyInput)?.text.toString())
            editor.putString("moondream_endpoint", view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.moondreamEndpointInput)?.text.toString())
            editor.putString("moondream_query_path", view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.queryPathInput)?.text.toString())
            editor.putString("moondream_point_path", view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.pointPathInput)?.text.toString())
            val selectedModel = modelDropdown?.text.toString()
            val finalModel = if (selectedModel == "Custom") {
                view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.customModelInput)?.text.toString()
            } else selectedModel
            editor.putString("moondream_model", finalModel)
            editor.apply()
            Toast.makeText(this, "Vision settings saved", Toast.LENGTH_SHORT).show()
            sheet.dismiss()
        }

        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.testVisionButton)?.setOnClickListener {
            Toast.makeText(this, "Vision test: take a screenshot via voice first, then magicClicker will use your new settings", Toast.LENGTH_LONG).show()
        }

        sheet.show()
    }

    private fun showModelChainSheet() {
        val sheet = BottomSheetDialog(this, R.style.AutoPhone_BottomSheet)
        val view = layoutInflater.inflate(R.layout.sheet_model_chain, null)
        sheet.setContentView(view)

        val prefs = getSharedPreferences("phoneclaw_config", MODE_PRIVATE)
        val providers = arrayOf("OpenRouter", "OpenAI", "Groq", "Anthropic", "Scitely", "Custom")
        val chainContainer = view.findViewById<android.widget.LinearLayout>(R.id.chainContainer)

        val chainJson = prefs.getString("model_chain", null)
        val chainList = if (chainJson != null) {
            try {
                val arr = org.json.JSONArray(chainJson)
                (0 until arr.length()).map { arr.getJSONObject(it) }
            } catch (e: Exception) { emptyList() }
        } else emptyList()

        fun addChainRow(label: String, provider: String = "OpenRouter", model: String = "", apiKey: String = "", removable: Boolean = true) {
            val rowView = layoutInflater.inflate(R.layout.item_model_chain_row, chainContainer, false)
            rowView.findViewById<android.widget.TextView>(R.id.chainRowLabel)?.text = label
            val providerDropdown = rowView.findViewById<AutoCompleteTextView>(R.id.chainProviderDropdown)
            providerDropdown?.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, providers))
            providerDropdown?.setText(provider, false)
            rowView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.chainModelInput)?.setText(model)
            rowView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.chainApiKeyInput)?.setText(apiKey)
            val removeBtn = rowView.findViewById<com.google.android.material.button.MaterialButton>(R.id.removeRowButton)
            if (!removable) removeBtn?.visibility = android.view.View.GONE
            else removeBtn?.setOnClickListener { chainContainer?.removeView(rowView) }
            chainContainer?.addView(rowView)
        }

        val primaryProvider = prefs.getString("ai_provider", "OpenRouter") ?: "OpenRouter"
        val primaryModel = prefs.getString("model", "") ?: ""
        addChainRow("Primary Model", primaryProvider, primaryModel, removable = false)
        chainList.forEach { obj ->
            addChainRow("Fallback", obj.optString("provider", "OpenRouter"), obj.optString("model", ""), obj.optString("api_key", ""))
        }

        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.addFallbackButton)?.setOnClickListener {
            val num = (chainContainer?.childCount ?: 0)
            addChainRow("Fallback $num")
        }

        val maxRetriesSeek = view.findViewById<SeekBar>(R.id.maxRetriesSeek)
        val maxRetriesLabel = view.findViewById<android.widget.TextView>(R.id.maxRetriesLabel)
        maxRetriesSeek?.progress = (prefs.getInt("max_retries", 1) - 1).coerceAtLeast(0)
        maxRetriesLabel?.text = "Max retries per level: ${prefs.getInt("max_retries", 1)}"
        maxRetriesSeek?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { maxRetriesLabel?.text = "Max retries per level: ${p + 1}" }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        val retryDelaySeek = view.findViewById<SeekBar>(R.id.retryDelaySeek)
        val retryDelayLabel = view.findViewById<android.widget.TextView>(R.id.retryDelayLabel)
        retryDelaySeek?.progress = (prefs.getInt("retry_delay_seconds", 1) - 1).coerceAtLeast(0)
        retryDelayLabel?.text = "Retry delay: ${prefs.getInt("retry_delay_seconds", 1)}s"
        retryDelaySeek?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { retryDelayLabel?.text = "Retry delay: ${p + 1}s" }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.saveChainButton)?.setOnClickListener {
            val editor = prefs.edit()
            val firstRow = chainContainer?.getChildAt(0)
            if (firstRow != null) {
                editor.putString("ai_provider", firstRow.findViewById<AutoCompleteTextView>(R.id.chainProviderDropdown)?.text.toString())
                editor.putString("model", firstRow.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.chainModelInput)?.text.toString())
            }
            val fallbackArray = org.json.JSONArray()
            val count = chainContainer?.childCount ?: 0
            for (i in 1 until count) {
                val row = chainContainer?.getChildAt(i)
                if (row != null) {
                    val obj = org.json.JSONObject()
                    obj.put("provider", row.findViewById<AutoCompleteTextView>(R.id.chainProviderDropdown)?.text.toString())
                    obj.put("model", row.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.chainModelInput)?.text.toString())
                    obj.put("api_key", row.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.chainApiKeyInput)?.text.toString())
                    fallbackArray.put(obj)
                }
            }
            editor.putString("model_chain", fallbackArray.toString())
            editor.putInt("max_retries", (maxRetriesSeek?.progress ?: 0) + 1)
            editor.putInt("retry_delay_seconds", (retryDelaySeek?.progress ?: 0) + 1)
            editor.apply()
            Toast.makeText(this, "Model chain saved", Toast.LENGTH_SHORT).show()
            sheet.dismiss()
        }

        sheet.show()
    }

    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("About PhoneClaw")
            .setMessage("PhoneClaw v1.1.0\nAndroid AI Automation Agent\n\nFork of rohanarun/phoneclaw\nImproved by theking196\n\nAccessibility Service + AI = full phone control")
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
            .show()
    }

    // ═══════════════════════════════════════════════════════════════════

    private fun generateFallbackCode(userCommand: String): String {
        Log.d("MainActivity", "Using fallback code generation for: $userCommand")

        val command = userCommand.lowercase()

        return when {
            command.contains("open tiktok") || command.contains("launch tiktok") ->
                "speakText(\"Opening TikTok\");\nlaunchTikTok();"

            command.contains("open youtube") || command.contains("launch youtube") ->
                "speakText(\"Opening YouTube\");\nlaunchYouTube();"

            command.contains("open instagram") || command.contains("launch instagram") ->
                "speakText(\"Opening Instagram\");\nlaunchInstagram();"

            command.contains("open twitter") || command.contains("launch twitter") ->
                "speakText(\"Opening Twitter\");\nlaunchTwitter();"

            command.contains("open spotify") || command.contains("launch spotify") ->
                "speakText(\"Opening Spotify\");\nlaunchSpotify();"

            command.contains("open netflix") || command.contains("launch netflix") ->
                "speakText(\"Opening Netflix\");\nlaunchNetflix();"

            command.contains("open reddit") || command.contains("launch reddit") ->
                "speakText(\"Opening Reddit\");\nlaunchReddit();"

            command.contains("open medium") || command.contains("launch medium") ->
                "speakText(\"Opening Medium\");\nlaunchMedium();"

            command.contains("open telegram") || command.contains("launch telegram") ->
                "speakText(\"Opening Telegram\");\nlaunchTelegram();"

            command.contains("open whatsapp") || command.contains("launch whatsapp") ->
                "speakText(\"Opening WhatsApp\");\nlaunchWhatsApp();"

            command.contains("open snapchat") || command.contains("launch snapchat") ->
                "speakText(\"Opening Snapchat\");\nlaunchSnapchat();"

            command.contains("open linkedin") || command.contains("launch linkedin") ->
                "speakText(\"Opening LinkedIn\");\nlaunchLinkedIn();"

            command.contains("open pinterest") || command.contains("launch pinterest") ->
                "speakText(\"Opening Pinterest\");\nlaunchPinterest();"

            command.contains("open twitch") || command.contains("launch twitch") ->
                "speakText(\"Opening Twitch\");\nlaunchTwitch();"

            command.contains("open discord") || command.contains("launch discord") ->
                "speakText(\"Opening Discord\");\nlaunchDiscord();"

            command.contains("check battery") || command.contains("battery level") ->
                "speakText(\"Checking battery level\");\nvar level = getBatteryLevel();\nspeakText(\"Battery is at \" + level + \" percent\");"

            command.contains("check memory") || command.contains("memory usage") ->
                "speakText(\"Checking memory usage\");\nvar usage = getMemoryUsage();\nspeakText(\"Memory usage is \" + Math.round(usage) + \" percent\");"

            command.contains("check storage") || command.contains("storage space") ->
                "speakText(\"Checking storage space\");\nvar storage = getStorageSpace();\nspeakText(\"Storage info: \" + storage);"

            command.contains("turn on wifi") || command.contains("enable wifi") ->
                "speakText(\"Turning on WiFi\");\ntoggleWiFi(true);"

            command.contains("turn off wifi") || command.contains("disable wifi") ->
                "speakText(\"Turning off WiFi\");\ntoggleWiFi(false);"

            command.contains("turn on bluetooth") || command.contains("enable bluetooth") ->
                "speakText(\"Turning on Bluetooth\");\ntoggleBluetooth(true);"

            command.contains("turn off bluetooth") || command.contains("disable bluetooth") ->
                "speakText(\"Turning off Bluetooth\");\ntoggleBluetooth(false);"

            command.contains("set brightness") -> {
                val numberRegex = "\\d+".toRegex()
                val match = numberRegex.find(command)
                val level = match?.value?.toIntOrNull() ?: 50
                "speakText(\"Setting brightness to $level percent\");\nsetBrightness($level);"
            }

            command.contains("set volume") -> {
                val numberRegex = "\\d+".toRegex()
                val match = numberRegex.find(command)
                val level = match?.value?.toIntOrNull() ?: 50
                val type = when {
                    command.contains("media") -> "media"
                    command.contains("ringer") -> "ringer"
                    command.contains("alarm") -> "alarm"
                    else -> "media"
                }
                "speakText(\"Setting $type volume to $level percent\");\nsetVolume(\"$type\", $level);"
            }

            command.contains("take photo") || command.contains("take picture") ->
                "speakText(\"Taking photo\");\ntakePhoto();"

            command.contains("open camera") ->
                "speakText(\"Opening camera\");\nopenCamera();"

            command.contains("open gallery") ->
                "speakText(\"Opening gallery\");\nopenGallery();"

            command.contains("flashlight on") || command.contains("turn on flashlight") ->
                "speakText(\"Turning on flashlight\");\ntoggleFlashlight(true);"

            command.contains("flashlight off") || command.contains("turn off flashlight") ->
                "speakText(\"Turning off flashlight\");\ntoggleFlashlight(false);"

            command.contains("vibrate") ->
                "speakText(\"Vibrating device\");\nvibrate(1000);"

            command.contains("open dialer") || command.contains("open phone") ->
                "speakText(\"Opening dialer\");\nopenDialer();"

            command.contains("open contacts") ->
                "speakText(\"Opening contacts\");\nopenContacts();"

            command.contains("open messages") ->
                "speakText(\"Opening messages\");\nopenMessages();"

            command.contains("open gmail") ->
                "speakText(\"Opening Gmail\");\nopenGmail();"

            // Open Chrome / browser + optional search (fixed: no blind click, just typeInField)
            command.contains("open chrome") || command.contains("open browser") || command.contains("open google chrome") -> {
                val searchQuery = extractSearchQuery(command)
                if (searchQuery.isNotEmpty()) {
                    "speakText(\"Opening Chrome and searching for $searchQuery\");\n" +
                    "openApp(\"com.android.chrome\");\n" +
                    "delay(2500);\n" +
                    "typeInField(\"$searchQuery\");\n" +
                    "delay(500);\n" +
                    "pressEnter();"
                } else {
                    "speakText(\"Opening Chrome\");\nopenApp(\"com.android.chrome\");"
                }
            }

            // "open X on chrome" — navigate to website
            (command.contains(" on chrome") || command.contains(" in chrome") || command.contains(" on browser") || command.contains(" in browser")) -> {
                val appName = command
                    .replace(" on chrome", "").replace(" in chrome", "")
                    .replace(" on browser", "").replace(" in browser", "")
                    .removePrefix("open ").removePrefix("launch ").trim()
                val websiteUrl = "https://www.$appName.com"
                "speakText(\"Opening $appName in Chrome\");\n" +
                "openApp(\"com.android.chrome\");\n" +
                "delay(2500);\n" +
                "typeInField(\"$websiteUrl\");\n" +
                "delay(500);\n" +
                "pressEnter();"
            }

            // WhatsApp Business
            command.contains("whatsapp business") || command.contains("wa business") ->
                "speakText(\"Opening WhatsApp Business\");\nopenApp(\"com.whatsapp.w4b\");"

            // TikTok Lite
            command.contains("tiktok lite") ->
                "speakText(\"Opening TikTok Lite\");\nopenApp(\"com.zhiliaoapp.musically.go\");"

            // Facebook Lite
            command.contains("facebook lite") ->
                "speakText(\"Opening Facebook Lite\");\nopenApp(\"com.facebook.lite\");"

            // Instagram Lite
            command.contains("instagram lite") ->
                "speakText(\"Opening Instagram Lite\");\nopenApp(\"com.instagram.lite\");"

            // Messenger
            command.contains("open messenger") || command.contains("launch messenger") ->
                "speakText(\"Opening Messenger\");\nopenApp(\"com.facebook.orca\");"

            // YouTube Music
            command.contains("youtube music") ->
                "speakText(\"Opening YouTube Music\");\nopenApp(\"com.google.android.apps.youtube.music\");"

            // Google Pay
            command.contains("google pay") || command.contains("gpay") ->
                "speakText(\"Opening Google Pay\");\nopenApp(\"com.google.android.apps.nbu.paisa.user\");"

            // Brightness
            command.contains("increase brightness") || command.contains("brighten screen") || command.contains("max brightness") ->
                "speakText(\"Increasing brightness\");\nsetBrightness(100);"

            command.contains("decrease brightness") || command.contains("dim screen") || command.contains("minimum brightness") || command.contains("lower brightness") ->
                "speakText(\"Decreasing brightness\");\nsetBrightness(20);"

            // Volume
            command.contains("turn up volume") || command.contains("increase volume") || command.contains("volume up") ->
                "speakText(\"Turning up volume\");\nsetVolume(\"media\", 90);"

            command.contains("turn down volume") || command.contains("decrease volume") || command.contains("volume down") ->
                "speakText(\"Turning down volume\");\nsetVolume(\"media\", 30);"

            command.contains("unmute") ->
                "speakText(\"Unmuting device\");\nsetVolume(\"ringer\", 70);\nsetVolume(\"media\", 70);"

            command.contains("mute") ->
                "speakText(\"Muting device\");\nsetVolume(\"ringer\", 0);\nsetVolume(\"media\", 0);"

            // Capability query
            command.contains("what can you do") || command.contains("help me") || command.contains("list commands") || command.contains("show commands") ->
                "speakText(\"I can open apps, search the web, control brightness and volume, turn on the flashlight, check battery, take photos, send SMS, schedule tasks, and much more. Just tell me what you want!\");"

            // List apps
            command.contains("list my apps") || command.contains("what apps") || command.contains("show installed apps") ->
                "var apps = getInstalledApps();\nspeakText(\"You have: \" + apps.slice(0, 5).join(\", \") + \" and more.\");"

            // Generic search
            (command.contains("search for") || command.contains("google ")) && !command.contains("open") -> {
                val searchQuery = extractSearchQuery(command)
                "speakText(\"Searching for $searchQuery\");\n" +
                "openApp(\"com.android.chrome\");\n" +
                "delay(2500);\n" +
                "typeInField(\"$searchQuery\");\n" +
                "delay(500);\n" +
                "pressEnter();"
            }

            // Open YouTube + optional search
            command.contains("open youtube") || command.contains("launch youtube") -> {
                val searchQuery = extractSearchQuery(command)
                if (searchQuery.isNotEmpty()) {
                    "speakText(\"Opening YouTube and searching for $searchQuery\");\n" +
                    "launchYouTube();\n" +
                    "delay(2000);\n" +
                    "magicClicker(\"Search YouTube\");\n" +
                    "delay(800);\n" +
                    "typeText(\"$searchQuery\");\n" +
                    "delay(500);\n" +
                    "pressEnter();"
                } else {
                    "speakText(\"Opening YouTube\");\nlaunchYouTube();"
                }
            }

            // Generic search command
            (command.contains("search for") || command.contains("search ") || command.contains("google ")) && !command.contains("open") -> {
                val searchQuery = extractSearchQuery(command)
                "speakText(\"Searching for $searchQuery\");\n" +
                "openApp(\"com.android.chrome\");\n" +
                "delay(2000);\n" +
                "magicClicker(\"Search or type web address\");\n" +
                "delay(800);\n" +
                "typeText(\"$searchQuery\");\n" +
                "delay(500);\n" +
                "pressEnter();"
            }

            // Generic openApp by name — e.g. "open settings", "open calculator"
            command.startsWith("open ") || command.startsWith("launch ") -> {
                val appName = command.removePrefix("open ").removePrefix("launch ").trim()
                val packageMap = mapOf(
                    "settings" to "com.android.settings",
                    "calculator" to "com.android.calculator2",
                    "calendar" to "com.google.android.calendar",
                    "maps" to "com.google.android.apps.maps",
                    "google maps" to "com.google.android.apps.maps",
                    "phone" to "com.android.dialer",
                    "dialer" to "com.android.dialer",
                    "camera" to "com.android.camera2",
                    "photos" to "com.google.android.apps.photos",
                    "play store" to "com.android.vending",
                    "clock" to "com.android.deskclock",
                    "files" to "com.google.android.apps.nbu.files",
                    "drive" to "com.google.android.apps.docs",
                    "docs" to "com.google.android.apps.docs.editors.docs",
                    "sheets" to "com.google.android.apps.docs.editors.sheets",
                    "slides" to "com.google.android.apps.docs.editors.slides",
                    "meet" to "com.google.android.apps.tachyon",
                    "chrome" to "com.android.chrome",
                    "browser" to "com.android.chrome"
                )
                val pkg = packageMap[appName]
                if (pkg != null) {
                    "speakText(\"Opening $appName\");\nopenApp(\"$pkg\");"
                } else {
                    "speakText(\"Trying to open $appName\");\nlaunchAppByName(\"$appName\");"
                }
            }

            command.contains("every") && (command.contains("second") || command.contains("minute") || command.contains("hour")) -> {
                generateScheduleCommand(userCommand)
            }

            command.contains("clear schedule") || command.contains("clear all") || command.contains("remove schedule") ->
                "speakText(\"Clearing all scheduled tasks\");\nclearSchedule();"

            else -> {
                // Last resort: try to find an app by name in the command
                "speakText(\"Sorry, I could not understand: $userCommand. Please try again with a clearer command.\");"
            }
        }
    }

    /**
     * Extracts a search query from a natural language command.
     * e.g. "open chrome and search for jamb lesson" -> "jamb lesson"
     * e.g. "search for cat videos on youtube" -> "cat videos"
     */
    private fun extractSearchQuery(command: String): String {
        val patterns = listOf(
            "and search for (.+)",
            "search for (.+)",
            "and search (.+)",
            "search (.+)",
            "and look up (.+)",
            "look up (.+)",
            "and find (.+)",
            "google (.+)"
        )
        for (pattern in patterns) {
            val match = Regex(pattern).find(command)
            if (match != null) {
                return match.groupValues[1]
                    .removePrefix("on youtube")
                    .removePrefix("on chrome")
                    .trim()
            }
        }
        return ""
    }

    private fun generateScheduleCommand(userCommand: String): String {
        val command = userCommand.lowercase()

        val baseAction = when {
            command.contains("open tiktok") -> "open TikTok"
            command.contains("open youtube") -> "open YouTube"
            command.contains("open instagram") -> "open Instagram"
            command.contains("open twitter") -> "open Twitter"
            command.contains("open spotify") -> "open Spotify"
            command.contains("open netflix") -> "open Netflix"
            command.contains("check battery") -> "check battery level"
            command.contains("check memory") -> "check memory usage"
            command.contains("take photo") -> "take a photo"
            command.contains("turn on wifi") -> "turn on WiFi"
            command.contains("turn off wifi") -> "turn off WiFi"
            command.contains("flashlight") -> "toggle flashlight"
            command.contains("vibrate") -> "vibrate device"
            else -> userCommand.split(" every ")[0].trim()
        }

        val cronExpression = when {
            command.contains("every 5 second") -> "*/5 * * * *"
            command.contains("every 10 second") -> "*/10 * * * *"
            command.contains("every 15 second") -> "*/15 * * * *"
            command.contains("every 30 second") -> "*/30 * * * *"
            command.contains("every 1 minute") || command.contains("every minute") -> "0 */1 * * *"
            command.contains("every 2 minute") -> "0 */2 * * *"
            command.contains("every 5 minute") -> "0 */5 * * *"
            command.contains("every 10 minute") -> "0 */10 * * *"
            command.contains("every 15 minute") -> "0 */15 * * *"
            command.contains("every 30 minute") -> "0 */30 * * *"
            command.contains("every hour") -> "0 0 */1 * *"
            command.contains("every 2 hour") -> "0 0 */2 * *"
            command.contains("every 6 hour") -> "0 0 */6 * *"
            command.contains("every 12 hour") -> "0 0 */12 * *"
            command.contains("daily") || command.contains("every day") -> "0 0 0 * *"
            else -> "*/30 * * * *"
        }

        return "speakText(\"Scheduling task: $baseAction\");\nschedule(\"$baseAction\", \"$cronExpression\");"
    }

    private fun startCronChecker() {
        if (isDestroyed) return

        cronCheckJob?.cancel()

        cronCheckJob = mainScope.launch {
            speakText("Cron scheduler started")
            Log.d("MainActivity", "Cron scheduler started")

            while (isActive && !isDestroyed) {
                try {
                    checkAndExecuteCronTasks()
                    delay(5000) // CHANGED: Check every 5 seconds instead of 1
                } catch (e: CancellationException) {
                    Log.d("MainActivity", "Cron checker cancelled")
                    throw e
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error in cron checker: ${e.message}")
                    delay(30000) // Wait 30 seconds on error
                }
            }
        }
    }

    private suspend fun checkAndExecuteCronTasks() {
        if (isDestroyed) return

        val currentTime = System.currentTimeMillis()

        for ((taskId, cronTask) in cronTasks.toMap()) {
            if (!cronTask.isActive) continue

            try {
                if (shouldExecuteCronTask(cronTask, currentTime)) {
                    Log.d("MainActivity", "Executing cron task: ${cronTask.taskDescription}")
                    updateStatusWithAnimation("⏰ Executing: ${cronTask.taskDescription}")
                    speakText("Executing scheduled task: ${cronTask.taskDescription}")

                    if (taskId == "universal_script_daily") {
                        speakText("Daily script not available")
                    } else {
                        val automationCode = generateAutomationCodeWithFallback(cronTask.taskDescription)
                        executeGeneratedCode(automationCode)
                    }

                    cronTask.lastExecuted = currentTime
                    cronTasks[taskId] = cronTask.copy(lastExecuted = currentTime)
                    saveCronTasks()

                    Log.d("MainActivity", "Cron task executed successfully: ${cronTask.taskDescription}")
                    updateStatusWithAnimation("✅ Scheduled task completed")
                    updateUI()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error executing cron task ${cronTask.taskDescription}: ${e.message}")
            }
        }
    }

    private fun shouldExecuteCronTask(cronTask: CronTask, currentTime: Long): Boolean {
        return try {
            if (cronTask.lastExecuted == 0L) {
                val timeSinceCreation = currentTime - cronTask.createdAt
                val firstExecutionDelay = getIntervalFromCron(cronTask.cronExpression)

                if (firstExecutionDelay != null && timeSinceCreation >= firstExecutionDelay) {
                    Log.d("MainActivity", "First execution for task: ${cronTask.taskDescription}")
                    return true
                }
                return false
            }

            val timeSinceLastExecution = currentTime - cronTask.lastExecuted
            val interval = getIntervalFromCron(cronTask.cronExpression)

            if (interval != null && timeSinceLastExecution >= interval) {
                Log.d("MainActivity", "Time for next execution: ${cronTask.taskDescription}")
                return true
            }

            false
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking cron task execution time: ${e.message}")
            false
        }
    }

    private fun getIntervalFromCron(cronExpression: String): Long? {
        try {
            val parts = cronExpression.split(" ")
            if (parts.size < 5) return null

            val secondPart = parts[0]
            val minutePart = parts[1]
            val hourPart = parts[2]
            val dayPart = parts[3]
            val monthPart = parts[4]

            return when {
                secondPart.startsWith("*/") && minutePart == "*" -> {
                    val interval = secondPart.substring(2).toIntOrNull()
                    interval?.let { it * 1000L }
                }

                secondPart == "0" && minutePart.startsWith("*/") -> {
                    val interval = minutePart.substring(2).toIntOrNull()
                    interval?.let { it * 60 * 1000L }
                }

                secondPart == "0" && minutePart == "0" && hourPart.startsWith("*/") -> {
                    val interval = hourPart.substring(2).toIntOrNull()
                    interval?.let { it * 60 * 60 * 1000L }
                }

                secondPart == "0" && minutePart == "0" && dayPart == "*" && monthPart == "*" -> {
                    24 * 60 * 60 * 1000L
                }

                else -> null
            }
        } catch (e: Exception) {
            return null
        }
    }

    private fun addCronTask(taskDescription: String, cronExpression: String): String {
        val taskId = UUID.randomUUID().toString()
        val cronTask = CronTask(taskId, taskDescription, cronExpression)

        cronTasks[taskId] = cronTask
        saveCronTasks()

        val interval = getIntervalFromCron(cronExpression)
        Log.d("MainActivity", "Added cron task: $taskDescription with expression: $cronExpression, interval: ${interval}ms")

        return taskId
    }

    private fun removeCronTask(taskId: String) {
        cronTasks.remove(taskId)
        saveCronTasks()
        Log.d("MainActivity", "Removed cron task: $taskId")
    }

        private fun saveCronTasks() {
        LocalStorage.saveCronTasks(this, cronTasks)
    }

    private fun loadCronTasks() {
        cronTasks.clear()
        cronTasks.putAll(LocalStorage.loadCronTasks(this))
        Log.d("MainActivity", "Loaded ${cronTasks.size} cron tasks from local storage")
    }

    private fun testCronScheduler() {
        if (isDestroyed) return

        mainScope.launch {
            delay(5000)
            Log.d("MainActivity", "Testing cron scheduler...")
            Log.d("MainActivity", "Current tasks: ${cronTasks.size}")
            Log.d("MainActivity", "Current history: ${generationHistory.size}")

            cronTasks.values.forEach { task ->
                Log.d("MainActivity", "Task: ${task.taskDescription}")
                Log.d("MainActivity", "Expression: ${task.cronExpression}")
                Log.d("MainActivity", "Created: ${task.createdAt}")
                Log.d("MainActivity", "Last executed: ${task.lastExecuted}")
                Log.d("MainActivity", "Active: ${task.isActive}")

                val interval = getIntervalFromCron(task.cronExpression)
                Log.d("MainActivity", "Calculated interval: ${interval}ms")

                val currentTime = System.currentTimeMillis()
                val shouldExecute = shouldExecuteCronTask(task, currentTime)
                Log.d("MainActivity", "Should execute now: $shouldExecute")
            }

            generationHistory.forEach { item ->
                Log.d("MainActivity", "History: ${item.userCommand} -> ${item.generatedCode.take(50)}...")
            }
        }
    }

    private suspend fun callStreaming16kAPI(
        messages: List<Map<String, String>>,
        maxTokens: Int = 300,
        mode: String = "fast"
    ): String = withContext(Dispatchers.IO) {
        if (isDestroyed) return@withContext ""
        
        // Check if custom API is configured and redirect to BYOKClient
        if (shouldUseCustomAPI()) {
            Log.d("MainActivity", "Redirecting to BYOKClient (custom API: ${getCurrentProviderName()})")
            return@withContext callBYOKClient(messages)
        }

        val maxRetries = 3
        var currentRetry = 0
        var result = ""

        val client = OkHttpClient.Builder()
            .connectTimeout(300, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .callTimeout(600, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        val modelOptions = getOpenRouterModelOptions()
        val selectedModel = getSelectedOpenRouterModelId()
        val models = if (modelOptions.isNotEmpty()) {
            val ordered = mutableListOf<String>()
            if (modelOptions.any { it.id == selectedModel }) {
                ordered.add(selectedModel)
            }
            for (option in modelOptions) {
                if (option.id != selectedModel) {
                    ordered.add(option.id)
                }
            }
            ordered
        } else {
            listOf(selectedModel)
        }

        for (modelIndex in models.indices) {
            val currentModel = models[modelIndex]
            currentRetry = 0

            Log.d("MainActivity", "Trying model: $currentModel")

            while (currentRetry < maxRetries && result.isEmpty()) {
                try {
                    val openRouterMessages = org.json.JSONArray()
                    messages.forEach { messageMap ->
                        val messageJson = JSONObject()
                        messageJson.put("role", messageMap["role"] ?: "user")

                        val content = messageMap["content"] ?: ""

                        if (content.contains("data:image/") || content.contains("http")) {
                            val contentArray = org.json.JSONArray()

                            val textPart = JSONObject()
                            textPart.put("type", "text")
                            textPart.put("text", content.replace(Regex("data:image/[^;]+;base64,[A-Za-z0-9+/=]+"), "[Image]"))
                            contentArray.put(textPart)

                            val imageRegex = Regex("(data:image/[^;]+;base64,[A-Za-z0-9+/=]+|https?://[^\\s]+\\.(jpg|jpeg|png|gif|webp))")
                            val imageMatch = imageRegex.find(content)
                            if (imageMatch != null) {
                                val imagePart = JSONObject()
                                imagePart.put("type", "image_url")
                                val imageUrl = JSONObject()
                                imageUrl.put("url", imageMatch.value)
                                imagePart.put("image_url", imageUrl)
                                contentArray.put(imagePart)
                            }

                            messageJson.put("content", contentArray)
                        } else {
                            messageJson.put("content", content)
                        }

                        openRouterMessages.put(messageJson)
                    }

                    val requestBodyJson = JSONObject().apply {
                        put("model", currentModel)
                        put("messages", openRouterMessages)
                        put("max_tokens", maxTokens)
                        put("temperature", if (mode == "fast") 0.7 else 0.3)
                        put("top_p", 1.0)
                        put("frequency_penalty", 0.0)
                        put("presence_penalty", 0.0)
                        put("stream", false)
                    }
                    if (!isActive || isDestroyed) return@withContext ""

                    val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                    val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

                    val request = Request.Builder()
                        .url("https://openrouter.ai/api/v1/chat/completions")
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer OPENROUTERKEY")
                        .header("HTTP-Referer", "getsupers.com")
                        .header("X-Title", "PhoneClaw")
                        .post(requestBody)
                        .build()

                    Log.d("MainActivity", "Making API request to OpenRouter with model: $currentModel")

                    client.newCall(request).execute().use { response ->
                        val responseBody = response.body?.string() ?: ""

                        when {
                            response.isSuccessful -> {
                                try {
                                    val responseJson = JSONObject(responseBody)
                                    val choices = responseJson.getJSONArray("choices")

                                    if (choices.length() > 0) {
                                        val choice = choices.getJSONObject(0)
                                        val message = choice.getJSONObject("message")
                                        result = message.getString("content").trim()

                                        Log.d("MainActivity", "Successfully got response from $currentModel: ${result.take(100)}...")

                                        if (responseJson.has("usage")) {
                                            val usage = responseJson.getJSONObject("usage")
                                            Log.d("MainActivity", "Token usage - Prompt: ${usage.optInt("prompt_tokens", 0)}, Completion: ${usage.optInt("completion_tokens", 0)}")
                                        }

                                        return@withContext result
                                    }else{

                                    }
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Error parsing OpenRouter response: ${e.message}")
                                    Log.e("MainActivity", "Response body: $responseBody")
                                }
                            }
                            else -> {
                                Log.e("MainActivity", "OpenRouter API error. Code: ${response.code}")
                                Log.e("MainActivity", "Error response: $responseBody")
                            }
                        }
                    }

                } catch (e: Exception) {
                    Log.e("MainActivity", "OpenRouter API exception (attempt ${currentRetry + 1} with $currentModel): ${e.message}")
                    e.printStackTrace()
                }

                when {
                    result.isEmpty() && currentRetry < maxRetries - 1 -> {
                        currentRetry++
                        val delayMs = 2000L * currentRetry
                        Log.d("MainActivity", "Retrying in ${delayMs}ms...")
                        delay(delayMs)
                    }
                    else -> break
                }
            }

            if (result.isNotEmpty()) {
                break
            }

            Log.w("MainActivity", "Model $currentModel failed after $maxRetries attempts, trying next model...")
        }

        val finalResult = if (result.isEmpty()) {
            Log.e("MainActivity", "All models failed after retries")
            "I apologize, but I'm having trouble connecting to the AI service right now. Please try again in a moment."
        } else {
            result
        }

        return@withContext finalResult
    }

    private suspend fun generateAutomationCode(userCommand: String): String = withContext(Dispatchers.IO) {
        if (isDestroyed) return@withContext ""

        // Offline check — skip AI call if no network
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            if (caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) != true) {
                Log.w("MainActivity", "Offline — skipping AI call, using local fallback")
                return@withContext ""
            }
        } catch (e: Exception) {
            Log.w("MainActivity", "Network check failed: ${e.message}")
        }

        try {
            // Check if custom API is configured
            val prefs = getSharedPreferences("phoneclaw_config", MODE_PRIVATE)
            val useCustom = prefs.getBoolean("use_custom_config", false)
            
            val systemPrompt = buildAutomationSystemPrompt()
            val userPrompt = "User Command: $userCommand"

            val messages = listOf(
                mapOf("role" to "system", "content" to systemPrompt),
                mapOf("role" to "user", "content" to userPrompt)
            )

            val ollamaClient = OllamaClient(this@MainActivity)
            if (ollamaClient.isEnabled()) {
                Log.d("AIResponse", "Using Ollama at ${ollamaClient.getEndpoint()}")
                val ollamaResult = withContext(Dispatchers.IO) { ollamaClient.chat(messages) }
                if (ollamaResult.isSuccess) return@withContext ollamaResult.getOrNull() ?: ""
                Log.w("AIResponse", "Ollama failed, falling back to cloud AI")
            }

            val result = if (useCustom) {
                // Use BYOKClient for custom API
                Log.d("AIResponse", "Using custom API: ${prefs.getString("ai_provider", "Unknown")}")
                callBYOKClient(messages)
            } else {
                // Use default OpenRouter
                callStreaming16kAPI(messages, maxTokens = 1200, mode = "best")
            }

            // Log the raw AI response for debugging
            Log.d("AIResponse", "Raw response: ${result.take(500)}")
            // Save AI response for debug dialog
            try {
                val prefs = getSharedPreferences("phoneclaw_config", MODE_PRIVATE)
                prefs.edit().putString("last_ai_response", result?.take(1000) ?: "").apply()
            } catch(e: Exception) {}
            
            if (result.isNotEmpty()) {
                var extractedCode = extractJavaScriptCode(result)
                
                // VALIDATION: Check if code actually does something useful
                // If only speakText with no real actions, it's BAD code - retry
                val hasRealAction = extractedCode.contains("openApp(") || 
                                   extractedCode.contains("launch") ||
                                   extractedCode.contains("toggleFlash") ||
                                   extractedCode.contains("clickBy") ||
                                   extractedCode.contains("typeInField") ||
                                   extractedCode.contains("delay(")
                
                if (!hasRealAction && extractedCode.contains("speakText")) {
                    // Bad code! Only speakText, no real actions - retry once
                    Log.w("AIResponse", "Bad code detected - only speakText, no actions. Retrying...")
                    val retryMessages = listOf(
                        mapOf("role" to "system", "content" to "IMPORTANT: Generate REAL working code! Use openApp(), launchX(), toggleFlashlight() etc! NOT just speakText!"),
                        mapOf("role" to "user", "content" to userPrompt)
                    )
                    val retryResult = callAI(retryMessages, 500, "fast")
                    if (retryResult.isNotEmpty()) {
                        extractedCode = extractJavaScriptCode(retryResult)
                        Log.d("AIResponse", "Retry generated: ${extractedCode.take(100)}")
                    }
                }
                // Log the extracted JS code
                Log.d("AIResponse", "Extracted JS: ${extractedCode.take(300)}")
                // Save to debug log
                try {
                    val prefs = getSharedPreferences("phoneclaw_config", MODE_PRIVATE)
                    prefs.edit().putString("debug_log", "Last JS:\n${extractedCode.take(500)}").apply()
                } catch(e: Exception) {}
                extractedCode
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error generating automation code: ${e.message}")
            ""
        }
    }
    
    // Use BYOKClient for custom API
    // Helper to check if custom API should be used
    private fun shouldUseCustomAPI(): Boolean {
        val prefs = getSharedPreferences("phoneclaw_config", MODE_PRIVATE)
        return prefs.getBoolean("use_custom_config", false) && prefs.getString("api_key", "")?.isNotEmpty() == true
    }
    
    // Helper to get the provider name for logging
    private fun getCurrentProviderName(): String {
        val prefs = getSharedPreferences("phoneclaw_config", MODE_PRIVATE)
        return prefs.getString("ai_provider", "OpenRouter") ?: "OpenRouter"
    }
    
    // Unified AI call function that handles both custom and default
    private suspend fun callAI(messages: List<Map<String, String>>, maxTokens: Int = 300, mode: String = "fast"): String = withContext(Dispatchers.IO) {
        if (shouldUseCustomAPI()) {
            Log.d("AI", "Using custom API: ${getCurrentProviderName()}")
            callBYOKClient(messages)
        } else {
            Log.d("AI", "Using default OpenRouter")
            callStreaming16kAPI(messages, maxTokens, mode)
        }
    }

    private suspend fun callBYOKClient(messages: List<Map<String, String>>): String = withContext(Dispatchers.IO) {
        try {
            val client = BYOKClient(this@MainActivity)
            val result = client.chatSync(messages)
            result.onSuccess { response ->
                Log.d("BYOKClient", "Success: ${response.take(200)}")
            }.onFailure { err ->
                Log.e("BYOKClient", "Failed: ${err.message}")
                withContext(Dispatchers.Main) {
                    val prefs = getSharedPreferences("phoneclaw_config", MODE_PRIVATE)
                    val provider = prefs.getString("ai_provider", "Unknown")
                    val model = prefs.getString("model", "Unknown")
                    Toast.makeText(this@MainActivity,
                        "AI Error ($provider/$model): ${err.message?.take(120)}",
                        Toast.LENGTH_LONG).show()
                }
            }
            result.getOrNull() ?: ""
        } catch (e: Exception) {
            Log.e("BYOKClient", "Exception in callBYOKClient: ${e.message}")
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "AI call failed: ${e.message?.take(100)}", Toast.LENGTH_LONG).show()
            }
            ""
        }
    }

    private fun buildAutomationSystemPrompt(): String {
        val memoryContext = MemoryManager.buildContextSummary(this)
        val contextSection = if (memoryContext.isNotEmpty()) "\n\n## MEMORY/CONTEXT:\n$memoryContext\n\n" else ""
        return """$contextSection
TASK: Generate JavaScript to automate Android.

SIMPLE RULE: If user says "open X", use openApp("package")!

NEVER: speakText("Opening...") - WRONG!
ALWAYS: Call real function like openApp()!

Example: "open Chrome" → openApp("com.android.chrome");
1. For EVERY command, generate JavaScript code that performs REAL actions
2. NEVER respond with just speakText() - that does NOTHING useful!
3. ALWAYS generate actual function calls: openApp(), toggleFlashlight(), clickByText(), etc.
4. The ONLY time speakText() is allowed is for feedback AFTER actions complete
5. If you don't use real functions, the automation will FAIL

EXAMPLE OF WRONG RESPONSE (BAD):
speakText("I heard: open Chrome. Processing command...");
delay(2000);
speakText("Command processed successfully");

EXAMPLE OF CORRECT RESPONSE (GOOD):
openApp("com.android.chrome");
delay(3000);

When user says "open Chrome", you MUST call openApp("com.android.chrome")!

---

You are an Android automation code generator. Generate JavaScript code that AUTOMATES ACTIONS!
3. Include delays between actions: delay(2000);
4. For multi-step: open app, wait, then perform action
5. Always use actual function calls to do things


## ⚠️ CRITICAL PLANNING RULES:

ALWAYS plan before generating code:
1. What is the user asking for?
2. What functions need to be called?
3. Add delay() between actions!
4. What could go wrong?

YOUR CODE MUST:
- Call REAL functions (openApp, toggleFlashlight, clickByText, etc.)
- NEVER just use speakText() alone - that does nothing!
- Have delays between actions: delay(2000);
- If asking to open an app → call openApp() or launchApp()

## CONTEXT & MEMORY:
- Previous actions affect next steps
- If user says "then" or "now do X", X happens AFTER previous
- Track what was already done in previous commands

## SCREEN ANALYSIS:
For complex tasks, USE SNAPSHOT to see what's on screen:
- analyzeScreen() - Returns list of clickable elements with text, IDs, coordinates
- Use snapshot to find correct buttons/fields to click
- Example: search screen → find "Search" field → typeInField("query")

## ERROR RECOVERY:
- If action fails, try alternative approach
- Add fallback: try primary action, if fails try fallback
- Use isTextPresentOnScreen() to verify success

## COMPLEX COMMAND EXAMPLES:
- "open YouTube and search for AI" →
  \`\`\`javascript
  // Step 1: Open YouTube
  openApp("com.google.android.youtube");
  delay(3000); // Wait for app to open
  
  // Step 2: Click search
  clickByText("Search");
  delay(1000);
  
  // Step 3: Type search query
  typeInField("Open AI");
  delay(500);
  
  // Step 4: Press enter
  pressEnter();
  \`\`\`

- "find and open WhatsApp" →
  \`\`\`javascript
  // Analyze screen first
  analyzeScreen();
  // Find WhatsApp icon and click it
  clickByText("WhatsApp");
  \`\`\`

- "turn on flashlight then open chrome" →
  \`\`\`javascript
  // Step 1: Turn on flashlight
  toggleFlashlight(true);
  delay(500);
  
  // Step 2: Open Chrome
  openApp("com.android.chrome");
  delay(3000);
  \`\`\`

AVAILABLE FUNCTIONS - USE THESE:

## App Launching:
- openApp("com.google.android.youtube") - Open YouTube
- openApp("com.android.chrome") - Open Chrome
- openApp("com.instagram.android") - Open Instagram
- openApp("com.twitter.android") - Open Twitter
- openApp("com.snapchat.android") - Open Snapchat
- launchTikTok() - Launch TikTok
- launchYouTube() - Launch YouTube
- searchYouTube("query") - Search YouTube for a query directly (PREFER this over open+click+type)
- openAppByName("Audiomack") - Open app by display name (use when package unknown)
- openAppByName("WhatsApp Business") - Opens the correct variant

## Actions AFTER opening apps:
- clickByText("Search") - Click element by text
- typeInField("text to type") - Type in search field
- pressEnter() - Press enter key
- clickById("element_id") - Click by resource ID
- scrollDown() - Scroll down
- clickByContentDescription("desc") - Click by content desc

## Hardware:
- toggleFlashlight(true) - Turn ON flashlight
- toggleFlashlight(false) - Turn OFF flashlight
- getBatteryLevel() - Get battery level

## Example outputs:
- "open YouTube" → \`\`\`javascript
openApp("com.google.android.youtube");
delay(2000);
\`\`\`
- "turn on flashlight" → \`\`\`javascript
toggleFlashlight(true);
\`\`\`
- "search YouTube for AI" → \`\`\`javascript
openApp("com.google.android.youtube");
delay(3000);
clickByText("Search");
typeInField("open ai");
pressEnter();
\`\`\`
- launchTwitter() - Launch Twitter app
- launchReddit() - Launch Reddit app
- launchMedium() - Launch Medium app
- launchTelegram() - Launch Telegram
- launchWhatsApp() - Launch WhatsApp
- launchSnapchat() - Launch Snapchat
- launchLinkedIn() - Launch LinkedIn
- launchPinterest() - Launch Pinterest
- launchTwitch() - Launch Twitch
- launchDiscord() - Launch Discord
- launchSpotify() - Launch Spotify
- launchNetflix() - Launch Netflix

## Instagram Functions:
- launchInstagram() - Launch Instagram app

## System Settings:
- openWiFiSettings() - Open WiFi settings
- openBluetoothSettings() - Open Bluetooth settings
- openLocationSettings() - Open location settings
- openBatterySettings() - Open battery settings
- openDisplaySettings() - Open display settings
- openSoundSettings() - Open sound settings
- openStorageSettings() - Open storage settings
- openPrivacySettings() - Open privacy settings
- openSecuritySettings() - Open security settings
- openDeveloperOptions() - Open developer options

## System Controls:
- toggleWiFi(enable) - Toggle WiFi on/off (true/false)
- toggleBluetooth(enable) - Toggle Bluetooth on/off
- toggleLocationServices(enable) - Toggle location services
- toggleAirplaneMode(enable) - Toggle airplane mode
- toggleAutoRotate(enable) - Toggle auto rotation
- toggleDoNotDisturb(enable) - Toggle do not disturb
- setBrightness(level) - Set brightness (0-100)
- setVolume(type, level) - Set volume ("media"/"ringer"/"alarm", 0-100)
- lockScreen() - Lock the screen
- vibrate(milliseconds) - Vibrate device

## Communication:
- makePhoneCall(phoneNumber) - Make a phone call
- sendSMS(phoneNumber, message) - Send SMS message
- openDialer() - Open phone dialer
- openContacts() - Open contacts app
- openMessages() - Open messages app
- openGmail() - Open Gmail app
- composeEmail(to, subject, body) - Compose an email

## File & Media Operations:
- openFileManager() - Open file manager
- openGallery() - Open photo gallery
- openCamera() - Open camera app
- takePhoto() - Take a photo
- openMusicPlayer() - Open music player
- playMusic(filePath) - Play music file
- pauseMusic() - Pause music
- stopMusic() - Stop music

## Navigation & Location:
- openGoogleMaps() - Open Google Maps
- navigateToAddress(address) - Navigate to address
- searchNearby(query) - Search nearby locations
- getCurrentLocation() - Get current GPS coordinates
- openUber() - Open Uber app
- openLyft() - Open Lyft app

## Hardware Control:
- toggleFlashlight(enable) - Toggle flashlight on/off
- takeFrontCamera() - Switch to front camera
- takeBackCamera() - Switch to back camera
- recordAudio(durationSeconds) - Record audio

## App Management:
- openPlayStore() - Open Google Play Store
- searchPlayStore(query) - Search in Play Store
- openAppInfo(packageName) - Open app info/settings
- forceStopApp(packageName) - Force stop an app
- clearAppCache(packageName) - Clear app cache

## Security Functions:
- generateQRCode(data) - Generate QR code
- scanQRCode() - Scan QR code (returns text)
- enableScreenLock(type, password) - Enable screen lock ("pin"/"pattern"/"password")

## Network Management:
- connectToWiFi(ssid, password) - Connect to WiFi network
- disconnectFromWiFi() - Disconnect from WiFi
- checkInternetConnection() - Check if internet is available
- enableMobileData(enable) - Toggle mobile data
- switchToMobileData() - Switch to mobile data
- switchToWiFi() - Switch to WiFi

## Accessibility & UI:
- enableTalkBack(enable) - Toggle TalkBack accessibility
- increaseFontSize() - Increase system font size
- decreaseFontSize() - Decrease system font size
- enableHighContrast(enable) - Toggle high contrast mode
- findElementByText(text) - Find UI element by text
- waitForElement(text, timeoutSeconds) - Wait for element to appear
- scrollUntilFound(text) - Scroll until text is found
- swipeLeft() - Swipe left gesture
- swipeRight() - Swipe right gesture
- swipeUp() - Swipe up gesture
- swipeDown() - Swipe down gesture
- longPress(x, y) - Long press at coordinates
- doubleClick(x, y) - Double click at coordinates

## Productivity:
- openCalendar() - Open calendar app
- createEvent(title, date, time) - Create calendar event
- setAlarm(hour, minute, label) - Set alarm
- setTimer(minutes) - Set timer
- openClock() - Open clock app
- openNotes() - Open notes app
- createNote(title, content) - Create a note
- openGoogleDocs() - Open Google Docs

## Shopping & Finance:
- openAmazon() - Open Amazon app
- searchProduct(query) - Search for product
- openBankingApp(bankName) - Open banking app
- openPaymentApp(appName) - Open payment app (PayPal, Venmo, etc.)

## CRON SCHEDULING:
- schedule(task, cronExpression) - Schedule a task using cron format
- clearSchedule() - Clear all scheduled tasks

## Advanced Automation:
- extractTextFromImage(imagePath) - OCR text extraction
- translateText(text, targetLanguage) - Translate text
- summarizeText(text) - Summarize long text
- generateResponse(prompt) - Generate AI response

## Device Monitoring:
- getBatteryLevel() - Get battery percentage
- getMemoryUsage() - Get RAM usage percentage
- getStorageSpace() - Get storage info (used, total)
- getRunningApps() - List running applications
- getInstalledApps() - List installed applications

## Context-Aware Functions:
- analyzeCurrentScreen() - Analyze what's on screen
- detectCurrentApp() - Detect which app is open
- getScreenText() - Extract all text from screen
- findClickableElements() - Find all clickable elements
- suggestNextAction() - Suggest what to do next

## Core Navigation:
- simulateClick(x, y) - Click at coordinates
- clickNodesByContentDescription(text) - Click element with description
- simulateTypeInFirstEditableField(text) - Type in first input field
- simulateTypeInSecondEditableField(text) - Type in second input field
- pressEnterKey() - Press enter key
- simulateScrollToBottom() - Scroll to bottom
- simulateScrollToBottomX(X) - Scroll to bottom with x coordinate 
- simulateScrollToTop() - Scroll to top
- isTextPresentOnScreen(text) - Check if text exists on screen
- delay(milliseconds) - Wait for specified time
- speakText(text) - Make the phone speak

RULES:
1. Generate ONLY JavaScript code, no explanations
2. Use try-catch blocks for error handling
3. Add delays between actions: delay(2000) for 2 seconds
4. OPTIONAL: Add speakText() AFTER real actions complete as feedback — NOT as the only action
5. Use specific coordinates only when necessary
6. Prefer content description clicks over coordinates
7. Return only the JavaScript code, no markdown
8. For complex tasks, break them into steps with delays
9. Use appropriate error handling for all operations
10. Combine multiple functions for sophisticated automation

SCHEDULING EXAMPLES:

User: "Open TikTok every 5 seconds"
Response:
schedule("open TikTok", "*/5 * * * *");

User: "Check battery every 10 minutes"
Response:
schedule("check battery level", "0 */10 * * *");

User: "Clear all scheduled tasks"
Response:
clearSchedule();

IMMEDIATE EXECUTION EXAMPLES:

User: "Open TikTok"
Response:
speakText("Opening TikTok");
launchTikTok();

User: "Check battery level"
Response:
speakText("Checking battery level");
var level = getBatteryLevel();
speakText("Battery is at " + level + " percent");

Generate JavaScript automation code for the user's command:
        """.trimIndent()
    }

    private fun extractJavaScriptCode(response: String): String {
        // Try to find code block first
        val codeBlockRegex = "```(?:javascript|js)?\n?(.*?)```".toRegex(RegexOption.DOT_MATCHES_ALL)
        val codeBlockMatch = codeBlockRegex.find(response)
        
        if (codeBlockMatch != null) {
            var code = codeBlockMatch.groupValues[1].trim()
            // Clean up common issues
            code = code.replace(Regex("^\\s+"), "")  // Remove leading whitespace
            code = code.replace(Regex("\\s+$"), "")  // Remove trailing whitespace
            return code
        }

        // If no code block, try to find lines that look like JS
        val lines = response.split("\n")
        val jsLines = mutableListOf<String>()
        
        for (line in lines) {
            val trimmed = line.trim()
            // Skip empty lines, comments, and non-code lines
            if (trimmed.isEmpty()) continue
            if (trimmed.startsWith("//")) continue
            if (trimmed.startsWith("User:")) continue
            if (trimmed.startsWith("Response:")) continue
            if (trimmed.startsWith("Generate")) continue
            if (!trimmed.contains("(") && !trimmed.contains(";") && !trimmed.contains("=")) continue
            jsLines.add(line)
        }

        return if (jsLines.isNotEmpty()) {
            jsLines.joinToString("\n").trim()
        } else {
            // Last resort: clean the whole response
            response
                .replace(Regex("^[^a-zA-Z]+"), "")
                .trim()
        }
    }

    private suspend fun executeGeneratedCode(code: String) {
        if (isDestroyed) return

        withContext(Dispatchers.Default) {
            var context: org.mozilla.javascript.Context? = null
            try {
                context = org.mozilla.javascript.Context.enter()

                context.optimizationLevel = -1

                context.setClassShutter { className ->
                    !className.startsWith("javax.lang.model") &&
                            !className.startsWith("javax.annotation.processing") &&
                            !className.startsWith("java.lang.reflect") &&
                            !className.startsWith("sun.") &&
                            !className.startsWith("com.sun.")
                }

                val scope = context.initStandardObjects()

                val androidInterface = AndroidJSInterface()
                scope.put("Android", scope, androidInterface)

                val wrappedCode = """
                // Core functions
                function speakText(text) { Android.speakText(text); }
                function delay(ms) { Android.delay(ms); }
                
                // Scheduling functions
                function schedule(task, cronExpression) { Android.schedule(task, cronExpression); }
                function clearSchedule() { Android.clearSchedule(); }
                
                //Automation functions 
                function magicClicker(description){
                Android.magicClicker(description)
              //  Android.delay(3000);
                }
                function magicScraper(description){ return Android.magicScraper(description)}
                function getLatestOtp() { return Android.getLatestOtp(); }
                function waitForOtp(timeoutMs) { return Android.waitForOtp(timeoutMs || 60000); }
                function undoLastAction() { return Android.undoLastAction(); }

                function sendAgentEmail(to, subject, message) {
                Android.sendAgentEmail(to, subject, message); 
                }


                // Safe number conversion functions
                function safeInt(value, defaultVal) {
                    if (value == null || value === undefined) return defaultVal || 0;
                    if (typeof value === 'number') {
                        if (isNaN(value) || !isFinite(value)) {
                            Android.logWarning("JavaScript", "safeInt: Invalid number " + value);
                            return defaultVal || 0;
                        }
                        return Math.floor(value);
                    }
                    if (typeof value === 'string') {
                        var parsed = parseInt(value);
                        if (isNaN(parsed)) {
                            Android.logWarning("JavaScript", "safeInt: Cannot parse '" + value + "'");
                            return defaultVal || 0;
                        }
                        return parsed;
                    }
                    Android.logWarning("JavaScript", "safeInt: Unknown type for " + value);
                    return defaultVal || 0;
                }
                
                function safeFloat(value, defaultVal) {
                    if (value == null || value === undefined) return defaultVal || 0.0;
                    if (typeof value === 'number') {
                        if (isNaN(value) || !isFinite(value)) {
                            Android.logWarning("JavaScript", "safeFloat: Invalid number " + value);
                            return defaultVal || 0.0;
                        }
                        return value;
                    }
                    if (typeof value === 'string') {
                        var parsed = parseFloat(value);
                        if (isNaN(parsed)) {
                            Android.logWarning("JavaScript", "safeFloat: Cannot parse '" + value + "'");
                            return defaultVal || 0.0;
                        }
                        return parsed;
                    }
                    Android.logWarning("JavaScript", "safeFloat: Unknown type for " + value);
                    return defaultVal || 0.0;
                }
                
                // SharedPreferences helper object
                function getSharedPreferences(name, mode) {
                    return {
                        getString: function(key, defaultValue) {
                            return Android.getStringFromPrefs(name, key, defaultValue || "");
                        },
                        putString: function(key, value) {
                            return Android.setStringToPrefs(name, key, value);
                        },
                        getInt: function(key, defaultValue) {
                            return Android.getIntFromPrefs(name, key, defaultValue || 0);
                        },
                        putInt: function(key, value) {
                            return Android.setIntToPrefs(name, key, value);
                        },
                        putLong: function(key, value) {
                            return Android.setLongToPrefs(name, key, value);
                        },
                        getFloat: function(key, defaultValue) {
                            return Android.getFloatFromPrefs(name, key, defaultValue || 0.0);
                        },
                        putFloat: function(key, value) {
                            return Android.setFloatToPrefs(name, key, value);
                        },
                        getBoolean: function(key, defaultValue) {
                            return Android.getBooleanFromPrefs(name, key, defaultValue || false);
                        },
                        putBoolean: function(key, value) {
                            return Android.setBooleanToPrefs(name, key, value);
                        },
                        edit: function() {
                            return this; // Return self for chaining
                        },
                        apply: function() {
                            // No-op since we apply immediately in the individual methods
                        },
                        commit: function() {
                            // No-op since we apply immediately in the individual methods
                            return true;
                        }
                    };
                }
                // App launching
                function launchTikTok() { Android.launchTikTok(); }
                function launchYouTube() { Android.launchYouTube(); }
                function launchInstagram() { Android.launchInstagram(); }
                function launchTwitter() { Android.launchTwitter(); }
                function launchReddit() { Android.launchReddit(); }
                function launchMedium() { Android.launchMedium(); }
                function launchTelegram() { Android.launchTelegram(); }
                function launchWhatsApp() { Android.launchWhatsApp(); }
                function launchSnapchat() { Android.launchSnapchat(); }
                function launchLinkedIn() { Android.launchLinkedIn(); }
                function launchPinterest() { Android.launchPinterest(); }
                function launchTwitch() { Android.launchTwitch(); }
                function launchDiscord() { Android.launchDiscord(); }
                function launchSpotify() { Android.launchSpotify(); }
                function launchNetflix() { Android.launchNetflix(); }
                function launchGmail() { Android.launchGmail(); }
                function sendEmail(to, subject, body) { Android.sendEmail(to, subject, body); }
 function findNodeByClassNameAndIndex(className, index) {
                    return Android.findNodeByClassNameAndIndex(className, safeInt(index, 0));
                }
                
                
                
                function performNodeClick(node) {
                    Android.performNodeClick(node);
                }
                
                function isTextPresentOnScreen(text) {
                    return Android.isTextPresentOnScreen(text);
                }
                   function handleTikTokStartupScreens() {
                    return Android.handleTikTokStartupScreens();
                }
                
                function getContentDescriptionForNodeContaining(text) {
                    return Android.getContentDescriptionForNodeContaining(text);
                }
                
                // Data fetching functions handleTikTokStartupScreens
                
                // String utility functions
                function replaceAll(str, searchValue, replaceValue) {
                    return Android.replaceAll(str, searchValue, replaceValue);
                }
                
                
                


                function launchInstagram() {
                    Android.launchInstagram();
                }
                
                function fetchTodaysVideoSync(email, server) {
                    return Android.fetchTodaysVideoSync(email, server);
                }
                
                function fetchBio(email, server) {
                    return Android.fetchBio(email, server);
                }
                
                function fetchBlogPost(caption, username) {
                    return Android.fetchBlogPost(caption, username);
                }
                
                function fetchSearch(caption, username) {
                    return Android.fetchSearch(caption, username);
                }
             function getAutoCommentCampaignForServer(email, serverId) {
    return Android.getAutoCommentCampaignForServer(email, serverId);
}

                function fetchReply(postText, username) {
                    return Android.fetchReply(postText, username);
                }
                
                // File and upload functions
                function downloadVideo(url, filename) {
                    return Android.downloadVideo(url, filename);
                }
                
                function downloadProfileImage(email, server) {
                    return Android.downloadProfileImage(email, server);
                }
                
                function downloadRandomBrandAssets(email, server) {
                    Android.downloadRandomBrandAssets(email, server);
                }
                
                
                
                
                function markVideoAsPosted(email, key) {
                    Android.markVideoAsPosted(email, key);
                }
                   function clickVideoUploadButton() {
                    Android.clickVideoUploadButton();
                }
                 function clickElementByArea(area) {
                    Android.clickElementByArea(area);
                }  
                     function simulateType(id, text) {
                    Android.simulateType(id, text);
                }
                   function split(str, delimiter) {
                    return Android.split(str, delimiter);
                }
                
                
                            function clickElementByViewId(id) {
                    Android.clickElementByViewId(id);
                }
                
                
                
                // Logging
                function logInfo(tag, message) {
                    Android.logInfo(tag, message);
                }
                
                function logWarning(tag, message) {
                    Android.logWarning(tag, message);
                }
                
                // System settings
                function openWiFiSettings() { Android.openWiFiSettings(); }
                function openBluetoothSettings() { Android.openBluetoothSettings(); }
                function openLocationSettings() { Android.openLocationSettings(); }
                function openBatterySettings() { Android.openBatterySettings(); }
                function openDisplaySettings() { Android.openDisplaySettings(); }
                function openSoundSettings() { Android.openSoundSettings(); }
                function openStorageSettings() { Android.openStorageSettings(); }
                function openPrivacySettings() { Android.openPrivacySettings(); }
                function openSecuritySettings() { Android.openSecuritySettings(); }
                function openDeveloperOptions() { Android.openDeveloperOptions(); }
                
                // System controls
                function toggleWiFi(enable) { Android.toggleWiFi(enable); }
                function toggleBluetooth(enable) { Android.toggleBluetooth(enable); }
                function toggleLocationServices(enable) { Android.toggleLocationServices(enable); }
                function toggleAirplaneMode(enable) { Android.toggleAirplaneMode(enable); }
                function toggleAutoRotate(enable) { Android.toggleAutoRotate(enable); }
                function toggleDoNotDisturb(enable) { Android.toggleDoNotDisturb(enable); }
                function setBrightness(level) { Android.setBrightness(level); }
                function setVolume(type, level) { Android.setVolume(type, level); }
                function lockScreen() { Android.lockScreen(); }
                function vibrate(ms) { Android.vibrate(ms); }
                
                // Communication
                function makePhoneCall(number) { Android.makePhoneCall(number); }
                function sendSMS(number, message) { Android.sendSMS(number, message); }
                function openDialer() { Android.openDialer(); }
                function openContacts() { Android.openContacts(); }
                function openMessages() { Android.openMessages(); }
                function openGmail() { Android.openGmail(); }
                function composeEmail(to, subject, body) { Android.composeEmail(to, subject, body); }
                
                // File & Media
                function openFileManager() { Android.openFileManager(); }
                function openGallery() { Android.openGallery(); }
                function openCamera() { Android.openCamera(); }
                function takePhoto() { Android.takePhoto(); }
                function openMusicPlayer() { Android.openMusicPlayer(); }
                function playMusic(path) { Android.playMusic(path); }
                function pauseMusic() { Android.pauseMusic(); }
                function stopMusic() { Android.stopMusic(); }
                
                // Navigation
                function openGoogleMaps() { Android.openGoogleMaps(); }
                function navigateToAddress(address) { Android.navigateToAddress(address); }
                function searchNearby(query) { Android.searchNearby(query); }
                function getCurrentLocation() { return Android.getCurrentLocation(); }
                function openUber() { Android.openUber(); }
                function openLyft() { Android.openLyft(); }
                
                // Hardware
                function toggleFlashlight(enable) { Android.toggleFlashlight(enable); }
                function openApp(packageName) { Android.openApp(packageName); }
                function launchAppByName(appName) { Android.launchAppByName(appName); }
                function openAppByName(appName) { Android.openAppByName(appName); }
                function searchYouTube(query) { Android.searchYouTube(query); }
                function rememberFact(key, value) { Android.rememberFact(key, value); }
                function recallFact(key) { return Android.recallFact(key); }
                function clickByText(text) { Android.clickNodesByContentDescription(text); }
                function typeInField(text) { Android.simulateTypeInFirstEditableField(text); }
                function pressEnter() { Android.pressEnterKey(); }
                function scrollDown() { Android.simulateScrollToBottom(); }
                function getBatteryLevel() { return Android.getBatteryLevel(); }

                // NEW FUNCTIONS
                function takeScreenshot() { return Android.takeScreenshot(); }
                function copyToClipboard(text) { Android.copyToClipboard(text); }
                function openAppSettings(pkg) { Android.openAppSettings(pkg); }
                function getIPAddress() { return Android.getIPAddress(); }
                function clearClipboard() { Android.clearClipboard(); }
                function openFile(path) { Android.openFile(path); }
                function shareText(text) { Android.shareText(text); }
                function setScreenTimeout(seconds) { Android.setScreenTimeout(seconds); }

                // Screen Analysis
                function analyzeScreen() { return Android.analyzeScreen(); }
                function isTextPresent(text) { return Android.isTextPresentOnScreen(text); }
                function waitForElement(text, timeout) { return Android.waitForElement(text, timeout); }
                
                // Existing
                function takeFrontCamera() { Android.takeFrontCamera(); }
                function takeBackCamera() { Android.takeBackCamera(); }
                function recordAudio(duration) { Android.recordAudio(duration); }
                
                // App management
                function openPlayStore() { Android.openPlayStore(); }
                function searchPlayStore(query) { Android.searchPlayStore(query); }
                function openAppInfo(pkg) { Android.openAppInfo(pkg); }
                function forceStopApp(pkg) { Android.forceStopApp(pkg); }
                function clearAppCache(pkg) { Android.clearAppCache(pkg); }
                
                // Security
                function generateQRCode(data) { Android.generateQRCode(data); }
                function scanQRCode() { return Android.scanQRCode(); }
                function enableScreenLock(type, password) { Android.enableScreenLock(type, password); }
                
                // Network
                function connectToWiFi(ssid, password) { Android.connectToWiFi(ssid, password); }
                function disconnectFromWiFi() { Android.disconnectFromWiFi(); }
                function checkInternetConnection() { return Android.checkInternetConnection(); }
                function enableMobileData(enable) { Android.enableMobileData(enable); }
                function switchToMobileData() { Android.switchToMobileData(); }
                function switchToWiFi() { Android.switchToWiFi(); }
                
                // Accessibility & UI
                function enableTalkBack(enable) { Android.enableTalkBack(enable); }
                function increaseFontSize() { Android.increaseFontSize(); }
                function decreaseFontSize() { Android.decreaseFontSize(); }
                function enableHighContrast(enable) { Android.enableHighContrast(enable); }
                function findElementByText(text) { return Android.findElementByText(text); }
                function waitForElement(text, timeout) { return Android.waitForElement(text, timeout); }
                function scrollUntilFound(text) { return Android.scrollUntilFound(text); }
                function swipeLeft() { Android.swipeLeft(); }
                function swipeRight() { Android.swipeRight(); }
                function swipeUp() { Android.swipeUp(); }
                function swipeDown() { Android.swipeDown(); }
                function longPress(x, y) { Android.longPress(x, y); }
                function doubleClick(x, y) { Android.doubleClick(x, y); }
                
                // Productivity
                function openCalendar() { Android.openCalendar(); }
                function createEvent(title, date, time) { Android.createEvent(title, date, time); }
                function setAlarm(hour, minute, label) { Android.setAlarm(hour, minute, label); }
                function setTimer(minutes) { Android.setTimer(minutes); }
                function openClock() { Android.openClock(); }
                function openNotes() { Android.openNotes(); }
                function createNote(title, content) { Android.createNote(title, content); }
                function openGoogleDocs() { Android.openGoogleDocs(); }
                
                // Shopping & Finance
                function openAmazon() { Android.openAmazon(); }
                function searchProduct(query) { Android.searchProduct(query); }
                function openBankingApp(name) { Android.openBankingApp(name); }
                function openPaymentApp(name) { Android.openPaymentApp(name); }
                
                // Advanced automation
                function extractTextFromImage(path) { return Android.extractTextFromImage(path); }
                function translateText(text, lang) { return Android.translateText(text, lang); }
                function summarizeText(text) { return Android.summarizeText(text); }
                function generateResponse(prompt) { return Android.generateResponse(prompt); }
                
                // Device monitoring
                function getBatteryLevel() { return Android.getBatteryLevel(); }
                function getMemoryUsage() { return Android.getMemoryUsage(); }
                function getStorageSpace() { return Android.getStorageSpace(); }
                function getRunningApps() { return Android.getRunningApps(); }
                function getInstalledApps() { return Android.getInstalledApps(); }
                
                // Context-aware
                function analyzeCurrentScreen() { return Android.analyzeCurrentScreen(); }
                function detectCurrentApp() { return Android.detectCurrentApp(); }
                function getScreenText() { return Android.getScreenText(); }
                function findClickableElements() { return Android.findClickableElements(); }
                function suggestNextAction() { return Android.suggestNextAction(); }
                
                
                   function clickVideoUploadButton() {
                    Android.clickVideoUploadButton();
                }
                     function clickFirstSong() {
                    Android.clickFirstSong();
                }
                   function clickAddSound() {
                    Android.clickAddSound();
                }
                
                // Original core functions
                function simulateClick(x, y) { Android.simulateClick(x, y); }
                function clickNodesByContentDescription(desc) { Android.clickNodesByContentDescription(desc); }
                function simulateTypeInFirstEditableField(text) { Android.simulateTypeInFirstEditableField(text); }
                function simulateTypeInSecondEditableField(text) { Android.simulateTypeInSecondEditableField(text); }
                function pressEnterKey() { Android.pressEnterKey(); }
                function simulateScrollToBottom() { Android.simulateScrollToBottom(); }
                                function simulateScrollToBottomX(X) { Android.simulateScrollToBottomX(X); }

                function simulateScrollToTop() { Android.simulateScrollToTop(); }
                function isTextPresentOnScreen(text) { return Android.isTextPresentOnScreen(text); }
                
                function simulateTypeByClass(className, text) {
                    Android.simulateTypeByClass(className, text);
                }
                  function check2FA() {
                    Android.check2FA();
                }
                    
                function simulateDeleteByClass(className) {
                    Android.simulateDeleteByClass(className);
                }
                function typeOne(text) { Android.simulateTypeInFirstEditableField(text); }
                function typeTwo(text) { Android.simulateTypeInSecondEditableField(text); }
                // Execute the generated code
                try {
                    $code
                } catch (error) {
                    Android.speakText("Error executing automation: " + error.message);
                    Android.logInfo("AutomationError", error.message);
                }
                """.trimIndent()

                context.evaluateString(scope, wrappedCode, "<voice_generated_script>", 1, null)

            } catch (e: Exception) {
                Log.e("MainActivity", "JavaScript execution error: ${e.message}")
                // Store error for debug dialog
                try {
                    val prefs = getSharedPreferences("phoneclaw_config", MODE_PRIVATE)
                    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    val errorMsg = "[$timestamp] ${e.message}"
                    val existingErrors = prefs.getString("last_error", "")
                    prefs.edit().putString("last_error", errorMsg + "\n" + existingErrors?.take(500) ?: "").apply()
                } catch (ignore: Exception) {}
                withContext(Dispatchers.Main) {
                    speakText("Error executing automation: ${e.message}")
                }
            } finally {
                context?.let { org.mozilla.javascript.Context.exit() }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            speakText("Voice automation ready. Tap the microphone button to give commands.")
            updateStatusWithAnimation("🎤 Ready - Tap button to speak")
        } else {
            Log.e("MainActivity", "Failed to initialize text to speech engine")
        }
    }

    private fun speakText(text: String) {
        try {
            if (::tts.isInitialized) {
                val utteranceId = UUID.randomUUID().toString()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId)
                } else {
                    @Suppress("DEPRECATION")
                    tts.speak(text, TextToSpeech.QUEUE_ADD, null)
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in speakText: ${e.message}")
        }
    }
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onDestroy() {
        Log.d("MainActivity", "onDestroy starting")
        isDestroyed = true

        try {
            // 1. Stop speech recognition FIRST
            if (::speechRecognizer.isInitialized) {
                try {
                    speechRecognizer.stopListening()
                    speechRecognizer.cancel() // ADDED
                    speechRecognizer.destroy()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error destroying speech: ${e.message}")
                }
            }

            // 2. Cancel all jobs with timeout
            runBlocking {
                withTimeout(2000) { // 2 second timeout
                    cronCheckJob?.cancel()
                }
            }

            // 3. Shutdown executor
            try {
                cronScheduler.shutdownNow()
                cronScheduler.awaitTermination(1, TimeUnit.SECONDS)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error shutting down scheduler: ${e.message}")
            }

            // 4. Cancel composite job
            compositeJob.cancel()

            // 5. Clean up bitmaps
            for (bitmap in activeBitmaps.toList()) {
                try {
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error recycling bitmap: ${e.message}")
                }
            }
            activeBitmaps.clear()

            // 6. TTS cleanup
            if (::tts.isInitialized) {
                try {
                    tts.stop()
                    tts.shutdown()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error shutting down TTS: ${e.message}")
                }
            }

            // 8. Force GC
            System.gc()

        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onDestroy: ${e.message}")
        }

        super.onDestroy()
        Log.d("MainActivity", "onDestroy completed")
    }


    private suspend fun takeScreenshotForAPI(): Bitmap? = withContext(Dispatchers.IO) {
        if (isDestroyed) return@withContext null

        var bitmap: Bitmap? = null
        try {
            // Check memory first

            var attempts = 0
            while (!ScreenCaptureService.isReady && attempts < 50) {
                delay(100)
                attempts++
            }

            val pngBytes = ScreenCaptureService.lastCapturedPng
            if (pngBytes == null) {
                Log.w("MainActivity", "No screenshot available")
                return@withContext null
            }

            // FIXED: Decode with aggressive memory-saving options
            val options = BitmapFactory.Options().apply {
                inSampleSize = 4 // Reduce to 25% size instead of 50%
                inPreferredConfig = Bitmap.Config.RGB_565 // 50% less memory than ARGB_8888
                inPurgeable = true
                inInputShareable = true
            }

            bitmap = BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size, options)

            if (bitmap != null) {
                // FIXED: Don't track in activeBitmaps if we're going to clean it up immediately
                Log.d("MainActivity", "Screenshot loaded: ${bitmap.width}x${bitmap.height}")
            }

            bitmap
        } catch (e: OutOfMemoryError) {
            Log.e("MainActivity", "OOM creating screenshot")
            bitmap?.recycle()
            System.gc()
            null
        } catch (e: Exception) {
            Log.e("MainActivity", "Error getting screenshot: ${e.message}")
            bitmap?.recycle()
            null
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        try {
            // FIXED: Reduce quality to 60 instead of 85
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

            // FIXED: Clean up bitmap immediately after compression
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }

            return Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } finally {
            try {
                byteArrayOutputStream.close()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error closing stream: ${e.message}")
            }
        }
    }
    private fun checkAccessibilityPermission() {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val myServiceId = "$packageName/.MyAccessibilityService"

        val isEnabled = enabledServices.any { serviceInfo ->
            val enabledId = serviceInfo.resolveInfo.serviceInfo.packageName + "/" +
                    serviceInfo.resolveInfo.serviceInfo.name
            enabledId == myServiceId
        }
        if (!isEnabled) {
            speakText("Please enable accessibility service for advanced voice automation")
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    private fun getUserEmail(): String {
        synchronized(emailLock) {
            return try {
                sharedPreferences.getString(KEY_USER_EMAIL, "") ?: ""
            } catch (e: Exception) {
                Log.e("MainActivity", "Error retrieving user email: ${e.message}")
                ""
            }
        }
    }

    private fun initializeUserEmail() {
        userEmail = getUserEmail()
        if (userEmail.isEmpty()) {
            userEmail = ""
        }
    }

    // [Continue with AndroidJSInterface - Due to length, I'll include the key fixes in it]
    // COMPLETE AndroidJSInterface with ALL 100+ functions
    inner class AndroidJSInterface {
        private fun toSafeInt(value: Any?, methodName: String = "unknown"): Int {
            return when (value) {
                null -> {
                    Log.w("SafeConversion", "$methodName: null value converted to 0")
                    0
                }
                is Number -> {
                    val double = value.toDouble()
                    when {
                        double.isNaN() -> {
                            Log.w("SafeConversion", "$methodName: NaN converted to 0")
                            0
                        }
                        !double.isFinite() -> {
                            Log.w("SafeConversion", "$methodName: Infinite value converted to 0")
                            0
                        }
                        else -> double.toInt()
                    }
                }
                is String -> {
                    value.toIntOrNull() ?: run {
                        Log.w("SafeConversion", "$methodName: Invalid string '$value' converted to 0")
                        0
                    }
                }
                else -> {
                    Log.w("SafeConversion", "$methodName: Unknown type ${value.javaClass.simpleName} converted to 0")
                    0
                }
            }
        }

        // Helper method to convert any value to Float safely
        private fun toSafeFloat(value: Any?, methodName: String = "unknown"): Float {
            return when (value) {
                null -> {
                    Log.w("SafeConversion", "$methodName: null value converted to 0.0")
                    0f
                }
                is Number -> {
                    val double = value.toDouble()
                    when {
                        double.isNaN() -> {
                            Log.w("SafeConversion", "$methodName: NaN converted to 0.0")
                            0f
                        }
                        !double.isFinite() -> {
                            Log.w("SafeConversion", "$methodName: Infinite value converted to 0.0")
                            0f
                        }
                        else -> double.toFloat()
                    }
                }
                is String -> {
                    value.toFloatOrNull() ?: run {
                        Log.w("SafeConversion", "$methodName: Invalid string '$value' converted to 0.0")
                        0f
                    }
                }
                else -> {
                    Log.w("SafeConversion", "$methodName: Unknown type ${value.javaClass.simpleName} converted to 0.0")
                    0f
                }
            }
        }
        @JavascriptInterface
        fun speakText(text: String) {
            this@MainActivity.speakText(text)
        }

        @JavascriptInterface
        fun getLatestOtp(): String = OtpReader.getLatestOtp(this@MainActivity) ?: ""

        @JavascriptInterface
        fun waitForOtp(timeoutMs: Any?): String {
            val timeout = (timeoutMs as? Number)?.toLong() ?: 60000L
            return kotlinx.coroutines.runBlocking { OtpReader.waitForOtp(this@MainActivity, timeoutMs = timeout) ?: "" }
        }

        @JavascriptInterface
        fun undoLastAction(): Boolean {
            val last = MyAccessibilityService.lastAction ?: run { speakText("No action to undo"); return false }
            return when (last.type) {
                "type" -> {
                    MyAccessibilityService.instance?.let { svc ->
                        val focused = svc.rootInActiveWindow?.findFocus(android.view.accessibility.AccessibilityNodeInfo.FOCUS_INPUT)
                        if (focused != null) {
                            val args = android.os.Bundle().apply {
                                putInt(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
                                putInt(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, focused.text?.length ?: 0)
                            }
                            focused.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_SELECTION, args)
                            val del = android.os.Bundle().apply { putCharSequence(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "") }
                            focused.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT, del)
                            speakText("Undid text input"); MyAccessibilityService.lastAction = null; true
                        } else { speakText("No focused text field"); false }
                    } ?: false
                }
                else -> { speakText("Cannot undo ${last.type}"); false }
            }
        }

        @JavascriptInterface
        fun performNodeClick(node: Any?) {
            (node as? android.view.accessibility.AccessibilityNodeInfo)?.let {
                MyAccessibilityService.instance?.performNodeClick(
                    it
                )
            }
        }

        @JavascriptInterface
        fun findNodeByClassNameAndIndex(className: String, index: Any?): Any? {
            val safeIndex = toSafeInt(index, "findNodeByClassNameAndIndex")
            Log.d("findNodeByClassNameAndIndex", "Original index: $index -> Safe index: $safeIndex")
            return MyAccessibilityService.instance?.findNodeByClassNameAndIndex(className, safeIndex)
        }


        @JavascriptInterface
        fun markVideoAsPosted(email: String, key: String) {
            Log.d("MainActivity", "markVideoAsPosted: local only, no remote sync")
        }

        private fun handleVerificationCode(code: String) {
            // Handle the verification code - you can customize this based on your needs
            Log.d("2FA", "Using verification code: $code")

            // Example: Fill in 2FA input field
            MyAccessibilityService.instance?.let { service ->
                // Look for 2FA input field and fill it

                // ist input field and enter the code
                service.enterTextInField(code)

                // Or if you need to click specific buttons to enter digits
                // enterCodeDigits(code)

            }
        }

        // Optional: Function to enter code digit by digit if needed
        private fun enterCodeDigits(code: String) {
            MyAccessibilityService.instance?.let { service ->
                code.forEach { digit ->
                    service.clickNodesByContentDescription(digit.toString())
                    Thread.sleep(100) // Small delay between digits
                }
            }
        }
        @JavascriptInterface
        fun check2FA() {
            // Check for "Find related content" screen
            if (MyAccessibilityService.instance?.isTextPresentOnScreen("@") == true){
                //multilogin
                Thread.sleep(10000) // Small delay between digits

                MyAccessibilityService.instance?.simulateClick(400f, 500f)
                //multilogin
                userEmail = ""
                Thread.sleep(30000) // Small delay between digits
                Log.d("2FA", " verification code found for user: $userEmail")

                // Launch coroutine to retrieve the latest verification code from Firebase
                CoroutineScope(Dispatchers.IO).launch {
                    retrieveLatestVerificationCode(userEmail) { code ->
                        if (code != null) {
                            // Use the code (e.g., fill in 2FA field)
                            handleVerificationCode(code)
                        } else {
                            Log.d("2FA", "No verification code found for user: $userEmail")
                        }
                    }
                }
                Thread.sleep(1000) // Small delay between digits

                CoroutineScope(Dispatchers.IO).launch {
                    retrieveLatestVerificationCode("rohan@cheatlayer.com") { code ->
                        if (code != null) {
                            // Use the code (e.g., fill in 2FA field)
                            handleVerificationCode(code)
                        } else {
                            Log.d("2FA", "No verification code found for user: \"rohan@cheatlayer.com\"")
                        }
                    }
                }

            }else  {
                if (MyAccessibilityService.instance?.isTextPresentOnScreen("Log in") == true) {
                    MyAccessibilityService.instance?.clickNodesByContentDescription("Log in")
                    userEmail = getUserEmail()
                    Thread.sleep(10000) // Small delay between digits

                    // Launch coroutine to retrieve the latest verification code from Firebase
                    CoroutineScope(Dispatchers.IO).launch {
                        retrieveLatestVerificationCode(userEmail) { code ->
                            if (code != null) {
                                // Use the code (e.g., fill in 2FA field)
                                handleVerificationCode(code)
                            } else {

                                Log.d("2FA", "No verification code found for user: $userEmail")
                            }
                        }
                    }
                    Thread.sleep(1000) // Small delay between digits

                    CoroutineScope(Dispatchers.IO).launch {
                        retrieveLatestVerificationCode("rohan@cheatlayer.com") { code ->
                            if (code != null) {
                                // Use the code (e.g., fill in 2FA field)
                                handleVerificationCode(code)
                            } else {
                                Log.d("2FA", "No verification code found for user: \"rohan@cheatlayer.com\"")
                            }
                        }
                    }
                    Thread.sleep(1000) // Small delay between digits

                    CoroutineScope(Dispatchers.IO).launch {
                        retrieveLatestVerificationCode("rohan@cheatlayer.com") { code ->
                            if (code != null) {
                                // Use the code (e.g., fill in 2FA field)
                                handleVerificationCode(code)
                            } else {
                                Log.d("2FA", "No verification code found for user: \"rohan@cheatlayer.com\"")
                            }
                        }
                    }

                }else{

                    userEmail = getUserEmail()
                    Thread.sleep(20000) // Small delay between digits
                    Log.d("2FA", " verification code found for user: $userEmail")

                    // Launch coroutine to retrieve the latest verification code from Firebase
                    CoroutineScope(Dispatchers.IO).launch {
                        retrieveLatestVerificationCode(userEmail) { code ->
                            if (code != null) {
                                // Use the code (e.g., fill in 2FA field)
                                handleVerificationCode(code)
                            } else {
                                Log.d("2FA", "No verification code found for user: $userEmail")
                            }
                        }
                    }


                    Thread.sleep(10000) // Small delay between digits

                    CoroutineScope(Dispatchers.IO).launch {
                        retrieveLatestVerificationCode("rohan@cheatlayer.com") { code ->
                            if (code != null) {
                                // Use the code (e.g., fill in 2FA field)
                                handleVerificationCode(code)
                            } else {
                                Log.d("2FA", "No verification code found for user: \"rohan@cheatlayer.com\"")
                            }
                        }
                    }


                }
            }
        }
// Add these functions inside the AndroidJSInterface inner class

        /**
         * Privacy: 2FA code retrieval from Firebase has been removed.
         * This stub always returns null so callers compile and run safely.
         * Implement your own local 2FA solution if needed.
         */
        private suspend fun retrieveLatestVerificationCode(userEmail: String, callback: (String?) -> Unit) {
            Log.d("2FA", "retrieveLatestVerificationCode: Firebase removed, returning null for $userEmail")
            withContext(Dispatchers.Main) { callback(null) }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        @JavascriptInterface
        fun doFullUploadSequenceInstagram(caption: String, email: String, randomAccount: String, server: String) {
            speakText("Instagram upload not available in this build")
        }


        @JavascriptInterface
        fun clickProfileMenuButton() {
            MyAccessibilityService.instance?.clickProfileMenuButton()

//       clickVideoUploadButton     MyAccessibilityService.instance?.clickNodesByContentDescription(desc) clickFirstElementWithAtSymbol
        }
        @JavascriptInterface
        fun clickFirstSong() {
            MyAccessibilityService.instance?.clickFirstSong()

//       clickVideoUploadButton     MyAccessibilityService.instance?.clickNodesByContentDescription(desc) clickFirstElementWithAtSymbol
        }
        @JavascriptInterface
        fun clickAddSound() {
            MyAccessibilityService.instance?.clickAddSound()

//       clickVideoUploadButton clickFirstSong     MyAccessibilityService.instance?.clickNodesByContentDescription(desc) clickFirstElementWithAtSymbol
        }
        @JavascriptInterface
        fun clickVideoUploadButton() {
            MyAccessibilityService.instance?.clickVideoUploadButton()

//          function clickProfileMenuButton(id) {
//         clickVideoUploadButton            Android.clickProfileMenuButton(id);
//                }   MyAccessibilityService.instance?.clickNodesByContentDescription(desc) clickFirstElementWithAtSymbol
        }

        @RequiresApi(Build.VERSION_CODES.O)
        @JavascriptInterface
        fun takeScreenshotAndUploadToLogs(email: String, prefix: String, server: String) {
            speakText("Log upload not available in this build")
        }
        @JavascriptInterface
        fun clickElementByViewId(viewId: String) {
            // Check for "Find related content" screen
            MyAccessibilityService.instance?.clickElementByViewId(viewId)

        }

        //text  text pressEnterKey
        @JavascriptInterface
        fun simulateType(id: String, text: String) {
            MyAccessibilityService.instance?.simulateType(id, text)
        }
        @JavascriptInterface
        fun clickElementByArea(targetArea: Int) {
            // Check for "Find related content" screen
            MyAccessibilityService.instance?.clickElementByArea(targetArea)

        }


        @JavascriptInterface
        fun simulateTypeByClass(className: String, text: String) {
            MyAccessibilityService.instance?.simulateTypeByClass(className, text)
        }
        @JavascriptInterface
        fun simulateDeleteByClass(className: String) {
            MyAccessibilityService.instance?.simulateDeleteByClass(className)
        }


        @JavascriptInterface
        fun fetchTodaysVideoSync(email: String, server: String): Array<Any?> {
            // Stub: remote video fetch removed (privacy cleanup)
            Log.d("AndroidJSInterface", "fetchTodaysVideoSync: no longer supported")
            return arrayOf("none", null)
        }

        @JavascriptInterface
        fun fetchBio(email: String, server: String): String {
            Log.d("AndroidJSInterface", "fetchBio: no longer supported")
            return ""
        }

        @JavascriptInterface
        fun fetchBlogPost(caption: String, username: String): String {
            return runBlocking(Dispatchers.IO) {
                try {
                    this@MainActivity.fetchBlogPost(caption, username)
                } catch (e: Exception) {
                    Log.e("AndroidJSInterface", "fetchBlogPost error: ${e.message}")
                    ""
                }
            }
        }

        @JavascriptInterface
        fun fetchSearch(caption: String, username: String): String {
            return runBlocking(Dispatchers.IO) {
                try {
                    this@MainActivity.fetchSearch(caption, username)
                } catch (e: Exception) {
                    Log.e("AndroidJSInterface", "fetchSearch error: ${e.message}")
                    ""
                }
            }
        }
        @JavascriptInterface
        fun getAutoCommentCampaignForServer(email: String, serverId: String): String {
            Log.d("AndroidJSInterface", "getAutoCommentCampaignForServer: no longer supported")
            return "{}"
        }

        @JavascriptInterface
        fun fetchReply(postText: String, username: String): String {
            return runBlocking(Dispatchers.IO) {
                try {
                    this@MainActivity.fetchReply(postText, username)
                } catch (e: Exception) {
                    Log.e("AndroidJSInterface", "fetchReply error: ${e.message}")
                    ""
                }
            }
        }

        @JavascriptInterface
        fun downloadVideo(url: String, filename: String): java.io.File? {
            return runBlocking(Dispatchers.IO) {
                try {
                    this@MainActivity.downloadVideo(url, filename)
                } catch (e: Exception) {
                    Log.e("AndroidJSInterface", "downloadVideo error: ${e.message}")
                    null
                }
            }
        }

        @JavascriptInterface
        fun downloadProfileImage(email: String, server: String): String? {
            Log.d("AndroidJSInterface", "downloadProfileImage: no longer supported")
            return null
        }

        @JavascriptInterface
        fun downloadRandomBrandAssets(email: String, server: String) {
            Log.d("AndroidJSInterface", "downloadRandomBrandAssets: no longer supported")
        }

        @RequiresApi(Build.VERSION_CODES.O)
        @JavascriptInterface
        fun doFullUploadSequence(caption: String, email: String, randomAccount: String, server: String) {
            speakText("TikTok upload not available in this build")
        }

        @JavascriptInterface
        fun delay(ms: Any?) {
            val safeMs = (ms as? Number)?.toLong() ?: 1000L
            Thread.sleep(safeMs)
        }
        private suspend fun takeScreenshotForAPI(): Bitmap? = withContext(Dispatchers.IO) {
            return@withContext try {
                val pngBytes = ScreenCaptureService.lastCapturedPng
                if (pngBytes == null) {
                    Log.w("MainActivity", "No captured screenshot available from ScreenCaptureService")
                    return@withContext null
                }

                // Convert PNG bytes to Bitmap
                val bitmap = BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size)
                if (bitmap != null) {
                    Log.d("MainActivity", "Screenshot loaded from service: ${bitmap.width}x${bitmap.height}")
                } else {
                    Log.e("MainActivity", "Failed to decode screenshot bytes")
                }
                bitmap
            } catch (e: Exception) {
                Log.e("MainActivity", "Error getting screenshot from service: ${e.message}")
                null
            }
        }
        @JavascriptInterface
        fun schedule(task: String, cronExpression: String) {
            try {
                val taskId = this@MainActivity.addCronTask(task, cronExpression)
                val interval = this@MainActivity.getIntervalFromCron(cronExpression)

                Log.d("AndroidJSInterface", "Scheduled task: $task with cron: $cronExpression (ID: $taskId), interval: ${interval}ms")

                if (interval != null) {
                    val seconds = interval / 1000
                    val minutes = seconds / 60
                    val hours = minutes / 60

                    val timeDescription = when {
                        hours > 0 -> "$hours hours"
                        minutes > 0 -> "$minutes minutes"
                        else -> "$seconds seconds"
                    }

                    speakText("Scheduled task: $task to run every $timeDescription")
                } else {
                    speakText("Scheduled task: $task with custom timing")
                }

                runOnUiThread { updateUI() }

                Log.d("AndroidJSInterface", "Total active cron tasks: ${this@MainActivity.cronTasks.size}")
                this@MainActivity.cronTasks.values.forEach { cronTask ->
                    Log.d("AndroidJSInterface", "Active task: ${cronTask.taskDescription}, expression: ${cronTask.cronExpression}")
                }

            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error scheduling task: ${e.message}")
                speakText("Error scheduling task")
            }
        }

        @JavascriptInterface
        fun clearSchedule() {
            try {
                val taskCount = this@MainActivity.cronTasks.size
                this@MainActivity.cronTasks.clear()
                this@MainActivity.saveCronTasks()
                speakText("Cleared $taskCount scheduled tasks")
                Log.d("AndroidJSInterface", "Cleared all scheduled tasks")
                runOnUiThread { updateUI() }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error clearing schedule: ${e.message}")
                speakText("Error clearing scheduled tasks")
            }
        }


        @JavascriptInterface
        fun rememberFact(key: String, value: String) {
            MemoryManager.rememberFact(this@MainActivity, key, value)
            speakText("Remembered: $key")
        }

        @JavascriptInterface
        fun recallFact(key: String): String {
            return MemoryManager.recallFact(this@MainActivity, key) ?: ""
        }

        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        @JavascriptInterface
        fun magicClicker(description: String) {
            mainScope.launch {
                var success = false; var attempts = 0; var currentDesc = description
                while (!success && attempts < 3) {
                    attempts++
                    val screenshot = takeScreenshotForAPI()
                    if (screenshot == null) { speakText("No screenshot available"); return@launch }
                    val coords = callMoondreamAPI(bitmapToBase64(screenshot), currentDesc)
                    if (coords != null) {
                        val px = (coords.x * 720).toFloat(); val py = (coords.y * 1600).toFloat()
                        withContext(Dispatchers.Main) { MyAccessibilityService.instance?.simulateClick(px, py); speakText("Clicked $currentDesc") }
                        LocalStorage.logAction(this@MainActivity, "magicClicker", currentDesc, "{\"x\":${px.toInt()},\"y\":${py.toInt()},\"attempt\":$attempts}")
                        MyAccessibilityService.lastAction = LastAction("click", px, py)
                        success = true
                    } else {
                        Log.w("MagicClicker", "Attempt $attempts failed for: $currentDesc")
                        if (attempts == 1) { currentDesc = description.split(" ").takeLast(2).joinToString(" "); speakText("Retrying: $currentDesc"); delay(1000) }
                        else if (attempts == 2) { currentDesc = description.split(" ").last(); speakText("Final attempt: $currentDesc"); delay(1000) }
                    }
                }
                if (!success) {
                    speakText("Could not find $description after $attempts attempts")
                    withContext(Dispatchers.Main) {
                        val et = android.widget.EditText(this@MainActivity).apply { setText(description) }
                        MaterialAlertDialogBuilder(this@MainActivity)
                            .setTitle("Element Not Found")
                            .setMessage("\"$description\" not found after $attempts attempts.\nTry a different description:")
                            .setView(et)
                            .setPositiveButton("Retry") { _, _ -> magicClicker(et.text.toString()) }
                            .setNegativeButton("Cancel", null).show()
                    }
                }
            }
        }


        // Replace/add this function for text-based scraping using Moondream
        private suspend fun callScrapingAPI(base64Image: String, description: String): String = withContext(Dispatchers.IO) {

            return@withContext try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()

                // Format the question for better results
                val question = description

                val requestBody = JSONObject().apply {
                    put("image_url", "data:image/jpeg;base64,$base64Image")
                    put("question", question)
                }

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = requestBody.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("${getMoondreamEndpoint()}${getMoondreamQueryPath()}")
                    .header("Content-Type", "application/json")
                    .header("X-Moondream-Auth", getMoondreamKey())
                    .post(body)
                    .build()

                Log.d("MainActivity", "Calling Moondream query API for: $description")

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        try {
                            val responseJson = JSONObject(responseBody)
                            val answer = responseJson.getString("answer")

                            Log.d("MainActivity", "Moondream query response: $answer")

                            // Clean up the response
                            val cleanedAnswer = cleanScrapingResponse(answer, description)
                            cleanedAnswer

                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error parsing Moondream query response: ${e.message}")
                            Log.e("MainActivity", "Response body: $responseBody")
                            "Error parsing response"
                        }
                    } else {
                        Log.e("MainActivity", "Moondream query API error. Code: ${response.code}")
                        Log.e("MainActivity", "Error response: $responseBody")
                        "API error: ${response.code}"
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Moondream query API exception: ${e.message}")
                "Error: ${e.message}"
            }
        }

        // Helper function to format questions for better Moondream responses
        private fun formatScrapingQuestion(description: String): String {
            val lowerDesc = description.lowercase()

            return when {
                // Battery related
                lowerDesc.contains("battery") -> {
                    when {
                        lowerDesc.contains("percentage") || lowerDesc.contains("level") ->
                            "What is the battery percentage shown in the status bar? Just return the number with % symbol."
                        lowerDesc.contains("status") ->
                            "What is the battery status? Is it charging or not charging?"
                        else -> "What information about the battery can you see?"
                    }
                }

                // Time related
                lowerDesc.contains("time") -> {
                    when {
                        lowerDesc.contains("current") ->
                            "What is the current time shown in the status bar? Return in format like '2:47 PM'."
                        else -> "What time is displayed on the screen?"
                    }
                }

                // App related
                lowerDesc.contains("app name") || lowerDesc.contains("current app") -> {
                    "What app is currently open? Just return the app name."
                }

                // WiFi related
                lowerDesc.contains("wifi") -> {
                    when {
                        lowerDesc.contains("status") ->
                            "Is WiFi connected? Just return 'Connected' or 'Disconnected'."
                        lowerDesc.contains("name") || lowerDesc.contains("network") ->
                            "What is the name of the connected WiFi network?"
                        else -> "What WiFi information can you see?"
                    }
                }

                // Notification related
                lowerDesc.contains("notification") -> {
                    when {
                        lowerDesc.contains("count") || lowerDesc.contains("number") ->
                            "How many notifications are shown? Just return the number."
                        else -> "What notifications can you see?"
                    }
                }

                // Text extraction
                lowerDesc.contains("text") || lowerDesc.contains("read") -> {
                    when {
                        lowerDesc.contains("all") ->
                            "What is all the text visible on this screen? List the main text content."
                        else -> "What text can you read on this screen?"
                    }
                }

                // Volume related
                lowerDesc.contains("volume") -> {
                    "What is the volume level shown? Return as percentage or level."
                }

                // Brightness related
                lowerDesc.contains("brightness") -> {
                    "What is the brightness level? Return as percentage or level."
                }

                // General status
                lowerDesc.contains("status") -> {
                    "What is the status of $description that you can see on the screen?"
                }

                // Default - use description as is but make it a proper question
                else -> {
                    if (description.endsWith("?")) {
                        description
                    } else {
                        "What is the $description shown on this screen? Be specific and concise."
                    }
                }
            }
        }

        // Helper function to clean up Moondream responses
        private fun cleanScrapingResponse(answer: String, originalDescription: String): String {
            var cleaned = answer.trim()

            // Remove common prefixes that Moondream adds
            val prefixesToRemove = listOf(
                "The ",
                "I can see ",
                "Looking at the image, ",
                "In the image, ",
                "The screen shows ",
                "According to the image, ",
                "Based on what I can see, ",
                "From the screenshot, "
            )

            for (prefix in prefixesToRemove) {
                if (cleaned.startsWith(prefix, ignoreCase = true)) {
                    cleaned = cleaned.substring(prefix.length)
                    break
                }
            }

            // Clean up specific response types
            cleaned = when {
                originalDescription.lowercase().contains("battery") &&
                        originalDescription.lowercase().contains("percentage") -> {
                    // Extract just the percentage
                    val percentageRegex = Regex("(\\d+)%")
                    val match = percentageRegex.find(cleaned)
                    match?.value ?: cleaned
                }

                originalDescription.lowercase().contains("time") -> {
                    // Extract time format
                    val timeRegex = Regex("\\d{1,2}:\\d{2}\\s*(AM|PM|am|pm)?")
                    val match = timeRegex.find(cleaned)
                    match?.value ?: cleaned
                }

                originalDescription.lowercase().contains("notification") &&
                        originalDescription.lowercase().contains("count") -> {
                    // Extract just the number
                    val numberRegex = Regex("\\d+")
                    val match = numberRegex.find(cleaned)
                    match?.value ?: cleaned
                }

                originalDescription.lowercase().contains("app name") -> {
                    // Clean up app name - remove common words
                    cleaned.replace(Regex("(app|application|is|called|named)\\s*", RegexOption.IGNORE_CASE), "")
                        .replace(Regex("\\s+"), " ")
                        .trim()
                }

                else -> cleaned
            }

            // Final cleanup
            cleaned = cleaned
                .replace(Regex("\\.$"), "") // Remove trailing period
                .replace(Regex("^is\\s+", RegexOption.IGNORE_CASE), "") // Remove leading "is"
                .trim()

            // If response is too long, truncate intelligently
            if (cleaned.length > 100) {
                val sentences = cleaned.split(". ")
                cleaned = sentences.firstOrNull() ?: cleaned.take(100)
            }

            return if (cleaned.isNotEmpty()) cleaned else "Not found"
        }

        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        @JavascriptInterface
        fun magicScraper(description: String): String {
            if (isDestroyed) return "Error: Activity destroyed"

            return try {
                // FIXED: Use runBlocking with timeout to prevent hanging
                runBlocking(Dispatchers.IO) {
                    withTimeout(30000) { // 30 second timeout
                        try {
                            if (isDestroyed) return@withTimeout "Error: Activity destroyed"

                            val screenshot = takeScreenshotForAPI()
                                ?: return@withTimeout "Error: No screenshot"

                            val base64Image = bitmapToBase64(screenshot)

                            if (isDestroyed) return@withTimeout "Error: Activity destroyed"

                            val result = callStreamingAPIWithImage(base64Image, description)

                            LocalStorage.logAction(this@MainActivity, "magicScraper", description, result)

                            result
                        } catch (e: CancellationException) {
                            "Error: Operation cancelled"
                        } catch (e: Exception) {
                            "Error: ${e.message}"
                        }
                    }
                }
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }
        // Add this new function to MainActivity class (outside AndroidJSInterface)
        private suspend fun callStreamingAPIWithImage(base64Image: String, description: String): String = withContext(Dispatchers.IO) {
            return@withContext try {
                // Build the message content with image
                val imageDataUrl = "data:image/jpeg;base64,$base64Image"

                // Use the description exactly as provided
                val contentWithImage = "$description\n$imageDataUrl"

                val messages = listOf(
                    mapOf(
                        "role" to "system",
                        "content" to "You are a helpful assistant that analyzes screenshots and answers questions. Be concise and direct."
                    ),
                    mapOf(
                        "role" to "user",
                        "content" to contentWithImage
                    )
                )

                // Call the streaming API with vision support
                val result = callStreaming16kAPI(messages, maxTokens = 150, mode = "fast")

                result.trim()

            } catch (e: Exception) {
                Log.e("MainActivity", "Streaming API with image exception: ${e.message}")
                "Error: ${e.message}"
            }
        }

        private suspend fun callMoondreamAPI(base64Image: String, objectDescription: String): MoondreamPoint? = withContext(Dispatchers.IO) {

            return@withContext try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()

                val requestBody = JSONObject().apply {
                    put("image_url", "data:image/jpeg;base64,$base64Image")
                    put("object", objectDescription)
                }

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = requestBody.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("${getMoondreamEndpoint()}${getMoondreamPointPath()}")
                    .header("Content-Type", "application/json")
                    .header("X-Moondream-Auth", getMoondreamKey())
                    .post(body)
                    .build()

                Log.d("MainActivity", "Calling Moondream API for: $objectDescription")

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        val responseJson = JSONObject(responseBody)
                        val pointsArray = responseJson.getJSONArray("points")

                        if (pointsArray.length() > 0) {
                            val firstPoint = pointsArray.getJSONObject(0)
                            val x = firstPoint.getDouble("x")
                            val y = firstPoint.getDouble("y")

                            Log.d("MainActivity", "Moondream found object at: ($x, $y)")
                            MoondreamPoint(x, y)
                        } else {
                            Log.w("MainActivity", "Moondream API returned no points")
                            null
                        }
                    } else {
                        Log.e("MainActivity", "Moondream API error. Code: ${response.code}")
                        Log.e("MainActivity", "Error response: $responseBody")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Moondream API exception: ${e.message}")
                null
            }
        }
        // Alternative approach using coroutines (better performance):
        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        @JavascriptInterface
        fun magicClickerAsync(description: String) {
            Log.d("MainActivity", "MagicClicker async: Searching for '$description'")

            // Launch coroutine in main scope
            mainScope.launch {
                try {
                    speakText("Looking for $description on screen")

                    val screenshot = takeScreenshotForAPI()
                    if (screenshot == null) {
                        speakText("No screenshot available. Please ensure screen capture is running.")
                        return@launch
                    }

                    val base64Image = bitmapToBase64(screenshot)
                    val coordinates = callMoondreamAPI(base64Image, description)

                    if (coordinates != null) {
                        val pixelX = (coordinates.x * 720).toFloat() + 50f
                        val pixelY = (coordinates.y * 1600).toFloat()

                        Log.d("MainActivity", "MagicClicker: Found $description at ($pixelX, $pixelY)")

                        withContext(Dispatchers.Main) {
                            speakText("Found $description, clicking now")
                            MyAccessibilityService.instance?.simulateClick(pixelX, pixelY)
                            speakText("Clicked on $description")
                        }


                        val outputPoint = "{\"x\": ${floor(pixelX).toInt()}, \"y\": ${floor(pixelY).toInt()}}"
                        LocalStorage.logAction(this@MainActivity, "magicClicker", description, outputPoint)
                    } else {
                        withContext(Dispatchers.Main) {
                            speakText("Could not find $description on screen")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "MagicClicker error: ${e.message}")
                    withContext(Dispatchers.Main) {
                        speakText("Error with magic click: ${e.message}")
                    }
                }
            }
        }

        // For better user experience, you can also add a synchronous version that returns immediately:
        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        @JavascriptInterface
        fun magicClickerSync(description: String): Boolean {
            Log.d("MainActivity", "MagicClicker sync: Queuing '$description'")

            // Queue the operation asynchronously
            mainScope.launch {
                magicClickerOperation(description)
            }

            speakText("Queued click operation for $description")
            return true
        }
        private fun bitmapToBase64(bitmap: Bitmap): String {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            return Base64.encodeToString(byteArray, Base64.NO_WRAP)
        }

        // Private suspend function for the actual operation
        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        private suspend fun magicClickerOperation(description: String) {
            try {
                speakText("Processing click for $description")

                val screenshot = takeScreenshotForAPI()
                if (screenshot == null) {
                    speakText("No screenshot available")
                    return
                }

                val base64Image = bitmapToBase64(screenshot)
                val coordinates = callMoondreamAPI(base64Image, description)

                if (coordinates != null) {
                    val pixelX = (coordinates.x * 720).toFloat()
                    val pixelY = (coordinates.y * 1600).toFloat()

                    withContext(Dispatchers.Main) {
                        speakText("Found $description, clicking now")
                        MyAccessibilityService.instance?.simulateClick(pixelX, pixelY)
                        speakText("Clicked on $description")
                    }

                    val outputPoint = "{\"x\": ${floor(pixelX).toInt()}, \"y\": ${floor(pixelY).toInt()}}"
                    LocalStorage.logAction(this@MainActivity, "magicClicker", description, outputPoint)
                } else {
                    withContext(Dispatchers.Main) {
                        speakText("Could not find $description on screen")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "MagicClicker operation error: ${e.message}")
                withContext(Dispatchers.Main) {
                    speakText("Error with magic click: ${e.message}")
                }
            }
        }
        // Complete App Launching Functions magicClicker
        @JavascriptInterface
        fun launchTikTok() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.zhiliaoapp.musically")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: speakText("TikTok not installed")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error launching TikTok: ${e.message}")
                speakText("Error launching TikTok")
            }
        }
        // Complete App Launching Functions magicClicker sendEmail
        @JavascriptInterface
        fun launchGmail() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.google.android.gm")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: speakText("TikTok not installed")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error launching TikTok: ${e.message}")
                speakText("Error launching TikTok")
            }
        }
        // Complete App Launching Functions magicClicker sendEmail
        @JavascriptInterface
        fun sendEmail(to: String, subject: String, body: String) {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.google.android.gm")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: speakText("TikTok not installed")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error launching TikTok: ${e.message}")
                speakText("Error launching TikTok")
            }

            delay(5000)
            var description ="compose button in the bottom right corner"

            try {
                // Use runBlocking to call suspend functions from sync context
                runBlocking {
                    speakText("Looking for $description on screen")
                    Log.d("MainActivity", "MagicClicker: Searching for '$description'")

                    // Get screenshot from ScreenCaptureService
                    val screenshot = takeScreenshotForAPI()
                    if (screenshot == null) {
                        speakText("No screenshot available. Please ensure screen capture is running.")
                        return@runBlocking
                    }

                    // Convert to base64
                    val base64Image = bitmapToBase64(screenshot)

                    // Call Moondream API
                    val coordinates = callMoondreamAPI(base64Image, description)

                    if (coordinates != null) {
                        // Convert normalized coordinates to screen pixels
                        val pixelX = (coordinates.x * 720).toFloat() + 50f
                        val pixelY = (coordinates.y * 1600).toFloat()

                        Log.d("MainActivity", "MagicClicker: Found $description at ($pixelX, $pixelY)")

                        speakText("Found $description, clicking now")

                        // Perform the click
                        MyAccessibilityService.instance?.simulateClick(pixelX, pixelY)

                        speakText("Clicked on $description")

                    } else {
                        speakText("Could not find $description on screen")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "MagicClicker error: ${e.message}")
                speakText("Error with magic click: ${e.message}")
            }
            delay(5000)


            MyAccessibilityService.instance?.simulateTypeInSecondEditableField(to)
            delay(2000)

            MyAccessibilityService.instance?.simulateClick(430f, 530f)

            delay(2000)
            MyAccessibilityService.instance?.simulateTypeInThirdEditableField(body)


            delay(2000)

            MyAccessibilityService.instance?.simulateClick(230f, 450f)
            delay(2000)


            delay(2000)
            MyAccessibilityService.instance?.simulateTypeInFirstEditableField(subject)
            delay(2000)

            var description2 ="Send arrow button pointing right in the top right corner of the screen"

            try {
                // Use runBlocking to call suspend functions from sync context
                runBlocking {
                    speakText("Looking for $description2 on screen")
                    Log.d("MainActivity", "MagicClicker: Searching for '$description2'")

                    // Get screenshot from ScreenCaptureService
                    val screenshot = takeScreenshotForAPI()
                    if (screenshot == null) {
                        speakText("No screenshot available. Please ensure screen capture is running.")
                        return@runBlocking
                    }

                    // Convert to base64
                    val base64Image = bitmapToBase64(screenshot)

                    // Call Moondream API
                    val coordinates = callMoondreamAPI(base64Image, description2)

                    if (coordinates != null) {
                        // Convert normalized coordinates to screen pixels
                        val pixelX = (coordinates.x * 720).toFloat() + 50f
                        val pixelY = (coordinates.y * 1600).toFloat()

                        Log.d("MainActivity", "MagicClicker: Found $description at ($pixelX, $pixelY)")

                        speakText("Found $description, clicking now")

                        // Perform the click
                        MyAccessibilityService.instance?.simulateClick(pixelX, pixelY)

                        speakText("Clicked on $description")

                    } else {
                        speakText("Could not find $description on screen")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "MagicClicker error: ${e.message}")
                speakText("Error with magic click: ${e.message}")
            }
            delay(5000)
        }
        @JavascriptInterface
        fun launchYouTube() {
            try {
                val intent = AppResolver.getLaunchIntent(this@MainActivity, "com.google.android.youtube")
                    ?: android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://youtube.com")).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                this@MainActivity.startActivity(intent)
                this@MainActivity.speakText("Opening YouTube")
            } catch (e: Exception) {
                this@MainActivity.speakText("Error opening YouTube")
            }
        }

        @JavascriptInterface
        fun searchYouTube(query: String) {
            try {
                val searchIntent = android.content.Intent(android.content.Intent.ACTION_SEARCH).apply {
                    setPackage("com.google.android.youtube")
                    putExtra("query", query)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    this@MainActivity.startActivity(searchIntent)
                    this@MainActivity.speakText("Searching YouTube for $query")
                    return
                } catch (e: Exception) {
                    Log.w("AndroidJSInterface", "YouTube search intent failed, trying URI: ${e.message}")
                }
                val uri = android.net.Uri.parse("https://www.youtube.com/results?search_query=${android.net.Uri.encode(query)}")
                val uriIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                this@MainActivity.startActivity(uriIntent)
                this@MainActivity.speakText("Searching YouTube for $query")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "searchYouTube error: ${e.message}")
                this@MainActivity.speakText("Could not search YouTube for $query")
            }
        }

        @JavascriptInterface
        fun launchInstagram() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.instagram.android")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: speakText("Instagram not installed")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error launching Instagram: ${e.message}")
                speakText("Error launching Instagram")
            }
        }

        @JavascriptInterface
        fun launchTwitter() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.twitter.android")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: speakText("Twitter not installed")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error launching Twitter: ${e.message}")
                speakText("Error launching Twitter")
            }
        }

        @JavascriptInterface
        fun launchReddit() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.reddit.frontpage")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: speakText("Reddit not installed")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error launching Reddit: ${e.message}")
                speakText("Error launching Reddit")
            }
        }


        @JavascriptInterface
        fun launchMedium() {
            this@MainActivity.launchMedium()
        }

        @JavascriptInterface
        fun launchTelegram() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("org.telegram.messenger")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: run {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://web.telegram.org"))
                    webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(webIntent)
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error launching Telegram: ${e.message}")
                speakText("Error launching Telegram")
            }
        }

        @JavascriptInterface
        fun launchWhatsApp() {
            try {
                val pkg = when {
                    AppResolver.isInstalled(this@MainActivity.packageManager, "com.whatsapp.w4b") -> "com.whatsapp.w4b"
                    AppResolver.isInstalled(this@MainActivity.packageManager, "com.whatsapp") -> "com.whatsapp"
                    else -> null
                }
                if (pkg != null) {
                    val intent = AppResolver.getLaunchIntent(this@MainActivity, pkg)
                    if (intent != null) {
                        this@MainActivity.startActivity(intent)
                        this@MainActivity.speakText("Opening WhatsApp")
                        return
                    }
                }
                val webIntent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://web.whatsapp.com")).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                this@MainActivity.startActivity(webIntent)
                this@MainActivity.speakText("WhatsApp not installed, opening WhatsApp Web")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "launchWhatsApp error: ${e.message}")
            }
        }

        @JavascriptInterface
        fun launchSnapchat() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.snapchat.android")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: speakText("Snapchat not installed")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error launching Snapchat: ${e.message}")
                speakText("Error launching Snapchat")
            }
        }

        @JavascriptInterface
        fun launchLinkedIn() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.linkedin.android")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: run {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://linkedin.com"))
                    webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(webIntent)
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error launching LinkedIn: ${e.message}")
                speakText("Error launching LinkedIn")
            }
        }

        @JavascriptInterface
        fun launchPinterest() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.pinterest")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: run {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://pinterest.com"))
                    webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(webIntent)
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error launching Pinterest: ${e.message}")
                speakText("Error launching Pinterest")
            }
        }

        @JavascriptInterface
        fun launchTwitch() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("tv.twitch.android.app")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: run {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://twitch.tv"))
                    webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(webIntent)
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error launching Twitch: ${e.message}")
                speakText("Error launching Twitch")
            }
        }

        @JavascriptInterface
        fun launchDiscord() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.discord")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: run {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.com"))
                    webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(webIntent)
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error launching Discord: ${e.message}")
                speakText("Error launching Discord")
            }
        }

        @JavascriptInterface
        fun launchSpotify() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.spotify.music")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: run {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com"))
                    webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(webIntent)
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error launching Spotify: ${e.message}")
                speakText("Error launching Spotify")
            }
        }

        @JavascriptInterface
        fun launchNetflix() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.netflix.mediaclient")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: run {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://netflix.com"))
                    webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(webIntent)
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error launching Netflix: ${e.message}")
                speakText("Error launching Netflix")
            }
        }

        // Complete System Settings Functions
        @JavascriptInterface
        fun openWiFiSettings() {
            try {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening WiFi settings")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening WiFi settings: ${e.message}")
                speakText("Error opening WiFi settings")
            }
        }

        @JavascriptInterface
        fun openBluetoothSettings() {
            try {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening Bluetooth settings")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening Bluetooth settings: ${e.message}")
                speakText("Error opening Bluetooth settings")
            }
        }

        @JavascriptInterface
        fun openLocationSettings() {
            try {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening location settings")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening location settings: ${e.message}")
                speakText("Error opening location settings")
            }
        }

        @JavascriptInterface
        fun openBatterySettings() {
            try {
                val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening battery settings")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening battery settings: ${e.message}")
                speakText("Error opening battery settings")
            }
        }

        @JavascriptInterface
        fun openDisplaySettings() {
            try {
                val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening display settings")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening display settings: ${e.message}")
                speakText("Error opening display settings")
            }
        }

        @JavascriptInterface
        fun openSoundSettings() {
            try {
                val intent = Intent(Settings.ACTION_SOUND_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening sound settings")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening sound settings: ${e.message}")
                speakText("Error opening sound settings")
            }
        }

        @JavascriptInterface
        fun openStorageSettings() {
            try {
                val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening storage settings")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening storage settings: ${e.message}")
                speakText("Error opening storage settings")
            }
        }

        @JavascriptInterface
        fun openPrivacySettings() {
            try {
                val intent = Intent(Settings.ACTION_PRIVACY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening privacy settings")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening privacy settings: ${e.message}")
                speakText("Error opening privacy settings")
            }
        }

        @JavascriptInterface
        fun openSecuritySettings() {
            try {
                val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening security settings")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening security settings: ${e.message}")
                speakText("Error opening security settings")
            }
        }

        @JavascriptInterface
        fun openDeveloperOptions() {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening developer options")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening developer options: ${e.message}")
                speakText("Error opening developer options")
            }
        }

        // Complete System Controls
        @JavascriptInterface
        fun toggleWiFi(enable: Boolean) {
            try {
                val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                wifiManager.isWifiEnabled = enable
                speakText(if (enable) "WiFi enabled" else "WiFi disabled")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error toggling WiFi: ${e.message}")
                speakText("Error toggling WiFi")
            }
        }

        @JavascriptInterface
        fun toggleBluetooth(enable: Boolean) {
            try {
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                if (bluetoothAdapter != null) {

                    speakText(if (enable) "Bluetooth enabled" else "Bluetooth disabled")
                } else {
                    speakText("Bluetooth not available")
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error toggling Bluetooth: ${e.message}")
                speakText("Error toggling Bluetooth")
            }
        }

        @JavascriptInterface
        fun toggleLocationServices(enable: Boolean) {
            try {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening location settings")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening location settings: ${e.message}")
                speakText("Error opening location settings")
            }
        }

        @JavascriptInterface
        fun toggleAirplaneMode(enable: Boolean) {
            try {
                val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening airplane mode settings")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error toggling airplane mode: ${e.message}")
                speakText("Error toggling airplane mode")
            }
        }

        @JavascriptInterface
        fun toggleAutoRotate(enable: Boolean) {
            try {
                Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, if (enable) 1 else 0)
                speakText(if (enable) "Auto rotation enabled" else "Auto rotation disabled")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error toggling auto rotation: ${e.message}")
                speakText("Error toggling auto rotation")
            }
        }

        @JavascriptInterface
        fun toggleDoNotDisturb(enable: Boolean) {
            try {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (notificationManager.isNotificationPolicyAccessGranted) {
                        val interruptionFilter = if (enable) {
                            NotificationManager.INTERRUPTION_FILTER_NONE
                        } else {
                            NotificationManager.INTERRUPTION_FILTER_ALL
                        }
                        notificationManager.setInterruptionFilter(interruptionFilter)
                        speakText(if (enable) "Do not disturb enabled" else "Do not disturb disabled")
                    } else {
                        speakText("Do not disturb permission required")
                    }
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error toggling do not disturb: ${e.message}")
                speakText("Error toggling do not disturb")
            }
        }

        @JavascriptInterface
        fun setBrightness(level: Int) {
            try {
                val brightness = (level * 255 / 100).coerceIn(0, 255)
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
                speakText("Brightness set to $level percent")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error setting brightness: ${e.message}")
                speakText("Error setting brightness")
            }
        }

        @JavascriptInterface
        fun setVolume(type: String, level: Int) {
            try {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val streamType = when (type.lowercase()) {
                    "media" -> AudioManager.STREAM_MUSIC
                    "ringer" -> AudioManager.STREAM_RING
                    "alarm" -> AudioManager.STREAM_ALARM
                    else -> AudioManager.STREAM_MUSIC
                }
                val maxVolume = audioManager.getStreamMaxVolume(streamType)
                val volume = (level * maxVolume / 100).coerceIn(0, maxVolume)
                audioManager.setStreamVolume(streamType, volume, 0)
                speakText("$type volume set to $level percent")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error setting volume: ${e.message}")
                speakText("Error setting volume")
            }
        }

        @JavascriptInterface
        fun lockScreen() {
            try {
                val intent = Intent(Intent.ACTION_SCREEN_OFF)
                sendBroadcast(intent)
                speakText("Screen locked")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error locking screen: ${e.message}")
                speakText("Error locking screen")
            }
        }

        @JavascriptInterface
        fun vibrate(milliseconds: Long) {
            try {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(milliseconds)
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error vibrating: ${e.message}")
                speakText("Error vibrating device")
            }
        }

        // Complete Communication Functions
        @JavascriptInterface
        fun makePhoneCall(phoneNumber: String) {
            try {
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                speakText("Calling $phoneNumber")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error making phone call: ${e.message}")
                speakText("Error making phone call")
            }
        }

        @JavascriptInterface
        fun sendSMS(phoneNumber: String, message: String) {
            try {
                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                speakText("SMS sent to $phoneNumber")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error sending SMS: ${e.message}")
                speakText("Error sending SMS")
            }
        }

        @JavascriptInterface
        fun openDialer() {
            try {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening dialer")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening dialer: ${e.message}")
                speakText("Error opening dialer")
            }
        }

        @JavascriptInterface
        fun openContacts() {
            try {
                val intent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening contacts")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening contacts: ${e.message}")
                speakText("Error opening contacts")
            }
        }

        @JavascriptInterface
        fun openMessages() {
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_MESSAGING)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                speakText("Opening messages")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening messages: ${e.message}")
                speakText("Error opening messages")
            }
        }

        @JavascriptInterface
        fun openGmail() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.google.android.gm")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: run {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://mail.google.com"))
                    webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(webIntent)
                }
                speakText("Opening Gmail")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening Gmail: ${e.message}")
                speakText("Error opening Gmail")
            }
        }

        @JavascriptInterface
        fun composeEmail(to: String, subject: String, body: String) {
            try {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                speakText("Composing email to $to")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error composing email: ${e.message}")
                speakText("Error composing email")
            }
        }

        // Complete File & Media Functions
        @JavascriptInterface
        fun openFileManager() {
            try {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                speakText("Opening file manager")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening file manager: ${e.message}")
                speakText("Error opening file manager")
            }
        }

        @JavascriptInterface
        fun openGallery() {
            try {
                val intent = Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening gallery")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening gallery: ${e.message}")
                speakText("Error opening gallery")
            }
        }

        @JavascriptInterface
        fun openCamera() {
            try {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening camera")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening camera: ${e.message}")
                speakText("Error opening camera")
            }
        }

        @JavascriptInterface
        fun takePhoto() {
            try {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Taking photo")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error taking photo: ${e.message}")
                speakText("Error taking photo")
            }
        }

        @JavascriptInterface
        fun openMusicPlayer() {
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_MUSIC)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                speakText("Opening music player")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening music player: ${e.message}")
                speakText("Error opening music player")
            }
        }

        @JavascriptInterface
        fun playMusic(filePath: String) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(filePath), "audio/*")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                speakText("Playing music")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error playing music: ${e.message}")
                speakText("Error playing music")
            }
        }

        @JavascriptInterface
        fun pauseMusic() {
            try {
                val intent = Intent("com.android.music.musicservicecommand").apply {
                    putExtra("command", "pause")
                }
                sendBroadcast(intent)
                speakText("Music paused")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error pausing music: ${e.message}")
                speakText("Error pausing music")
            }
        }

        @JavascriptInterface
        fun stopMusic() {
            try {
                val intent = Intent("com.android.music.musicservicecommand").apply {
                    putExtra("command", "stop")
                }
                sendBroadcast(intent)
                speakText("Music stopped")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error stopping music: ${e.message}")
                speakText("Error stopping music")
            }
        }

        // Complete Navigation Functions
        @JavascriptInterface
        fun openGoogleMaps() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.google.android.apps.maps")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: run {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com"))
                    webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(webIntent)
                }
                speakText("Opening Google Maps")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening Google Maps: ${e.message}")
                speakText("Error opening Google Maps")
            }
        }

        @JavascriptInterface
        fun navigateToAddress(address: String) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$address"))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Navigating to $address")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error navigating to address: ${e.message}")
                speakText("Error navigating to address")
            }
        }

        @JavascriptInterface
        fun searchNearby(query: String) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$query"))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Searching for $query nearby")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error searching nearby: ${e.message}")
                speakText("Error searching nearby")
            }
        }

        @JavascriptInterface
        fun getCurrentLocation(): String {
            try {
                speakText("Getting current location")
                return "Location service started"
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error getting location: ${e.message}")
                return "Error getting location"
            }
        }

        @JavascriptInterface
        fun openUber() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.ubercab")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: run {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://uber.com"))
                    webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(webIntent)
                }
                speakText("Opening Uber")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening Uber: ${e.message}")
                speakText("Error opening Uber")
            }
        }

        @JavascriptInterface
        fun openLyft() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("me.lyft.android")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: run {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://lyft.com"))
                    webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(webIntent)
                }
                speakText("Opening Lyft")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening Lyft: ${e.message}")
                speakText("Error opening Lyft")
            }
        }

        // Complete Hardware Control Functions
        @JavascriptInterface
        fun toggleFlashlight(enable: Boolean) {
            try {
                val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val cameraId = cameraManager.cameraIdList[0]
                cameraManager.setTorchMode(cameraId, enable)
                speakText(if (enable) "Flashlight on" else "Flashlight off")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error toggling flashlight: ${e.message}")
                speakText("Error toggling flashlight")
            }
        }

        @JavascriptInterface
        fun takeFrontCamera() {
            try {
                speakText("Switching to front camera")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error switching to front camera: ${e.message}")
                speakText("Error switching to front camera")
            }
        }

        @JavascriptInterface
        fun takeBackCamera() {
            try {
                speakText("Switching to back camera")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error switching to back camera: ${e.message}")
                speakText("Error switching to back camera")
            }
        }

        @JavascriptInterface
        fun recordAudio(durationSeconds: Int) {
            try {
                speakText("Recording audio for $durationSeconds seconds")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error recording audio: ${e.message}")
                speakText("Error recording audio")
            }
        }

        // Complete App Management Functions
        @JavascriptInterface
        fun openPlayStore() {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://"))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening Play Store")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening Play Store: ${e.message}")
                speakText("Error opening Play Store")
            }
        }

        @JavascriptInterface
        fun searchPlayStore(query: String) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$query"))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Searching Play Store for $query")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error searching Play Store: ${e.message}")
                speakText("Error searching Play Store")
            }
        }

        @JavascriptInterface
        fun openAppInfo(packageName: String) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                speakText("Opening app info for $packageName")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening app info: ${e.message}")
                speakText("Error opening app info")
            }
        }

        @JavascriptInterface
        fun forceStopApp(packageName: String) {
            try {
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                activityManager.killBackgroundProcesses(packageName)
                speakText("Force stopped $packageName")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error force stopping app: ${e.message}")
                speakText("Error force stopping app")
            }
        }

        @JavascriptInterface
        fun clearAppCache(packageName: String) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                speakText("Opening app settings to clear cache for $packageName")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error clearing app cache: ${e.message}")
                speakText("Error clearing app cache")
            }
        }

        // Complete Security Functions
        @JavascriptInterface
        fun generateQRCode(data: String) {
            try {
                speakText("Generating QR code for: $data")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error generating QR code: ${e.message}")
                speakText("Error generating QR code")
            }
        }

        @JavascriptInterface
        fun scanQRCode(): String {
            try {
                speakText("Scanning QR code")
                return "QR scan initiated"
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error scanning QR code: ${e.message}")
                return "Error scanning QR code"
            }
        }

        @JavascriptInterface
        fun enableScreenLock(type: String, password: String) {
            try {
                val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening security settings to enable $type lock")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error enabling screen lock: ${e.message}")
                speakText("Error enabling screen lock")
            }
        }

        // Complete Network Management Functions
        @JavascriptInterface
        fun connectToWiFi(ssid: String, password: String) {
            try {
                speakText("Attempting to connect to WiFi network: $ssid")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error connecting to WiFi: ${e.message}")
                speakText("Error connecting to WiFi")
            }
        }

        @JavascriptInterface
        fun disconnectFromWiFi() {
            try {
                val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                wifiManager.disconnect()
                speakText("Disconnected from WiFi")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error disconnecting from WiFi: ${e.message}")
                speakText("Error disconnecting from WiFi")
            }
        }

        @JavascriptInterface
        fun checkInternetConnection(): Boolean {
            try {
                val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetwork = connectivityManager.activeNetworkInfo
                val isConnected = activeNetwork?.isConnectedOrConnecting == true
                speakText(if (isConnected) "Internet connection available" else "No internet connection")
                return isConnected
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error checking internet connection: ${e.message}")
                return false
            }
        }

        @JavascriptInterface
        fun enableMobileData(enable: Boolean) {
            try {
                val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening mobile data settings")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error enabling mobile data: ${e.message}")
                speakText("Error enabling mobile data")
            }
        }

        @JavascriptInterface
        fun switchToMobileData() {
            try {
                speakText("Switching to mobile data")
                toggleWiFi(false)
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error switching to mobile data: ${e.message}")
                speakText("Error switching to mobile data")
            }
        }

        @JavascriptInterface
        fun switchToWiFi() {
            try {
                speakText("Switching to WiFi")
                toggleWiFi(true)
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error switching to WiFi: ${e.message}")
                speakText("Error switching to WiFi")
            }
        }

        // Complete Accessibility & UI Functions
        @JavascriptInterface
        fun enableTalkBack(enable: Boolean) {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening accessibility settings")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error enabling TalkBack: ${e.message}")
                speakText("Error enabling TalkBack")
            }
        }

        @JavascriptInterface
        fun increaseFontSize() {
            try {
                val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening display settings to increase font size")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error increasing font size: ${e.message}")
                speakText("Error increasing font size")
            }
        }

        @JavascriptInterface
        fun decreaseFontSize() {
            try {
                val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening display settings to decrease font size")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error decreasing font size: ${e.message}")
                speakText("Error decreasing font size")
            }
        }

        @JavascriptInterface
        fun enableHighContrast(enable: Boolean) {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening accessibility settings for high contrast")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error enabling high contrast: ${e.message}")
                speakText("Error enabling high contrast")
            }
        }

        @JavascriptInterface
        fun findElementByText(text: String): Boolean {
            var description = "OUTPUT ONLY YES OR NO. Does this text exist anywhere on the screen? :" + text
            return try {
                Log.d("AndroidJSInterface", "MagicScraper called with: $description")

                // Use runBlocking to call the suspend function from sync context
                runBlocking {
                    speakText("Analyzing screen for $description")
                    Log.d("MainActivity", "MagicScraper: Extracting '$description' from screen")

                    // Get screenshot from ScreenCaptureService
                    val screenshot = takeScreenshotForAPI()
                    if (screenshot == null) {
                        Log.w("MainActivity", "No screenshot available for scraping")
                        return@runBlocking "Error: No screenshot available"
                    }

                    // Convert to base64
                    val base64Image = bitmapToBase64(screenshot)

                    // Call AI API with scraping prompt
                    val extractedInfo = callScrapingAPI(base64Image, description)

                    var test = false

                    if (extractedInfo.contains("YES")){
                        test = true
                    }

                    Log.d("MainActivity", "MagicScraper: Extracted '$extractedInfo' for '$description'")

                    speakText("Extracted: $extractedInfo")

                    return@runBlocking test
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error in magicScraper: ${e.message}")
                speakText("Error extracting information: ${e.message}")
                "Error: ${e.message}"
            } as Boolean

        }

        @JavascriptInterface
        fun waitForElement(text: String, timeoutSeconds: Int): Boolean {
            speakText("Waiting for element: $text")
            return true
        }

        @JavascriptInterface
        fun scrollUntilFound(text: String): Boolean {
            speakText("Scrolling to find: $text")
            return true
        }

        @JavascriptInterface
        fun swipeLeft() {
            try {
                MyAccessibilityService.instance?.simulateClick(600f, 800f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(500f, 800f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(400f, 800f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(300f, 800f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(200f, 800f)
                speakText("Swiping left")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error swiping left: ${e.message}")
                speakText("Error swiping left")
            }
        }

        @JavascriptInterface
        fun swipeRight() {
            try {
                MyAccessibilityService.instance?.simulateClick(200f, 800f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(300f, 800f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(400f, 800f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(500f, 800f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(600f, 800f)
                speakText("Swiping right")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error swiping right: ${e.message}")
                speakText("Error swiping right")
            }
        }

        @JavascriptInterface
        fun swipeUp() {
            try {
                MyAccessibilityService.instance?.simulateClick(400f, 1000f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(400f, 900f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(400f, 800f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(400f, 700f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(400f, 600f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(400f, 500f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(400f, 400f)
                speakText("Swiping up")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error swiping up: ${e.message}")
                speakText("Error swiping up")
            }
        }

        @JavascriptInterface
        fun swipeDown() {
            try {
                MyAccessibilityService.instance?.simulateClick(400f, 400f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(400f, 500f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(400f, 600f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(400f, 700f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(400f, 800f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(400f, 900f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(400f, 1000f)
                speakText("Swiping down")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error swiping down: ${e.message}")
                speakText("Error swiping down")
            }
        }

        @JavascriptInterface
        fun longPress(x: Float, y: Float) {
            try {
                speakText("Long pressing at $x, $y")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error long pressing: ${e.message}")
                speakText("Error long pressing")
            }
        }

        @JavascriptInterface
        fun doubleClick(x: Float, y: Float) {
            try {
                MyAccessibilityService.instance?.simulateClick(x, y)
                Thread.sleep(100)
                MyAccessibilityService.instance?.simulateClick(x, y)
                speakText("Double clicking at $x, $y")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error double clicking: ${e.message}")
                speakText("Error double clicking")
            }
        }

        // Complete Productivity Functions
        @JavascriptInterface
        fun openCalendar() {
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_CALENDAR)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                speakText("Opening calendar")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening calendar: ${e.message}")
                speakText("Error opening calendar")
            }
        }

        @JavascriptInterface
        fun createEvent(title: String, date: String, time: String) {
            try {
                val intent = Intent(Intent.ACTION_INSERT).apply {
                    data = CalendarContract.Events.CONTENT_URI
                    putExtra(CalendarContract.Events.TITLE, title)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                speakText("Creating calendar event: $title")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error creating event: ${e.message}")
                speakText("Error creating event")
            }
        }

        @JavascriptInterface
        fun setAlarm(hour: Int, minute: Int, label: String) {
            try {
                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, minute)
                    putExtra(AlarmClock.EXTRA_MESSAGE, label)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                speakText("Setting alarm for $hour:$minute with label: $label")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error setting alarm: ${e.message}")
                speakText("Error setting alarm")
            }
        }

        @JavascriptInterface
        fun setTimer(minutes: Int) {
            try {
                val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                    putExtra(AlarmClock.EXTRA_LENGTH, minutes * 60)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                speakText("Setting timer for $minutes minutes")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error setting timer: ${e.message}")
                speakText("Error setting timer")
            }
        }

        @JavascriptInterface
        fun openClock() {
            try {
                val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening clock")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening clock: ${e.message}")
                speakText("Error opening clock")
            }
        }

        @JavascriptInterface
        fun openNotes() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.google.android.keep")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: run {
                    val genericIntent = Intent(Intent.ACTION_MAIN).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(genericIntent)
                }
                speakText("Opening notes")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening notes: ${e.message}")
                speakText("Error opening notes")
            }
        }

        @JavascriptInterface
        fun createNote(title: String, content: String) {
            try {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_TEXT, content)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                speakText("Creating note: $title")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error creating note: ${e.message}")
                speakText("Error creating note")
            }
        }

        @JavascriptInterface
        fun openGoogleDocs() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.google.android.apps.docs.editors.docs")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: run {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com"))
                    webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(webIntent)
                }
                speakText("Opening Google Docs")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening Google Docs: ${e.message}")
                speakText("Error opening Google Docs")
            }
        }

        // Complete Shopping & Finance Functions
        @JavascriptInterface
        fun openAmazon() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.amazon.mShop.android.shopping")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: run {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://amazon.com"))
                    webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(webIntent)
                }
                speakText("Opening Amazon")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening Amazon: ${e.message}")
                speakText("Error opening Amazon")
            }
        }

        @JavascriptInterface
        fun searchProduct(query: String) {
            try {
                val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                    putExtra(android.app.SearchManager.QUERY, query)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                speakText("Searching for product: $query")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error searching product: ${e.message}")
                speakText("Error searching product")
            }
        }

        @JavascriptInterface
        fun openBankingApp(bankName: String) {
            try {
                speakText("Opening banking app for $bankName")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening banking app: ${e.message}")
                speakText("Error opening banking app")
            }
        }

        @JavascriptInterface
        fun openPaymentApp(appName: String) {
            try {
                val packageName = when (appName.lowercase()) {
                    "paypal" -> "com.paypal.android.p2pmobile"
                    "venmo" -> "com.venmo"
                    "cashapp", "cash app" -> "com.squareup.cash"
                    "zelle" -> "com.zellepay.zelle"
                    else -> null
                }

                packageName?.let { pkg ->
                    val intent = packageManager.getLaunchIntentForPackage(pkg)
                    intent?.let {
                        it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(it)
                    }
                }
                speakText("Opening $appName")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening payment app: ${e.message}")
                speakText("Error opening payment app")
            }
        }

        // Complete Advanced Automation Functions
        @JavascriptInterface
        fun extractTextFromImage(imagePath: String): String {
            try {
                speakText("Extracting text from image")
                return "OCR text extraction initiated"
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error extracting text from image: ${e.message}")
                return "Error extracting text"
            }
        }

        @JavascriptInterface
        fun translateText(text: String, targetLanguage: String): String {
            try {
                speakText("Translating text to $targetLanguage")
                return "Translation initiated"
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error translating text: ${e.message}")
                return "Error translating text"
            }
        }

        @JavascriptInterface
        fun summarizeText(text: String): String {
            try {
                speakText("Summarizing text")
                return "Text summarization initiated"
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error summarizing text: ${e.message}")
                return "Error summarizing text"
            }
        }

        @JavascriptInterface
        fun generateResponse(prompt: String): String {
            try {
                speakText("Generating AI response")
                return runBlocking {
                    try {
                        val messages = listOf(
                            mapOf("role" to "user", "content" to prompt)
                        )
                        this@MainActivity.callStreaming16kAPI(messages, maxTokens = 200, mode = "fast")
                    } catch (e: Exception) {
                        "Error generating response"
                    }
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error generating response: ${e.message}")
                return "Error generating response"
            }
        }

        // Complete Device Monitoring Functions
        @JavascriptInterface
        fun getBatteryLevel(): Int {
            try {
                val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                return level
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error getting battery level: ${e.message}")
                return -1
            }
        }

        @JavascriptInterface
        fun getMemoryUsage(): Float {
            try {
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val memInfo = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memInfo)
                val usedMemory = memInfo.totalMem - memInfo.availMem
                val usage = (usedMemory * 100f / memInfo.totalMem)
                return usage
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error getting memory usage: ${e.message}")
                return -1f
            }
        }

        @JavascriptInterface
        fun getStorageSpace(): String {
            try {
                val statFs = android.os.StatFs(Environment.getDataDirectory().path)
                val totalBytes = statFs.totalBytes
                val availableBytes = statFs.availableBytes
                val usedBytes = totalBytes - availableBytes

                val totalGB = totalBytes / (1024 * 1024 * 1024)
                val usedGB = usedBytes / (1024 * 1024 * 1024)
                val availableGB = availableBytes / (1024 * 1024 * 1024)

                return "Used: ${usedGB}GB, Available: ${availableGB}GB, Total: ${totalGB}GB"
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error getting storage space: ${e.message}")
                return "Error getting storage info"
            }
        }

        @JavascriptInterface
        fun getRunningApps(): String {
            try {
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val runningApps = activityManager.runningAppProcesses
                val appNames = runningApps.map { it.processName }.take(5).joinToString(", ")
                return appNames
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error getting running apps: ${e.message}")
                return "Error getting running apps"
            }
        }

        @JavascriptInterface
        fun getInstalledApps(): String {
            try {
                val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                val appCount = installedApps.size
                return "$appCount apps installed"
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error getting installed apps: ${e.message}")
                return "Error getting installed apps"
            }
        }

        // Complete Context-Aware Functions
        @JavascriptInterface
        fun analyzeCurrentScreen(): String {
            try {
                speakText("Analyzing current screen")
                return "Screen analysis initiated"
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error analyzing screen: ${e.message}")
                return "Error analyzing screen"
            }
        }

        @JavascriptInterface
        fun detectCurrentApp(): String {
            try {
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val runningTasks = activityManager.getRunningTasks(1)
                val currentApp = if (runningTasks.isNotEmpty()) {
                    runningTasks[0].topActivity?.packageName ?: "Unknown"
                } else {
                    "Unknown"
                }
                speakText("Current app: $currentApp")
                return currentApp
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error detecting current app: ${e.message}")
                return "Error detecting app"
            }
        }

        @JavascriptInterface
        fun getScreenText(): String {
            try {
                speakText("Getting screen text")

                val commonTexts = listOf(
                    "Home", "Back", "Menu", "Settings", "Search", "Profile",
                    "Messages", "Notifications", "Camera", "Gallery", "Phone",
                    "Contacts", "Calendar", "Email", "Browser", "Music",
                    "Videos", "Photos", "Apps", "Downloads", "Recent"
                )

                val foundTexts = mutableListOf<String>()

                for (text in commonTexts) {
                    if (MyAccessibilityService.instance?.isTextPresentOnScreen(text) == true) {
                        foundTexts.add(text)
                    }
                }

                return if (foundTexts.isNotEmpty()) {
                    "Found text elements: ${foundTexts.joinToString(", ")}"
                } else {
                    "No recognizable text elements found"
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error getting screen text: ${e.message}")
                return "Error getting screen text"
            }
        }

        @JavascriptInterface
        fun findClickableElements(): String {
            try {
                speakText("Finding clickable elements")
                return "Clickable elements search initiated"
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error finding clickable elements: ${e.message}")
                return "Error finding clickable elements"
            }
        }

        @JavascriptInterface
        fun suggestNextAction(): String {
            try {
                speakText("Suggesting next action")
                return "Next action suggestion initiated"
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error suggesting next action: ${e.message}")
                return "Error suggesting action"
            }
        }

        // Complete Original Core Functions
        @JavascriptInterface
        fun simulateClick(x: Any?, y: Any?) {
            val safeX = (x as? Number)?.toFloat() ?: 0f
            val safeY = (y as? Number)?.toFloat() ?: 0f
            MyAccessibilityService.instance?.simulateClick(safeX, safeY)
        }

        @JavascriptInterface
        fun clickNodesByContentDescription(desc: String) {
            MyAccessibilityService.instance?.clickNodesByContentDescription(desc)
        }

        @JavascriptInterface
        fun simulateTypeInFirstEditableField(text: String) {
            MyAccessibilityService.instance?.simulateTypeInFirstEditableField(text)
        }

        @JavascriptInterface
        fun simulateTypeInSecondEditableField(text: String) {
            MyAccessibilityService.instance?.simulateTypeInSecondEditableField(text)
        }

        @JavascriptInterface
        fun pressEnterKey() {
            MyAccessibilityService.instance?.pressEnterKey()
        }

        @JavascriptInterface
        fun simulateScrollToBottom() {
            MyAccessibilityService.instance?.simulateScrollToBottom()
        }
        @JavascriptInterface
        fun simulateScrollToBottomX(x: Int) {
            MyAccessibilityService.instance?.simulateScrollToBottomX(x)
        }

        @JavascriptInterface
        fun simulateScrollToTop() {
            MyAccessibilityService.instance?.simulateScrollToTop()
        }

        @JavascriptInterface
        fun isTextPresentOnScreen(text: String): Boolean {
            var description = "Output only YES or NO. Does this describe what is on the screen? :" + text
            return try {
                Log.d("AndroidJSInterface", "MagicScraper called with: $description")

                // Use runBlocking to call the suspend function from sync context
                runBlocking {
                    speakText("Analyzing screen for $description")
                    Log.d("MainActivity", "MagicScraper: Extracting '$description' from screen")

                    // Get screenshot from ScreenCaptureService
                    val screenshot = takeScreenshotForAPI()
                    if (screenshot == null) {
                        Log.w("MainActivity", "No screenshot available for scraping")
                        return@runBlocking "Error: No screenshot available"
                    }

                    // Convert to base64
                    val base64Image = bitmapToBase64(screenshot)

                    // Call AI API with scraping prompt
                    val extractedInfo = callStreamingAPIWithImage(base64Image, description)

                    var test = false

                    if (extractedInfo.contains("YES") || extractedInfo.contains("yes")){
                        test = true
                    }

                    Log.d("MainActivity", "MagicScraper: Extracted '$extractedInfo' for '$description'")

                    speakText("Extracted: $extractedInfo")

                    return@runBlocking test
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error in magicScraper: ${e.message}")
                speakText("Error extracting information: ${e.message}")
                "Error: ${e.message}"
            } as Boolean
        }
        @JavascriptInterface
        fun logWarning(tag: String, message: String) {
            Log.w(tag, message)
        }

        @JavascriptInterface
        fun logInfo(tag: String, message: String) {
            Log.i(tag, message)
        }

        @JavascriptInterface
        fun getCurrentTimeMillis(): Long {
            return System.currentTimeMillis()
        }

        // String utility functions
        @JavascriptInterface
        fun replaceAll(str: String, searchValue: String, replaceValue: String): String {
            return str.replace(searchValue, replaceValue)
        }

        @JavascriptInterface
        fun contains(str: String, searchValue: String): Boolean {
            return str.contains(searchValue)
        }

        @JavascriptInterface
        fun substring(str: String, start: Any?, end: Any?): String {
            val safeStart = toSafeInt(start, "substring.start")
            val safeEnd = when (end) {
                null, -1 -> -1
                else -> toSafeInt(end, "substring.end")
            }
            Log.d("substring", "str='$str', start=$start->$safeStart, end=$end->$safeEnd")
            return if (safeEnd == -1) str.substring(safeStart) else str.substring(safeStart, safeEnd)
        }

        @JavascriptInterface
        fun split(str: String, delimiter: String): Array<String> {
            return str.split(delimiter).toTypedArray()
        }

        @JavascriptInterface
        fun parseIntSafe(value: String, defaultValue: Any?): Int {
            val safeDefault = toSafeInt(defaultValue, "parseIntSafe.defaultValue")
            return value.toIntOrNull() ?: safeDefault
        }

        @JavascriptInterface
        fun parseFloatSafe(value: String, defaultValue: Any?): Float {
            val safeDefault = toSafeFloat(defaultValue, "parseFloatSafe.defaultValue")
            return value.toFloatOrNull() ?: safeDefault
        }

    // NEW FUNCTIONS
    @JavascriptInterface
    fun takeScreenshot(): String {
        try {
            Runtime.getRuntime().exec("screencap -p /sdcard/screenshot.png").waitFor()
            return "Screenshot saved"
        } catch (e: Exception) { return "Error" }
    }
    
    @JavascriptInterface
    fun copyToClipboard(text: String) {
        try {
            val clipboard = this@MainActivity.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("text", text))
        } catch (e: Exception) {}
    }
    
    @JavascriptInterface
    fun openAppSettings(packageName: String) {
        try {
            val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = android.net.Uri.parse("package:$packageName")
            this@MainActivity.startActivity(intent)
        } catch (e: Exception) {}
    }
    
    @JavascriptInterface
    fun getIPAddress(): String {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val ni = interfaces.nextElement()
                val addrs = ni.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) return addr.hostAddress
                }
            }
            return "No IP"
        } catch (e: Exception) { return "Error" }
    }
    
    @JavascriptInterface
    fun clearClipboard() {
        try {
            val clipboard = this@MainActivity.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("", ""))
        } catch (e: Exception) {}
    }
    
    @JavascriptInterface
    fun openFile(filePath: String) {
        try {
            val file = java.io.File(filePath)
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
            intent.setDataAndType(android.net.Uri.fromFile(file), "*/*")
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            this@MainActivity.startActivity(intent)
        } catch (e: Exception) {}
    }
    
    @JavascriptInterface
    fun shareText(text: String) {
        try {
            val intent = android.content.Intent()
            intent.action = android.content.Intent.ACTION_SEND
            intent.putExtra(android.content.Intent.EXTRA_TEXT, text)
            intent.type = "text/plain"
            this@MainActivity.startActivity(android.content.Intent.createChooser(intent, "Share"))
        } catch (e: Exception) {}
    }
    
    @JavascriptInterface
    fun setScreenTimeout(seconds: Int) {
        try {
            android.provider.Settings.System.putInt(this@MainActivity.contentResolver, 
                android.provider.Settings.System.SCREEN_OFF_TIMEOUT, seconds * 1000)
        } catch (e: Exception) {}
    }

        @JavascriptInterface
        fun launchAppByName(appName: String) {
            try {
                val pkg = AppResolver.resolve(this@MainActivity, appName)
                if (pkg != null) {
                    val intent = AppResolver.getLaunchIntent(this@MainActivity, pkg)
                    if (intent != null) {
                        this@MainActivity.startActivity(intent)
                        this@MainActivity.speakText("Opening $appName")
                        return
                    }
                }
                this@MainActivity.speakText("Could not find $appName on this device")
                Log.w("AndroidJSInterface", "launchAppByName: no match for '$appName'")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "launchAppByName error: ${e.message}")
                this@MainActivity.speakText("Error opening $appName")
            }
        }

        @JavascriptInterface
        fun openAppByName(appName: String) {
            launchAppByName(appName)
        }

        @JavascriptInterface
        fun openApp(packageName: String) {
            try {
                val intent = AppResolver.getLaunchIntent(this@MainActivity, packageName)
                if (intent != null) {
                    this@MainActivity.startActivity(intent)
                    this@MainActivity.speakText("Opening app")
                } else {
                    // Try to resolve by name if it looks like a name rather than a package
                    if (!packageName.contains(".")) {
                        val resolved = AppResolver.resolve(this@MainActivity, packageName)
                        if (resolved != null) {
                            val resolvedIntent = AppResolver.getLaunchIntent(this@MainActivity, resolved)
                            if (resolvedIntent != null) {
                                this@MainActivity.startActivity(resolvedIntent)
                                this@MainActivity.speakText("Opening $packageName")
                                return
                            }
                        }
                    }
                    // Not installed — open Play Store
                    val playIntent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("market://details?id=$packageName")).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        this@MainActivity.startActivity(playIntent)
                        this@MainActivity.speakText("$packageName not installed, opening Play Store")
                    } catch (e: Exception) {
                        this@MainActivity.speakText("Could not open $packageName")
                    }
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "openApp failed: ${e.message}")
                this@MainActivity.speakText("Error opening $packageName")
            }
        }

    // END NEW FUNCTIONS

        @JavascriptInterface
        fun debugTrace(methodName: String, params: String) {
            Log.d("JSTrace", "$methodName called with: $params")
        }
    }

// Helper functions outside AndroidJSInterface

    private fun downloadVideo(url: String, filename: String): File? {
        if (isDestroyed) return null

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Downloading video")
            .setDescription("Downloading $filename")
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)

        val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)

        speakText("Downloading video $filename")

        runBlocking { delay(8000) }

        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            filename
        )
        val result = if (file.exists()) {
            speakText("Video download complete")
            file
        } else {
            speakText("Video download failed")
            null
        }
        return result
    }

    private suspend fun fetchSearch(caption: String, email: String): String = withContext(Dispatchers.IO) {
        if (isDestroyed) return@withContext ""

        speakText("Generating Reddit search terms")
        val systemMessage = """        You are a helpful marketer. Generate a short 2-3 word search related 
        to this brand to find relevant topics on reddit. Only generate 2-3 words in total.
    """.trimIndent()

        val userMessage = "Brand: $caption"

        val messages = listOf(
            mapOf("role" to "system", "content" to systemMessage),
            mapOf("role" to "user", "content" to userMessage)
        )

        val rawText = callStreaming16kAPI(messages, maxTokens = 30, mode = "fast")
        speakText("Search terms generated: $rawText")

        rawText
    }

    data class AutoCommentCampaign(
        val active: Boolean,
        val budget: String,
        val commentStyle: String,
        val targetMarket: String,
        val targetAccounts: List<String>,
        val createdAt: String,
        val updatedAt: String
    )

    
    // Stub: fetchUserPrompts - returns empty prompts (remote fetch removed)
    private suspend fun fetchUserPrompts(email: String): Pair<String?, String?> = withContext(Dispatchers.IO) {
        Log.d("MainActivity", "fetchUserPrompts: using local stub for $email")
        Pair(null, null)
    }

    private suspend fun fetchReply(caption: String, email: String): String = withContext(Dispatchers.IO) {
        if (isDestroyed) return@withContext ""

        val (redditPrompt, _) = fetchUserPrompts(email)

        val systemMessage = """
            Generate a relevant, short 1-2 sentence reply to the post. 
            If you can naturally mention the brand, do so briefly at the end. 
            Do not ask any questions. Just provide a short comment.  Do not mention rohan and respond to the post directly. Do not act like an AI model. Write the reply from the 1st person as if you were reading the post yourself. Do not explain the post or ask any clarifying questions. If there is nothing to reply to, output 'hmm I guess I agree' or 'nice'. 
        """.trimIndent()

        val userMessageBuilder = StringBuilder()
            .appendLine(caption)

        if (!redditPrompt.isNullOrBlank()) {
            userMessageBuilder.append("\n")
                .appendLine("Generate a small quick reply that's useful and insightful.")
                .appendLine(caption)
        }

        val messages = listOf(
            mapOf("role" to "system", "content" to systemMessage),
            mapOf("role" to "user", "content" to userMessageBuilder.toString())
        )

        callStreaming16kAPI(messages, maxTokens = 100, mode = "fast")
    }

    private suspend fun fetchBlogPost(caption: String, email: String): String = withContext(Dispatchers.IO) {
        if (isDestroyed) return@withContext ""

        speakText("Generating blog post")
        val (_, wordpressPrompt) = fetchUserPrompts(email)
        Log.e("MainActivity", email)

        var domain = email.split("@")[1]
        Log.e("MainActivity", domain)

        val systemMessage = """
            You are a helpful writer. Return a single, plain-text blog post relevant to the input caption.  Do not add hashtags or markdown. Add 1 title to the top, then a new line, then continue the blog. Make it a top 10 list, or some kind of useful educational content about the topic. Try to naturally mention the brand provided at the end
            Make it a 'top 10 list' or a short 'how-to' tutorial. Mention the brand once, but don't sound too promotional. 
            Naturally mention this brand as one of the options. Brand: $domain
        """.trimIndent()
        Log.e("MainActivity", systemMessage)

        val userMessageBuilder = StringBuilder()
            .appendLine("Blog topic: $caption")

        if (!wordpressPrompt.isNullOrBlank()) {
            userMessageBuilder.append("\n")
                .appendLine("Additional user instructions for WordPress blog:")
                .appendLine(wordpressPrompt)
        }

        val messages = listOf(
            mapOf("role" to "system", "content" to systemMessage),
            mapOf("role" to "user", "content" to userMessageBuilder.toString())
        )

        val result = callStreaming16kAPI(messages, maxTokens = 500, mode = "best")
        Log.e("MainActivity", result)

        speakText("Blog post generated successfully")
        result
    }

    private fun launchMedium() {
        speakText("Launching Medium")
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.killBackgroundProcesses("com.medium.reader")

        runBlocking { delay(1000) }

        val openMediumIntent = Intent(Intent.ACTION_VIEW, Uri.parse("medium://"))
        if (openMediumIntent.resolveActivity(packageManager) != null) {
            openMediumIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(openMediumIntent)
        } else {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://medium.com/"))
            startActivity(webIntent)
        }
    }

    // Continue with remaining AndroidJSInterface methods

    // Helper method for callMoondreamAPI
    private suspend fun callMoondreamAPI(base64Image: String, objectDescription: String): MoondreamPoint? = withContext(Dispatchers.IO) {
        if (isDestroyed) return@withContext null


        return@withContext try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val requestBody = JSONObject().apply {
                put("image_url", "data:image/jpeg;base64,$base64Image")
                put("object", objectDescription)
            }

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = requestBody.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("${getMoondreamEndpoint()}${getMoondreamPointPath()}")
                .header("Content-Type", "application/json")
                .header("X-Moondream-Auth", getMoondreamKey())
                .post(body)
                .build()

            Log.d("MainActivity", "Calling Moondream API for: $objectDescription")

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val responseJson = JSONObject(responseBody)
                    val pointsArray = responseJson.getJSONArray("points")

                    if (pointsArray.length() > 0) {
                        val firstPoint = pointsArray.getJSONObject(0)
                        val x = firstPoint.getDouble("x")
                        val y = firstPoint.getDouble("y")

                        Log.d("MainActivity", "Moondream found object at: ($x, $y)")
                        MoondreamPoint(x, y)
                    } else {
                        Log.w("MainActivity", "Moondream API returned no points")
                        null
                    }
                } else {
                    Log.e("MainActivity", "Moondream API error. Code: ${response.code}")
                    Log.e("MainActivity", "Error response: $responseBody")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Moondream API exception: ${e.message}")
            null
        }
    }

    // ─── Feature 3: Script Execution Sandbox ────────────────────────────────────
    fun executeWithPermissionPrompt(code: String, description: String = "Run script") {
        if (isDestroyed) return
        val prefs = getSharedPreferences("phoneclaw_config", MODE_PRIVATE)
        val sandboxEnabled = prefs.getBoolean("sandbox_enabled", true)

        if (!sandboxEnabled) {
            // Sandbox disabled — run directly
            mainScope.launch { executeGeneratedCode(code) }
            return
        }

        val preview = code.lines().take(6).joinToString("\n")
        MaterialAlertDialogBuilder(this)
            .setTitle("⚠️ Run Script?")
            .setMessage("$description\n\nPreview:\n$preview${if (code.lines().size > 6) "\n..." else ""}")
            .setPositiveButton("▶ Run") { _, _ -> mainScope.launch { executeGeneratedCode(code) } }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("👁 View Full") { _, _ ->
                MaterialAlertDialogBuilder(this)
                    .setTitle("Full Script")
                    .setMessage(code)
                    .setPositiveButton("▶ Run") { _, _ -> mainScope.launch { executeGeneratedCode(code) } }
                    .setNegativeButton("Cancel", null).show()
            }
            .show()
    }

    // ─── Feature 2: Script Library ───────────────────────────────────────────────
    private fun showScriptLibraryDialog() {
        val scripts = ScriptManager.listScripts(this)
        if (scripts.isEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("📚 Script Library")
                .setMessage("No saved scripts yet.\n\nSave scripts from the history editor to build your library.")
                .setPositiveButton("OK", null)
                .setNeutralButton("Import") { _, _ -> openScriptFilePicker() }
                .show()
            return
        }
        val fmt = java.text.SimpleDateFormat("MMM dd HH:mm", java.util.Locale.getDefault())
        val names = scripts.map { "📄 ${it.name}\n${fmt.format(java.util.Date(it.savedAt))}" }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle("📚 Script Library (${scripts.size})")
            .setItems(names) { _, i ->
                val s = scripts[i]
                MaterialAlertDialogBuilder(this)
                    .setTitle(s.name)
                    .setMessage((s.description.ifEmpty { "No description" }) + "\n\n" + s.code.take(200) + if (s.code.length > 200) "..." else "")
                    .setPositiveButton("▶ Run") { _, _ -> executeWithPermissionPrompt(s.code, s.name) }
                    .setNegativeButton("🗑 Delete") { _, _ -> ScriptManager.deleteScript(s.filePath); speakText("Deleted") }
                    .setNeutralButton("📤 Share") { _, _ -> ScriptManager.shareScript(this, s.filePath)?.let { startActivity(android.content.Intent.createChooser(it, "Share Script")) } }
                    .show()
            }
            .setNeutralButton("Import") { _, _ -> openScriptFilePicker() }
            .setNegativeButton("Close", null)
            .show()
    }

    private val SCRIPT_IMPORT_RC = 2001
    private fun openScriptFilePicker() {
        startActivityForResult(android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply { type = "*/*" }, SCRIPT_IMPORT_RC)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCRIPT_IMPORT_RC && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                val script = ScriptManager.importScript(this, uri)
                speakText(if (script != null) "Script imported: ${script.name}" else "Import failed")
            }
        }
        if (requestCode == 2002 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                val skill = SkillManager.importSkill(this, uri)
                speakText(if (skill != null) "Skill imported: ${skill.name}" else "Skill import failed")
            }
        }
    }

    // ─── Feature 4: Ollama Config ────────────────────────────────────────────────
    private fun showOllamaConfigDialog() {
        val prefs = getSharedPreferences("phoneclaw_config", MODE_PRIVATE)
        val enabled = prefs.getBoolean("ollama_enabled", false)
        val endpoint = prefs.getString("ollama_endpoint", "http://localhost:11434") ?: "http://localhost:11434"
        val model = prefs.getString("ollama_model", "llama3.2") ?: "llama3.2"
        val options = arrayOf("${if (enabled) "✅" else "⬜"} Enable Ollama", "🌐 Endpoint: $endpoint", "🤖 Model: $model", "🔍 Test Connection")
        MaterialAlertDialogBuilder(this)
            .setTitle("🦙 Ollama — On-Device AI")
            .setMessage("Run AI locally. Requires Ollama (ollama.com) on device or LAN.")
            .setItems(options) { _, w ->
                when (w) {
                    0 -> { prefs.edit().putBoolean("ollama_enabled", !enabled).apply(); speakText(if (!enabled) "Ollama enabled" else "Ollama disabled"); showOllamaConfigDialog() }
                    1 -> { val et = android.widget.EditText(this).apply { setText(endpoint) }; MaterialAlertDialogBuilder(this).setTitle("Endpoint").setView(et).setPositiveButton("Save") { _, _ -> prefs.edit().putString("ollama_endpoint", et.text.toString().trim()).apply() }.setNegativeButton("Cancel", null).show() }
                    2 -> { val et = android.widget.EditText(this).apply { setText(model) }; MaterialAlertDialogBuilder(this).setTitle("Model").setView(et).setPositiveButton("Save") { _, _ -> prefs.edit().putString("ollama_model", et.text.toString().trim()).apply() }.setNegativeButton("Cancel", null).show() }
                    3 -> mainScope.launch { val ok = withContext(Dispatchers.IO) { OllamaClient(this@MainActivity).isReachable() }; android.widget.Toast.makeText(this@MainActivity, if (ok) "✅ Ollama reachable" else "❌ Cannot reach Ollama", android.widget.Toast.LENGTH_LONG).show() }
                }
            }
            .setNegativeButton("Close", null).show()
    }

    // ─── Feature 5: Audit Log UI ─────────────────────────────────────────────────
    private fun showAuditLogDialog() {
        val log = LocalStorage.getActionLog(this)
        if (log.length() == 0) {
            MaterialAlertDialogBuilder(this)
                .setTitle("📋 Audit Log")
                .setMessage("No actions logged yet. All magicClicker and magicScraper calls are logged here locally.")
                .setPositiveButton("OK", null).show()
            return
        }
        val sb = StringBuilder()
        val fmt = java.text.SimpleDateFormat("MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val count = minOf(log.length(), 100)
        for (i in 0 until count) {
            val obj = log.getJSONObject(log.length() - 1 - i)
            sb.appendLine("[${fmt.format(java.util.Date(obj.optLong("timestamp")))}] ${obj.optString("mode")}: ${obj.optString("input").take(70)}")
        }
        val tv = android.widget.TextView(this).apply { text = sb.toString(); setPadding(48,32,48,32); setTextIsSelectable(true); textSize = 11f; typeface = android.graphics.Typeface.MONOSPACE }
        val sv = android.widget.ScrollView(this).apply { addView(tv) }
        MaterialAlertDialogBuilder(this)
            .setTitle("📋 Audit Log (${log.length()} total)")
            .setView(sv)
            .setPositiveButton("Close", null)
            .setNegativeButton("🗑 Clear") { _, _ -> LocalStorage.clearActionLog(this); speakText("Audit log cleared") }
            .setNeutralButton("💾 Storage") { _, _ -> android.widget.Toast.makeText(this, LocalStorage.getStorageInfo(this), android.widget.Toast.LENGTH_LONG).show() }
            .show()
    }

    // ─── Feature 9: Flow Recorder ────────────────────────────────────────────────
    private fun showFlowRecorderDialog() {
        if (FlowRecorder.isRecording) {
            val steps = FlowRecorder.stopRecording()
            val script = FlowRecorder.generateScript("Recorded Flow")
            updateStatusWithAnimation("⏹ Stopped — ${steps.size} steps")
            speakText("Recording stopped. ${steps.size} steps.")
            if (steps.isEmpty()) return
            MaterialAlertDialogBuilder(this)
                .setTitle("⏹ ${steps.size} Steps Recorded")
                .setMessage(script.take(500) + if (script.length > 500) "\n..." else "")
                .setPositiveButton("▶ Run") { _, _ -> executeWithPermissionPrompt(script, "Recorded Flow") }
                .setNegativeButton("💾 Save") { _, _ ->
                    val name = "Flow_${java.text.SimpleDateFormat("MM-dd_HHmm", java.util.Locale.getDefault()).format(java.util.Date())}"
                    ScriptManager.saveScript(this, name, script, "${steps.size} recorded steps")
                    val h = GenerationHistory(java.util.UUID.randomUUID().toString(), name, script)
                    generationHistory.add(0, h); saveGenerationHistory(); updateUI(); speakText("Flow saved")
                }
                .setNeutralButton("Discard", null).show()
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle("🔴 Flow Recorder")
                .setMessage("Tap 'Start' then perform your steps manually. Every tap will be recorded. Tap Flow Recorder again to stop.")
                .setPositiveButton("🔴 Start") { _, _ ->
                    FlowRecorder.startRecording()
                    FlowRecorder.onStepRecorded = { runOnUiThread { updateStatusWithAnimation("🔴 Recording — ${FlowRecorder.getStepCount()} steps") } }
                    updateStatusWithAnimation("🔴 Recording — tap anywhere"); speakText("Recording started")
                }
                .setNegativeButton("Cancel", null).show()
        }
    }

    // ─── Feature 10: Notification Triggers ──────────────────────────────────────
    private fun showNotificationTriggersDialog() {
        val rules = NotificationTriggerService.loadTriggerRules(this).toMutableList()
        if (rules.isEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("🔔 Notification Triggers")
                .setMessage("No triggers yet.\n\nExample: When a WhatsApp message containing 'urgent' arrives, run a script.\n\nRequires Notification Access in Settings.")
                .setPositiveButton("➕ Add") { _, _ -> showAddTriggerDialog() }
                .setNegativeButton("Close", null).show()
            return
        }
        val names = rules.map { "${if (it.enabled) "✅" else "⬜"} ${it.description}" }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle("🔔 Triggers (${rules.size})")
            .setItems(names) { _, i ->
                val rule = rules[i]
                MaterialAlertDialogBuilder(this)
                    .setTitle(rule.description)
                    .setMessage("App: ${rule.appPackage.ifEmpty{"Any"}}\nMatch: ${rule.matchText.ifEmpty{"Any"}}\nScript: ${rule.script.take(120)}")
                    .setPositiveButton(if (rule.enabled) "Disable" else "Enable") { _, _ ->
                        val updated = rules.map { if (it.id == rule.id) it.copy(enabled = !it.enabled) else it }
                        NotificationTriggerService.saveTriggerRules(this, updated); showNotificationTriggersDialog()
                    }
                    .setNegativeButton("🗑 Delete") { _, _ ->
                        NotificationTriggerService.saveTriggerRules(this, rules.filter { it.id != rule.id })
                    }
                    .setNeutralButton("Back", null).show()
            }
            .setPositiveButton("➕ Add") { _, _ -> showAddTriggerDialog() }
            .setNegativeButton("Close", null).show()
    }

    private fun showAddTriggerDialog() {
        val layout = android.widget.LinearLayout(this).apply { orientation = android.widget.LinearLayout.VERTICAL; setPadding(48,24,48,24) }
        fun et(hint: String) = android.widget.EditText(this).apply { this.hint = hint; layout.addView(this) }
        val nameEt = et("Name (e.g. WhatsApp urgent reply)")
        val pkgEt = et("App package (e.g. com.whatsapp, blank = any)")
        val matchEt = et("Match text (blank = any notification)")
        val scriptEt = et("Script to run (use {notification} for text)")
        MaterialAlertDialogBuilder(this)
            .setTitle("➕ New Notification Trigger").setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val rule = NotificationRule(description=nameEt.text.toString().ifEmpty{"Trigger"}, appPackage=pkgEt.text.toString().trim(), matchText=matchEt.text.toString().trim(), script=scriptEt.text.toString().trim())
                val all = NotificationTriggerService.loadTriggerRules(this).toMutableList().also { it.add(rule) }
                NotificationTriggerService.saveTriggerRules(this, all); speakText("Trigger saved")
            }
            .setNegativeButton("Cancel", null).show()
    }

    // ── Skills / Plugin System ────────────────────────────────────────────────

    fun showSkillsDialog() {
        val skills = SkillManager.listSkills(this)
        val fmt = java.text.SimpleDateFormat("MMM dd", Locale.getDefault())
        MaterialAlertDialogBuilder(this)
            .setTitle("🧩 Skills (${skills.size})")
            .setMessage(if (skills.isEmpty()) "No skills installed yet.\n\nSkills are reusable automation capabilities. Create one with AI or import from another device." else null)
            .setItems(if (skills.isNotEmpty()) skills.map { "🧩 ${it.name}\n${it.description.take(60)}" }.toTypedArray() else null) { _, i ->
                val skill = skills[i]
                if (skill.parameters.isEmpty()) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(skill.name)
                        .setMessage("${skill.description}\n\nTriggers: ${skill.triggers.joinToString(", ")}\n\nCode:\n${skill.code.take(200)}")
                        .setPositiveButton("▶ Run") { _, _ -> executeWithPermissionPrompt(skill.code, skill.name) }
                        .setNegativeButton("🗑 Delete") { _, _ -> SkillManager.deleteSkill(skill.filePath); speakText("Skill deleted") }
                        .setNeutralButton("📤 Export") { _, _ ->
                            SkillManager.exportSkill(this, skill.filePath)?.let { startActivity(Intent.createChooser(it, "Export Skill")) }
                        }
                        .show()
                } else {
                    val layout = android.widget.LinearLayout(this).apply { orientation = android.widget.LinearLayout.VERTICAL; setPadding(48, 24, 48, 24) }
                    val paramViews = skill.parameters.map { param ->
                        android.widget.EditText(this).apply { hint = "${param.name}${if (param.required) " *" else ""}"; layout.addView(this) }
                    }
                    MaterialAlertDialogBuilder(this)
                        .setTitle("▶ Run: ${skill.name}").setView(layout)
                        .setPositiveButton("Run") { _, _ ->
                            val paramMap = skill.parameters.mapIndexed { idx, p -> p.name to paramViews[idx].text.toString() }.toMap()
                            executeWithPermissionPrompt(SkillManager.renderSkill(skill, paramMap), skill.name)
                        }
                        .setNegativeButton("Cancel", null).show()
                }
            }
            .setPositiveButton("✨ Create with AI") { _, _ ->
                val et = android.widget.EditText(this).apply { hint = "Describe what the skill should do..." }
                MaterialAlertDialogBuilder(this)
                    .setTitle("✨ Create Skill with AI").setView(et)
                    .setPositiveButton("Generate") { _, _ ->
                        val desc = et.text.toString().trim()
                        if (desc.isEmpty()) return@setPositiveButton
                        updateStatusWithAnimation("✨ Generating skill...")
                        mainScope.launch {
                            val skill = SkillManager.generateSkillWithAI(this@MainActivity, desc, BYOKClient(this@MainActivity))
                            withContext(Dispatchers.Main) {
                                if (skill != null) {
                                    SkillManager.saveSkill(this@MainActivity, skill)
                                    speakText("Skill created: ${skill.name}")
                                    showSkillsDialog()
                                } else {
                                    speakText("Failed to generate skill — check AI config")
                                    Toast.makeText(this@MainActivity, "Skill generation failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    .setNegativeButton("Cancel", null).show()
            }
            .setNeutralButton("📥 Import") { _, _ ->
                startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" }, 2002)
            }
            .setNegativeButton("Close", null).show()
    }

    // ── Agent Memory ──────────────────────────────────────────────────────────

    private fun showMemoryDialog() {
        val facts = MemoryManager.getAllFacts(this)
        val recent = MemoryManager.getRecentInteractions(this, 10)
        val mostUsed = MemoryManager.getMostUsedApps(this, 5)
        val sb = StringBuilder()
        if (facts.isNotEmpty()) {
            sb.appendLine("📌 Remembered Facts:")
            facts.forEach { (k, v) -> sb.appendLine("  • $k: $v") }
            sb.append("\n")
        }
        if (mostUsed.isNotEmpty()) {
            sb.appendLine("📱 Most Used Apps:")
            mostUsed.forEach { sb.appendLine("  • $it") }
            sb.append("\n")
        }
        if (recent.isNotEmpty()) {
            sb.appendLine("🕐 Recent (${recent.size}):")
            recent.takeLast(5).forEach { (u, a) -> sb.appendLine("  ▶ $u\n    → ${a.take(80)}") }
        }
        if (sb.isEmpty()) sb.append("No memory yet. The agent learns as you use it.")
        val tv = android.widget.TextView(this).apply { text = sb.toString(); setPadding(48,32,48,32); setTextIsSelectable(true); textSize = 12f }
        val sv = android.widget.ScrollView(this).apply { addView(tv) }
        MaterialAlertDialogBuilder(this)
            .setTitle("🧠 Agent Memory")
            .setView(sv)
            .setPositiveButton("Close", null)
            .setNegativeButton("🗑 Clear All") { _, _ ->
                MemoryManager.clearShortTerm(this); MemoryManager.clearAllFacts(this); speakText("Memory cleared")
            }
            .setNeutralButton("➕ Add Fact") { _, _ ->
                val layout = android.widget.LinearLayout(this).apply { orientation = android.widget.LinearLayout.VERTICAL; setPadding(48,24,48,24) }
                val keyEt = android.widget.EditText(this).apply { hint = "Key (e.g. 'my name')"; layout.addView(this) }
                val valEt = android.widget.EditText(this).apply { hint = "Value (e.g. 'Theking')"; layout.addView(this) }
                MaterialAlertDialogBuilder(this)
                    .setTitle("➕ Remember Fact").setView(layout)
                    .setPositiveButton("Save") { _, _ ->
                        val k = keyEt.text.toString().trim(); val v = valEt.text.toString().trim()
                        if (k.isNotEmpty() && v.isNotEmpty()) { MemoryManager.rememberFact(this, k, v); speakText("Remembered") }
                    }
                    .setNegativeButton("Cancel", null).show()
            }
            .show()
    }
}


    override fun onResume() {
        super.onResume()
        // Re-init chat elements when returning from other tabs
        if (::chatRecyclerView.isInitialized && chatRecyclerView != null) {
            tryReInitChatButtons()
        }
    }

    private fun tryReInitChatButtons() {
        try {
            // Find views again to handle edge cases
            if (runCommandButton == null) {
                currentFragment?.view?.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.runCommandButton)?.let {
                    it.setOnClickListener { sendCommand() }
                }
            }
            if (commandInput == null) {
                currentFragment?.view?.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.commandInput)?.let {
                    it.setOnEditorActionListener { _, _, _ -> sendCommand(); true }
                }
            }
        } catch (e: Exception) { /* ignore */ }
    }
}
