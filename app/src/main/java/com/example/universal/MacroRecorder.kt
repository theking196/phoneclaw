package com.example.universal

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Macro Recorder - Record and replay action sequences
 * 
 * Usage:
 * 1. Start recording: macroStart("My Macro Name")
 * 2. Perform actions manually
 * 3. Stop recording: macroStop()
 * 4. Play later: macroPlay("My Macro Name")
 */
class MacroRecorder(ctx: Context) : SQLiteOpenHelper(ctx, MACRO_DB, null, 1) {

    companion object {
        private const val MACRO_DB = "phoneclaw_macros.db"
        
        // States
        private var isRecording = false
        private var currentMacro: MutableList<MacroAction> = mutableListOf()
        private var macroName = ""
        
        // Database table
        private const val T_MACROS = """
            CREATE TABLE macros (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE NOT NULL,
                actions TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
        """
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(T_MACROS)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldV: Int, newV: Int) {
        db.execSQL("DROP TABLE IF EXISTS macros")
        onCreate(db)
    }

    // === RECORDING ===
    
    fun startRecording(name: String) {
        if (isRecording) {
            throw IllegalStateException("Already recording. Stop first.")
        }
        currentMacro.clear()
        macroName = name
        isRecording = true
    }
    
    fun recordAction(action: String, params: String = "") {
        if (!isRecording) return
        currentMacro.add(MacroAction(action, params, System.currentTimeMillis()))
    }
    
    fun stopRecording() {
        if (!isRecording) return
        isRecording = false
        
        if (currentMacro.isNotEmpty() && macroName.isNotBlank()) {
            saveMacro(macroName, currentMacro.toList())
            currentMacro.clear()
        }
    }

    // === PLAYBACK ===
    
    fun playMacro(name: String, context: Context, onAction: (MacroAction) -> Unit) {
        val macro = loadMacro(name) ?: return
        
        // Execute each action with delay
        var delay = 0L
        macro.forEach { action ->
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                onAction(action)
            }, delay)
            delay += 500 // Default 500ms between actions
        }
    }

    // === STORAGE ===
    
    private fun saveMacro(name: String, actions: List<MacroAction>) {
        val db = writableDatabase
        val actionsJson = actions.joinToString("|||") { "${it.name}||${it.params}" }
        val cv = android.content.ContentValues().apply {
            put("name", name)
            put("actions", actionsJson)
            put("created_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict("macros", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }
    
    private fun loadMacro(name: String): List<MacroAction>? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT actions FROM macros WHERE name = ?", arrayOf(name))
        
        var result: List<MacroAction>? = null
        if (cursor.moveToNext()) {
            val actionsJson = cursor.getString(0)
            result = actionsJson.split("|||").mapNotNull { part ->
                val parts = part.split("||")
                if (parts.size == 2) MacroAction(parts[0], parts[1], 0) else null
            }
        }
        cursor.close()
        return result
    }
    
    fun listMacros(): List<String> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT name FROM macros ORDER BY created_at DESC", null)
        val list = mutableListOf<String>()
        while (cursor.moveToNext()) {
            list.add(cursor.getString(0))
        }
        cursor.close()
        return list
    }
    
    fun deleteMacro(name: String) {
        writableDatabase.delete("macros", "name = ?", arrayOf(name))
    }

    // === REFERENCE ===
    
    // How to use in scripts
    val scriptReference = """
# Macro functions available:
# Note: These are called from MainActivity, not directly in scripts

# When recording:
macroStart("My Macro")  # Start recording
tap(200, 300)           # Actions are recorded
tap(400, 500)
macroStop()             # Stop and save

# When playing:
macroPlay("My Macro")   # Replay saved macro

# From UI: Library → Macros tab
    """.trimIndent()
}

/**
 * Single action in a macro
 */
data class MacroAction(
    val name: String,    // e.g., "click", "swipe", "type"
    val params: String,  // e.g., "200,300" or "Hello"
    val timestamp: Long = System.currentTimeMillis()
)