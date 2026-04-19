package com.example.universal

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

/**
 * AppResolver — dynamically finds installed apps by name or known package aliases.
 * No hardcoded assumptions. Falls back to fuzzy label matching.
 */
object AppResolver {
    private const val TAG = "AppResolver"

    // Well-known package aliases — multiple package names per app name
    // Order matters: first match wins
    private val KNOWN_PACKAGES = mapOf(
        "whatsapp"          to listOf("com.whatsapp", "com.whatsapp.w4b"),
        "whatsapp business" to listOf("com.whatsapp.w4b", "com.whatsapp"),
        "youtube"           to listOf("com.google.android.youtube"),
        "tiktok"            to listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"),
        "instagram"         to listOf("com.instagram.android"),
        "twitter"           to listOf("com.twitter.android", "com.x.android.auth"),
        "x"                 to listOf("com.twitter.android", "com.x.android.auth"),
        "facebook"          to listOf("com.facebook.katana"),
        "messenger"         to listOf("com.facebook.orca"),
        "telegram"          to listOf("org.telegram.messenger", "org.telegram.messenger.web"),
        "snapchat"          to listOf("com.snapchat.android"),
        "spotify"           to listOf("com.spotify.music"),
        "netflix"           to listOf("com.netflix.mediaclient"),
        "audiomack"         to listOf("com.audiomack", "com.audiomack.beta"),
        "chrome"            to listOf("com.android.chrome"),
        "firefox"           to listOf("org.mozilla.firefox"),
        "gmail"             to listOf("com.google.android.gm"),
        "maps"              to listOf("com.google.android.apps.maps"),
        "google maps"       to listOf("com.google.android.apps.maps"),
        "photos"            to listOf("com.google.android.apps.photos"),
        "drive"             to listOf("com.google.android.apps.docs"),
        "play store"        to listOf("com.android.vending"),
        "settings"          to listOf("com.android.settings"),
        "camera"            to listOf("com.google.android.GoogleCamera", "com.android.camera2", "com.android.camera"),
        "clock"             to listOf("com.google.android.deskclock", "com.android.deskclock"),
        "calculator"        to listOf("com.google.android.calculator", "com.android.calculator2"),
        "calendar"          to listOf("com.google.android.calendar"),
        "meet"              to listOf("com.google.android.apps.tachyon"),
        "discord"           to listOf("com.discord"),
        "reddit"            to listOf("com.reddit.frontpage"),
        "linkedin"          to listOf("com.linkedin.android"),
        "pinterest"         to listOf("com.pinterest"),
        "twitch"            to listOf("tv.twitch.android.app"),
        "amazon"            to listOf("com.amazon.mShop.android.shopping"),
        "uber"              to listOf("com.ubercab"),
        "lyft"              to listOf("me.lyft.android"),
        "paypal"            to listOf("com.paypal.android.p2pmobile"),
        "zoom"              to listOf("us.zoom.videomeetings"),
        "teams"             to listOf("com.microsoft.teams"),
        "outlook"           to listOf("com.microsoft.office.outlook"),
        "word"              to listOf("com.microsoft.office.word"),
        "excel"             to listOf("com.microsoft.office.excel"),
        "shazam"            to listOf("com.shazam.android"),
        "soundcloud"        to listOf("com.soundcloud.android"),
        "apple music"       to listOf("com.apple.android.music"),
        "youtube music"     to listOf("com.google.android.apps.youtube.music"),
        "files"             to listOf("com.google.android.apps.nbu.files", "com.android.documentsui"),
        "phone"             to listOf("com.google.android.dialer", "com.android.dialer"),
        "messages"          to listOf("com.google.android.apps.messaging", "com.android.mms"),
        "contacts"          to listOf("com.google.android.contacts", "com.android.contacts"),
        "notes"             to listOf("com.google.keep", "com.samsung.android.app.notes"),
        "keep"              to listOf("com.google.keep"),
        "docs"              to listOf("com.google.android.apps.docs.editors.docs"),
        "sheets"            to listOf("com.google.android.apps.docs.editors.sheets"),
        "slides"            to listOf("com.google.android.apps.docs.editors.slides"),
    )

    /**
     * Find an installed app by name. Returns package name if found, null otherwise.
     * Tries known aliases first, then fuzzy label scan.
     */
    fun resolve(ctx: Context, appName: String): String? {
        val pm = ctx.packageManager
        val normalized = appName.lowercase().trim()

        // 1. Try known aliases
        val candidates = KNOWN_PACKAGES[normalized]
            ?: KNOWN_PACKAGES.entries.firstOrNull { normalized.contains(it.key) || it.key.contains(normalized) }?.value
            ?: emptyList()

        for (pkg in candidates) {
            if (isInstalled(pm, pkg)) {
                Log.d(TAG, "Resolved '$appName' -> $pkg (known alias)")
                return pkg
            }
        }

        // 2. Fuzzy scan installed apps by label
        return try {
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val match = apps.firstOrNull { app ->
                val label = pm.getApplicationLabel(app).toString().lowercase()
                label == normalized || label.contains(normalized) || normalized.contains(label)
            }
            if (match != null) {
                Log.d(TAG, "Resolved '$appName' -> ${match.packageName} (fuzzy label)")
                match.packageName
            } else {
                Log.w(TAG, "Could not resolve '$appName'")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "resolve error: ${e.message}")
            null
        }
    }

    fun isInstalled(pm: PackageManager, pkg: String): Boolean {
        return try {
            pm.getPackageInfo(pkg, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getLaunchIntent(ctx: Context, pkg: String): Intent? {
        return ctx.packageManager.getLaunchIntentForPackage(pkg)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
    }

    /** Build a YouTube search URI intent */
    fun buildYouTubeSearchIntent(query: String): Intent {
        return Intent(Intent.ACTION_SEARCH).apply {
            setPackage("com.google.android.youtube")
            putExtra("query", query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /** Build a generic web search intent */
    fun buildWebSearchIntent(query: String): Intent {
        return Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(android.app.SearchManager.QUERY, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
