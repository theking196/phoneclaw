package com.example.universal

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class PhoneClawWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_phoneclaw)

        // Voice button - opens voice input
        val voiceIntent = Intent(context, MainActivity::class.java).apply {
            action = "android.intent.action.VOICE_SEARCH"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val voicePending = PendingIntent.getActivity(context, 0, voiceIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        views.setOnClickPendingIntent(R.id.widget_mic, voicePending)

        // Chat button - opens chat
        val chatIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chatPending = PendingIntent.getActivity(context, 1, chatIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        views.setOnClickPendingIntent(R.id.widget_chat, chatPending)

        // Skills button - opens library
        val skillsIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("open_tab", "library")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val skillsPending = PendingIntent.getActivity(context, 2, skillsIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        views.setOnClickPendingIntent(R.id.widget_skills, skillsPending)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}