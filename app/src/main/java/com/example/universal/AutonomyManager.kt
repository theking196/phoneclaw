package com.example.universal

import android.content.Context

/**
 * AutonomyManager
 *
 * Granular user control over AI autonomy levels.
 * Gives user full upper hand over what the AI can do automatically.
 */
class AutonomyManager(private val ctx: Context) {

    enum class AutonomyLevel(val level: Int) {
        CONFIRM_EVERYTHING(0),
        CONFIRM_CRITICAL(1),
        SEMI_AUTONOMOUS(2),
        FULL_AUTONOMY(3)
    }

    enum class ActionType {
        CLICK,
        TYPE,
        FILE_WRITE,
        FILE_DELETE,
        NETWORK_REQUEST,
        INSTALL_SKILL,
        RUN_SCHEDULED_TASK,
        MODIFY_SETTINGS,
        SEND_MESSAGE,
        MAKE_CALL
    }

    fun getCurrentLevel(): AutonomyLevel {
        val level = LocalStorage.getInt(ctx, "autonomy_level", 1)
        return AutonomyLevel.values().getOrElse(level) { AutonomyLevel.CONFIRM_CRITICAL }
    }

    fun setCurrentLevel(level: AutonomyLevel) {
        LocalStorage.putInt(ctx, "autonomy_level", level.level)
    }

    fun requiresUserConfirmation(actionType: ActionType): Boolean {
        val level = getCurrentLevel()

        return when (level) {
            AutonomyLevel.CONFIRM_EVERYTHING -> true

            AutonomyLevel.CONFIRM_CRITICAL -> when (actionType) {
                ActionType.FILE_DELETE,
                ActionType.INSTALL_SKILL,
                ActionType.MODIFY_SETTINGS,
                ActionType.SEND_MESSAGE,
                ActionType.MAKE_CALL -> true
                else -> false
            }

            AutonomyLevel.SEMI_AUTONOMOUS -> when (actionType) {
                ActionType.FILE_DELETE,
                ActionType.INSTALL_SKILL,
                ActionType.MAKE_CALL -> true
                else -> false
            }

            AutonomyLevel.FULL_AUTONOMY -> false
        }
    }

    fun isActionAllowed(actionType: ActionType): Boolean {
        return LocalStorage.getBoolean(ctx, "allow_${actionType.name.lowercase()}", true)
    }

    fun setActionAllowed(actionType: ActionType, allowed: Boolean) {
        LocalStorage.putBoolean(ctx, "allow_${actionType.name.lowercase()}", allowed)
    }

    fun getDefaultConfirmations(): Map<ActionType, Boolean> {
        return ActionType.values().associateWith { requiresUserConfirmation(it) }
    }

    companion object {
        @Volatile
        private var INSTANCE: AutonomyManager? = null

        fun getInstance(ctx: Context): AutonomyManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AutonomyManager(ctx.applicationContext).also { INSTANCE = it }
            }
        }
    }
}