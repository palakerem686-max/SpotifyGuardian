package com.castrum.spotifyguardian;

import android.service.notification.NotificationListenerService;

/**
 * Android requires a user-enabled NotificationListenerService for apps to query active media sessions.
 * The service itself does not read or transmit notification contents.
 */
public class SpotifyNotificationListener extends NotificationListenerService {
}
