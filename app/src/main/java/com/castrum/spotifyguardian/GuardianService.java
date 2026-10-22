package com.castrum.spotifyguardian;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.util.List;

public class GuardianService extends Service {
    public static final String ACTION_START = "com.castrum.spotifyguardian.START";
    private static final String CHANNEL_ID = "spotify_guardian_status";
    private static final int ONGOING_ID = 4101;
    private static final int ALERT_ID = 4102;
    private static final long POLL_MS = 10_000L;
    private static final long MISSING_SESSION_MS = 20_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private MediaSessionManager mediaSessionManager;
    private boolean spotifyWasPlaying = false;
    private long lastSeenPlayingAt = 0L;
    private boolean alertSent = false;

    private final Runnable poller = new Runnable() {
        @Override public void run() {
            inspectMediaSessions();
            handler.postDelayed(this, POLL_MS);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        startForeground(ONGOING_ID, buildNotification(
                "Koruyucu etkin", "Spotify medya oturumu izleniyor.", false));
        handler.post(poller);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void inspectMediaSessions() {
        if (mediaSessionManager == null) return;
        try {
            ComponentName listener = new ComponentName(this, SpotifyNotificationListener.class);
            List<MediaController> controllers = mediaSessionManager.getActiveSessions(listener);
            MediaController spotify = null;
            for (MediaController controller : controllers) {
                if ("com.spotify.music".equals(controller.getPackageName())) {
                    spotify = controller;
                    break;
                }
            }

            if (spotify != null) {
                PlaybackState state = spotify.getPlaybackState();
                if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                    spotifyWasPlaying = true;
                    lastSeenPlayingAt = System.currentTimeMillis();
                    alertSent = false;
                    updateOngoing("Spotify çalıyor", "Medya oturumu aktif.");
                } else {
                    // A present but paused session is not treated as an unexpected process death.
                    updateOngoing("Koruyucu etkin", "Spotify oturumu mevcut; çalma durumu aktif değil.");
                }
                return;
            }

            if (spotifyWasPlaying && !alertSent && lastSeenPlayingAt > 0L
                    && System.currentTimeMillis() - lastSeenPlayingAt >= MISSING_SESSION_MS) {
                alertSent = true;
                notifySpotifyMayHaveStopped();
                updateOngoing("Spotify oturumu kayboldu", "Spotify'ı yeniden açmak için uyarıya dokun.");
            } else if (!spotifyWasPlaying) {
                updateOngoing("Koruyucu etkin", "Spotify çalmaya başladığında izlemeye alınacak.");
            }
        } catch (SecurityException e) {
            updateOngoing("Erişim gerekli", "Uygulama ayarlarından medya/bildirim erişimini aç.");
        } catch (RuntimeException e) {
            updateOngoing("İzleme sürüyor", "Medya oturumu şu anda okunamadı.");
        }
    }

    private void notifySpotifyMayHaveStopped() {
        Notification notification = buildNotification(
                "Spotify durmuş olabilir",
                "Spotify'ın medya oturumu kayboldu. Yeniden açmak için dokun.", true);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(ALERT_ID, notification);
    }

    private void updateOngoing(String title, String text) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(ONGOING_ID, buildNotification(title, text, false));
    }

    private Notification buildNotification(String title, String text, boolean alert) {
        Intent openIntent = getPackageManager().getLaunchIntentForPackage("com.spotify.music");
        if (openIntent == null) openIntent = new Intent(this, MainActivity.class);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pending = PendingIntent.getActivity(this, alert ? 4103 : 4104, openIntent, flags);

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        builder.setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(pending)
                .setOngoing(!alert)
                .setAutoCancel(alert)
                .setOnlyAlertOnce(true);
        return builder.build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Spotify Guardian", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Spotify medya oturumu izleme bildirimi");
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(poller);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
