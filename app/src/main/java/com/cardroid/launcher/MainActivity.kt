package com.cardroid.launcher

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cardroid.launcher.apps.AppEntry
import com.cardroid.launcher.apps.AppLauncher
import com.cardroid.launcher.media.NowPlayingTracker
import com.cardroid.launcher.ui.AppDrawer
import com.cardroid.launcher.ui.CardroidTheme
import com.cardroid.launcher.ui.HomeScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var tracker: NowPlayingTracker

    /** Bumped every time the home screen comes back, to refresh app lists and icons. */
    private var resumeCount by mutableIntStateOf(0)
    private var showApps by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()

        tracker = NowPlayingTracker(applicationContext)

        setContent {
            CardroidTheme {
                val nowPlaying by tracker.nowPlaying.collectAsStateWithLifecycle()
                val hasAccess by tracker.hasAccess.collectAsStateWithLifecycle()
                val iconPx = with(LocalDensity.current) { 72.dp.roundToPx() }

                // A launcher must never close on Back; Back only leaves the app list.
                BackHandler { showApps = false }

                if (showApps) {
                    val apps by produceState<List<AppEntry>?>(null, resumeCount) {
                        value = withContext(Dispatchers.IO) {
                            AppLauncher.loadApps(applicationContext, iconPx)
                        }
                    }
                    AppDrawer(apps = apps, onClose = { showApps = false })
                } else {
                    HomeScreen(
                        nowPlaying = nowPlaying,
                        hasMediaAccess = hasAccess,
                        installedVersion = resumeCount,
                        onPlayPause = tracker::playPause,
                        onNext = tracker::next,
                        onPrevious = tracker::previous,
                        onOpenApps = { showApps = true },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeCount++
        hideSystemBars()
        // Picks up notification access right after the user grants it.
        tracker.start()
    }

    override fun onPause() {
        super.onPause()
        tracker.stop()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // Pressing Home while on the launcher returns to the main screen.
        showApps = false
    }

    private fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}
