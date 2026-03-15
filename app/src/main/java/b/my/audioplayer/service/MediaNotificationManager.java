package b.my.audioplayer.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;

import androidx.core.app.NotificationCompat;

import b.my.audioplayer.R;
import b.my.audioplayer.activity.MainActivity;
import b.my.audioplayer.model.Song;
import b.my.audioplayer.utils.Constants;

public class MediaNotificationManager {

    private static final String TAG = "MediaNotificationMgr";
    private static final String CHANNEL_ID = "BPlayerMusicChannel";
    private static final int NOTIFICATION_ID = 1;

    private final Context context;
    private final NotificationManager notificationManager;
    private MediaSessionCompat mediaSession;
    private final Object lock = new Object();

    private ServiceActionCallback serviceCallback;

    public interface ServiceActionCallback {
        void onPlay();
        void onPause();
        void onNext();
        void onPrevious();
        void onStop();
        void onTogglePlayback();
    }

    public MediaNotificationManager(Context context) {
        this.context = context;
        this.notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
        setupMediaSession();
    }

    public void setServiceCallback(ServiceActionCallback cb) {
        synchronized (lock) {
            this.serviceCallback = cb;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Playback",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Music playback controls");
            channel.setShowBadge(false);
            channel.setSound(null, null);
            channel.enableVibration(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void setupMediaSession() {
        try {
            mediaSession = new MediaSessionCompat(context, "BPlayer");

            mediaSession.setCallback(new MediaSessionCompat.Callback() {

                @Override
                public void onPlay() {
                    Log.d(TAG, "MediaSession onPlay");
                    synchronized (lock) {
                        if (serviceCallback != null) serviceCallback.onPlay();
                    }
                }

                @Override
                public void onPause() {
                    Log.d(TAG, "MediaSession onPause");
                    synchronized (lock) {
                        if (serviceCallback != null) serviceCallback.onPause();
                    }
                }

                @Override
                public void onSkipToNext() {
                    Log.d(TAG, "MediaSession onSkipToNext");
                    synchronized (lock) {
                        if (serviceCallback != null) serviceCallback.onNext();
                    }
                }

                @Override
                public void onSkipToPrevious() {
                    Log.d(TAG, "MediaSession onSkipToPrevious");
                    synchronized (lock) {
                        if (serviceCallback != null) serviceCallback.onPrevious();
                    }
                }

                @Override
                public void onStop() {
                    Log.d(TAG, "MediaSession onStop");
                    synchronized (lock) {
                        if (serviceCallback != null) serviceCallback.onStop();
                    }
                }

                @Override
                public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                    KeyEvent ke = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                    if (ke != null && ke.getAction() == KeyEvent.ACTION_DOWN) {
                        Log.d(TAG, "MediaSession onMediaButtonEvent: " + ke.getKeyCode());
                        synchronized (lock) {
                            if (serviceCallback == null) return false;

                            switch (ke.getKeyCode()) {
                                case KeyEvent.KEYCODE_MEDIA_PLAY:
                                    serviceCallback.onPlay();
                                    return true;
                                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                                    serviceCallback.onPause();
                                    return true;
                                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                                case KeyEvent.KEYCODE_HEADSETHOOK:
                                    serviceCallback.onTogglePlayback();
                                    return true;
                                case KeyEvent.KEYCODE_MEDIA_NEXT:
                                    serviceCallback.onNext();
                                    return true;
                                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                                    serviceCallback.onPrevious();
                                    return true;
                                case KeyEvent.KEYCODE_MEDIA_STOP:
                                    serviceCallback.onStop();
                                    return true;
                            }
                        }
                    }
                    return super.onMediaButtonEvent(mediaButtonIntent);
                }
            });

            mediaSession.setFlags(
                    MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                            MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            );
            mediaSession.setActive(true);

        } catch (Exception e) {
            Log.e(TAG, "Failed to create MediaSession", e);
        }
    }

    private void updatePlaybackState(boolean isPlaying, long position) {
        if (mediaSession == null) return;

        long actions = PlaybackStateCompat.ACTION_PLAY_PAUSE
                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                | PlaybackStateCompat.ACTION_STOP
                | PlaybackStateCompat.ACTION_SEEK_TO
                | (isPlaying ? PlaybackStateCompat.ACTION_PAUSE : PlaybackStateCompat.ACTION_PLAY);

        PlaybackStateCompat state = new PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(
                        isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED,
                        position,
                        1.0f
                )
                .build();

        mediaSession.setPlaybackState(state);
    }

    private void updateMediaMetadata(Song song, Bitmap albumArt) {
        if (song == null || mediaSession == null) return;

        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.getTitle())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.getArtist())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.getAlbum())
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.getDuration());

        if (albumArt != null) {
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, albumArt)
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt);
        }

        mediaSession.setMetadata(builder.build());
    }

    public Notification createNotification(Song song, boolean isPlaying) {
        return buildNotification(song, isPlaying, 0L);
    }

    public Notification createNotification(Song song, boolean isPlaying, long position) {
        return buildNotification(song, isPlaying, position);
    }

    private Notification buildNotification(Song song, boolean isPlaying, long position) {
        Log.d(TAG, "buildNotification: song=" + (song != null ? song.getTitle() : "null")
                + ", isPlaying=" + isPlaying);

        Bitmap albumArt = loadAlbumArt(song);

        updatePlaybackState(isPlaying, position);
        if (song != null) {
            updateMediaMetadata(song, albumArt);
        }

        // Content intent
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                context, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Action intents
        PendingIntent prevPI = buildServiceIntent(Constants.ACTION_PREVIOUS, 1);
        PendingIntent playPausePI = buildServiceIntent(Constants.ACTION_TOGGLE_PLAYBACK, 2);
        PendingIntent nextPI = buildServiceIntent(Constants.ACTION_NEXT, 3);
        PendingIntent stopPI = buildServiceIntent(Constants.ACTION_STOP, 4);

        // Display text
        String title = (song != null && song.getTitle() != null) ? song.getTitle() : "No song playing";
        String artist = (song != null && song.getArtist() != null) ? song.getArtist() : "B Player";
        String album = (song != null) ? song.getAlbum() : null;

        // IMPORTANT: Choose correct icon based on isPlaying
        int playPauseIcon = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
        String playPauseText = isPlaying ? "Pause" : "Play";

        Log.d(TAG, "Button icon: " + (isPlaying ? "PAUSE" : "PLAY") + ", title: " + title);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(albumArt)
                .setContentTitle(title)
                .setContentText(artist)
                .setSubText(album)
                .setContentIntent(openPendingIntent)
                .setDeleteIntent(stopPI)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(isPlaying)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession != null ? mediaSession.getSessionToken() : null)
                        .setShowActionsInCompactView(0, 1, 2)
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(stopPI))
                .addAction(R.drawable.ic_skip_previous, "Previous", prevPI)
                .addAction(playPauseIcon, playPauseText, playPausePI)
                .addAction(R.drawable.ic_skip_next, "Next", nextPI);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        }

        return builder.build();
    }

    private PendingIntent buildServiceIntent(String action, int requestCode) {
        Intent intent = new Intent(context, MusicPlaybackService.class);
        intent.setAction(action);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getService(context, requestCode, intent, flags);
    }

    public void updateNotification(Song song, boolean isPlaying) {
        updateNotification(song, isPlaying, 0L);
    }

    public void updateNotification(Song song, boolean isPlaying, long position) {
        Log.d(TAG, "updateNotification: song=" + (song != null ? song.getTitle() : "null")
                + ", isPlaying=" + isPlaying);

        Notification notification = buildNotification(song, isPlaying, position);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    public void cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private Bitmap loadAlbumArt(Song song) {
        if (song != null && song.getAlbumArt() != null && !song.getAlbumArt().isEmpty()) {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(song.getAlbumArt(), options);

                options.inSampleSize = calculateInSampleSize(options, 256, 256);
                options.inJustDecodeBounds = false;

                Bitmap bmp = BitmapFactory.decodeFile(song.getAlbumArt(), options);
                if (bmp != null) return bmp;
            } catch (Exception e) {
                Log.e(TAG, "Error loading album art", e);
            }
        }
        return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_music_note);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public MediaSessionCompat getMediaSession() {
        return mediaSession;
    }

    public int getNotificationId() {
        return NOTIFICATION_ID;
    }

    public void release() {
        synchronized (lock) {
            if (mediaSession != null) {
                mediaSession.setActive(false);
                mediaSession.release();
                mediaSession = null;
            }
            serviceCallback = null;
        }
    }
}