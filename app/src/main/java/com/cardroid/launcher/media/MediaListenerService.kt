package com.cardroid.launcher.media

import android.service.notification.NotificationListenerService

/**
 * Android only lets apps read other apps' media sessions (song, artist,
 * play/pause) when they hold notification access. This service exists so the
 * user can grant that access; it does not read or store any notification.
 */
class MediaListenerService : NotificationListenerService()
