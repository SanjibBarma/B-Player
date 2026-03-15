package b.my.audioplayer.audio_player.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.preference.PreferenceManager;
import b.my.audioplayer.audio_player.database.AppDatabase;
import b.my.audioplayer.audio_player.model.Song;
import b.my.audioplayer.utils.PreferenceHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusicPlayer {

    private ExoPlayer exoPlayer;
    private ExoPlayer nextPlayer;
    private Context context;
    private List<Song> originalPlaylist;
    private List<Song> currentPlaylist;
    private int currentIndex = 0;
    private boolean isShuffleEnabled = false;
    private int repeatMode = Player.REPEAT_MODE_OFF;
    private PreferenceHelper preferenceHelper;
    private SharedPreferences sharedPreferences;
    private PlayerCallback callback;
    private Handler handler;
    private Runnable positionUpdateRunnable;
    private boolean isPositionUpdateRunning = false;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private float currentPlaybackSpeed = 1.0f;

    // Crossfade variables
    private boolean isCrossfading = false;
    private Handler crossfadeHandler;
    private Runnable crossfadeMonitorRunnable;
    private static final int FADE_INTERVAL = 50;

    // Volume Normalization variables
    private static final float NORMALIZED_VOLUME_LEVEL = 0.75f;
    private float currentVolume = 1.0f;

    public interface PlayerCallback {
        void onPlaybackStateChanged(boolean isPlaying);
        void onSongChanged(Song song);
        void onPositionChanged(long position, long duration);
        void onPlaybackError(String message);
    }

    public MusicPlayer(Context context) {
        this.context = context;
        this.preferenceHelper = new PreferenceHelper(context);
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.originalPlaylist = new ArrayList<>();
        this.currentPlaylist = new ArrayList<>();
        this.handler = new Handler(Looper.getMainLooper());
        this.crossfadeHandler = new Handler(Looper.getMainLooper());

        this.currentPlaybackSpeed = preferenceHelper.getPlaybackSpeed();

        initializePlayer();
        loadSavedState();
    }

    private void initializePlayer() {
        exoPlayer = new ExoPlayer.Builder(context).build();
        exoPlayer.setPlaybackSpeed(currentPlaybackSpeed);
        setupPlayerListener();
    }

    private void setupPlayerListener() {
        if (exoPlayer == null) return;

        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_ENDED) {
                    handlePlaybackEnded();
                }
                // Volume normalization apply koro jokhon ready
                if (state == Player.STATE_READY) {
                    applyVolumeNormalization();
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (callback != null) {
                    callback.onPlaybackStateChanged(isPlaying);
                }
                if (isPlaying) {
                    startPositionUpdates();
                    if (isCrossfadeEnabled()) {
                        startCrossfadeMonitoring();
                    }
                } else {
                    stopPositionUpdates();
                }
            }

            @Override
            public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                if (!isCrossfading) {
                    int newIndex = exoPlayer.getCurrentMediaItemIndex();
                    if (newIndex >= 0 && newIndex < currentPlaylist.size()) {
                        currentIndex = newIndex;
                        if (callback != null) {
                            callback.onSongChanged(currentPlaylist.get(currentIndex));
                        }
                    }
                    // Notun song e volume normalization apply koro
                    applyVolumeNormalization();
                }
                saveCurrentState();
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                if (callback != null) {
                    callback.onPlaybackError(error.getMessage());
                }
            }
        });
    }

    // ==================== VOLUME NORMALIZATION METHODS ====================

    private void applyVolumeNormalization() {
        if (exoPlayer == null || isCrossfading) return;

        if (isVolumeNormalizationEnabled()) {
            currentVolume = NORMALIZED_VOLUME_LEVEL;
        } else {
            currentVolume = 1.0f;
        }

        exoPlayer.setVolume(currentVolume);
    }

    public void updateVolumeNormalization() {
        applyVolumeNormalization();
    }

    // ==================== CROSSFADE METHODS ====================

    private boolean isCrossfadeEnabled() {
        return sharedPreferences.getBoolean("crossfade_enabled", false);
    }

    private int getCrossfadeDuration() {
        return sharedPreferences.getInt("crossfade_duration", 3);
    }

    private boolean isVolumeNormalizationEnabled() {
        return sharedPreferences.getBoolean("volume_normalization", false);
    }

    private void startCrossfadeMonitoring() {
        stopCrossfadeMonitoring();

        crossfadeMonitorRunnable = new Runnable() {
            @Override
            public void run() {
                if (exoPlayer != null && exoPlayer.isPlaying() && !isCrossfading) {
                    long currentPosition = exoPlayer.getCurrentPosition();
                    long duration = exoPlayer.getDuration();
                    int crossfadeDuration = getCrossfadeDuration() * 1000;

                    if (duration > 0 && (duration - currentPosition) <= crossfadeDuration) {
                        int nextIndex = getNextIndex();
                        if (nextIndex != -1) {
                            startCrossfade(crossfadeDuration);
                        }
                    } else {
                        crossfadeHandler.postDelayed(this, 200);
                    }
                }
            }
        };

        crossfadeHandler.postDelayed(crossfadeMonitorRunnable, 200);
    }

    private void stopCrossfadeMonitoring() {
        if (crossfadeMonitorRunnable != null) {
            crossfadeHandler.removeCallbacks(crossfadeMonitorRunnable);
            crossfadeMonitorRunnable = null;
        }
    }

    private int getNextIndex() {
        if (currentPlaylist.isEmpty()) return -1;

        if (repeatMode == Player.REPEAT_MODE_ONE) {
            return currentIndex;
        }

        int next = currentIndex + 1;
        if (next >= currentPlaylist.size()) {
            if (repeatMode == Player.REPEAT_MODE_ALL) {
                return 0;
            }
            return -1;
        }
        return next;
    }

    private void startCrossfade(int durationMs) {
        if (isCrossfading) return;

        int nextIndex = getNextIndex();
        if (nextIndex == -1) return;

        isCrossfading = true;

        try {
            Song nextSong = currentPlaylist.get(nextIndex);

            nextPlayer = new ExoPlayer.Builder(context).build();
            nextPlayer.setPlaybackSpeed(currentPlaybackSpeed);

            MediaItem mediaItem = MediaItem.fromUri(nextSong.getPath());
            nextPlayer.setMediaItem(mediaItem);
            nextPlayer.prepare();
            nextPlayer.setVolume(0f);
            nextPlayer.play();

            executeCrossfade(durationMs, nextIndex);

        } catch (Exception e) {
            e.printStackTrace();
            isCrossfading = false;
            if (nextPlayer != null) {
                nextPlayer.release();
                nextPlayer = null;
            }
        }
    }

    private void executeCrossfade(int durationMs, int nextIndex) {
        int steps = durationMs / FADE_INTERVAL;
        final float volumeStep = 1.0f / steps;

        // Volume normalization check
        final float maxVolume = isVolumeNormalizationEnabled() ? NORMALIZED_VOLUME_LEVEL : 1.0f;

        new Thread(() -> {
            for (int i = 0; i <= steps && isCrossfading; i++) {
                float currentVol = (1.0f - (volumeStep * i)) * maxVolume;
                float nextVol = (volumeStep * i) * maxVolume;

                final float finalCurrentVol = Math.max(0f, Math.min(maxVolume, currentVol));
                final float finalNextVol = Math.max(0f, Math.min(maxVolume, nextVol));

                handler.post(() -> {
                    try {
                        if (exoPlayer != null) {
                            exoPlayer.setVolume(finalCurrentVol);
                        }
                        if (nextPlayer != null) {
                            nextPlayer.setVolume(finalNextVol);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                try {
                    Thread.sleep(FADE_INTERVAL);
                } catch (InterruptedException e) {
                    break;
                }
            }

            handler.post(() -> completeCrossfade(nextIndex));

        }).start();
    }

    private void completeCrossfade(int nextIndex) {
        try {
            if (exoPlayer != null) {
                exoPlayer.stop();
                exoPlayer.release();
            }

            exoPlayer = nextPlayer;
            nextPlayer = null;

            if (exoPlayer != null) {
                // Volume normalization based volume set koro
                float targetVolume = isVolumeNormalizationEnabled() ? NORMALIZED_VOLUME_LEVEL : 1.0f;
                exoPlayer.setVolume(targetVolume);
                currentVolume = targetVolume;

                setupPlayerListener();
            }

            currentIndex = nextIndex;

            if (callback != null && currentIndex < currentPlaylist.size()) {
                callback.onSongChanged(currentPlaylist.get(currentIndex));
                callback.onPlaybackStateChanged(exoPlayer != null && exoPlayer.isPlaying());
            }

            isCrossfading = false;
            saveCurrentState();

            if (isCrossfadeEnabled() && exoPlayer != null && exoPlayer.isPlaying()) {
                startCrossfadeMonitoring();
            }

        } catch (Exception e) {
            e.printStackTrace();
            isCrossfading = false;
        }
    }

    private void cancelCrossfade() {
        isCrossfading = false;
        stopCrossfadeMonitoring();
        if (nextPlayer != null) {
            nextPlayer.release();
            nextPlayer = null;
        }
        // Crossfade cancel hole volume thik koro
        applyVolumeNormalization();
    }

    // ==================== EXISTING METHODS ====================

    private void loadSavedState() {
        isShuffleEnabled = preferenceHelper.isShuffleEnabled();
        repeatMode = preferenceHelper.getRepeatMode();
        exoPlayer.setRepeatMode(repeatMode);

        long lastSongId = preferenceHelper.getLastPlayedSongId();
        executorService.execute(() -> {
            List<Song> allSongs = AppDatabase.getInstance(context).songDao().getAllSongsSync();
            if (allSongs != null && !allSongs.isEmpty()) {
                handler.post(() -> {
                    setPlaylistInternal(allSongs, true);

                    if (lastSongId != -1) {
                        for (int i = 0; i < currentPlaylist.size(); i++) {
                            if (currentPlaylist.get(i).getId() == lastSongId) {
                                currentIndex = i;
                                break;
                            }
                        }
                    }
                    exoPlayer.seekTo(currentIndex, preferenceHelper.getLastPosition());
                    if (callback != null && currentIndex < currentPlaylist.size()) {
                        callback.onSongChanged(currentPlaylist.get(currentIndex));
                    }
                });
            }
        });
    }

    private void saveCurrentState() {
        if (currentIndex >= 0 && currentIndex < currentPlaylist.size()) {
            preferenceHelper.setLastPlayedSongId(currentPlaylist.get(currentIndex).getId());
        }
        if (exoPlayer != null) {
            preferenceHelper.setLastPosition(exoPlayer.getCurrentPosition());
        }
        preferenceHelper.setShuffleEnabled(isShuffleEnabled);
        preferenceHelper.setRepeatMode(repeatMode);
    }

    public void setCallback(PlayerCallback callback) {
        this.callback = callback;
    }

    public void setPlaylist(List<Song> songs) {
        setPlaylist(songs, 0);
    }

    public void setPlaylist(List<Song> songs, int startIndex) {
        cancelCrossfade();

        this.originalPlaylist = new ArrayList<>(songs);
        this.currentPlaylist = new ArrayList<>(songs);

        if (startIndex < 0 || startIndex >= currentPlaylist.size()) {
            this.currentIndex = 0;
        } else {
            this.currentIndex = startIndex;
        }

        preparePlaylist(0);
    }

    private void setPlaylistInternal(List<Song> songs, boolean applyShuffle) {
        this.originalPlaylist = new ArrayList<>(songs);
        this.currentPlaylist = new ArrayList<>(songs);
        this.currentIndex = 0;

        if (applyShuffle && isShuffleEnabled) {
            shufflePlaylist();
        }

        preparePlaylist(0);
    }

    private void preparePlaylist(long startPositionMs) {
        if (currentPlaylist == null || currentPlaylist.isEmpty()) return;

        List<MediaItem> mediaItems = new ArrayList<>();
        for (Song song : currentPlaylist) {
            MediaItem mediaItem = MediaItem.fromUri(song.getPath());
            mediaItems.add(mediaItem);
        }

        exoPlayer.setMediaItems(mediaItems, currentIndex, startPositionMs);
        exoPlayer.prepare();
    }

    private void shufflePlaylist() {
        Song currentSong = null;
        if (currentIndex >= 0 && currentIndex < currentPlaylist.size()) {
            currentSong = currentPlaylist.get(currentIndex);
        }

        Collections.shuffle(currentPlaylist);

        if (currentSong != null) {
            currentPlaylist.remove(currentSong);
            currentPlaylist.add(0, currentSong);
            currentIndex = 0;
        }
    }

    public void play() {
        if (exoPlayer == null) return;

        if (currentPlaylist == null || currentPlaylist.isEmpty()) {
            if (callback != null) {
                callback.onPlaybackError("Playlist is empty");
            }
            return;
        }

        if (currentIndex < 0 || currentIndex >= currentPlaylist.size()) {
            currentIndex = 0;
        }

        exoPlayer.play();
    }

    public void pause() {
        exoPlayer.pause();
        saveCurrentState();
    }

    public void stop() {
        cancelCrossfade();
        exoPlayer.stop();
        saveCurrentState();
    }

    public void playNext() {
        if (isCrossfading) return;

        cancelCrossfade();

        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem();
            play();
        } else if (repeatMode == Player.REPEAT_MODE_ALL) {
            exoPlayer.seekTo(0, 0);
            play();
        }
    }

    public void playPrevious() {
        cancelCrossfade();

        if (exoPlayer.getCurrentPosition() > 3000) {
            exoPlayer.seekTo(0);
        } else if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem();
            play();
        } else if (repeatMode == Player.REPEAT_MODE_ALL) {
            exoPlayer.seekTo(currentPlaylist.size() - 1, 0);
            play();
        }
    }

    public void playSongAt(int index) {
        cancelCrossfade();

        if (index >= 0 && index < currentPlaylist.size()) {
            currentIndex = index;
            exoPlayer.seekTo(currentIndex, 0);
            play();
        }
    }

    public void seekTo(long position) {
        if (isCrossfading) {
            cancelCrossfade();
        }
        exoPlayer.seekTo(position);
    }

    public void toggleShuffle() {
        isShuffleEnabled = !isShuffleEnabled;
        long currentPos = exoPlayer.getCurrentPosition();

        Song currentSong = getCurrentSong();
        if (isShuffleEnabled) {
            shufflePlaylist();
        } else {
            currentPlaylist = new ArrayList<>(originalPlaylist);
            if (currentSong != null) {
                currentIndex = currentPlaylist.indexOf(currentSong);
                if (currentIndex < 0) currentIndex = 0;
            }
        }

        preparePlaylist(currentPos);
        preferenceHelper.setShuffleEnabled(isShuffleEnabled);
    }

    public void cycleRepeatMode() {
        switch (repeatMode) {
            case Player.REPEAT_MODE_OFF:
                repeatMode = Player.REPEAT_MODE_ALL;
                break;
            case Player.REPEAT_MODE_ALL:
                repeatMode = Player.REPEAT_MODE_ONE;
                break;
            case Player.REPEAT_MODE_ONE:
                repeatMode = Player.REPEAT_MODE_OFF;
                break;
        }
        exoPlayer.setRepeatMode(repeatMode);
        preferenceHelper.setRepeatMode(repeatMode);
    }

    public void setRepeatMode(int mode) {
        this.repeatMode = mode;
        exoPlayer.setRepeatMode(mode);
        preferenceHelper.setRepeatMode(mode);
    }

    private void handlePlaybackEnded() {
        if (repeatMode == Player.REPEAT_MODE_OFF) {
            if (!exoPlayer.hasNextMediaItem()) {
                stop();
            }
        }
    }

    private void startPositionUpdates() {
        if (isPositionUpdateRunning) return;

        isPositionUpdateRunning = true;
        positionUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (callback != null && exoPlayer != null && exoPlayer.isPlaying()) {
                    callback.onPositionChanged(
                            exoPlayer.getCurrentPosition(),
                            exoPlayer.getDuration()
                    );
                }
                if (isPositionUpdateRunning) {
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(positionUpdateRunnable);
    }

    private void stopPositionUpdates() {
        isPositionUpdateRunning = false;
        if (positionUpdateRunnable != null) {
            handler.removeCallbacks(positionUpdateRunnable);
        }
    }

    // ==================== GETTERS ====================

    public ExoPlayer getExoPlayer() {
        return exoPlayer;
    }

    public Song getCurrentSong() {
        if (currentIndex >= 0 && currentIndex < currentPlaylist.size()) {
            return currentPlaylist.get(currentIndex);
        }
        return null;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public List<Song> getCurrentPlaylist() {
        return currentPlaylist;
    }

    public boolean isPlaying() {
        return exoPlayer != null && exoPlayer.isPlaying();
    }

    public boolean isShuffleEnabled() {
        return isShuffleEnabled;
    }

    public int getRepeatMode() {
        return repeatMode;
    }

    public long getCurrentPosition() {
        return exoPlayer != null ? exoPlayer.getCurrentPosition() : 0;
    }

    public long getDuration() {
        return exoPlayer != null ? exoPlayer.getDuration() : 0;
    }

    public int getAudioSessionId() {
        return exoPlayer != null ? exoPlayer.getAudioSessionId() : 0;
    }

    public void addToQueue(Song song) {
        if (currentPlaylist == null) currentPlaylist = new ArrayList<>();
        if (originalPlaylist == null) originalPlaylist = new ArrayList<>();

        currentPlaylist.add(song);
        originalPlaylist.add(song);
        MediaItem mediaItem = MediaItem.fromUri(song.getPath());
        exoPlayer.addMediaItem(mediaItem);
    }

    public void addToQueueNext(Song song) {
        if (currentPlaylist == null) currentPlaylist = new ArrayList<>();
        if (originalPlaylist == null) originalPlaylist = new ArrayList<>();

        int insertIndex;
        if (currentPlaylist.isEmpty() || currentIndex < 0) {
            insertIndex = 0;
        } else {
            insertIndex = Math.min(currentIndex + 1, currentPlaylist.size());
        }

        currentPlaylist.add(insertIndex, song);
        originalPlaylist.add(insertIndex, song);
        MediaItem mediaItem = MediaItem.fromUri(song.getPath());
        exoPlayer.addMediaItem(insertIndex, mediaItem);
    }

    public void removeFromQueue(int index) {
        if (index >= 0 && index < currentPlaylist.size()) {
            currentPlaylist.remove(index);
            originalPlaylist.remove(index);
            exoPlayer.removeMediaItem(index);

            if (index < currentIndex) {
                currentIndex--;
            }
        }
    }

    public void release() {
        cancelCrossfade();
        saveCurrentState();
        stopPositionUpdates();
        stopCrossfadeMonitoring();

        if (exoPlayer != null) {
            exoPlayer.release();
        }
        executorService.shutdown();
    }

    public void moveQueueItem(int fromPosition, int toPosition) {
        if (fromPosition < 0 || fromPosition >= currentPlaylist.size() ||
                toPosition < 0 || toPosition >= currentPlaylist.size()) {
            return;
        }

        Song movedSong = currentPlaylist.remove(fromPosition);
        currentPlaylist.add(toPosition, movedSong);

        if (!isShuffleEnabled && fromPosition < originalPlaylist.size() && toPosition < originalPlaylist.size()) {
            Song originalSong = originalPlaylist.remove(fromPosition);
            originalPlaylist.add(toPosition, originalSong);
        }

        if (currentIndex == fromPosition) {
            currentIndex = toPosition;
        } else if (fromPosition < currentIndex && toPosition >= currentIndex) {
            currentIndex--;
        } else if (fromPosition > currentIndex && toPosition <= currentIndex) {
            currentIndex++;
        }

        exoPlayer.moveMediaItem(fromPosition, toPosition);
        saveCurrentState();
    }

    public void setPlaybackSpeed(float speed) {
        this.currentPlaybackSpeed = speed;
        if (exoPlayer != null) {
            exoPlayer.setPlaybackSpeed(speed);
        }
        preferenceHelper.setPlaybackSpeed(speed);
    }

    public float getPlaybackSpeed() {
        return currentPlaybackSpeed;
    }
}