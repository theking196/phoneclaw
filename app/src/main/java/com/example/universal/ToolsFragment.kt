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

    // Tool definitions with descriptions and examples
    private val tools = listOf(
        ToolInfo("magicClicker", "Click a UI element by description",
            "Click 'Create account' button", 
            "Click the 'Send' button in the chat"),
        
        ToolInfo("magicScraper", "Read text from screen",
            "Read the OTP code from SMS", 
            "Get the price shown on the screen"),
        
        ToolInfo("speakText", "Text-to-speech output",
            "speakText('Hello!')", 
            "Speak the current battery level"),
        
        ToolInfo("delay", "Wait for milliseconds",
            "delay(1000) // wait 1 second", 
            "delay(500) // half second"),
        
        ToolInfo("clickElementByViewId", "Click by View ID",
            "clickElementByViewId('com.example:id/btnSend')", 
            "Click button with ID 'send'"),
        
        ToolInfo("clickElementByArea", "Click by screen coordinates",
            "clickElementByArea(320, 480)", 
            "Tap center of screen"),
        
        ToolInfo("swipeUp", "Swipe up on screen",
            "swipeUp()", 
            "Scroll down by swiping up"),
        
        ToolInfo("swipeDown", "Swipe down on screen", 
            "swipeDown()", 
            "Scroll up by swiping down"),
        
        ToolInfo("showToast", "Show toast message",
            "showToast('Done!')", 
            "Show 'Processing...' toast"),
        
        ToolInfo("logMessage", "Log to debug console",
            "logMessage('Clicked button')", 
            "Log when action completes"),
        
        ToolInfo("schedule", "Schedule a task",
            "schedule('myTask', '0 * * * *')", 
            "Run every hour"),
        
        ToolInfo("clearSchedule", "Clear all scheduled tasks",
            "clearSchedule()", 
            "Stop all scheduled tasks"),
        
        ToolInfo("httpGet", "HTTP GET request",
            "httpGet('https://api.example.com/data')", 
            "Fetch data from URL"),
        
        ToolInfo("httpPost", "HTTP POST request", 
            "httpPost('https://api.example.com/submit', '{\"key\":\"value\"}')",
            "Send JSON data to endpoint"),
        
        ToolInfo("saveToMemory", "Save to phone memory",
            "saveToMemory('lastCode', 'my script')", 
            "Remember this script"),
        
        ToolInfo("readFromMemory", "Read from phone memory",
            "readFromMemory('lastCode')", 
            "Get remembered script"),
            
        ToolInfo("getScreenState", "Get current screen info",
            "getScreenState()", 
            "Returns screen package and elements"),
            
        ToolInfo("waitForElement", "Wait for element to appear", 
            "waitForElement('Submit', 5000)",
            "Wait up to 5s for button")
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_tools, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        try {
            val rv = view.findViewById<RecyclerView>(R.id.toolsRecyclerView)
            val empty = view.findViewById<LinearLayout>(R.id.emptyState)

            if (tools.isEmpty()) {
                rv.visibility = View.GONE
                empty.visibility = View.VISIBLE
            } else {
                rv.visibility = View.VISIBLE
                empty.visibility = View.GONE
                rv.layoutManager = LinearLayoutManager(requireContext())
                rv.adapter = ToolsAdapter(tools)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Tools error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

data class ToolInfo(
    val name: String,
    val description: String,
    val syntax: String,
    val example: String,
    val enabled: Boolean = true
)

class ToolsAdapter(
    private val tools: List<ToolInfo>
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