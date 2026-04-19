package com.example.universal

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class NotificationTriggerService : NotificationListenerService() {
    companion object {
        private const val TAG = "NotifTrigger"
        var instance: NotificationTriggerService? = null
        var onTriggerFired: ((NotificationRule, String) -> Unit)? = null

        fun saveTriggerRules(ctx: android.content.Context, rules: List<NotificationRule>) {
            val arr = JSONArray()
            rules.forEach { r -> arr.put(JSONObject().apply { put("id",r.id); put("appPackage",r.appPackage); put("matchText",r.matchText); put("script",r.script); put("enabled",r.enabled); put("description",r.description) }) }
            LocalStorage.put(ctx, "notification_triggers", arr.toString())
        }

        fun loadTriggerRules(ctx: android.content.Context): List<NotificationRule> = try {
            val arr = JSONArray(LocalStorage.get(ctx, "notification_triggers", "[]"))
            (0 until arr.length()).map { i -> val o = arr.getJSONObject(i); NotificationRule(o.getString("id"), o.optString("appPackage"), o.optString("matchText"), o.optString("script"), o.optBoolean("enabled", true), o.optString("description")) }
        } catch (e: Exception) { emptyList() }
    }

    override fun onCreate() { super.onCreate(); instance = this }
    override fun onDestroy() { super.onDestroy(); instance = null }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val pkg = sbn.packageName ?: return
        val extras = sbn.notification?.extras ?: return
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val full = "$title $text"
        loadTriggerRules(this).filter { it.enabled }.forEach { rule ->
            val pkgOk = rule.appPackage.isEmpty() || pkg.contains(rule.appPackage, ignoreCase = true)
            val txtOk = rule.matchText.isEmpty() || full.contains(rule.matchText, ignoreCase = true)
            if (pkgOk && txtOk) {
                Log.d(TAG, "Trigger fired: ${rule.description}"); LocalStorage.appendDebugLog(this, "NotifTrigger: ${rule.description}")
                onTriggerFired?.invoke(rule, full)
            }
        }
    }
}

data class NotificationRule(val id: String = UUID.randomUUID().toString(), val appPackage: String = "", val matchText: String = "", val script: String = "", val enabled: Boolean = true, val description: String = "")
