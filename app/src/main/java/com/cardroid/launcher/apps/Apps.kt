package com.cardroid.launcher.apps

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class AppEntry(
    val label: String,
    val packageName: String,
    val icon: ImageBitmap,
)

/** A big button on the home screen that opens a specific app. */
data class Shortcut(
    val label: String,
    val packageName: String,
    /** Where to get the app when it is not installed. */
    val installUrl: String,
)

object Shortcuts {
    const val DIPLAY = "com.shihab.diplay"
    const val SPOTIFY = "com.spotify.music"

    val carPlay = Shortcut("CarPlay", DIPLAY, "https://github.com/shihabal3amri/DiPlay/releases")

    /** Apps in the bottom bar. Missing ones are hidden, except CarPlay. */
    val dock = listOf(
        carPlay,
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

    /** Every app that shows up in a normal app drawer, sorted by name. */
    fun loadApps(context: Context, iconSizePx: Int): List<AppEntry> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        return activities
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
            .map {
                AppEntry(
                    label = it.loadLabel(pm).toString(),
                    packageName = it.activityInfo.packageName,
                    icon = it.loadIcon(pm).toBitmap(iconSizePx, iconSizePx).asImageBitmap(),
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
