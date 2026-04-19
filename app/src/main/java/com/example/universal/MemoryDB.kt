package com.example.universal

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * SQLite Memory System
 * - Stores all AI interactions
 * - Full-text search capability
 * - Makes AI remember past actions
 */
class MemoryDB(ctx: Context) : SQLiteOpenHelper(ctx, MEMORY_DB, null, VER) {

    companion object {
        private const val MEMORY_DB = "phoneclaw_memory.db"
        private const val VER = 1
        
        // Table: interactions
        private const val T_INTERACTIONS = """
            CREATE TABLE interactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp INTEGER NOT NULL,
                user_input TEXT,
                ai_response TEXT,
                generated_code TEXT,
                result TEXT,
                tags TEXT
            )
        """
        
        // Table: memories (key-value)
        private const val T_MEMORIES = """
            CREATE TABLE memories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                key TEXT UNIQUE NOT NULL,
                value TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """
        
        // Table: facts (learned info)
        private const val T_FACTS = """
            CREATE TABLE facts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                fact TEXT NOT NULL,
                context TEXT,
                created_at INTEGER NOT NULL
            )
        """
        
        // Index for search
        private const val I_SEARCH = """
            CREATE VIRTUAL TABLE interactions_fts USING fts5(user_input, ai_response, generated_code)
        """
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(T_INTERACTIONS)
        db.execSQL(T_MEMORIES)
        db.execSQL(T_FACTS)
        // Full-text search - optional on older Android
        try { db.execSQL(I_SEARCH) } catch (e: Exception) { }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldV: Int, newV: Int) {
        db.execSQL("DROP TABLE IF EXISTS interactions")
        db.execSQL("DROP TABLE IF EXISTS memories")
        db.execSQL("DROP TABLE IF EXISTS facts")
        onCreate(db)
    }

    // === INTERACTIONS ===
    
    fun logInteraction(
        userInput: String,
        aiResponse: String,
        generatedCode: String,
        result: String,
        tags: String = ""
    ) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("timestamp", System.currentTimeMillis())
            put("user_input", userInput)
            put("ai_response", aiResponse)
            put("generated_code", generatedCode)
            put("result", result)
            put("tags", tags)
        }
        db.insert("interactions", null, cv)
        
        // Also update FTS if available
        try {
            db.execSQL("""INSERT INTO interactions_fts VALUES(?, ?, ?)""",
                arrayOf(userInput, aiResponse, generatedCode))
        } catch (e: Exception) { }
    }
    
    fun searchInteractions(query: String, limit: Int = 50): List<Interaction> {
        val db = readableDatabase
        val results = mutableListOf<Interaction>()
        
        // Try FTS first, fallback to LIKE
        try {
            val cursor = db.rawQuery("""
                SELECT * FROM interactions WHERE id IN (
                    SELECT rowid FROM interactions_fts WHERE interactions_fts MATCH ?
                ) ORDER BY timestamp DESC LIMIT ?
            """, arrayOf("$query*", limit.toString()))
            
            while (cursor.moveToNext()) {
                results.add(cursorToInteraction(cursor))
            }
            cursor.close()
        } catch (e: Exception) {
            // Fallback to LIKE
            val cursor = db.rawQuery("""
                SELECT * FROM interactions WHERE 
                user_input LIKE ? OR ai_response LIKE ? OR generated_code LIKE ?
                ORDER BY timestamp DESC LIMIT ?
            """, arrayOf("%$query%", "%$query%", "%$query%", limit.toString()))
            
            while (cursor.moveToNext()) {
                results.add(cursorToInteraction(cursor))
            }
            cursor.close()
        }
        
        return results
    }
    
    fun getRecentInteractions(limit: Int = 20): List<Interaction> {
        val db = readableDatabase
        val cursor = db.rawQuery("""
            SELECT * FROM interactions ORDER BY timestamp DESC LIMIT ?
        """, arrayOf(limit.toString()))
        
        val results = mutableListOf<Interaction>()
        while (cursor.moveToNext()) {
            results.add(cursorToInteraction(cursor))
        }
        cursor.close()
        return results
    }

    // === MEMORIES (Key-Value) ===
    
    fun remember(key: String, value: String) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("key", key)
            put("value", value)
            put("updated_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict("memories", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }
    
    fun recall(key: String): String? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT value FROM memories WHERE key = ?", arrayOf(key))
        var result: String? = null
        if (cursor.moveToNext()) {
            result = cursor.getString(0)
        }
        cursor.close()
        return result
    }
    
    fun forget(key: String) {
        val db = writableDatabase
        db.delete("memories", "key = ?", arrayOf(key))
    }

    // === FACTS (Learned Info) ===
    
    fun learnFact(fact: String, context: String = "") {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("fact", fact)
            put("context", context)
            put("created_at", System.currentTimeMillis())
        }
        db.insert("facts", null, cv)
    }
    
    fun searchFacts(query: String, limit: Int = 20): List<Fact> {
        val db = readableDatabase
        val cursor = db.rawQuery("""
            SELECT * FROM facts WHERE fact LIKE ? OR context LIKE ?
            ORDER BY created_at DESC LIMIT ?
        """, arrayOf("%$query%", "%$query%", limit.toString()))
        
        val results = mutableListOf<Fact>()
        while (cursor.moveToNext()) {
            results.add(Fact(
                id = cursor.getLong(0),
                fact = cursor.getString(1),
                context = cursor.getString(2),
                createdAt = cursor.getLong(3)
            ))
        }
        cursor.close()
        return results
    }

    // === UTILITY ===
    
    fun getContextForAI(limit: Int = 10): String {
        val recent = getRecentInteractions(limit)
        if (recent.isEmpty()) return ""
        
        return recent.joinToString("\n") {
            "User: ${it.userInput}\nAI: ${it.aiResponse}\nCode: ${it.generatedCode.take(100)}"
        }
    }
    
    private fun cursorToInteraction(cursor: android.database.Cursor) = Interaction(
        id = cursor.getLong(0),
        timestamp = cursor.getLong(1),
        userInput = cursor.getString(2),
        aiResponse = cursor.getString(3),
        generatedCode = cursor.getString(4),
        result = cursor.getString(5),
        tags = cursor.getString(6)
    )
}

data class Interaction(
    val id: Long,
    val timestamp: Long,
    val userInput: String,
    val aiResponse: String,
    val generatedCode: String,
    val result: String,
    val tags: String
)

data class Fact(
    val id: Long,
    val fact: String,
    val context: String,
    val createdAt: Long
)