package com.cardroid.launcher.apps

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/** A big button on the home screen that opens a specific app. */
data class Shortcut(
    val label: String,
    val packageName: String,
    /** Where to get the app when it is not installed. */
    val installUrl: String,
)

object Shortcuts {
    const val SPOTIFY = "com.spotify.music"

    /** Apps in the bottom bar. Missing ones are hidden. */
    val dock = listOf(
        Shortcut("Spotify", SPOTIFY, playStore(SPOTIFY)),
        Shortcut("Waze", "com.waze", playStore("com.waze")),
        Shortcut("Google Maps", "com.google.android.apps.maps", playStore("com.google.android.apps.maps")),
        Shortcut("YouTube Music", "com.google.android.apps.youtube.music", playStore("com.google.android.apps.youtube.music")),
    )

    private fun playStore(pkg: String) = "https://play.google.com/store/apps/details?id=$pkg"
}

object AppLauncher {

    fun isInstalled(context: Context, packageName: String): Boolean =
        context.packageManager.getLaunchIntentForPackage(packageName) != null

    fun open(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return start(context, intent)
    }

    fun openShortcut(context: Context, shortcut: Shortcut) {
        if (!open(context, shortcut.packageName)) {
            start(context, Intent(Intent.ACTION_VIEW, Uri.parse(shortcut.installUrl)))
        }
    }

    fun openSettings(context: Context) {
        start(context, Intent(Settings.ACTION_SETTINGS))
    }

    /** Opens the voice assistant (Google Assistant or Gemini). */
    fun openAssistant(context: Context) {
        if (!start(context, Intent(Intent.ACTION_VOICE_COMMAND))) {
            start(context, Intent(Intent.ACTION_ASSIST))
        }
    }

    fun openNotificationAccessSettings(context: Context) {
        start(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    fun icon(context: Context, packageName: String, sizePx: Int): ImageBitmap? = try {
        context.packageManager.getApplicationIcon(packageName)
            .toBitmap(sizePx, sizePx)
            .asImageBitmap()
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
