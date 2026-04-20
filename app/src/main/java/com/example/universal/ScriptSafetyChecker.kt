package com.example.universal

import android.content.Context

/**
 * ScriptSafetyChecker
 *
 * Scans JavaScript code before execution to detect action types,
 * check autonomy permissions and request user confirmation when required.
 */
class ScriptSafetyChecker(private val ctx: Context) {

    private val autonomyManager = AutonomyManager.getInstance(ctx)

    data class DetectionResult(
        val actionTypes: Set<AutonomyManager.ActionType>,
        val requiresConfirmation: Boolean,
        val requiredConfirmations: List<AutonomyManager.ActionType>
    )

    private val actionPatterns = mapOf(
        AutonomyManager.ActionType.CLICK to listOf(
            "click", "magicClicker", "clickByText", "tap"
        ),
        AutonomyManager.ActionType.TYPE to listOf(
            "type", "typeInField", "paste", "input"
        ),
        AutonomyManager.ActionType.FILE_WRITE to listOf(
            "writeFile", "appendFile", "saveScript"
        ),
        AutonomyManager.ActionType.FILE_DELETE to listOf(
            "deleteFile", "remove", "unlink"
        ),
        AutonomyManager.ActionType.NETWORK_REQUEST to listOf(
            "fetch", "http", "request", "ajax", "getUrl"
        ),
        AutonomyManager.ActionType.INSTALL_SKILL to listOf(
            "installSkill", "importSkill"
        ),
        AutonomyManager.ActionType.MODIFY_SETTINGS to listOf(
            "setPreference", "changeSetting", "modifyConfig"
        ),
        AutonomyManager.ActionType.SEND_MESSAGE to listOf(
            "sendMessage", "postTweet", "sendEmail", "sendText"
        ),
        AutonomyManager.ActionType.MAKE_CALL to listOf(
            "makeCall", "dialNumber", "callPhone"
        )
    )

    fun scanCode(code: String): DetectionResult {
        val detectedTypes = mutableSetOf<AutonomyManager.ActionType>()

        actionPatterns.forEach { (actionType, patterns) ->
            patterns.forEach { pattern ->
                if (code.contains(pattern, ignoreCase = true)) {
                    detectedTypes.add(actionType)
                }
            }
        }

        val requiresConfirm = detectedTypes.any {
            autonomyManager.requiresUserConfirmation(it)
        }

        val required = detectedTypes.filter {
            autonomyManager.requiresUserConfirmation(it)
        }

        return DetectionResult(
            actionTypes = detectedTypes,
            requiresConfirmation = requiresConfirm,
            requiredConfirmations = required
        )
    }

    fun isCodeAllowed(code: String): Boolean {
        val result = scanCode(code)

        return result.actionTypes.all {
            autonomyManager.isActionAllowed(it)
        }
    }

    suspend fun requestUserConfirmation(code: String): Boolean {
        val result = scanCode(code)
        if (!result.requiresConfirmation) return true

        // This will be integrated with floating UI dialog
        val controller = AgentExecutionController.getInstance(ctx)
        val question = buildConfirmationMessage(result)

        val answer = controller.askUserClarification(question)

        return answer.equals("yes", ignoreCase = true) ||
               answer.equals("allow", ignoreCase = true) ||
               answer.equals("ok", ignoreCase = true)
    }

    private fun buildConfirmationMessage(result: DetectionResult): String {
        return buildString {
            appendLine("⚠️ Requesting permission to perform these actions:")
            appendLine()
            result.requiredConfirmations.forEach { action ->
                appendLine("  • ${action.name}")
            }
            appendLine()
            appendLine("Reply YES to allow, NO to deny.")
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: ScriptSafetyChecker? = null

        fun getInstance(ctx: Context): ScriptSafetyChecker {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ScriptSafetyChecker(ctx.applicationContext).also { INSTANCE = it }
            }
        }
    }
}