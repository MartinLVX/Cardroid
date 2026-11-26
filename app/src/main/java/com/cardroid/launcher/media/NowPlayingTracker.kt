package com.cardroid.launcher.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NowPlaying(
    val title: String,
    val artist: String,
    val art: Bitmap?,
    val isPlaying: Boolean,
    val packageName: String,
)

/** Follows whichever app is playing audio (Spotify, YouTube Music, etc.). */
class NowPlayingTracker(private val context: Context) {

    private val sessionManager = context.getSystemService(MediaSessionManager::class.java)
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val listenerComponent = ComponentName(context, MediaListenerService::class.java)
    private val handler = Handler(Looper.getMainLooper())

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    private val _hasAccess = MutableStateFlow(false)
    val hasAccess: StateFlow<Boolean> = _hasAccess.asStateFlow()

    private var controller: MediaController? = null
    private var listening = false

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onSessionDestroyed() = refreshSessions()
    }

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            pickController(controllers.orEmpty())
        }

    fun start() {
        val granted = NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
        _hasAccess.value = granted
        if (!granted || listening) return
        try {
            sessionManager.addOnActiveSessionsChangedListener(
                sessionsListener, listenerComponent, handler
            )
            listening = true
            refreshSessions()
        } catch (e: SecurityException) {
            _hasAccess.value = false
        }
    }

    fun stop() {
        if (listening) {
            sessionManager.removeOnActiveSessionsChangedListener(sessionsListener)
            listening = false
        }
        setController(null)
    }

    fun playPause() {
        val c = controller
        if (c == null) {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            return
        }
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) {
            c.transportControls.pause()
        } else {
            c.transportControls.play()
        }
    }

    fun next() {
        controller?.transportControls?.skipToNext() ?: sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    fun previous() {
        controller?.transportControls?.skipToPrevious()
            ?: sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    /** Works without notification access, so the buttons always do something. */
    private fun sendMediaKey(keyCode: Int) {
        val time = SystemClock.uptimeMillis()
        audioManager.dispatchMediaKeyEvent(KeyEvent(time, time, KeyEvent.ACTION_DOWN, keyCode, 0))
        audioManager.dispatchMediaKeyEvent(KeyEvent(time, time, KeyEvent.ACTION_UP, keyCode, 0))
    }

    private fun refreshSessions() {
        val controllers = try {
            sessionManager.getActiveSessions(listenerComponent)
        } catch (e: SecurityException) {
            emptyList()
        }
        pickController(controllers)
    }

    private fun pickController(controllers: List<MediaController>) {
        val playing = controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        }
        setController(playing ?: controllers.firstOrNull())
    }

    private fun setController(newController: MediaController?) {
        val current = controller
        if (current != null && newController != null &&
            current.sessionToken == newController.sessionToken
        ) {
            publish()
            return
        }
        current?.unregisterCallback(controllerCallback)
        controller = newController
        newController?.registerCallback(controllerCallback, handler)
        publish()
    }

    private fun publish() {
        val c = controller
        if (c == null) {
            _nowPlaying.value = null
            return
        }
        val metadata = c.metadata
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: ""
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: ""
        val art = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        _nowPlaying.value = NowPlaying(
            title = title,
            artist = artist,
            art = art,
            isPlaying = c.playbackState?.state == PlaybackState.STATE_PLAYING,
            packageName = c.packageName,
        )
    }
}
