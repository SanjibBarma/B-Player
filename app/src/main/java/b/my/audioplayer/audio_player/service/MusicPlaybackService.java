package b.my.audioplayer.audio_player.service;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.view.KeyEvent;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleService;

import java.util.List;

import b.my.audioplayer.audio_player.model.Song;
import b.my.audioplayer.audio_player.player.MusicPlayer;
import b.my.audioplayer.utils.Constants;
import b.my.audioplayer.audio_player.widget.MusicWidgetProvider;
import b.my.audioplayer.audio_player.widget.MusicWidgetProviderLarge;

public class MusicPlaybackService extends LifecycleService
        implements MusicPlayer.PlayerCallback,
        MediaNotificationManager.ServiceActionCallback {

    private static final String TAG = "MusicService";

    private MusicPlayer musicPlayer;
    private MediaNotificationManager notificationManager;
    private final IBinder binder = new MusicBinder();
    private AudioManager audioManager;
    private boolean isServiceRunning = false;
    private boolean receiversRegistered = false;

    public class MusicBinder extends Binder {
        public MusicPlaybackService getService() {
            return MusicPlaybackService.this;
        }
    }

    private final BroadcastReceiver audioReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
                if (musicPlayer != null && musicPlayer.isPlaying()) {
                    pause();
                }
            } else if (Intent.ACTION_MEDIA_BUTTON.equals(action)) {
                KeyEvent ke = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (ke != null && ke.getAction() == KeyEvent.ACTION_DOWN) {
                    handleMediaButtonEvent(ke);
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        musicPlayer = new MusicPlayer(this);
        musicPlayer.setCallback(this);
        notificationManager = new MediaNotificationManager(this);
        notificationManager.setServiceCallback(this);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        registerReceivers();
    }

    @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerReceivers() {
        if (receiversRegistered) return;
        IntentFilter audioFilter = new IntentFilter();
        audioFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        audioFilter.addAction(Intent.ACTION_MEDIA_BUTTON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(audioReceiver, audioFilter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(audioReceiver, audioFilter);
        }
        receiversRegistered = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        String action = intent != null ? intent.getAction() : null;
        if (!isServiceRunning) {
            startForegroundNotification();
            isServiceRunning = true;
        }
        if (action != null) {
            handleAction(action);
        }
        return START_STICKY;
    }

    private void startForegroundNotification() {
        Song currentSong = musicPlayer != null ? musicPlayer.getCurrentSong() : null;
        boolean isPlaying = musicPlayer != null && musicPlayer.isPlaying();
        Notification notification = notificationManager.createNotification(currentSong, isPlaying, 0L);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(notificationManager.getNotificationId(), notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(notificationManager.getNotificationId(), notification);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (receiversRegistered) {
            try {
                unregisterReceiver(audioReceiver);
                receiversRegistered = false;
            } catch (Exception ignored) {
            }
        }
        if (audioManager != null) {
            audioManager.abandonAudioFocus(audioFocusChangeListener);
        }
        if (notificationManager != null) {
            notificationManager.cancelNotification();
            notificationManager.release();
        }
        if (musicPlayer != null) {
            musicPlayer.release();
        }
        isServiceRunning = false;
    }

    private void handleAction(String action) {
        switch (action) {
            case Constants.ACTION_PLAY:
                play();
                break;
            case Constants.ACTION_PAUSE:
                pause();
                break;
            case Constants.ACTION_NEXT:
                playNext();
                break;
            case Constants.ACTION_PREVIOUS:
                playPrevious();
                break;
            case Constants.ACTION_STOP:
                stopSelf();
                break;
            case Constants.ACTION_TOGGLE_PLAYBACK:
                togglePlayback();
                break;
        }
    }

    private void handleMediaButtonEvent(KeyEvent ke) {
        switch (ke.getKeyCode()) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                play();
                break;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                pause();
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_HEADSETHOOK:
                togglePlayback();
                break;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                playNext();
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                playPrevious();
                break;
            case KeyEvent.KEYCODE_MEDIA_STOP:
                stopSelf();
                break;
        }
    }

    @Override
    public void onPlay() {
        play();
    }

    @Override
    public void onPause() {
        pause();
    }

    @Override
    public void onNext() {
        playNext();
    }

    @Override
    public void onPrevious() {
        playPrevious();
    }

    @Override
    public void onStop() {
        stopSelf();
    }

    @Override
    public void onTogglePlayback() {
        togglePlayback();
    }

    public void setPlaylistAndPlay(List<Song> songs, int startIndex) {
        if (musicPlayer != null) {
            musicPlayer.setPlaylist(songs, startIndex);
            play();
        }
    }

    public void setPlaylist(List<Song> songs) {
        if (musicPlayer != null) musicPlayer.setPlaylist(songs);
    }

    public void setPlaylist(List<Song> songs, int startIndex) {
        if (musicPlayer != null) musicPlayer.setPlaylist(songs, startIndex);
    }

    public void play() {
        if (musicPlayer == null) return;
        if (requestAudioFocus()) {
            musicPlayer.play();
        }
    }

    public void pause() {
        if (musicPlayer != null) musicPlayer.pause();
    }

    public void togglePlayback() {
        if (musicPlayer == null) return;
        if (musicPlayer.isPlaying()) pause();
        else play();
    }

    public void playNext() {
        if (musicPlayer != null) musicPlayer.playNext();
    }

    public void playPrevious() {
        if (musicPlayer != null) musicPlayer.playPrevious();
    }

    public void playSongAt(int index) {
        if (musicPlayer != null) musicPlayer.playSongAt(index);
    }

    public void seekTo(long position) {
        if (musicPlayer != null) musicPlayer.seekTo(position);
    }

    public MusicPlayer getMusicPlayer() {
        return musicPlayer;
    }

    public Song getCurrentSong() {
        return musicPlayer != null ? musicPlayer.getCurrentSong() : null;
    }

    public boolean isPlaying() {
        return musicPlayer != null && musicPlayer.isPlaying();
    }

    public List<Song> getCurrentPlaylist() {
        return musicPlayer != null ? musicPlayer.getCurrentPlaylist() : null;
    }

    public int getCurrentIndex() {
        return musicPlayer != null ? musicPlayer.getCurrentIndex() : -1;
    }

    private boolean requestAudioFocus() {
        if (audioManager == null) return false;
        int result = audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = focusChange -> {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (musicPlayer != null && musicPlayer.isPlaying()) pause();
                break;
        }
    };

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        Song song = musicPlayer != null ? musicPlayer.getCurrentSong() : null;
        long position = musicPlayer != null ? musicPlayer.getCurrentPosition() : 0L;
        long duration = musicPlayer != null ? musicPlayer.getDuration() : 0L;
        if (notificationManager != null)
            notificationManager.updateNotification(song, isPlaying, position);
        updateWidgets(song, isPlaying, position, duration);
    }

    @Override
    public void onSongChanged(Song song) {
        boolean isPlaying = musicPlayer != null && musicPlayer.isPlaying();
        long duration = musicPlayer != null ? musicPlayer.getDuration() : 0L;
        if (notificationManager != null)
            notificationManager.updateNotification(song, isPlaying, 0L);
        updateWidgets(song, isPlaying, 0L, duration);
    }

    @Override
    public void onPositionChanged(long position, long duration) {
        Song song = musicPlayer != null ? musicPlayer.getCurrentSong() : null;
        boolean isPlaying = musicPlayer != null && musicPlayer.isPlaying();
        updateWidgets(song, isPlaying, position, duration);
    }

    @Override
    public void onPlaybackError(String message) {
    }

    private void updateWidgets(Song song, boolean isPlaying, long position, long duration) {
        String title = song != null ? song.getTitle() : "No song playing";
        String artist = song != null ? song.getArtist() : "B Player";
        String albumArt = song != null ? song.getAlbumArt() : null;

        // Update Small Widget
        MusicWidgetProvider.updateWidgetInfo(
                this, title, artist, albumArt, isPlaying, position, duration
        );

        // Update Large Widget
        MusicWidgetProviderLarge.updateWidgetInfo(
                this, title, artist, albumArt, isPlaying, position, duration
        );
    }
}