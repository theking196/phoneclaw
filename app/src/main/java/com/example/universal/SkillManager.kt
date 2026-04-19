package com.example.universal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * SkillManager — Create, save, load, share and install skills (plugins).
 *
 * A Skill is a named capability: a JS snippet + metadata describing what it does,
 * what triggers it, and what parameters it accepts.
 *
 * Skills are stored as .clawskill JSON files on-device.
 * They can be exported/shared and imported from other devices.
 */
object SkillManager {
    private const val TAG = "SkillManager"
    private const val SKILLS_DIR = "skills"
    private const val EXT = ".clawskill"

    private fun skillsDir(ctx: Context): File =
        File(ctx.filesDir, SKILLS_DIR).also { it.mkdirs() }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    fun saveSkill(ctx: Context, skill: Skill): String {
        return try {
            val safeName = skill.name.replace(Regex("[^a-zA-Z0-9_\\- ]"), "_").trim().take(50)
            val file = File(skillsDir(ctx), "${safeName}${EXT}")
            file.writeText(skill.toJson().toString(2))
            Log.d(TAG, "Saved skill: ${file.name}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "saveSkill failed: ${e.message}")
            ""
        }
    }

    fun listSkills(ctx: Context): List<Skill> {
        return try {
            skillsDir(ctx).listFiles { f -> f.name.endsWith(EXT) }
                ?.mapNotNull { f ->
                    try { Skill.fromJson(JSONObject(f.readText())).copy(filePath = f.absolutePath) }
                    catch (e: Exception) { null }
                }
                ?.sortedByDescending { it.createdAt }
                ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    fun loadSkill(filePath: String): Skill? {
        return try { Skill.fromJson(JSONObject(File(filePath).readText())).copy(filePath = filePath) }
        catch (e: Exception) { null }
    }

    fun deleteSkill(filePath: String): Boolean =
        try { File(filePath).delete() } catch (e: Exception) { false }

    fun findSkillByName(ctx: Context, name: String): Skill? =
        listSkills(ctx).firstOrNull { it.name.equals(name, ignoreCase = true) }

    fun findSkillsByTrigger(ctx: Context, input: String): List<Skill> {
        val lower = input.lowercase()
        return listSkills(ctx).filter { skill ->
            skill.triggers.any { trigger -> lower.contains(trigger.lowercase()) }
        }
    }

    // ── Export / Import ───────────────────────────────────────────────────────

    fun exportSkill(ctx: Context, filePath: String): Intent? {
        return try {
            val file = File(filePath)
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
            Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "PhoneClaw Skill: ${file.nameWithoutExtension}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) { null }
    }

    fun importSkill(ctx: Context, uri: Uri): Skill? {
        return try {
            val content = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return null
            val skill = Skill.fromJson(JSONObject(content))
            val path = saveSkill(ctx, skill.copy(id = UUID.randomUUID().toString()))
            skill.copy(filePath = path)
        } catch (e: Exception) {
            Log.e(TAG, "importSkill failed: ${e.message}")
            null
        }
    }

    // ── AI-assisted skill creation ────────────────────────────────────────────

    /** Ask the LLM to generate a skill from a natural language description */
    suspend fun generateSkillWithAI(
        ctx: Context,
        description: String,
        byok: BYOKClient
    ): Skill? {
        val prompt = """
Create a PhoneClaw Skill from this description: "$description"

Respond with ONLY a JSON object in this exact format:
{
  "name": "Short skill name",
  "description": "What this skill does",
  "triggers": ["trigger phrase 1", "trigger phrase 2"],
  "parameters": [{"name": "param1", "description": "what it is", "required": true}],
  "code": "// JavaScript ClawScript code\nspeakText(\"running skill\");\n// ...actual automation code"
}

The code must use real ClawScript functions: openApp(), magicClicker(), speakText(), delay(), etc.
Parameters in code use {{paramName}} placeholder syntax.
""".trimIndent()

        return try {
            val messages = listOf(
                mapOf("role" to "system", "content" to "You are a PhoneClaw skill generator. Output only valid JSON."),
                mapOf("role" to "user", "content" to prompt)
            )
            val response = byok.chatSync(messages).getOrNull() ?: return null
            // Extract JSON from response
            val jsonStr = Regex("""\{.*\}""", RegexOption.DOT_MATCHES_ALL).find(response)?.value ?: response
            val json = JSONObject(jsonStr)
            Skill.fromJson(json)
        } catch (e: Exception) {
            Log.e(TAG, "generateSkillWithAI failed: ${e.message}")
            null
        }
    }

    /** Render a skill's code with parameter substitutions */
    fun renderSkill(skill: Skill, params: Map<String, String>): String {
        var code = skill.code
        params.forEach { (key, value) ->
            code = code.replace("{{$key}}", value)
        }
        return code
    }
}

data class Skill(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val triggers: List<String> = emptyList(),
    val parameters: List<SkillParameter> = emptyList(),
    val code: String,
    val version: String = "1.0",
    val author: String = "local",
    val createdAt: Long = System.currentTimeMillis(),
    val filePath: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name); put("description", description)
        put("triggers", JSONArray(triggers))
        put("parameters", JSONArray(parameters.map { p ->
            JSONObject().apply { put("name", p.name); put("description", p.description); put("required", p.required) }
        }))
        put("code", code); put("version", version); put("author", author); put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(j: JSONObject): Skill {
            val triggers = j.optJSONArray("triggers")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList()
            val params = j.optJSONArray("parameters")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val p = arr.getJSONObject(i)
                    SkillParameter(p.optString("name"), p.optString("description"), p.optBoolean("required", false))
                }
            } ?: emptyList()
            return Skill(
                id = j.optString("id", UUID.randomUUID().toString()),
                name = j.optString("name", "Unnamed Skill"),
                description = j.optString("description", ""),
                triggers = triggers, parameters = params,
                code = j.optString("code", ""),
                version = j.optString("version", "1.0"),
                author = j.optString("author", "local"),
                createdAt = j.optLong("createdAt", System.currentTimeMillis())
            )
        }
    }
}

data class SkillParameter(val name: String, val description: String, val required: Boolean = false)
