package com.example.universal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ToolsFragment : Fragment() {

    // Complete list of ALL ClawScript tools - from codebase
    // The single source of truth for available functions
    private val tools = listOf(
        // === CLICKING ===
        ToolInfo("magicClicker(description)", "Click UI element by natural language description", "magicClicker('Submit button')", "User sees and clicks it", 
            "Best for: Buttons, links, any clickable element"),
        ToolInfo("clickElementByViewId(id)", "Click element by Android View ID", "clickElementByViewId('com.app:id/btnSend')", "When you know the View ID",
            "Best for: Known element IDs"),
        ToolInfo("clickElementByArea(x, y)", "Click screen coordinates", "clickElementByArea(320, 480)", "Tap specific location",
            "Best for: Fixed position elements"),
        
        // === SCROLLING ===
        ToolInfo("swipeUp()", "Swipe up (scroll down)", "swipeUp()", "Fast scroll", "Best for: Feed, lists"),
        ToolInfo("swipeDown()", "Swipe down (scroll up)", "swipeDown()", "Go back", "Best for: Go back one screen"),
        ToolInfo("swipeLeft()", "Swipe left", "swipeLeft()", "Navigate right", "Best for: Horizontal pages"),
        ToolInfo("swipeRight()", "Swipe right", "swipeRight()", "Navigate back", "Best for: Horizontal pages"),

        // === READING ===
        ToolInfo("magicScraper(question)", "Read screen text to answer question", "magicScraper('What is the total?')", "Get value from screen",
            "Best for: OTPs, prices, status, any text"),
        ToolInfo("getScreenState()", "Get current screen info", "getScreenState()", "Returns screen package, elements",
            "Best for: Debug, understanding current state"),

        // === OUTPUT ===
        ToolInfo("speakText(message)", "Voice feedback via TTS", "speakText('Done!')", "Speaks to user",
            "Best for: Confirmations, information - USE THIS TO INFORM USER"),
        ToolInfo("showToast(message)", "Show toast notification", "showToast('Saved!')", "Quick popup",
            "Best for: Quick confirmation"),
        ToolInfo("logMessage(message)", "Debug logging", "logMessage('Clicked')", "Console output",
            "Best for: Debugging scripts"),

        // === WAITING ===
        ToolInfo("delay(ms)", "Wait milliseconds", "delay(1000)", "Wait 1 second",
            "Best for: Loading, animations"),
        ToolInfo("waitForElement(id, timeout)", "Wait for element", "waitForElement('btn', 5000)", "Wait up to 5s",
            "Best for: Dynamic content"),

        // === NETWORK ===
        ToolInfo("httpGet(url)", "HTTP GET request", "httpGet('https://api.app.com/data')", "Fetch data",
            "Best for: APIs, web requests"),
        ToolInfo("httpPost(url, data)", "HTTP POST request", "httpPost('url', '{\"a\":1}')", "Send data",
            "Best for: Form submissions, API calls"),

        // === MEMORY ===
        ToolInfo("saveToMemory(key, value)", "Save to persistent memory", "saveToMemory('name', 'John')", "Remember data",
            "Best for: Remembering user preferences"),
        ToolInfo("readFromMemory(key)", "Read from persistent memory", "readFromMemory('name')", "Recall saved data",
            "Best for: Using saved data"),

        // === SCHEDULING ===
        ToolInfo("schedule(taskCode, cron)", "Schedule recurring task", "schedule(code, '0 * * * *')", "Run hourly",
            "Best for: Periodic reminders, automation"),
        ToolInfo("clearSchedule()", "Clear all scheduled tasks", "clearSchedule()", "Stop all schedules",
            "Best for: Reset automation")
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_skills, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        try {
            val rv = view.findViewById<RecyclerView>(R.id.skillsRecyclerView)
            val tabs = view.findViewById<com.google.android.material.tabs.TabLayout>(R.id.libraryTabs)
            
            tabs?.addTab(tabs.newTab().setText("Skills"))
            tabs?.addTab(tabs.newTab().setText("Tools (${tools.size})"))
            
            tabs?.addOnTabSelectedListener(object: com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                    when (tab.position) {
                        0 -> showSkills(rv)
                        1 -> showTools(rv)
                    }
                }
                override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
                override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
            })
            
            showTools(rv)
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showSkills(rv: RecyclerView) {
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = SimpleAdapter(listOf("Create skill", "More coming soon"))
    }
    
    private fun showTools(rv: RecyclerView) {
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = ToolsAdapter(tools) { tool, enabled ->
            // Save enabled state - implementation pending
        }
    }
}

data class ToolInfo(
    val name: String,
    val description: String,
    val syntax: String,
    val example: String,
    val tip: String = ""
)

class SimpleAdapter(private val items: List<String>) : RecyclerView.Adapter<SimpleAdapter.VH>() {
    class VH(view: View) : RecyclerView.ViewHolder(view)
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(android.view.LayoutInflater.from(p.context).inflate(android.R.layout.simple_list_item_1, p, false))
    override fun getItemCount() = items.size
    override fun onBindVH(h: VH, i: Int) { (h.itemView as android.widget.TextView).text = items[i] }
}

class ToolsAdapter(
    private val tools: List<ToolInfo>,
    private val onToggle: (ToolInfo, Boolean) -> Unit
) : RecyclerView.Adapter<ToolsAdapter.VH>() {
    class VH(view: View) : RecyclerView.ViewHolder(view)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_tool_card, parent, false)
        return VH(v)
    }
    override fun onBindViewHolder(holder: VH, position: Int) {
        val tool = tools[position]
        holder.itemView.apply {
            findViewById<TextView>(R.id.toolName)?.text = tool.name
            findViewById<TextView>(R.id.toolDescription)?.text = tool.description
            findViewById<TextView>(R.id.toolSyntax)?.text = tool.syntax
            findViewById<TextView>(R.id.toolExample)?.text = "Ex: ${tool.example}"
        }
    }
    override fun getItemCount() = tools.size
}