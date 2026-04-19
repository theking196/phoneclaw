package com.example.universal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ScriptManager {
    private const val TAG = "ScriptManager"
    private const val SCRIPTS_DIR = "clawscripts"
    private const val FILE_EXT = ".clawscript"

    private fun scriptsDir(ctx: Context): File = File(ctx.filesDir, SCRIPTS_DIR).also { it.mkdirs() }

    fun saveScript(ctx: Context, name: String, code: String, description: String = ""): String {
        return try {
            val safeName = name.replace(Regex("[^a-zA-Z0-9_\\- ]"), "_").trim().take(50)
            val file = File(scriptsDir(ctx), "${safeName}${FILE_EXT}")
            file.writeText(JSONObject().apply {
                put("name", name); put("description", description)
                put("code", code); put("savedAt", System.currentTimeMillis()); put("version", "1.0")
            }.toString(2))
            Log.d(TAG, "Saved: ${file.name}")
            file.absolutePath
        } catch (e: Exception) { Log.e(TAG, "Save failed: ${e.message}"); "" }
    }

    fun listScripts(ctx: Context): List<SavedScript> {
        return try {
            scriptsDir(ctx).listFiles { f -> f.name.endsWith(FILE_EXT) }
                ?.mapNotNull { file ->
                    try {
                        val j = JSONObject(file.readText())
                        SavedScript(j.optString("name", file.nameWithoutExtension), j.optString("description"), j.optString("code"), j.optLong("savedAt", file.lastModified()), file.absolutePath)
                    } catch (e: Exception) { null }
                }?.sortedByDescending { it.savedAt } ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    fun deleteScript(filePath: String): Boolean = try { File(filePath).delete() } catch (e: Exception) { false }

    fun loadScript(filePath: String): String? = try { JSONObject(File(filePath).readText()).optString("code", null) } catch (e: Exception) { null }

    fun shareScript(ctx: Context, filePath: String): Intent? {
        return try {
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", File(filePath))
            Intent(Intent.ACTION_SEND).apply { type = "application/json"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        } catch (e: Exception) { null }
    }

    fun importScript(ctx: Context, uri: Uri): SavedScript? {
        return try {
            val content = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return null
            val j = JSONObject(content)
            val name = j.optString("name", "Imported"); val code = j.optString("code"); val desc = j.optString("description")
            val path = saveScript(ctx, name, code, desc)
            SavedScript(name, desc, code, System.currentTimeMillis(), path)
        } catch (e: Exception) { null }
    }
}

data class SavedScript(val name: String, val description: String, val code: String, val savedAt: Long, val filePath: String)
