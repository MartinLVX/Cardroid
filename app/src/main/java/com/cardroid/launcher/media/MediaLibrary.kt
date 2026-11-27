package com.cardroid.launcher.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.os.Bundle
import android.service.media.MediaBrowserService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LibraryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val art: Bitmap?,
    val browsable: Boolean,
    val playable: Boolean,
)

data class LibraryState(
    val status: Status = Status.Idle,
    /** Folder titles from the root to the folder on screen. */
    val path: List<String> = emptyList(),
    val items: List<LibraryItem> = emptyList(),
    val loading: Boolean = false,
) {
    enum class Status { Idle, Connecting, Ready, Unavailable }
}

/**
 * Browses a music app's library (playlists, albums...) the way car screens do.
 * Many apps, Spotify included, only let approved car systems in; when they
 * refuse, the state becomes Unavailable and the player shows only the queue.
 */
class MediaLibrary(private val context: Context) {

    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    private var browser: MediaBrowser? = null
    private var connectedPackage: String? = null
    /** (mediaId, title) of each open folder; the last one is on screen. */
    private val stack = mutableListOf<Pair<String, String>>()

    fun connect(packageName: String) {
        if (connectedPackage == packageName && browser != null) return
        disconnect()
        val service = findBrowserService(packageName)
        if (service == null) {
            _state.value = LibraryState(status = LibraryState.Status.Unavailable)
            return
        }
        connectedPackage = packageName
        _state.value = LibraryState(status = LibraryState.Status.Connecting)
        val newBrowser = MediaBrowser(context, service, object : MediaBrowser.ConnectionCallback() {
            override fun onConnected() {
                val root = browser?.root ?: return markUnavailable()
                stack.clear()
                stack.add(root to "Biblioteca")
                load(root)
            }

            override fun onConnectionFailed() = markUnavailable()

            override fun onConnectionSuspended() = markUnavailable()
        }, Bundle())
        browser = newBrowser
        try {
            newBrowser.connect()
        } catch (e: Exception) {
            markUnavailable()
        }
    }

    fun disconnect() {
        try {
            stack.lastOrNull()?.let { browser?.unsubscribe(it.first) }
            browser?.disconnect()
        } catch (e: Exception) {
            // Already disconnected.
        }
        browser = null
        connectedPackage = null
        stack.clear()
        _state.value = LibraryState()
    }

    fun open(item: LibraryItem) {
        if (item.browsable) {
            stack.lastOrNull()?.let { browser?.unsubscribe(it.first) }
            stack.add(item.id to item.title)
            load(item.id)
        } else if (item.playable) {
            play(item)
        }
    }

    /** Returns false when already at the top of the library. */
    fun back(): Boolean {
        if (stack.size <= 1) return false
        val current = stack.removeAt(stack.lastIndex)
        browser?.unsubscribe(current.first)
        load(stack.last().first)
        return true
    }

    private fun play(item: LibraryItem) {
        val token = browser?.sessionToken ?: return
        try {
            MediaController(context, token).transportControls.playFromMediaId(item.id, null)
        } catch (e: Exception) {
            // The app refused the request.
        }
    }

    private fun load(parentId: String) {
        val b = browser ?: return
        _state.value = _state.value.copy(
            status = LibraryState.Status.Ready,
            path = stack.map { it.second },
            loading = true,
        )
        try {
            b.subscribe(parentId, object : MediaBrowser.SubscriptionCallback() {
                override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowser.MediaItem>) {
                    if (stack.lastOrNull()?.first != parentId) return
                    _state.value = _state.value.copy(
                        items = children.map { child ->
                            LibraryItem(
                                id = child.mediaId.orEmpty(),
                                title = child.description.title?.toString().orEmpty(),
                                subtitle = child.description.subtitle?.toString().orEmpty(),
                                art = child.description.iconBitmap,
                                browsable = child.isBrowsable,
                                playable = child.isPlayable,
                            )
                        },
                        loading = false,
                    )
                }

                override fun onError(parentId: String) {
                    _state.value = _state.value.copy(items = emptyList(), loading = false)
                }
            })
        } catch (e: Exception) {
            markUnavailable()
        }
    }

    private fun markUnavailable() {
        _state.value = LibraryState(status = LibraryState.Status.Unavailable)
    }

    private fun findBrowserService(packageName: String): ComponentName? = try {
        val intent = Intent(MediaBrowserService.SERVICE_INTERFACE).setPackage(packageName)
        @Suppress("DEPRECATION")
        context.packageManager.queryIntentServices(intent, 0)
            .firstOrNull()
            ?.serviceInfo
            ?.let { ComponentName(it.packageName, it.name) }
    } catch (e: Exception) {
        null
    }
}
